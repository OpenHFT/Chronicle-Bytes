package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

public interface MultiReaderBytesRingBuffer extends BytesRingBuffer {

    @NotNull
    default RingBufferReader createReader() {
        return createReader(0);
    }

    /**
     * Create a reader
     * @param id of reader as each reader has separate read position etc.
     * @return reader
     */
    @NotNull
    RingBufferReader createReader(int id);
}
