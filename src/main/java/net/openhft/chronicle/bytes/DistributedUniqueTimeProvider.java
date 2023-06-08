/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.core.time.TimeProvider;
import net.openhft.chronicle.core.values.LongArrayValues;

import java.io.File;

/**
 * Timestamps are unique across systems using a predefined hostId
 */
public class DistributedUniqueTimeProvider extends SimpleCloseable implements TimeProvider {

    static final int HOST_IDS = 100;
    private static final int LAST_TIME = 128;
    private static final int DEDUPLICATOR = 192;
    private static final int NANOS_PER_MICRO = 1000;


    private final Bytes<?> bytes;
    private final MappedFile file;
    private final BinaryLongArrayReference values;
    private final VanillaDistributedUniqueTimeDeduplicator deduplicator;
    private TimeProvider provider = SystemTimeProvider.INSTANCE;
    private int hostId;

    private DistributedUniqueTimeProvider(@NonNegative int hostId, boolean unmonitor) {
        hostId(hostId);
        try {
            file = MappedFile.ofSingle(new File(BytesUtil.TIME_STAMP_PATH), OS.pageSize(), false);
            if (unmonitor) IOTools.unmonitor(file);
            bytes = file.acquireBytesForWrite(this, 0);
            if (unmonitor) IOTools.unmonitor(bytes);
            bytes.append8bit("&TSF\nTime stamp file used for sharing a unique id\n");
            values = new BinaryLongArrayReference(HOST_IDS);
            if (unmonitor) IOTools.unmonitor(values);
            values.bytesStore(bytes, DEDUPLICATOR, HOST_IDS * 8L + 16L);
            deduplicator = new VanillaDistributedUniqueTimeDeduplicator(values);

        } catch (Exception ioe) {
            throw new IORuntimeException(ioe);
        }
    }

    public static DistributedUniqueTimeProvider instance() {
        return DistributedUniqueTimeProviderHolder.INSTANCE;
    }

    public static DistributedUniqueTimeProvider forHostId(@NonNegative int hostId) {
        return new DistributedUniqueTimeProvider(hostId, false);
    }

    /**
     * Extract the timestamp in nanoseconds from the timestampWithHostId
     *
     * @param timestampWithHostId to extract from
     * @return the timestamp
     */
    public static long timestampFor(long timestampWithHostId) {
        return timestampWithHostId - timestampWithHostId % HOST_IDS;
    }

    /**
     * Extract the hostId from the timestampWithHostId
     *
     * @param timestampWithHostId to extract from
     * @return the hostId
     */
    public static long hostIdFor(long timestampWithHostId) {
        return timestampWithHostId % HOST_IDS;
    }

    @Override
    protected void performClose() {
        super.performClose();
        Closeable.closeQuietly(values);
        bytes.release(this);
        file.releaseLast();
    }
    /**
     * Sets the hostId of the DistributedUniqueTimeProvider. The hostId is used to
     * create unique timestamps across different hosts.
     *
     * @param hostId The host identifier, must be non-negative.
     * @return A reference to the current DistributedUniqueTimeProvider instance with the updated hostId.
     * @throws IllegalArgumentException if the provided hostId is negative.
     */
    public DistributedUniqueTimeProvider hostId(@NonNegative int hostId) {
        // Check if the provided hostId is negative and throw an exception if it is
        if (hostId < 0)
            throw new IllegalArgumentException("Invalid hostId: " + hostId);

        // Assign the provided hostId modulo the maximum number of host IDs
        // to ensure it's within the valid range
        this.hostId = hostId % HOST_IDS;

        // Return the current instance with updated hostId
        return this;
    }

    /**
     * Sets the TimeProvider of the DistributedUniqueTimeProvider. The TimeProvider
     * is responsible for providing the current time in nanoseconds.
     *
     * @param provider The TimeProvider instance to be used for fetching the current time.
     * @return A reference to the current DistributedUniqueTimeProvider instance with the updated TimeProvider.
     */
    public DistributedUniqueTimeProvider provider(TimeProvider provider) {
        // Assign the provided TimeProvider to the instance variable
        this.provider = provider;

        // Return the current instance with the updated TimeProvider
        return this;
    }

    /**
     * @return Ordinary millisecond timestamp
     */
    @Override
    public long currentTimeMillis() {
        return provider.currentTimeMillis();
    }

    /**
     * This method returns a unique, monotonically increasing microsecond timestamp where
     * the lowest two digits of the microseconds is the hostId, ensuring uniqueness across different hosts.
     *
     * @return the timestamps with hostId as a long
     * @throws IllegalStateException if the underlying resources have been released/closed
     */
    @Override
    public long currentTimeMicros() throws IllegalStateException {
        // Retrieve the current time in microseconds from the provider and
        // normalize it by dividing by the total number of host IDs
        long timeus = provider.currentTimeMicros() / HOST_IDS;

        // Loop indefinitely until a unique timestamp is generated
        while (true) {
            // Read the volatile long value stored in the 'LAST_TIME' field
            long time0 = bytes.readVolatileLong(LAST_TIME);

            // Convert 'time0' from nanoseconds to microseconds, and normalize it
            long time0us = time0 / (HOST_IDS * NANOS_PER_MICRO);

            // The next timestamp to generate
            long time;

            // If 'time0us' is greater or equal to 'timeus', set 'time' to ('time0us' + 1) microseconds
            // Otherwise, set 'time' to 'timeus' microseconds
            // This guarantees that 'time' is a unique and monotonically increasing timestamp
            if (time0us >= timeus)
                time = (time0us + 1) * (HOST_IDS * NANOS_PER_MICRO);
            else
                time = timeus * (HOST_IDS * NANOS_PER_MICRO);

            // Attempt a compare-and-swap operation to update the 'LAST_TIME' field with 'time'
            // If the operation is successful, return the timestamp in microseconds
            // with the hostId appended as the lowest two digits
            if (bytes.compareAndSwapLong(LAST_TIME, time0, time))
                return time / NANOS_PER_MICRO + hostId;

            // If the compare-and-swap operation failed (indicating that another thread has
            // modified the 'LAST_TIME' field), pause for a short period before retrying
            Jvm.nanoPause();
        }
    }


