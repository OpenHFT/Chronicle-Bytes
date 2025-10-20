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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Holds a 32-bit integer in little-endian native order within a
 * {@link BytesStore}.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try (BinaryIntReference ref = new BinaryIntReference()) {
 *     ref.bytesStore(store, offset, ref.maxSize());
 *     ref.setValue(10);
 * }
 * }</pre>
 *
 * <p> All volatile methods provide a full happens-before edge. Ordered
 * methods use lazy set semantics. {@link #maxSize()} always returns {@code
 * Integer.BYTES}.
 * <p> Negative values are valid; {@link #INT_NOT_COMPLETE} is only a
 * replay sentinel.
 *
 * @see BytesStore
 * @see IntValue
 */
@SuppressWarnings("rawtypes")
public class BinaryIntReference extends AbstractReference implements IntValue {
    /**
     * Sentinel value used when an integer operation fails to complete normally.
     */
    public static final int INT_NOT_COMPLETE = Integer.MIN_VALUE;

    /**
     * Sets the BytesStore which this reference points to.
     *
     * @param bytes  the BytesStore
     * @param offset the offset within the BytesStore
     * @param length the length of the value
     * @throws IllegalArgumentException If the length is not equal to maxSize
     * @throws BufferOverflowException  If the offset is too large
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
        if (bytesStore == null)
            return "bytes is null";
        try {
            return "value: " + getValue();
        } catch (Throwable e) {
            return "value: " + e;
        }
    }

    /**
     * Performs a plain read of the value from the backing store.
     *
     * @return the current value
     * @throws BufferUnderflowException if the offset is invalid
     * @throws ClosedIllegalStateException    if the resource has been released or closed.
     * @throws ThreadingIllegalStateException if accessed by multiple threads unsafely
     */
    @Override
    public int getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore == null ? 0 : bytesStore.readInt(offset);
    }

    /**
     * Writes the value to the backing store using plain semantics.
     *
     * @param value the value to store
     * @throws BufferOverflowException if the offset is invalid
     * @throws ClosedIllegalStateException    if the resource has been released or closed.
     * @throws ThreadingIllegalStateException if accessed by multiple threads unsafely
     */
    @Override
    public void setValue(int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytesStore.writeInt(offset, value);
    }

    /**
     * Retrieves the 32-bit integer value using volatile memory semantics.
     *
     * @return the 32-bit integer value
     * @throws BufferUnderflowException If the offset is too large
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public int getVolatileValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore.readVolatileInt(offset);
    }

    /**
     * Sets the 32-bit integer value using ordered or lazy set memory semantics.
     *
     * @param value the 32-bit integer value to set
     * @throws BufferOverflowException If the offset is too large
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setOrderedValue(int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytesStore.writeOrderedInt(offset, value);
    }

    /**
     * Adds a delta to the current 32-bit integer value and returns the result.
     *
     * @param delta the value to add
     * @return the resulting 32-bit integer value
     * @throws BufferUnderflowException If the offset is too large
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     * <p> This atomic fetch-and-add forms a happens-before edge with
     *           subsequent reads of the value.
     */
    @Override
    public int addValue(int delta)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore.addAndGetInt(offset, delta);
    }

    /**
     * Adds a delta to the current 32-bit integer value atomically and returns the result.
     *
     * @param delta the value to add
     * @return the resulting 32-bit integer value
     * @throws BufferUnderflowException If the offset is too large
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
     * @throws BufferOverflowException If the offset is too large
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     * <p> A successful swap establishes a happens-before relation with
     *           subsequent reads of the value.
     */
    @Override
    public boolean compareAndSwapValue(int expected, int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        return bytesStore.compareAndSwapInt(offset, expected, value);
    }
}
