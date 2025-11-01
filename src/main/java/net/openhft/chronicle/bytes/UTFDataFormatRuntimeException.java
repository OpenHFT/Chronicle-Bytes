/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * Thrown to indicate a failure when encoding or decoding UTF-8 data. Extends
 * {@link IORuntimeException} so callers need not catch it.
 */
public class UTFDataFormatRuntimeException extends IORuntimeException {
    private static final long serialVersionUID = 0L;
    /**
     * Constructs a new UTFDataFormatRuntimeException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public UTFDataFormatRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs a new UTFDataFormatRuntimeException with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is not automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UTFDataFormatRuntimeException(String message, Exception cause) {
        super(message, cause);
    }
}
