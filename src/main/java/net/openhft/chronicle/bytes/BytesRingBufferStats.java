package net.openhft.chronicle.bytes;

/**
 * @author Rob Austin.
 */
public interface BytesRingBufferStats {
    /**
     * each time the ring is read, this logs the number of bytes in the write buffer, calling this
     * method resets these statistics,
     *
     * @return Long.MAX_VALUE if no read calls were made since the last time this method was called.
     */
    long minNumberOfWriteBytesRemaining();

    /**
     * @return the total capacity in bytes
     */
    long capacity();

    long getAndClearReadCount();

    long getAndClearWriteCount();

    long maxCopyTimeNs();
}
