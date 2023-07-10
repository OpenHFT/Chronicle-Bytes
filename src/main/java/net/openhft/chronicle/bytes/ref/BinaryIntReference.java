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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;

/**
 * This class acts as a Binary 32-bit in values. c.f. TextIntReference
 */
public class BinaryIntReference extends AbstractReference implements IntValue {
    public static final int INT_NOT_COMPLETE = Integer.MIN_VALUE;

    /**
     * Sets the BytesStore which this reference points to.
     *
     * @param bytes  the BytesStore
     * @param offset the offset within the BytesStore
     * @param length the length of the value
     * @throws IllegalStateException       if closed
     * @throws IllegalArgumentException    if the length is not equal to maxSize
     * @throws BufferOverflowException    if the offset is too large
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(final @NotNull BytesStore bytes, @NonNegative long offset, @NonNegative final long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        if (length != maxSize())
            throw new IllegalArgumentException();
        if (bytes instanceof HexDumpBytes) {
            offset &= HexDumpBytes.MASK;
        }
        super.bytesStore(bytes, offset, length);
    }

    /**
     * Returns the maximum size of this reference in bytes (4 bytes for a 32-bit integer).
     *
     * @return the maximum size in bytes
     */
    @Override
    public long maxSize() {
        return Integer.BYTES;
    }

    /**
     * Returns a string representation of this BinaryIntReference.
     *
     * @return a string representation
     */
    @NotNull
    @Override
    public String toString() {
        if (bytes == null)
            return "bytes is null";
        try {
            return "value: " + getValue();
        } catch (Throwable e) {
            return "value: " + e;
        }
    }

    /**
     * Retrieves the 32-bit integer value from the BytesStore.
     *
     * @return the 32-bit integer value
     * @throws IllegalStateException      if closed
     * @throws BufferUnderflowException  if the offset is too large
     */
    @Override
    public int getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes == null ? 0 : bytes.readInt(offset);
    }

    /**
     * Sets the 32-bit integer value in the BytesStore.
     *
     * @param value the 32-bit integer value to set
     * @throws IllegalStateException    if closed
     * @throws BufferOverflowException if the offset is too large
     */
    @Override
    public void setValue(int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeInt(offset, value);
    }

    /**
     * Retrieves the 32-bit integer value using volatile memory semantics.
     *
     * @return the 32-bit integer value
     * @throws IllegalStateException      if closed
     * @throws BufferUnderflowException  if the offset is too large
     */
    @Override
    public int getVolatileValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(offset);
    }

    /**
     * Sets the 32-bit integer value using ordered or lazy set memory semantics.
     *
     * @param value the 32-bit integer value to set
     * @throws IllegalStateException    if closed
     * @throws BufferOverflowException if the offset is too large
     */
    @Override
    public void setOrderedValue(int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeOrderedInt(offset, value);
    }

    /**
     * Adds a delta to the current 32-bit integer value and returns the result.
     *
     * @param delta the value to add
     * @return the resulting 32-bit integer value
     * @throws IllegalStateException      if closed
     * @throws BufferUnderflowException  if the offset is too large
     */
    @Override
    public int addValue(int delta)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.addAndGetInt(offset, delta);
    }

    /**
     * Adds a delta to the current 32-bit integer value atomically and returns the result.
     *
     * @param delta the value to add
     * @return the resulting 32-bit integer value
     * @throws IllegalStateException      if closed
     * @throws BufferUnderflowException  if the offset is too large
     */
    @Override
    public int addAtomicValue(int delta)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return addValue(delta);
    }

    /**
     * Atomically sets the 32-bit integer value to the given updated value if the current value is
     * equal to the expected value.
     *
     * @param expected the expected 32-bit integer value
     * @param value    the new 32-bit integer value
     * @return true if successful, false otherwise
     * @throws IllegalStateException    if closed
     * @throws BufferOverflowException if the offset is too large
     */
    @Override
    public boolean compareAndSwapValue(int expected, int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        return bytes.compareAndSwapInt(offset, expected, value);
    }
}
