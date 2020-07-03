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
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.io.UnsafeCloseable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"rawtypes", "unchecked"})
public class UncheckedLongReference extends UnsafeCloseable implements LongReference, ReferenceOwner {
    private BytesStore bytes;

    @NotNull
    public static LongReference create(BytesStore bytesStore, long offset, int size) {
        @NotNull LongReference ref = Jvm.isDebug() ? new BinaryLongReference() : new UncheckedLongReference();
        ref.bytesStore(bytesStore, offset, size);
        return ref;
    }

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
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
    public String toString() {
        return address == 0 ? "addressForRead is 0" : "value: " + getValue();
    }

    @Override
    public long getValue() {
        return getLong();
    }

    @Override
    public void setValue(long value) {
        setLong(value);
    }

    @Override
    public long getVolatileValue() {
        return getVolatileLong();
    }

    @Override
    public void setVolatileValue(long value) {
        setVolatileLong(value);
    }

    @Override
    public long getVolatileValue(long closedValue) {
        return getVolatileLong(closedValue);
    }

    @Override
    public void setOrderedValue(long value) {
        setOrderedLong(value);
    }

    @Override
    public long addValue(long delta) {
        return addLong(delta);
    }

    @Override
    public long addAtomicValue(long delta) {
        return addAtomicLong(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        return compareAndSwapLong(expected, value);
    }

    @Override
    protected void performClose() {
        if (this.bytes != null)
            this.bytes.release(this);
        this.bytes = null;
        super.performClose();
    }
}