package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Jerry Shea on 23/04/18.
 */
public interface MultiReaderBytesRingBuffer extends BytesRingBuffer {

    @NotNull
    RingBufferReader createReader();

}
