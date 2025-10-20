/*
 * Copyright 2016-2025 chronicle.software
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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Reads data from a byte stream or buffer. Combines random access, sequential
 * streaming, and text parsing capabilities.
 *
 * @param <U> underlying buffer type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesIn<U> extends
        RandomDataInput,
        StreamingDataInput<Bytes<U>>,
        ByteStringParser<Bytes<U>> {
    /**
     * Creates a {@link MethodReader} that decodes method calls from this input
     * and dispatches them to the supplied handler objects.
     *
     * @param objects handler instances implementing the expected interfaces
     * @return a reader that processes one message per invocation of
     *         {@link MethodReader#readOne()}
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
     * Reads a {@link ReadBytesMarshallable} prefixed with a 16‑bit length.
     *
     * @param tClass type of object to create when {@code using} is {@code null}
     * @param using  optional instance to reuse
     * @return the populated instance
     */
    <T extends ReadBytesMarshallable> T readMarshallableLength16(@NotNull Class<T> tClass, @Nullable T using)
            throws BufferUnderflowException, InvalidMarshallableException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Reads a simple object such as {@code String}, {@code Long} or an
     * implementation of {@link BytesMarshallable}.
     *
     * @param componentType0 expected result type
     * @return the deserialised object
     * @throws UnsupportedOperationException if {@code componentType0} is not supported
     */
    default <T> T readObject(@NotNull Class<T> componentType0)
            throws BufferUnderflowException, ArithmeticException, BufferOverflowException, InvalidMarshallableException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
