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
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;

@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesOut<U> extends
        StreamingDataOutput<Bytes<U>>,
        ByteStringAppender<Bytes<U>>,
        BytesPrepender<Bytes<U>>,
        HexDumpBytesDescription<BytesOut<U>> {

    /**
     * Proxy an interface so each message called is written to a file for replay.
     *
     * @param tClass     primary interface
     * @param additional any additional interfaces
     * @return a proxy which implements the primary interface (additional interfaces have to be
     * cast)
     * @throws NullPointerException if the provided {@code tClass} is {@code null}
     * @throws ClosedIllegalStateException if this BytesOut has been previously released
     */
    @NotNull
    default <T> T bytesMethodWriter(@NotNull Class<T> tClass, Class... additional)
            throws IllegalArgumentException {
        throwExceptionIfReleased(this);
        Class[] interfaces = ObjectUtils.addAll(tClass, additional);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), interfaces,
                new BinaryBytesMethodWriterInvocationHandler(tClass, MethodEncoderLookup.BY_ANNOTATION, this));
    }

    void writeMarshallableLength16(WriteBytesMarshallable marshallable)
            throws IllegalArgumentException, BufferOverflowException, IllegalStateException, BufferUnderflowException, InvalidMarshallableException;

    /**
     * Write a limit set of writeObject types.
     *
     * @param componentType expected.
     * @param obj           of componentType
     */
    default void writeObject(Class componentType, Object obj)
            throws IllegalArgumentException, BufferOverflowException, ArithmeticException, IllegalStateException, BufferUnderflowException, InvalidMarshallableException {
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
