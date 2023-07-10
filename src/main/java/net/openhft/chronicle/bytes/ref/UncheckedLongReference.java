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
 * Provides a reference to a 64-bit long value that is not subject to bound checks.
 *
 * <p>The {@code UncheckedLongReference} class provides low-level access to a referenced long
 * value, optimizing for performance by bypassing safety checks for bounds and state.</p>
 *
 * <p>It's recommended to use this class with caution as it can result in undefined behavior if
 * misused. Typically, this class is used in scenarios where performance is critical and the
 * application guarantees safety through other means.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UncheckedLongReference extends UnsafeCloseable implements LongReference, ReferenceOwner {

    private BytesStore bytes;

    /**
     * Creates a new instance of LongReference backed by the given BytesStore.
     *
     * @param bytesStore the BytesStore to back this reference.
     * @param offset     the offset of the long value within the BytesStore.
     * @param size       the number of bytes to use (should be 8 for a long).
     * @return a LongReference instance.
     * @throws IllegalStateException    if released
     */
    @NotNull
    public static LongReference create(@NotNull BytesStore bytesStore, @NonNegative long offset, @NonNegative int size)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IllegalStateException {
        @NotNull LongReference ref = Jvm.isDebug() ? new BinaryLongReference() : new UncheckedLongReference();
        ref.bytesStore(bytesStore, offset, size);
        return ref;
    }

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