package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.core.time.TimeProvider;

import java.io.IOException;

/**
 * Timestamps are unique across threads/processes on a single machine.
 */
public enum MappedUniqueMicroTimeProvider implements TimeProvider {
    INSTANCE;

    private static final int LAST_TIME = 128;

    private final MappedFile file;
    private final Bytes bytes;
    private TimeProvider provider = SystemTimeProvider.INSTANCE;

    MappedUniqueMicroTimeProvider() {
        try {
            file = MappedFile.mappedFile(OS.TMP + "/.time-stamp.dat", OS.pageSize(), 0);
            bytes = file.acquireBytesForWrite(0);
            bytes.append8bit("&TSF\nTime stamp file uses for sharing a unique id\n");
        } catch (IOException ioe) {
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
    public long currentTimeMicros() {
        long time = provider.currentTimeMicros();
        while (true) {
            long time0 = bytes.readVolatileLong(LAST_TIME);
            if (time0 >= time)
                time = time0 + 1;
            if (bytes.compareAndSwapLong(LAST_TIME, time0, time))
                return time;
        }
    }
}
