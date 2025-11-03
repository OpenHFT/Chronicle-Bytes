/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;

import java.nio.BufferUnderflowException;

/**
 * Functional interface to facilitate the reading of data directly as Bytes. Primarily designed to
 * be used where a lambda or a method reference can simplify code when reading objects or data from
 * {@link BytesIn} instances.
 *
 * <p>The interface also implements {@link CommonMarshallable}, a common parent for classes and
 * interfaces that provides marshalling and unmarshalling methods for converting objects to bytes
 * and bytes to objects.
 *
 * <p>Implementations of this interface are expected to handle their own validation logic and may
 * need to call {@link net.openhft.chronicle.core.io.Validatable#validate()} as necessary.
 *
 * @see WriteBytesMarshallable
 * @see BytesIn
 * @see BytesOut
 */
@FunctionalInterface
@DontChain
public interface ReadBytesMarshallable extends CommonMarshallable {
    /**
     * Reads data from the provided {@link BytesIn} object. Implementations of this method are
     * responsible for handling their own data reading logic based on the structure of the data
     * they expect to read.
     *
     * <p>Note: Implementations are also responsible for calling
     * {@link net.openhft.chronicle.core.io.Validatable#validate()} when necessary.
     *
     * @param bytes The {@link BytesIn} instance to read data from.
     * @throws IORuntimeException           If an I/O error occurs during reading.
     * @throws BufferUnderflowException     If there is not enough data in the buffer to read.
     * @throws InvalidMarshallableException If there is a problem with marshalling data,
     *                                      such as incorrect format or type.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    void readMarshallable(BytesIn<?> bytes)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, InvalidMarshallableException;
}
