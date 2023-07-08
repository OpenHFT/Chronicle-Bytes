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
 * Represents a 64-bit long integer in binary form, backed by a {@link BytesStore}.
 * <p>
 * This class provides various operations to access and manipulate a single long integer in binary form.
 * The long integer is stored in a BytesStore, and this class provides methods for atomic operations,
 * reading/writing the value, and managing its state.
 * <p>
 * The class also supports volatile reads, ordered writes, and compare-and-swap operations.
 * The maximum size of the backing storage is 8 bytes, corresponding to a 64-bit long integer.
 * <p>
 * Example usage:
 * <pre>
 * BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(32);
 * try (BinaryLongReference ref = new BinaryLongReference()) {
 *     ref.bytesStore(bytesStore, 16, 8);
 *     ref.setValue(1234567890L);
 *     long value = ref.getVolatileValue();
 * }
 * </pre>
 * <p>
 * Note: This class is not thread-safe. External synchronization may be necessary if instances
 * are shared between threads.
 *
 * @author [Your Name]
 * @see BytesStore
 * @see LongReference
 */
public class BinaryLongReference extends AbstractReference implements LongReference {
    public static final long LONG_NOT_COMPLETE = -1;

    /**
     * Stores bytes from the given BytesStore into this BinaryLongReference.
     *
     * @param bytes  The BytesStore from which bytes will be stored.
     * @param offset The starting point in bytes from where the value will be stored.
     * @param length The number of bytes that should be stored.
     * @throws IllegalStateException    If the BinaryLongReference is in an invalid state.
     * @throws IllegalArgumentException If the length provided is not equal to 8.
     * @throws BufferOverflowException  If the bytes cannot be written.
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
