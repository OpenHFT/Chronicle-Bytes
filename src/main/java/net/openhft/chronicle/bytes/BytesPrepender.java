/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

/**
 * Supports writing data before the current {@link Bytes#readPosition()}.
 * After each operation the read position moves backwards.
 *
 * @param <B> self type
 */
@SuppressWarnings("unchecked")
public interface BytesPrepender<B extends BytesPrepender<B>> {

    /**
     * Clears the buffer then advances both cursors by {@code length} bytes so
     * data can be prepended later.
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
