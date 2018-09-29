package net.openhft.chronicle.bytes;

/**
 * Created by Jerry Shea on 23/04/18.
 */
public interface RingBufferReader extends RingBufferReaderStats {

    boolean isEmpty();

    boolean isClosed();

    long startRead(Bytes b);

    void endRead(long next);

    BytesStore byteStore();
}
