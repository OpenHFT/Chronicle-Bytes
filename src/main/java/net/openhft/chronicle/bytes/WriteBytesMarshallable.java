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

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.InvalidMarshallableException;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * An interface defining a contract for serializing data directly to a Bytes instance.
 * Objects implementing this interface can be marshalled into a sequence of bytes.
 *
 * @see net.openhft.chronicle.core.io.Validatable
 */
@FunctionalInterface
@DontChain
public interface WriteBytesMarshallable extends CommonMarshallable {

    /**
     * Serializes this object to the provided Bytes instance.
     *
     * <p>This method is responsible for calling
     * {@link net.openhft.chronicle.core.io.Validatable#validate()} as needed to ensure
     * the validity of the state of the object before serialization.</p>
     *
     * @param bytes the Bytes instance to write to.
     * @throws IllegalStateException if bytes instance is released or not in a writable state.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     * @throws BufferUnderflowException if there is not enough data available to read from the buffer.
     * @throws IllegalArgumentException if an argument is illegal or inappropriate.
     * @throws ArithmeticException if numeric overflow or underflow occurs.
     * @throws InvalidMarshallableException if an object fails validation checks before or during serialization.
     */
    void writeMarshallable(BytesOut<?> bytes)
            throws IllegalStateException, BufferOverflowException, BufferUnderflowException, IllegalArgumentException, ArithmeticException, InvalidMarshallableException;
}
