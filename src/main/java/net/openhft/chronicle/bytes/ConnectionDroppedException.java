/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * {@link IORuntimeException} signalling an unexpected loss of connection, typically used by
 * networking components.
 */
public class ConnectionDroppedException extends IORuntimeException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a {@code ConnectionDroppedException} with the specified detail message.
     *
     * @param message the detail message saved for later retrieval by the {@link #getMessage()} method.
     */
    public ConnectionDroppedException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code ConnectionDroppedException} with the specified cause.
     *
     * @param e the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *          (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ConnectionDroppedException(Throwable e) {
        super(e);
    }

    /**
     * Constructs a {@code ConnectionDroppedException} with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message saved for later retrieval by the {@link #getMessage()} method.
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ConnectionDroppedException(String message, Throwable cause) {
        super(message, cause);
    }
}
