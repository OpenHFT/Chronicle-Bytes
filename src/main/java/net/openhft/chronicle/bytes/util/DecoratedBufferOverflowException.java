/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import java.nio.BufferOverflowException;

/**
 * Customized {@link BufferOverflowException} with a descriptive message.
 * This exception is thrown to indicate that there is an attempt to write
 * data into a buffer beyond its capacity.
 */
public final class DecoratedBufferOverflowException extends BufferOverflowException {
    private static final long serialVersionUID = 0L;

    /**
     * The custom message describing this exception.
     */
    private final String message;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public DecoratedBufferOverflowException(final String message) {
        this.message = message;
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public DecoratedBufferOverflowException(final String message, final Throwable cause) {
        this.message = message;
        initCause(cause);
    }

    /**
     * Returns the detail message of this exception.
     *
     * @return the detail message string of this exception
     */
    @Override
    public String getMessage() {
        return message;
    }
}
