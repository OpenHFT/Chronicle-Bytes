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
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Defines an interface for reading bytes. This interface extends several interfaces
 * that provide methods for reading random data, streaming data, and string parsing.
 * It provides methods for reading Marshallable objects, creating a MethodReader for
 * reading methods serialized to the file, and a builder for the MethodReader. It also
 * provides a method to read an object of a specific class.
 * <p>
 * This interface supports reading of basic data types, Strings, Enums, and
 * BytesMarshallable objects. If an unsupported class is specified in the readObject method,
 * an UnsupportedOperationException is thrown.
 * <p>
 * The classes implementing this interface should handle any necessary synchronization.
 *
 * @param <U> the type of the bytes in this input.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesIn<U> extends
        RandomDataInput,
        StreamingDataInput<Bytes<U>>,
        ByteStringParser<Bytes<U>> {
    /**
     * Creates a MethodReader for reading methods serialized to the MarshallableOut.
     *
     * @param objects which implement the methods serialized to the MarshallableOut.
     * @return a MethodReader which will read one Excerpt at a time.
     */
    @NotNull
    default MethodReader bytesMethodReader(@NotNull Object... objects) {
        return new BytesMethodReaderBuilder(this).build(objects);
    }

    /**
     * Creates a builder for the MethodReader.
     *
     * @return a BytesMethodReaderBuilder for this BytesIn.
     */
    @NotNull
    default BytesMethodReaderBuilder bytesMethodReaderBuilder() {
        return new BytesMethodReaderBuilder(this);
    }

    /**
     * Reads a Marshallable object of a specific class from this BytesIn.
     *
     * @param tClass the class of the Marshallable object to be read.
     * @param using  the object to be used for reading, can be null.
     * @return the read Marshallable object.
     * @throws BufferUnderflowException     if there are not enough bytes left to read.
     * @throws IllegalStateException        if there is an error in the internal state.
     * @throws InvalidMarshallableException if the object cannot be read due to invalid data.
     */
    <T extends ReadBytesMarshallable> T readMarshallableLength16(@NotNull Class<T> tClass, @Nullable T using)
            throws BufferUnderflowException, IllegalStateException, InvalidMarshallableException;

    /**
     * Reads an object of a specific class from this BytesIn.
     *
     * @param componentType0 the class of the object to be read.
     * @return the read object.
     * @throws BufferUnderflowException      if there are not enough bytes left to read.
     * @throws IllegalStateException         if there is an error in the internal state.
     * @throws ArithmeticException           if there is an arithmetic error.
     * @throws BufferOverflowException       if there are too many bytes left to read.
     * @throws InvalidMarshallableException  if the object cannot be read due to invalid data.
     * @throws UnsupportedOperationException if an unsupported class is specified.
     */
    default <T> T readObject(@NotNull Class<T> componentType0)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException, BufferOverflowException, InvalidMarshallableException {
        Class<T> componentType = ObjectUtils.implementationToUse(componentType0);
        if (BytesMarshallable.class.isAssignableFrom(componentType)) {
            BytesMarshallable bm = (BytesMarshallable) ObjectUtils.newInstance(componentType);
            bm.readMarshallable(this);

            return (T) bm;
        }
        if (Enum.class.isAssignableFrom(componentType)) {
            return (T) readEnum((Class) componentType);
        }
        switch (componentType.getName()) {
            case "java.lang.String":
                return (T) readUtf8();
            case "java.lang.Double":
                return (T) (Double) readDouble();
            case "java.lang.Long":
                return (T) (Long) readLong();
            case "java.lang.Integer":
                return (T) (Integer) readInt();

            default:
                throw new UnsupportedOperationException("Unsupported " + componentType);
        }
    }
}
