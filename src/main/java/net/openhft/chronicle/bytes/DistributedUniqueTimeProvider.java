/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.SimpleCloseable;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.core.time.TimeProvider;

import java.io.File;

/**
 * Timestamps are unique across systems using a predefined hostId
 */
public class DistributedUniqueTimeProvider extends SimpleCloseable implements TimeProvider {
    private static final String USE_NAME = System.getProperty("user.name", "unknown");
    private static final String TIME_STAMP_DIR = System.getProperty("timestamp.dir", OS.TMP);
    private static final String TIME_STAMP_PATH = System.getProperty("timestamp.path", TIME_STAMP_DIR + File.pathSeparator + ".time-stamp." + USE_NAME + ".dat");
    private static final Integer DEFAULT_HOST_ID = Integer.getInteger("hostId", 0);
    public static final DistributedUniqueTimeProvider INSTANCE = new DistributedUniqueTimeProvider(DEFAULT_HOST_ID, true);
    private static final int LAST_TIME = 128;
    // package local for use in tests
    static final int HOST_IDS = 100;
    private static final int NANOS_PER_MICRO = 1000;

    @SuppressWarnings("rawtypes")
    private final Bytes bytes;
    private final MappedFile file;
    private TimeProvider provider = SystemTimeProvider.INSTANCE;
    private int hostId;

    private DistributedUniqueTimeProvider(int hostId, boolean unmonitor) {
        hostId(hostId);
        try {
            file = MappedFile.ofSingle(new File(TIME_STAMP_PATH), OS.pageSize(), false);
            bytes = file.acquireBytesForWrite(this, 0);
            bytes.append8bit("&TSF\nTime stamp file used for sharing a unique id\n");
            if (unmonitor) {
                IOTools.unmonitor(file);
                IOTools.unmonitor(bytes);
            }

        } catch (Exception ioe) {
            throw new IORuntimeException(ioe);
        }
    }

    public static DistributedUniqueTimeProvider forHostId(int hostId) {
        return new DistributedUniqueTimeProvider(hostId, false);
    }

    public DistributedUniqueTimeProvider hostId(int hostId) {
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
     * Return a unique, monotonically increasing nanosecond timestamp where the lowest two digits of the nanoseconds is the hostId.
     *
     * @return the timestamps with hostId as a long
     * @throws IllegalStateException if the underlying resources have been released/closed
     */
    @Override
    public long currentTimeNanos() throws IllegalStateException {
        long time = provider.currentTimeNanos();
        long time0 = bytes.readVolatileLong(LAST_TIME);
        long timeN = time - time % HOST_IDS + hostId;

        if (timeN > time0 && bytes.compareAndSwapLong(LAST_TIME, time0, timeN))
            return timeN;

        return currentTimeNanosLoop();
    }

    private long currentTimeNanosLoop() {
        while (true) {
            long time0 = bytes.readVolatileLong(LAST_TIME);
            long next = time0 - time0 % HOST_IDS + hostId;
            if (next <= time0)
                next += HOST_IDS;
            if (bytes.compareAndSwapLong(LAST_TIME, time0, next))
                return next;
            Jvm.nanoPause();
        }
    }

    @Override
    protected void performClose() {
        super.performClose();
        bytes.release(this);
        file.releaseLast();
    }
}
