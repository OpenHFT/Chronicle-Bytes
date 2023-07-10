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
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;

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
     *
     * @throws IORuntimeException If an I/O error occurs during reading.
     * @throws BufferUnderflowException If there is not enough data in the buffer to read.
     * @throws IllegalStateException If the buffer has been released or the method is called
     *                               in an inappropriate context.
     * @throws InvalidMarshallableException If there is a problem with marshalling data,
     *                                      such as incorrect format or type.
     */
    void readMarshallable(BytesIn<?> bytes)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, InvalidMarshallableException;
}
