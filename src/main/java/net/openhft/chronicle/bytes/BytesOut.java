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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;

/**
 * This interface represents an output stream for writing data to bytes.
 * It extends {@link StreamingDataOutput}, {@link ByteStringAppender},
 * {@link BytesPrepender}, and {@link HexDumpBytesDescription}, thus providing a
 * wide range of operations for handling byte data, including appending, prepending,
 * and producing a hex dump description.
 *
 * <p>Note: This interface suppresses rawtypes and unchecked warnings.
 *
 * @param <U> the type of the BytesOut
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesOut<U> extends
        StreamingDataOutput<Bytes<U>>,
        ByteStringAppender<Bytes<U>>,
        BytesPrepender<Bytes<U>>,
        HexDumpBytesDescription<BytesOut<U>> {

    /**
     * Creates a proxy for the provided interface(s), such that each method invocation on the proxy
     * is written to this {@code BytesOut} instance for replay.
     *
     * @param tClass     the primary interface to be proxied.
     * @param additional any additional interfaces to be proxied.
     * @return a proxy implementing the provided interfaces.
     * @throws IllegalArgumentException    If an argument is inappropriate
     * @throws NullPointerException        If the provided {@code tClass} is {@code null}
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     */
    @NotNull
    default <T> T bytesMethodWriter(@NotNull Class<T> tClass, Class... additional)
            throws IllegalArgumentException, ClosedIllegalStateException {
        throwExceptionIfReleased(this);
        Class[] interfaces = ObjectUtils.addAll(tClass, additional);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), interfaces,
                new BinaryBytesMethodWriterInvocationHandler(tClass, MethodEncoderLookup.BY_ANNOTATION, this));
    }

    /**
     * Writes a {@link WriteBytesMarshallable} to this {@code BytesOut} instance.
     *
     * @param marshallable the object to be written.
     * @throws IllegalArgumentException       If a method is invoked with an illegal or inappropriate argument.
     * @throws BufferOverflowException        If there is not enough space in the buffer.
     * @throws BufferUnderflowException       If there is not enough data available in the buffer.
     * @throws InvalidMarshallableException   If the object cannot be written due to invalid data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    void writeMarshallableLength16(WriteBytesMarshallable marshallable)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, InvalidMarshallableException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes an object of a given type to this {@code BytesOut} instance.
     * This method supports a limited set of writeObject types.
     *
     * @param componentType the expected type of the object.
     * @param obj           the object to be written.
     * @throws IllegalArgumentException       If a method is invoked with an illegal or inappropriate argument.
     * @throws BufferOverflowException        If there is not enough space in the buffer.
     * @throws ArithmeticException            If there is an arithmetic error.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     * @throws BufferUnderflowException       If there is not enough data available in the buffer.
     * @throws InvalidMarshallableException   If the object cannot be written due to invalid data.
     */
    default void writeObject(Class componentType, Object obj)
            throws IllegalArgumentException, BufferOverflowException, ArithmeticException, ClosedIllegalStateException, BufferUnderflowException, InvalidMarshallableException, ThreadingIllegalStateException {
        if (!componentType.isInstance(obj))
            throw new IllegalArgumentException("Cannot serialize " + obj.getClass() + " as an " + componentType);
        if (obj instanceof BytesMarshallable) {
            ValidatableUtil.validate(obj);
            ((BytesMarshallable) obj).writeMarshallable(this);
            return;
        }
        if (obj instanceof Enum) {
            writeEnum((Enum) obj);
            return;
        }
        if (obj instanceof BytesStore) {
            BytesStore bs = (BytesStore) obj;
            writeStopBit(bs.readRemaining());
            write(bs);
            return;
        }
        switch (componentType.getName()) {
            case "java.lang.String":
                writeUtf8((String) obj);
                return;
            case "java.lang.Double":
                writeDouble((Double) obj);
                return;
            case "java.lang.Long":
                writeLong((Long) obj);
                return;
            case "java.lang.Integer":
                writeInt((Integer) obj);
                return;

            default:
                throw new UnsupportedOperationException("Not supported " + componentType);
        }
    }
}
