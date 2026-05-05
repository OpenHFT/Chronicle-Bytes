/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * Signals an unexpected connection drop because the remote endpoint vanished,
 * so callers can treat network loss differently from other I/O failures.
 */
public class ConnectionDroppedException extends IORuntimeException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs with a message describing which connection dropped.
     *
     * @param message the detail message
     */
    public ConnectionDroppedException(String message) {
        super(message);
    }

    /**
     * Constructs with a cause describing the underlying failure.
     *
     * @param e the cause
     */
    public ConnectionDroppedException(Throwable e) {
        super(e);
    }

    /**
     * Constructs with a detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ConnectionDroppedException(String message, Throwable cause) {
        super(message, cause);
    }
}
