/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

@SuppressWarnings({"rawtypes", "unchecked"})
public class UncheckedLongReference extends AbstractCloseable implements LongReference, ReferenceOwner {
    private long address;
    private Unsafe unsafe;
    private BytesStore bytes;

    @NotNull
    public static LongReference create(BytesStore bytesStore, long offset, int size) {
        @NotNull LongReference ref = Jvm.isDebug() ? new BinaryLongReference() : new UncheckedLongReference();
        ref.bytesStore(bytesStore, offset, size);
        return ref;
    }

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        throwExceptionIfClosed();
        if (length != maxSize()) throw new IllegalArgumentException();
        address = bytes.addressForRead(offset);
        if (this.bytes != bytes) {
            if (this.bytes != null)
                this.bytes.release(this);
            this.bytes = bytes;
            bytes.reserve(this);
        }
        unsafe = UnsafeMemory.UNSAFE;
    }

    @NotNull
    @Override
    public BytesStore bytesStore() {
        throw new UnsupportedOperationException();
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
    public String toString() {
        return address == 0 ? "addressForRead is 0" : "value: " + getValue();
    }

    @Override
    public long getValue() {
        throwExceptionIfClosed();
        return unsafe.getLong(address);
    }

    @Override
    public void setValue(long value) {
        throwExceptionIfClosed();
        unsafe.putLong(address, value);
    }

    @Override
    public long getVolatileValue() {
        throwExceptionIfClosed();
        return unsafe.getLongVolatile(null, address);
    }

    @Override
    public void setVolatileValue(long value) {
        throwExceptionIfClosed();
        unsafe.putLongVolatile(null, address, value);
    }

    @Override
    public void setOrderedValue(long value) {
        throwExceptionIfClosed();
        unsafe.putOrderedLong(null, address, value);
    }

    @Override
    public long addValue(long delta) {
        throwExceptionIfClosed();
        return unsafe.getAndAddLong(null, address, delta) + delta;
    }

    @Override
    public long addAtomicValue(long delta) {
        throwExceptionIfClosed();
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        throwExceptionIfClosed();
        return unsafe.compareAndSwapLong(null, address, expected, value);
    }

    @Override
    protected void performClose() {
        this.bytes.release(this);
    }

    @Override
    protected boolean threadSafetyCheck() {
        return true;
    }
}