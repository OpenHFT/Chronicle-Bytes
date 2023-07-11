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
import net.openhft.chronicle.core.io.ValidatableUtil;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * An interface for objects that can be read from and written to bytes in a streaming manner.
 * The object's internal state can be directly serialized and deserialized from a BytesIn
 * or BytesOut object. This interface extends the ReadBytesMarshallable and
 * WriteBytesMarshallable interfaces, which provide methods for reading and writing
 * Marshallable objects, respectively.
 * <p>
 * Classes implementing this interface should override the methods to provide their own
 * custom serialization and deserialization logic. If not overridden, the default methods
 * use the BytesUtil's methods for reading and writing Marshallable objects.
 * <p>
 * The $toString method provides a default implementation for creating a string
 * representation of the object in a hexadecimal format.
 *
 * <p>Implementations of this interface must not be chained as suggested by the {@code DontChain} annotation.
 *
 */
@DontChain
public interface BytesMarshallable extends ReadBytesMarshallable, WriteBytesMarshallable {

    /**
     * Indicates whether the object uses self-describing messages for serialization.
     * By default, this method returns false.
     *
     * @return false by default.
     */
    @Override
    default boolean usesSelfDescribingMessage() {
        return false;
    }

    /**
     * Reads the state of this object from the bytes.
     *
     * @param bytes the BytesIn object to read from.
     * @throws IORuntimeException           if an I/O error occurs.
     * @throws BufferUnderflowException     if there is not enough data available in the buffer.
     * @throws IllegalStateException        if there is an error in the internal state.
     * @throws InvalidMarshallableException if the object cannot be read due to invalid data.
     */
    @Override
    default void readMarshallable(BytesIn<?> bytes)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, InvalidMarshallableException {
        BytesUtil.readMarshallable(this, bytes);
        ValidatableUtil.validate(this);
    }

    /**
     * Writes the state of this object to the bytes.
     *
     * @param bytes the BytesOut object to write to.
     * @throws IllegalStateException        if there is an error in the internal state.
     * @throws BufferOverflowException      if there is not enough space in the buffer.
     * @throws BufferUnderflowException     if there is not enough data available in the buffer.
     * @throws ArithmeticException          if there is an arithmetic error.
     * @throws InvalidMarshallableException if the object cannot be written due to invalid data.
     */
    @Override
    default void writeMarshallable(BytesOut<?> bytes)
            throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException, InvalidMarshallableException {
        ValidatableUtil.validate(this);
        BytesUtil.writeMarshallable(this, bytes);
    }

    /**
     * Provides a string representation of this object in a hexadecimal format.
     *
     * @return the hexadecimal string representation of this object.
     */
    default String $toString() {
        ValidatableUtil.startValidateDisabled();
        try {
            HexDumpBytes bytes = new HexDumpBytes();
            writeMarshallable(bytes);
            String s = "# " + getClass().getName() + "\n" + bytes.toHexString();
            bytes.releaseLast();
            return s;
        } catch (Throwable e) {
            return e.toString();
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }
}
