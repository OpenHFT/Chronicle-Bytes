package net.openhft.chronicle.bytes;

public interface RingBufferReaderStats {

    long getAndClearReadCount();

    long getAndClearMissedReadCount();

    long behind();
}
