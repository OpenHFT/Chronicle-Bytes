/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;

import java.lang.reflect.InvocationHandler;

/**
 * An {@link java.lang.reflect.InvocationHandler} for method writers operating
 * on {@link BytesOut}. It extends the standard handler contract by exposing a
 * hook for resource cleanup via {@link #onClose(Closeable)}.
 */
public interface BytesMethodWriterInvocationHandler extends InvocationHandler {

    /**
     * Register a {@link Closeable} resource that should be closed when the
     * associated writer is closed.
     *
     * @param closeable resource to close with the writer
     */
    void onClose(Closeable closeable);
}
