package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;

public interface MethodReader extends Closeable {
    String HISTORY = "history";

    MethodReaderInterceptor methodReaderInterceptor();

    boolean readOne();

    default boolean quickReadOne() {
        return readOne();
    }

    /**
     * Call close on the input when closed
     */
    MethodReader closeIn(boolean closeIn);
}
