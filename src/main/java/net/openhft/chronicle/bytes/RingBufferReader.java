package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Jerry Shea on 23/04/18.
 */
public interface RingBufferReader {

    boolean read(@NotNull final BytesOut using);
}
