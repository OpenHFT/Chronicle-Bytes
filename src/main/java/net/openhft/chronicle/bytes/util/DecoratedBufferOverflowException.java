package net.openhft.chronicle.bytes.util;

import java.nio.BufferOverflowException;

public final class DecoratedBufferOverflowException extends BufferOverflowException {
    private final String message;

    public DecoratedBufferOverflowException(final String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
