/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;

/**
 * Stores a boolean as a single byte.
 * <p>The encoding uses {@code 0xB0} for {@code false} and {@code 0xB1} for
 * {@code true} to avoid confusion with ASCII digits.</p>
 *
 * <p> Any other byte value yields undefined behaviour in
 * {@link #getValue()}.
 * @see BytesStore
 * @see BooleanValue
 */
@SuppressWarnings("rawtypes")
public class BinaryBooleanReference extends AbstractReference implements BooleanValue {

    private static final byte FALSE = (byte) 0xB0;
    private static final byte TRUE = (byte) 0xB1;

    /**
     * Sets the underlying BytesStore to work with, along with the offset and length.
     *
     * @param bytes  the BytesStore to set
     * @param offset the offset to set
     * @param length the length to set
     * @throws IllegalArgumentException If the arguments are invalid
     * @throws BufferOverflowException  If the provided buffer is too small
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(final @NotNull BytesStore bytes, @NonNegative long offset, @NonNegative final long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        if (length != maxSize())
            throw new IllegalArgumentException();
        if (bytes instanceof HexDumpBytes) {
            offset &= MASK;
        }
        super.bytesStore(bytes, offset, length);
    }

    /**
     * Returns the maximum size of the byte representation of a boolean value.
     *
     * @return The maximum size of a boolean in bytes
     */
    @Override
    public long maxSize() {
        return 1;
    }

    /**
     * Reads a boolean value from the bytes store.
     * Behaviour is undefined if the stored byte is neither
     *
     * @return The read boolean value
     * @throws BufferUnderflowException If the bytes store contains insufficient data
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     * {@code 0xB0} nor {@code 0xB1}.
     */
    @Override
    public boolean getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        byte b = bytesStore.readByte(offset);
        if (b == FALSE)
            return false;
        if (b == TRUE)
            return true;

        throw new IllegalStateException("unexpected code=" + b);
    }

    /**
     * Writes a boolean value to the bytes store.
     *
     * @param flag The boolean value to write
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setValue(final boolean flag)
            throws IllegalStateException {
        throwExceptionIfClosed();

        bytesStore.writeByte(offset, flag ? TRUE : FALSE);
    }
}
