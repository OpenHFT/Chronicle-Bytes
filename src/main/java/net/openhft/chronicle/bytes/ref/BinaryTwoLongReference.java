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

import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Represents a binary reference containing two 64-bit long values.
 * <p>
 * This class extends {@link BinaryLongReference} and allows access to two 64-bit long values in a binary form,
 * which are contiguous in memory.
 * </p>
 * Example usage:
 * <pre>
 * BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16); // 16 bytes for two long values.
 * BinaryTwoLongReference ref = new BinaryTwoLongReference();
 * ref.bytesStore(bytesStore, 0, ref.maxSize());
 * ref.setValue(1234567890L); // set the first long value
 * ref.setValue2(9876543210L); // set the second long value
 * </pre>
 *
 * Note: This class is not thread-safe. External synchronization may be necessary if instances
 * are shared between threads.
 *
 * @author [Your Name]
 * @see BinaryLongReference
 */
public class BinaryTwoLongReference extends BinaryLongReference implements TwoLongReference {

    /**
     * Returns the maximum size of this reference in bytes (16 bytes for two 64-bit longs).
     *
     * @return the maximum size in bytes
     */
    @Override
    public long maxSize() {
        return 2 * Long.BYTES;
    }

    /**
     * Returns a string representation of this BinaryTwoLongReference.
     *
     * @return a string representation
     */
    @NotNull
    @Override
    public String toString() {
        try {
            return bytes == null ? "bytes is null" : "value: " + getValue() + ", value2: " + getValue2();
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Gets the second long value from the BinaryTwoLongReference.
     *
     * @return the second long value.
     * @throws IllegalStateException if an error occurs while reading the value.
     */
    @Override
    public long getValue2()
            throws IllegalStateException {
        try {
            return bytes.readLong(offset + Long.BYTES);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Sets the second 64-bit long value in the BytesStore.
     *
     * @param value the second 64-bit long value to set
     * @throws IllegalStateException    if closed
     */
    @Override
    public void setValue2(long value)
            throws IllegalStateException {
        try {
            bytes.writeLong(offset + Long.BYTES, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Retrieves the second 64-bit long value using volatile memory semantics.
     *
     * @return the second 64-bit long value
     * @throws IllegalStateException      if closed
     */
    @Override
    public long getVolatileValue2()
            throws IllegalStateException {
        try {
            return bytes.readVolatileLong(offset + Long.BYTES);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Sets the second 64-bit long value using volatile memory semantics.
     *
     * @param value the second 64-bit long value to set
     * @throws IllegalStateException    if closed
     */
    @Override
    public void setVolatileValue2(long value)
            throws IllegalStateException {
        try {
            bytes.writeVolatileLong(offset + Long.BYTES, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Sets the second 64-bit long value using ordered or lazy set memory semantics.
     *
     * @param value the second 64-bit long value to set
     * @throws IllegalStateException    if closed
     */
    @Override
    public void setOrderedValue2(long value)
            throws IllegalStateException {
        try {
            bytes.writeOrderedLong(offset + Long.BYTES, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Adds a delta to the second 64-bit long value and returns the result.
     *
     * @param delta the value to add
     * @return the resulting second 64-bit long value
     * @throws IllegalStateException      if closed
     */
    @Override
    public long addValue2(long delta)
            throws IllegalStateException {
        try {
            return bytes.addAndGetLong(offset + Long.BYTES, delta);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Atomically adds a delta to the second 64-bit long value and returns the result.
     *
     * @param delta the value to add
     * @return the resulting second 64-bit long value
     * @throws IllegalStateException      if closed
     */
    @Override
    public long addAtomicValue2(long delta)
            throws IllegalStateException {
        try {
            return addValue2(delta);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }

    /**
     * Atomically sets the second 64-bit long value to the given updated value if the current value is
     * equal to the expected value.
     *
     * @param expected the expected second 64-bit long value
     * @param value    the new second 64-bit long value
     * @return true if successful, false otherwise
     * @throws IllegalStateException    if closed
     */
    @Override
    public boolean compareAndSwapValue2(long expected, long value)
            throws IllegalStateException {
        try {
            return bytes.compareAndSwapLong(offset + Long.BYTES, expected, value);
        } catch (NullPointerException e) {
            throwExceptionIfClosed();
            throw e;
        }
    }
}
