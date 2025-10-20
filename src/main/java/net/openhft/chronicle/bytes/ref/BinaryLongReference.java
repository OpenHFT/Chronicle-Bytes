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
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;

/**
 * Holds a 64-bit long in little-endian binary form.
 * <p>The value may temporarily be {@link #LONG_NOT_COMPLETE} when used as part
 * of a state machine.</p>
 *
 * <p> Volatile and ordered methods follow the same guarantees as
 * {@link java.util.concurrent.atomic.AtomicLong}.
 * <p> Dereferencing a {@code null} store in {@link #toString()} is solely
 * for debugging.
 *
 * @see BytesStore
 * @see LongReference
 */
@SuppressWarnings("rawtypes")
public class BinaryLongReference extends AbstractReference implements LongReference {
    /**
     * Sentinel value indicating that a long operation did not complete as expected.
     */
    public static final long LONG_NOT_COMPLETE = -1;

    /**
     * Stores bytes from the given BytesStore into this BinaryLongReference.
     *
     * @param bytes  The BytesStore from which bytes will be stored.
     * @param offset The starting point in bytes from where the value will be stored.
     * @param length The number of bytes that should be stored.
     *               If {@code bytes} is a {@link HexDumpBytes}, the offset is
     *               masked with {@link HexDumpBytes#MASK}.
     * @throws IllegalArgumentException If the length provided is not equal to 8.
     * @throws BufferOverflowException  If the bytes cannot be written.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(final @NotNull BytesStore bytes, @NonNegative long offset, @NonNegative final long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosed();

        if (length != maxSize())
            throw new IllegalArgumentException();

        if (bytes instanceof HexDumpBytes)
            offset &= MASK; // align with HexDump masking

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
        if (bytesStore == null) return "bytes is null";
        try {
            return "value: " + getValue();
        } catch (Throwable e) {
            return e.toString();
        }
    }

    /**
     * Performs a plain read of the value from the backing store.
     *
     * @return the current value
     * @throws ClosedIllegalStateException    if the resource has been released or closed.
     * @throws ThreadingIllegalStateException if accessed by multiple threads unsafely
     */
    @Override
    public long getValue()
            throws IllegalStateException {
        return bytesStore == null ? 0L : bytesStore.readLong(offset);
    }

    /**
     * Writes the value to the backing store using plain semantics.
     *
     * @param value the value to store
     * @throws ClosedIllegalStateException    if the resource has been released or closed.
     * @throws ThreadingIllegalStateException if accessed by multiple threads unsafely
     */
    @Override
    public void setValue(long value)
            throws IllegalStateException {
        try {
            bytesStore.writeLong(offset, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Retrieves the 64-bit long value using volatile memory semantics.
     *
     * @return the 64-bit long value
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public long getVolatileValue()
            throws IllegalStateException {
        try {
            return bytesStore.readVolatileLong(offset);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Sets the 64-bit long value using volatile memory semantics.
     *
     * @param value the 64-bit long value to set
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setVolatileValue(long value)
            throws IllegalStateException {
        try {
            bytesStore.writeVolatileLong(offset, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Sets the 64-bit long value using ordered or lazy set memory semantics.
     *
     * @param value the 64-bit long value to set
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setOrderedValue(long value)
            throws IllegalStateException {
        try {
            bytesStore.writeOrderedLong(offset, value);
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
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public long addValue(long delta)
            throws IllegalStateException {
        try {
            return bytesStore.addAndGetLong(offset, delta);
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
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public boolean compareAndSwapValue(long expected, long value)
            throws IllegalStateException {
        try {
            return bytesStore.compareAndSwapLong(offset, expected, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }
}
