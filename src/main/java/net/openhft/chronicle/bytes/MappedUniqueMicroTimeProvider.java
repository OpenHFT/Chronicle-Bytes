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

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Timestamps are unique across threads/processes on a single machine.
 *
 * @deprecated Use {@link MappedUniqueTimeProvider} instead.
 */
@Deprecated(/* to be removed in x.23 */)
public enum MappedUniqueMicroTimeProvider implements TimeProvider {
    INSTANCE;

    private static final int LAST_TIME = 128;

    private final MappedFile file;
    @SuppressWarnings("rawtypes")
    private final Bytes bytes;
    private TimeProvider provider = SystemTimeProvider.INSTANCE;

    MappedUniqueMicroTimeProvider() {
        try {
            String user = System.getProperty("user.name", "unknown");
            file = MappedFile.mappedFile(OS.TMP + "/.time-stamp." + user + ".dat", OS.pageSize(), 0);
            IOTools.unmonitor(file);
            ReferenceOwner mumtp = ReferenceOwner.temporary("mumtp");
            bytes = file.acquireBytesForWrite(mumtp, 0);
            bytes.append8bit("&TSF\nTime stamp file uses for sharing a unique id\n");
            IOTools.unmonitor(bytes);
        } catch (IOException | IllegalStateException | IllegalArgumentException | BufferOverflowException ioe) {
            throw new IORuntimeException(ioe);
        }
    }

    public MappedUniqueMicroTimeProvider provider(TimeProvider provider) {
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
        try {
            long timeus = provider.currentTimeMicros();
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
            while (true) {
                long time0 = bytes.readVolatileLong(LAST_TIME);
                if (time0 >= time)
                    time = time0 + 1;
                if (bytes.compareAndSwapLong(LAST_TIME, time0, time))
                    return time;
                Jvm.nanoPause();
            }
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }
}
