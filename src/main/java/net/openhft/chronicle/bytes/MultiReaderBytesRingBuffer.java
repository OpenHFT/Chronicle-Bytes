package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NotNull;

/**
 * Created by Jerry Shea on 23/04/18.
 */
public interface MultiReaderBytesRingBuffer extends BytesRingBuffer {

    @NotNull
    RingBufferReader createReader();

}
