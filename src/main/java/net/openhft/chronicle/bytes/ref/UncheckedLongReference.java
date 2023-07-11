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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.io.UnsafeCloseable;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Represents a reference to a long value stored in bytes. This reference does not perform bounds checks
 * for performance reasons and should be used carefully.
 * <p>
 * This class is useful when operating with off-heap memory or memory-mapped file storage, where it is critical
 * to avoid unnecessary bounds checking for performance reasons.
 * <p>
 * This class extends {@link UnsafeCloseable} to provide functionality for safely closing resources.
 *
 * @implSpec Implementations must ensure that all methods are thread-safe.
 * @implNote When Jvm debugging is enabled, an instance of {@link BinaryLongReference} is returned by
 * {@link #create(BytesStore, long, int)} for additional safety. Otherwise, an instance of {@link UncheckedLongReference}
 * is returned for performance.
 * @see LongReference
 * @see ReferenceOwner
 * @see UnsafeCloseable
 * @see BytesStore
 * @see BinaryLongReference
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UncheckedLongReference extends UnsafeCloseable implements LongReference, ReferenceOwner {

    private BytesStore bytes;

    /**
     * Creates an {@code UncheckedLongReference} or {@code BinaryLongReference} depending on JVM debug status.
     *
     * @param bytesStore the {@code BytesStore} to be used for storing the long value.
     * @param offset     the offset at which the long value is stored.
     * @param size       the size of the long value in bytes.
     * @return a new {@code LongReference} instance.
     * @throws IllegalArgumentException if the size does not match the expected size.
     * @throws BufferOverflowException  if the operation exceeds the bounds of the buffer.
     * @throws BufferUnderflowException if the operation exceeds the bounds of the buffer.
     * @throws IllegalStateException    if the object is not in a valid state for the operation.
     */
    @NotNull
    public static LongReference create(@NotNull BytesStore bytesStore, @NonNegative long offset, @NonNegative int size)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IllegalStateException {
        @NotNull LongReference ref = Jvm.isDebug() ? new BinaryLongReference() : new UncheckedLongReference();
        ref.bytesStore(bytesStore, offset, size);
        return ref;
    }

    /**
     * Stores the bytes of the long value reference.
     *
     * @param bytes  the {@code BytesStore} containing the bytes.
     * @param offset the offset at which the long value is stored.
     * @param length the length of the bytes in the {@code BytesStore}.
     * @throws IllegalStateException    if the object is not in a valid state for the operation.
     * @throws IllegalArgumentException if the length does not match the expected size.
     * @throws BufferUnderflowException if the operation exceeds the bounds of the buffer.
     */
    @Override
    public void bytesStore(@NotNull BytesStore bytes, @NonNegative long offset, @NonNegative long length)
            throws IllegalStateException, IllegalArgumentException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        if (length != maxSize()) throw new IllegalArgumentException();
        if (this.bytes != bytes) {
            if (this.bytes != null)
                this.bytes.release(this);
            this.bytes = bytes;
            bytes.reserve(this);
        }
        address(bytes.addressForRead(offset));
    }

    @NotNull
    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return address;
    }

    @Override
    public long maxSize() {
        return 8;
    }

    @NotNull
    @Override
    public String toString() {
        if (address == 0)
            return "addressForRead is 0";
        try {
            return "value: " + getValue();
        } catch (Throwable e) {
            return "value: " + e;
        }
    }

    @Override
    public long getValue()
            throws IllegalStateException {
        return getLong();
    }

    @Override
    public void setValue(long value)
            throws IllegalStateException {
        setLong(value);
    }

    @Override
    public long getVolatileValue()
            throws IllegalStateException {
        return getVolatileLong();
    }

    @Override
    public void setVolatileValue(long value)
            throws IllegalStateException {
        setVolatileLong(value);
    }

    @Override
    public long getVolatileValue(long closedValue) {
        return getVolatileLong(closedValue);
    }

    @Override
    public void setOrderedValue(long value)
            throws IllegalStateException {
        setOrderedLong(value);
    }

    @Override
    public long addValue(long delta)
            throws IllegalStateException {
        return addLong(delta);
    }

    @Override
    public long addAtomicValue(long delta)
            throws IllegalStateException {
        return addAtomicLong(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value)
            throws IllegalStateException {
        return compareAndSwapLong(expected, value);
    }

    @Override
    protected void performClose()
            throws IllegalStateException {
        if (this.bytes != null)
            this.bytes.release(this);
        this.bytes = null;
        super.performClose();
    }
}