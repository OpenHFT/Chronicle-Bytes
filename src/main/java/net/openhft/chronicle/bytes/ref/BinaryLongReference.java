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
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;
/**
 * Represents a 64-bit long reference in binary form.
 */
public class BinaryLongReference extends AbstractReference implements LongReference {
    public static final long LONG_NOT_COMPLETE = -1;

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
        throwExceptionIfClosed();

        if (length != maxSize())
            throw new IllegalArgumentException();

        if (bytes instanceof HexDumpBytes) {
            offset &= MASK;
        }

        super.bytesStore(bytes, offset, length);
    }

    /**
     * Returns the maximum size of this reference in bytes (8 bytes for a 64-bit long).
     *
     * @return the maximum size in bytes
     */
    @Override
    public long maxSize() {
        return Long.BYTES;
    }

    /**
     * Returns a string representation of this BinaryLongReference.
     *
     * @return a string representation
     */
    @NotNull
    @Override
    public String toString() {
        if (bytes == null) return "bytes is null";
        try {
            return "value: " + getValue();
        } catch (Throwable e) {
            return e.toString();
        }
    }

    /**
     * Retrieves the 64-bit long value from the BytesStore.
     *
     * @return the 64-bit long value
     * @throws IllegalStateException      if closed
     */
    @Override
    public long getValue()
            throws IllegalStateException {
        return bytes == null ? 0L : bytes.readLong(offset);
    }

    /**
     * Sets the 64-bit long value in the BytesStore.
     *
     * @param value the 64-bit long value to set
     * @throws IllegalStateException    if closed
     */
    @Override
    public void setValue(long value)
            throws IllegalStateException {
        try {
            bytes.writeLong(offset, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Retrieves the 64-bit long value using volatile memory semantics.
     *
     * @return the 64-bit long value
     * @throws IllegalStateException      if closed
     */
    @Override
    public long getVolatileValue()
            throws IllegalStateException {
        try {
            return bytes.readVolatileLong(offset);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Sets the 64-bit long value using volatile memory semantics.
     *
     * @param value the 64-bit long value to set
     * @throws IllegalStateException    if closed
     */
    @Override
    public void setVolatileValue(long value)
            throws IllegalStateException {
        try {
            bytes.writeVolatileLong(offset, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Sets the 64-bit long value using ordered or lazy set memory semantics.
     *
     * @param value the 64-bit long value to set
     * @throws IllegalStateException    if closed
     */
    @Override
    public void setOrderedValue(long value)
            throws IllegalStateException {
        try {
            bytes.writeOrderedLong(offset, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Adds a delta to the current 64-bit long value and returns the result.
     *
     * @param delta the value to add
     * @return the resulting 64-bit long value
     * @throws IllegalStateException      if closed
     */
    @Override
    public long addValue(long delta)
            throws IllegalStateException {
        try {
            return bytes.addAndGetLong(offset, delta);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Adds a delta to the current 64-bit long value atomically and returns the result.
     *
     * @param delta the value to add
     * @return the resulting 64-bit long value
     * @throws IllegalStateException      if closed
     */
    @Override
    public long addAtomicValue(long delta)
            throws IllegalStateException {
        return addValue(delta);
    }

    /**
     * Atomically sets the 64-bit long value to the given updated value if the current value is
     * equal to the expected value.
     *
     * @param expected the expected 64-bit long value
     * @param value    the new 64-bit long value
     * @return true if successful, false otherwise
     * @throws IllegalStateException    if closed
     */
    @Override
    public boolean compareAndSwapValue(long expected, long value)
            throws IllegalStateException {
        try {
            return bytes.compareAndSwapLong(offset, expected, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }
}
