/*
 * Copyright 2016 higherfrequencytrading.com
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
import net.openhft.chronicle.core.annotation.NotNull;
import sun.misc.Unsafe;

@SuppressWarnings({"rawtypes", "unchecked", "restriction"})
public class UncheckedLongReference implements LongReference {
    private long address;
    private Unsafe unsafe;

    @NotNull
    public static LongReference create(BytesStore bytesStore, long offset, int size) {
        @NotNull LongReference ref = Jvm.isDebug() ? new BinaryLongReference() : new UncheckedLongReference();
        ref.bytesStore(bytesStore, offset, size);
        return ref;
    }

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != maxSize()) throw new IllegalArgumentException();
        address = bytes.addressForRead(offset);
        bytes.reserve();
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
        return unsafe.getLong(address);
    }

    @Override
    public void setValue(long value) {
        unsafe.putLong(address, value);
    }

    @Override
    public long getVolatileValue() {
        return unsafe.getLong(address);
    }

    @Override
    public void setOrderedValue(long value) {
        unsafe.putOrderedLong(null, address, value);
    }

    @Override
    public long addValue(long delta) {
        return unsafe.getAndAddLong(null, address, delta) + delta;
    }

    @Override
    public long addAtomicValue(long delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        return unsafe.compareAndSwapLong(null, address, expected, value);
    }
}