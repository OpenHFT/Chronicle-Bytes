package net.openhft.chronicle.bytes;

/**
 * Created by Jerry Shea on 23/04/18.
 */
public interface RingBufferReader extends RingBufferReaderStats {

    boolean isEmpty();

    boolean isClosed();

    /**
     * the readPosition and readLimit will be adjusted so that the client can read the data
     *
     * @param bytes who's byteStore must be the ring buffer,
     * @return nextReadPosition which should be passed to {@link RingBufferReader#afterRead(long)}
     */
    @SuppressWarnings("rawtypes")
    long beforeRead(Bytes bytes);

    void afterRead(long next);

    /**
     * @return the byteStore which backs the ring buffer
     */
    @SuppressWarnings("rawtypes")
    BytesStore byteStore();
}
