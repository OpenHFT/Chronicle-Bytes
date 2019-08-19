package net.openhft.chronicle.bytes;

import java.io.Closeable;

public interface RingBufferReader extends RingBufferReaderStats, Closeable {
    long UNKNOWN_INDEX = -1;

    boolean isEmpty();

    boolean isClosed();

    /**
     * Close the reader. After being closed, the reader will not block writers
     */
    @Override
    void close();

    /**
     * the readPosition and readLimit will be adjusted so that the client can read the data
     *
     * @param bytes who's byteStore must be the ring buffer,
     * @return nextReadPosition which should be passed to {@link RingBufferReader#afterRead(long)}
     */
    @SuppressWarnings("rawtypes")
    long beforeRead(Bytes bytes);

    void afterRead(long next);

    void afterRead(long next, long payloadStart, long underlyingIndex);

    long underlyingIndex();

    /**
     * Convenience method calls both {@link #beforeRead(Bytes)} and {@link #afterRead(long)}
     * @param bytes
     * @return whether read succeeded
     */
    @SuppressWarnings("rawtypes")
    boolean read(BytesOut bytes);

    /**
     * @return the byteStore which backs the ring buffer
     */
    @SuppressWarnings("rawtypes")
    BytesStore byteStore();

    /**
     * Take reader to just past the end of the RB
     */
    void toEnd();
}
