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

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Timestamps are unique across threads/processes on a single machine.
 */
public enum MappedUniqueTimeProvider implements TimeProvider {
    INSTANCE;

    private static final int LAST_TIME = 128;

    @SuppressWarnings("rawtypes")
    private final Bytes bytes;
    private TimeProvider provider = SystemTimeProvider.INSTANCE;

    MappedUniqueTimeProvider() {
        try {
            String user = System.getProperty("user.name", "unknown");
            MappedFile file = MappedFile.mappedFile(OS.TMP + "/.time-stamp." + user + ".dat", OS.pageSize(), 0);
            IOTools.unmonitor(file);
            ReferenceOwner mumtp = ReferenceOwner.temporary("mumtp");
            bytes = file.acquireBytesForWrite(mumtp, 0);
            bytes.append8bit("&TSF\nTime stamp file uses for sharing a unique id\n");
            IOTools.unmonitor(bytes);
        } catch (Exception ioe) {
            throw new IORuntimeException(ioe);
        }
    }

    public MappedUniqueTimeProvider provider(TimeProvider provider) {
        this.provider = provider;
        return this;
    }

    @Override
    public long currentTimeMillis() {
        return provider.currentTimeMillis();
    }

    @Override
    public long currentTimeMicros()
            throws IllegalStateException {
        long timeus = provider.currentTimeMicros();
        try {
            while (true) {
                long time0 = bytes.readVolatileLong(LAST_TIME);
                long time0us = time0 / 1000;
                long time;
                if (time0us >= timeus)
                    time = (time0us + 1) * 1000;
                else
                    time = timeus * 1000;
                if (bytes.compareAndSwapLong(LAST_TIME, time0, time))
                    return time / 1_000;
                Jvm.nanoPause();
            }
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long currentTimeNanos()
            throws IllegalStateException {
        try {
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
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }
}