    /**
     * This method returns a unique, monotonically increasing nanosecond timestamp.
     * The lowest two digits of the nanoseconds is the hostId, providing uniqueness across different hosts.
     *
     * @return the timestamps with hostId as a long
     * @throws IllegalStateException if the underlying resources have been released/closed
     */
    @Override
    public long currentTimeNanos() throws IllegalStateException {

        // Retrieve the current time in nanoseconds from the provider
        long time = provider.currentTimeNanos();

        // Read the volatile long value stored in the 'LAST_TIME' field
        long time0 = bytes.readVolatileLong(LAST_TIME);

        // Calculate the timestamp for the current time and append the hostId to it,
        // This ensures uniqueness of the timestamp across different hosts
        long timeN = timestampFor(time) + hostId;

        // If 'timeN' is greater than 'time0' and if the compare-and-swap operation
        // is successful in updating the 'LAST_TIME' field with 'timeN', then return 'timeN'
        if (timeN > time0 && bytes.compareAndSwapLong(LAST_TIME, time0, timeN))
            return timeN;

        // If the compare-and-swap operation fails (indicating that another thread
        // has modified the 'LAST_TIME' field), call 'currentTimeNanosLoop' method to retry the operation
        return currentTimeNanosLoop();
    }

    /**
     * This is a helper method designed to generate the next timestamp
     * in a thread-safe manner when the compare-and-swap operation in the 'currentTimeNanos' method fails.
     * <p>
     * This method runs in a loop until the compare-and-swap operation
     * successfully updates the 'LAST_TIME' field with the next timestamp.
     *
     * @return the next timestamp as a long
     */
    private long currentTimeNanosLoop() {
        // Loop indefinitely until the compare-and-swap operation is successful
        while (true) {
            // Read the volatile long value stored in the 'LAST_TIME' field
            long time0 = bytes.readVolatileLong(LAST_TIME);

            // Calculate the timestamp for 'time0' and append the hostId to it
            long next = timestampFor(time0) + hostId;

            // If 'next' is less than or equal to 'time0', increment 'next' by the total number of HOST_IDS
            // to ensure that the timestamp is monotonically increasing
            if (next <= time0)
                next += HOST_IDS;

            // Attempt a compare-and-swap operation to update the 'LAST_TIME' field with 'next'
            // If the operation is successful, return 'next'
            if (bytes.compareAndSwapLong(LAST_TIME, time0, next))
                return next;

            // If the compare-and-swap operation failed (indicating that another thread has
            // modified the 'LAST_TIME' field), pause for a short period before retrying
            Jvm.nanoPause();
        }
    }

    /**
     * A deduplicator to help recognise duplicate timestamps for a hostId
     */
    @Deprecated(/* to be removed in x.26 */)
    public DistributedUniqueTimeDeduplicator deduplicator() {
        return deduplicator;
    }

    /**
     * This is a static inner class responsible for holding the singleton instance
     * of the DistributedUniqueTimeProvider with the default hostId.
     * <p>
     * This follows the Initialization-on-demand holder idiom for lazy initialization
     * of the singleton instance.
     */
    static final class DistributedUniqueTimeProviderHolder {

        // Retrieves the hostId value from the system properties, defaulting to 0 if not set
        private static final Integer DEFAULT_HOST_ID = Jvm.getInteger("hostId", 0);
        /*
         * The singleton instance of the DistributedUniqueTimeProvider,
         * initialized with the default hostId. This instance can be used to generate
         * timestamps with the default hostId embedded in them.
         *
         * The true argument in the DistributedUniqueTimeProvider constructor
         * signifies that this is the default instance.
         */
        public static final DistributedUniqueTimeProvider INSTANCE = new DistributedUniqueTimeProvider(DEFAULT_HOST_ID, true);

        // Private constructor to prevent instantiation of this holder class
        private DistributedUniqueTimeProviderHolder() {
        }
    }

    static class VanillaDistributedUniqueTimeDeduplicator implements ReferenceOwner, DistributedUniqueTimeDeduplicator {
        private final LongArrayValues values;

        private VanillaDistributedUniqueTimeDeduplicator(LongArrayValues values) {
            this.values = values;
        }

        @Override
        public int compareByHostId(long timestampHostId) {
            int hostId = (int) DistributedUniqueTimeProvider.hostIdFor(timestampHostId);
            long prev = values.getValueAt(hostId);
            return Long.compare(timestampHostId, prev);
        }

        @Override
        public int compareAndRetainNewer(long timestampHostId) {
            int hostId = (int) DistributedUniqueTimeProvider.hostIdFor(timestampHostId);
            for (; ; ) {
                long prev = values.getValueAt(hostId);
                int ret = Long.compare(timestampHostId, prev);
                if (ret <= 0 || values.compareAndSet(hostId, prev, timestampHostId))
                    return ret;
            }
        }
    }

}
