package net.openhft.chronicle.bytes.util;

import java.nio.BufferUnderflowException;

public final class DecoratedBufferUnderflowException extends BufferUnderflowException {
    private final String message;

    public DecoratedBufferUnderflowException(final String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
