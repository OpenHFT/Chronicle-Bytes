package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Jerry Shea on 23/04/18.
 */
public interface RingBufferReader extends RingBufferReaderStats {

    boolean read(@NotNull final BytesOut using);

    boolean readDataDocument(@NotNull final BytesOut using);

    boolean isEmpty();

    boolean isClosed();
}
