package net.openhft.chronicle.bytes;

/**
 * Created by Jerry Shea on 18/06/18.
 */
public interface RingBufferReaderStats {

    long getAndClearReadCount();

    long getAndClearMissedReadCount();
}
