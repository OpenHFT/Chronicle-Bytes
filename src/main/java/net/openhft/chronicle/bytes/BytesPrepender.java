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

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

/**
 * An interface defining a prependable buffer of bytes. A BytesPrepender can prepend bytes, byte arrays,
 * and numeric values to a buffer, making them accessible for reading operations from the front.
 * <p>
 * This interface is generic and can be parameterized with any type that extends BytesPrepender.
 * <p>
 * Note: For all prepend and prewrite operations, the read position (but not the write position or read limit)
 * is moved backward.
 * <p>
 * BufferOverflowException can occur if the capacity of the underlying
 * buffer is exceeded during operation execution.
 *
 * @param <B> the type of BytesPrepender. This is a self-referential generic type parameter.
 * @see BufferOverflowException
 * @see IllegalStateException
 */
@SuppressWarnings("unchecked")
public interface BytesPrepender<B extends BytesPrepender<B>> {

    /**
     * Clears the buffer and pads it with a specified length to allow prepending later.
     * clearAndPad(0) is equivalent to clear().
     *
     * @param length the padding length
     * @return this instance, after clearing and padding
     * @throws BufferOverflowException If the length is greater than the difference of capacity() and start()
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    B clearAndPad(@NonNegative long length)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Prepends a long value as a decimal text. This operation moves the readPosition() backward.
     * <p>Note: The operation shifts the readPosition, but not the writePosition or readLimit
     *
     * @param value the long value to prepend as text
     * @return this instance, after the operation
     * @throws BufferOverflowException If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default B prepend(long value)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.prepend(this, value);
        return (B) this;
    }

    /**
     * Writes a byte array backward in binary format. This operation moves the readPosition() backward.
     *
     * @param bytes the byte array to prepend
     * @return this instance, after the operation
     * @throws BufferOverflowException If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    B prewrite(byte[] bytes)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes a BytesStore instance backward in binary format. This operation moves the readPosition() backward.
     *
     * @param bytes the BytesStore to prepend
     * @return this instance, after the operation
     * @throws BufferOverflowException If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    B prewrite(BytesStore<?, ?> bytes)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes a byte backward in binary format. This operation moves the readPosition() backward.
     *
     * @param b the byte to prepend
     * @return this instance, after the operation
     * @throws BufferOverflowException If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    B prewriteByte(byte b)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes a short (2-byte int) backward in binary format. This operation moves the readPosition() backward.
     *
     * @param i the short to prepend
     * @return this instance, after the operation
     * @throws BufferOverflowException If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    B prewriteShort(short i)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes an int (4-byte int) backward in binary format. This operation moves the readPosition() backward.
     *
     * @param i the int to prepend
     * @return this instance, after the operation
     * @throws BufferOverflowException If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    B prewriteInt(int i)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes a long (8-byte int) backward in binary format. This operation moves the readPosition() backward.
     *
     * @param l the long to prepend
     * @return this instance, after the operation
     * @throws BufferOverflowException If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    B prewriteLong(long l)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;
}
