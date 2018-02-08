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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.util.WeakReferenceCleaner;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class acts as a Binary 32-bit in values. c.f. TextIntReference
 */
public class BinaryIntReference implements IntValue, Byteable {
    @Nullable
    private BytesStore bytes;
    private long offset;

    private final StoreRef ref = new StoreRef();

    public BinaryIntReference()
    {
        WeakReferenceCleaner.newCleaner(this, ref::clean);
    }


    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != maxSize())
            throw new IllegalArgumentException();

        acceptNewBytesStore(bytes);
        this.offset = offset;
    }

    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return 4;
    }

    @NotNull
    public String toString() {
        return bytes == null ? "bytes is null" : "value: " + getValue();
    }

    @Override
    public int getValue() {
        return bytes.readInt(offset);
    }

    @Override
    public void setValue(int value) {
        bytes.writeInt(offset, value);
    }

    @Override
    public int getVolatileValue() {
        return bytes.readVolatileInt(offset);
    }

    @Override
    public void setOrderedValue(int value) {
        bytes.writeOrderedInt(offset, value);
    }

    @Override
    public int addValue(int delta) {
        return bytes.addAndGetInt(offset, delta);
    }

    @Override
    public int addAtomicValue(int delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(int expected, int value) {
        return bytes.compareAndSwapInt(offset, expected, value);
    }

    private void acceptNewBytesStore(final BytesStore bytes) {
        if (this.bytes != null) {
            this.bytes.release();
        }
        this.bytes = bytes.bytesStore();
        ref.b = this.bytes;
        this.bytes.reserve();
    }
}
