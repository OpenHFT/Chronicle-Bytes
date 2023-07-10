/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.InvalidMarshallableException;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Interface defining the required operations for a method encoder.
 * <p>
 * A method encoder is responsible for encoding a method call into a {@link BytesOut} object and decoding the method call from a {@link BytesIn} object.
 * This encoding/decoding is often necessary for serializing method calls or for sending method calls over a network for remote procedure calls (RPC).
 * <p>
 * Implementations of this interface should ensure that the encode and decode methods are properly synchronized if they're intended to be used in a multi-threaded context.
 */
public interface MethodEncoder {

    /**
     * Returns a unique identifier for the method being encoded.
     *
     * @return the unique identifier of the method
     */
    long messageId();

    /**
     * Encodes a method call, represented by an array of objects, into a {@link BytesOut} object.
     *
     * @param objects the objects representing a method call
     * @param out the BytesOut object to write the method call to
     * @throws IllegalArgumentException if an argument is not valid
     * @throws BufferUnderflowException if there is not enough data available in the buffer
     * @throws IllegalStateException if a state-dependent method has been invoked at an inappropriate time
     * @throws BufferOverflowException if there is not enough space in the buffer
     * @throws ArithmeticException if numeric overflow occurs
     * @throws InvalidMarshallableException if an object is not correctly marshallable
     */
    void encode(Object[] objects, BytesOut<?> out)
            throws IllegalArgumentException, BufferUnderflowException, IllegalStateException, BufferOverflowException, ArithmeticException, InvalidMarshallableException;

    /**
     * Decodes a method call from a {@link BytesIn} object into an array of objects.
     *
     * @param lastObjects the previous objects used for the method call, can be used for delta encoding
     * @param in the BytesIn object to read the method call from
     * @return the objects representing a method call
     * @throws BufferUnderflowException if there is not enough data available in the buffer
     * @throws IllegalStateException if a state-dependent method has been invoked at an inappropriate time
     * @throws InvalidMarshallableException if an object is not correctly marshallable
     */
    Object[] decode(Object[] lastObjects, BytesIn<?> in)
            throws BufferUnderflowException, IllegalStateException, InvalidMarshallableException;
}
