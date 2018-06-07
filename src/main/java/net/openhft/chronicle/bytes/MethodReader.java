package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;

public interface MethodReader extends Closeable {
    String HISTORY = "history";

    MethodReaderInterceptor methodReaderInterceptor();

    /**
     * Moves the queue to read a message if there is one, but is more expensive
     *
     * @return true if there was a message, false if no more messages.
     */
    boolean readOne();

    /**
     * Does a quick read which is simpler but might not read the next message. readOne() has to be called periodically.
     *
     * @return true if there was a message, false if there is probably not a message.
     */
    default boolean lazyReadOne() {
        return readOne();
    }

    /**
     * Call close on the input when closed
     */
    MethodReader closeIn(boolean closeIn);
}
