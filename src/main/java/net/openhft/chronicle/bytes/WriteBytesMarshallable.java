/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;

import java.nio.BufferOverflowException;

/**
 * Functional interface for objects that can serialise their state directly to a
 * {@link BytesOut} stream. Implementations typically call
 * {@link net.openhft.chronicle.core.io.Validatable#validate()} before writing.
 */
@FunctionalInterface
@DontChain
public interface WriteBytesMarshallable extends CommonMarshallable {

    /**
     * Writes this object's state to {@code bytes} using a custom binary format.
     * Implementations may validate their state prior to writing and throw
     * {@link InvalidMarshallableException} if invalid.
     *
     * @param bytes the target stream
     * @throws BufferOverflowException        if there is insufficient space in the buffer
     * @throws InvalidMarshallableException   if the object is not in a valid state for serialisation
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if accessed by multiple threads in an unsafe way
     */
    void writeMarshallable(BytesOut<?> bytes)
            throws IllegalStateException, BufferOverflowException, InvalidMarshallableException;
}
