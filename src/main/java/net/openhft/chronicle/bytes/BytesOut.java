/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
 * Output interface for writing to a {@link Bytes} buffer. It combines streaming
 * writes with text appending and prepending utilities.
 *
 * @param <U> underlying store type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesOut<U> extends
        StreamingDataOutput<Bytes<U>>,
        ByteStringAppender<Bytes<U>>,
        BytesPrepender<Bytes<U>>,
        HexDumpBytesDescription<BytesOut<U>> {

    /**
     * Returns a proxy that serialises method calls to this output. Additional
     * interfaces may be supplied.
     */
    @NotNull
    default <T> T bytesMethodWriter(@NotNull Class<T> tClass, Class<?>... additional)
            throws IllegalArgumentException, ClosedIllegalStateException {
        throwExceptionIfReleased(this);
        Class[] interfaces = ObjectUtils.addAll(tClass, additional);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), interfaces,
                new BinaryBytesMethodWriterInvocationHandler(tClass, MethodEncoderLookup.BY_ANNOTATION, this));
    }

    /**
     * Serialises {@code marshallable} prefixed with a 16-bit length.
     */
    void writeMarshallableLength16(WriteBytesMarshallable marshallable)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, InvalidMarshallableException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes {@code obj} according to {@code componentType}. Supported types
     * include {@link String}, boxed primitives, {@link BytesStore} and
     * {@link BytesMarshallable} implementations.
     */
    default void writeObject(Class<?>componentType, Object obj)
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
            BytesStore<?, ?> bs = (BytesStore) obj;
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
