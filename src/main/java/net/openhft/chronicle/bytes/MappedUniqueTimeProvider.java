/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.Monitorable;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.core.time.TimeProvider;

import java.io.File;

/**
 * {@link TimeProvider} that yields monotonically increasing timestamps shared
 * between JVM processes by using a memory-mapped file.
 */
public enum MappedUniqueTimeProvider implements TimeProvider, ReferenceOwner {
    INSTANCE;

    /** offset within the mapped file where the last timestamp is stored */
    private static final int LAST_TIME = 128;
    /** conversion constant */
    private static final int NANOS_PER_MICRO = 1000;

    private final BytesStore<?, ?> bytesStore;
    private TimeProvider provider = SystemTimeProvider.INSTANCE;

    MappedUniqueTimeProvider() {
        try {
            String user = Jvm.getProperty("user.name", "unknown");
            String timeStampDir = Jvm.getProperty("timestamp.dir", OS.TMP);
            final File timeStampPath = new File(timeStampDir, ".time-stamp." + user + ".dat");
            MappedFile file = MappedFile.ofSingle(timeStampPath, PageUtil.getPageSize(timeStampPath.getAbsolutePath()), false);
            final Bytes<?> bytes = file.acquireBytesForWrite(this, 0);
            bytes.append8bit("&TSF\nTime stamp file used for sharing a unique id\n");
            this.bytesStore = bytes.bytesStore();
            Monitorable.unmonitor(file);
            Monitorable.unmonitor(bytes);
        } catch (Exception ioe) {
            throw new IORuntimeException(ioe);
        }
    }

    // Todo: Handle thread safety
    /**
     * Sets the underlying time source.
     */
    public MappedUniqueTimeProvider provider(TimeProvider provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Returns the wall clock time in milliseconds.
     * This value is not guaranteed to be unique.
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
            final long time0 = lastTimeStored();
            long time0us = time0 / NANOS_PER_MICRO;
            long time;
            if (time0us >= timeus)
                time = (time0us + 1) * NANOS_PER_MICRO;
            else
                time = timeus * NANOS_PER_MICRO;
            if (casLastTimeStored(time0, time))
                return time / NANOS_PER_MICRO;
            Jvm.nanoPause();
        }
    }

    @Override
    public long currentTimeNanos()
            throws IllegalStateException {
        long time = provider.currentTimeNanos();
        long time5 = time >>> 5;

        long time0 = lastTimeStored();
        long timeNanos5 = time0 >>> 5;

        if (time5 > timeNanos5 && casLastTimeStored(time0, time))
            return time;

        while (true) {
            time0 = lastTimeStored();
            long next = (time0 + 0x20) & ~0x1f;
            if (casLastTimeStored(time0, next))
                return next;
            Jvm.nanoPause();
        }
    }

    private long lastTimeStored() {
        return bytesStore.readVolatileLong(LAST_TIME);
    }

    private boolean casLastTimeStored(final long expected, final long value) {
        return ((RandomDataOutput<?>) bytesStore).compareAndSwapLong(LAST_TIME, expected, value);
    }
}
