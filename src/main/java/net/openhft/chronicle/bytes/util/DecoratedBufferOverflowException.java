/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import java.nio.BufferOverflowException;

/**
 * {@link BufferOverflowException} with a descriptive message because the
 * JDK exception omits context, so callers can see capacity/limit/position.
 */
public final class DecoratedBufferOverflowException extends BufferOverflowException {
    private static final long serialVersionUID = 0L;

    /**
     * Custom detail message because {@link BufferOverflowException} omits context.
     */
    private final String message;

    /**
     * Constructs with a detail message describing the overflow context.
     *
     * @param message the detail message
     */
    public DecoratedBufferOverflowException(final String message) {
        this.message = message;
    }

    /**
     * Constructs with a detail message and cause describing the overflow.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public DecoratedBufferOverflowException(final String message, final Throwable cause) {
        this.message = message;
        initCause(cause);
    }

    /**
     * Returns the detail message with capacity/limit/position context.
     *
     * @return the detail message
     */
    @Override
    public String getMessage() {
        return message;
    }
}
