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
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.io.UnsafeCloseable;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * A class for managing references to long values stored in a {@link BytesStore} without performing bounds checking.
 * This class is optimized for high performance in scenarios involving off-heap memory or memory-mapped files where bounds checks could impair performance.
 * <p>
 * This class is thread-safe provided that external synchronization is applied. It uses different implementations based on the JVM's debug status:
 * in debug mode, it uses {@link BinaryLongReference} for safety checks; otherwise, it uses {@link UncheckedLongReference} for better performance.
 * </p>
 * @see LongReference
 * @see ReferenceOwner
 * @see UnsafeCloseable
 * @see BytesStore
 * @see BinaryLongReference
 */
@SuppressWarnings("rawtypes")
public class UncheckedLongReference extends UnsafeCloseable implements LongReference, ReferenceOwner {

    private BytesStore bytes;

    /**
     * Factory method to create a {@code UncheckedLongReference} or {@code BinaryLongReference} based on the JVM's debug status.
     * This method initializes the reference with a specific {@code BytesStore}, offset, and size.
     *
     * @param bytesStore The {@code BytesStore} used to store the long value.
     * @param offset     The offset within the {@code BytesStore} where the long value starts.
     * @param size       The size in bytes of the long value (expected to be 8 for a long).
     * @return a new {@code LongReference} instance appropriate for the JVM debug status.
     * @throws IllegalArgumentException If the provided size does not match the expected size of a long.
     * @throws BufferOverflowException  If the offset and size exceed the bounds of the {@code BytesStore}.
     * @throws BufferUnderflowException If the offset is negative or not within the bounds of the {@code BytesStore}.
     * @throws ClosedIllegalStateException    If the {@code BytesStore} has already been closed.
     * @throws ThreadingIllegalStateException If this method is called from multiple threads without proper synchronization.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public static LongReference create(@NotNull BytesStore bytesStore, @NonNegative long offset, @NonNegative int size)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IllegalStateException {
        @NotNull LongReference ref = Jvm.isDebug() ? new BinaryLongReference() : new UncheckedLongReference();
        ref.bytesStore(bytesStore, offset, size);
        return ref;
    }

    /**
     * Associates this {@code UncheckedLongReference} with a {@code BytesStore}, specifying where the long value is stored and its length.
     *
     * @param bytes  the {@code BytesStore} containing the long value.
     * @param offset the offset within the {@code BytesStore} where the long value starts.
     * @param length the length in bytes of the long value (expected to be 8).
     * @throws IllegalArgumentException If the specified length does not match the expected size of a long.
     * @throws BufferUnderflowException If the specified length and offset are out of the bounds of the {@code BytesStore}.
     * @throws ClosedIllegalStateException If this reference or the {@code BytesStore} has been closed.
     * @throws ThreadingIllegalStateException If this method is accessed by multiple threads without proper synchronization.
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
