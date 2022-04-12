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
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.core.time.TimeProvider;

import java.io.File;

/**
 * Timestamps are unique across threads/processes on a single machine.
 */
public enum MappedUniqueTimeProvider implements TimeProvider, ReferenceOwner {
    INSTANCE;

    private static final int LAST_TIME = 128;
    private static final int NANOS_PER_MICRO = 1000;

    @SuppressWarnings("rawtypes")
    private final Bytes<?> bytes;
    private TimeProvider provider = SystemTimeProvider.INSTANCE;

    MappedUniqueTimeProvider() {
        try {
            String user = Jvm.getProperty("user.name", "unknown");
            String timeStampDir = Jvm.getProperty("timestamp.dir", OS.TMP);
            final File timeStampPath = new File(timeStampDir, ".time-stamp." + user + ".dat");
            MappedFile file = MappedFile.ofSingle(timeStampPath, OS.pageSize(), false);
            bytes = file.acquireBytesForWrite(this, 0);
            bytes.append8bit("&TSF\nTime stamp file used for sharing a unique id\n");
            IOTools.unmonitor(file);
            IOTools.unmonitor(bytes);
        } catch (Exception ioe) {
            throw new IORuntimeException(ioe);
        }
    }

    public MappedUniqueTimeProvider provider(TimeProvider provider) {
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

    @Override
    public long currentTimeMicros()
            throws IllegalStateException {
        long timeus = provider.currentTimeMicros();
        while (true) {
            long time0 = bytes.readVolatileLong(LAST_TIME);
            long time0us = time0 / NANOS_PER_MICRO;
            long time;
            if (time0us >= timeus)
                time = (time0us + 1) * NANOS_PER_MICRO;
            else
                time = timeus * NANOS_PER_MICRO;
            if (bytes.compareAndSwapLong(LAST_TIME, time0, time))
                return time / NANOS_PER_MICRO;
            Jvm.nanoPause();
        }
    }

    @Override
    public long currentTimeNanos()
            throws IllegalStateException {
        long time = provider.currentTimeNanos();
        long time5 = time >>> 5;

        long time0 = bytes.readVolatileLong(LAST_TIME);
        long timeNanos5 = time0 >>> 5;

        if (time5 > timeNanos5 && bytes.compareAndSwapLong(LAST_TIME, time0, time))
            return time;

        while (true) {
            time0 = bytes.readVolatileLong(LAST_TIME);
            long next = (time0 + 0x20) & ~0x1f;
            if (bytes.compareAndSwapLong(LAST_TIME, time0, next))
                return next;
            Jvm.nanoPause();
        }
    }
}
