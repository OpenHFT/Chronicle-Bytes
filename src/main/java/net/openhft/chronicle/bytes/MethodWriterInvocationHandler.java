/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;

import java.lang.reflect.InvocationHandler;
/**
 * Interface for handling invocations in a MethodWriter.
 * This provides advanced configurations for MethodWriters, including
 * history recording, resource cleanup, generic event specification,
 * and the option to use method IDs.
 */
public interface MethodWriterInvocationHandler extends InvocationHandler {

    /**
     * Enable or disable the recording of method invocation history.
     *
     * @param recordHistory boolean flag to indicate if history should be recorded
     */
    void recordHistory(boolean recordHistory);

    /**
     * Attach a Closeable resource that will be closed when this handler is closed.
     * This is useful for handling resource cleanup after the handler is done.
     *
     * @param closeable the Closeable resource to be managed
     */
    void onClose(Closeable closeable);

    /**
     * Specify the identifier for generic events.
     * A generic event uses the first argument as the method name, providing a flexible way to write arbitrary methods at runtime.
     *
     * @param genericEvent the identifier used for generic events
     */
    void genericEvent(String genericEvent);

    /**
     * Enable or disable the use of method IDs.
     * When enabled, methods can be encoded with a numeric ID instead of a string for efficiency.
     *
     * @param useMethodIds boolean flag to indicate if method IDs should be used
     */
    void useMethodIds(boolean useMethodIds);

}
