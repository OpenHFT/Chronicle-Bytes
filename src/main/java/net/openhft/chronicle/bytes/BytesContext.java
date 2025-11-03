/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;

/**
 * Holds a {@link Bytes} buffer and optional key for a unit of work. Implementations
 * may support rollback of writes when the context is closed.
 */
public interface BytesContext extends Closeable {

    /**
     * Returns the buffer associated with this context.
     */
    Bytes<?> bytes();

    /**
     * Provides a context-dependent key, such as a message type.
     */
    int key();

    /**
     * Indicates whether this context has been closed. The default implementation
     * throws {@link UnsupportedOperationException} and should be overridden.
     */
    @Override
    default boolean isClosed() {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * Marks this context to roll back any writes when {@link #close()} is called.
     */
    default void rollbackOnClose() {
    }
}
