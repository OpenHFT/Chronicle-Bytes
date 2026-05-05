/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import java.nio.BufferUnderflowException;

/**
 * {@link BufferUnderflowException} with a descriptive message because the
 * JDK exception omits context, so callers can see limit/position details.
 */
public final class DecoratedBufferUnderflowException extends BufferUnderflowException {

    private static final long serialVersionUID = 0L;
    /**
     * Custom detail message because {@link BufferUnderflowException} omits context.
     */
    private final String message;

    /**
     * Constructs with a detail message describing the underflow context.
     *
     * @param message the detail message
     */
    public DecoratedBufferUnderflowException(final String message) {
        this.message = message;
    }

    /**
     * Constructs with a detail message and cause describing the underflow.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public DecoratedBufferUnderflowException(final String message, final Throwable cause) {
        this.message = message;
        initCause(cause);
    }

    /**
     * Returns the detail message with limit/position context.
     *
     * @return the detail message
     */
    @Override
    public String getMessage() {
        return message;
    }
}
