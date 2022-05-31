/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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

    private static final int LAST_TIME = 128;
    private static final int DEDUPLICATOR = 192;
    static final int HOST_IDS = 100;
    private static final int NANOS_PER_MICRO = 1000;

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

    private final Bytes<?> bytes;
    private final MappedFile file;
    private final BinaryLongArrayReference values;
    private final VanillaDistributedUniqueTimeDeduplicator deduplicator;
    private TimeProvider provider = SystemTimeProvider.INSTANCE;
    private int hostId;

    @Override
    protected void performClose() {
        super.performClose();
        Closeable.closeQuietly(values);
        bytes.release(this);
        file.releaseLast();
    }

    public static DistributedUniqueTimeProvider forHostId(@NonNegative int hostId) {
        return new DistributedUniqueTimeProvider(hostId, false);
    }

    public DistributedUniqueTimeProvider hostId(@NonNegative int hostId) {
        if (hostId < 0)
            throw new IllegalArgumentException("Invalid hostId: " + hostId);
        this.hostId = hostId % HOST_IDS;
        return this;
    }

    public DistributedUniqueTimeProvider provider(TimeProvider provider) {
        this.provider = provider;
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
     * Return a unique, monotonically increasing microsecond timestamp where the lowest two digits of the microseconds is the hostId.
     *
     * @return the timestamps with hostId as a long
     * @throws IllegalStateException if the underlying resources have been released/closed
     */
    @Override
    public long currentTimeMicros() throws IllegalStateException {
        long timeus = provider.currentTimeMicros() / HOST_IDS;
        while (true) {
            long time0 = bytes.readVolatileLong(LAST_TIME);
            long time0us = time0 / (HOST_IDS * NANOS_PER_MICRO);
            long time;
            if (time0us >= timeus)
                time = (time0us + 1) * (HOST_IDS * NANOS_PER_MICRO);
            else
                time = timeus * (HOST_IDS * NANOS_PER_MICRO);
            if (bytes.compareAndSwapLong(LAST_TIME, time0, time))
                return time / NANOS_PER_MICRO + hostId;
            Jvm.nanoPause();
        }
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

    static final class DistributedUniqueTimeProviderHolder {

        private DistributedUniqueTimeProviderHolder() {
        }

        private static final Integer DEFAULT_HOST_ID = Jvm.getInteger("hostId", 0);
        /*
         * Instance you can use for generating timestamps with the default hostId embedded
         */
        public static final DistributedUniqueTimeProvider INSTANCE = new DistributedUniqueTimeProvider(DEFAULT_HOST_ID, true);
    }

    /**
     * Return a unique, monotonically increasing nanosecond timestamp where the lowest two digits of the nanoseconds is the hostId.
     *
     * @return the timestamps with hostId as a long
     * @throws IllegalStateException if the underlying resources have been released/closed
     */
    @Override
    public long currentTimeNanos() throws IllegalStateException {
        long time = provider.currentTimeNanos();
        long time0 = bytes.readVolatileLong(LAST_TIME);
        long timeN = timestampFor(time) + hostId;

        if (timeN > time0 && bytes.compareAndSwapLong(LAST_TIME, time0, timeN))
            return timeN;

        return currentTimeNanosLoop();
    }

    private long currentTimeNanosLoop() {
        while (true) {
            long time0 = bytes.readVolatileLong(LAST_TIME);
            long next = timestampFor(time0) + hostId;
            if (next <= time0)
                next += HOST_IDS;
            if (bytes.compareAndSwapLong(LAST_TIME, time0, next))
                return next;
            Jvm.nanoPause();
        }
    }

    /**
     * A deduplicator to help recognise duplicate timestamps for a hostId
     */
    public DistributedUniqueTimeDeduplicator deduplicator() {
        return deduplicator;
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
