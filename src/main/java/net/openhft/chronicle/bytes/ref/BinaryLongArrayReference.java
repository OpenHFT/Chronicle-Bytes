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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.util.WeakReferenceCleaner;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static net.openhft.chronicle.bytes.ref.BinaryLongReference.LONG_NOT_COMPLETE;

/**
 * This class acts a Binary array of 64-bit values. c.f. TextLongArrayReference
 */
public class BinaryLongArrayReference implements ByteableLongArrayValues {
    private static final long CAPACITY = 0;
    private static final long USED = CAPACITY + Long.BYTES;
    private static final long VALUES = USED + Long.BYTES;
    private static final int MAX_TO_STRING = 1024;
    @Nullable
    private static Set<WeakReference<BinaryLongArrayReference>> binaryLongArrayReferences = null;
    @Nullable
    private BytesStore bytes;
    private long offset;
    private long length = VALUES;

    private final StoreRef ref = new StoreRef();

    public BinaryLongArrayReference()
    {
        WeakReferenceCleaner.newCleaner(this, ref::clean);
    }

    public static void startCollecting() {
        binaryLongArrayReferences = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public static void forceAllToNotCompleteState() {
        binaryLongArrayReferences.forEach(x -> {
            @Nullable BinaryLongArrayReference binaryLongReference = x.get();
            if (binaryLongReference != null) {
                binaryLongReference.setValueAt(0, LONG_NOT_COMPLETE);
            }
        });
        binaryLongArrayReferences = null;
    }

    public static void write(@NotNull Bytes bytes, long capacity) throws BufferOverflowException, IllegalArgumentException {
        assert (bytes.writePosition() & 0x7) == 0;

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        long start = bytes.writePosition();
        bytes.zeroOut(start, start + (capacity << 3));
        bytes.writeSkip(capacity << 3);
    }

    public static void lazyWrite(@NotNull Bytes bytes, long capacity) throws BufferOverflowException {
        assert (bytes.writePosition() & 0x7) == 0;

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        bytes.writeSkip(capacity << 3);
    }

    public static long peakLength(@NotNull BytesStore bytes, long offset) throws BufferUnderflowException {
        final long capacity = bytes.readLong(offset);
        assert capacity > 0 : "capacity too small";
        return (capacity << 3) + VALUES;
    }

    @Override
    public long getCapacity() {
        return (length - VALUES) >>> 3;
    }

    @Override
    public long getUsed() {
        return bytes.readVolatileLong(offset + USED);
    }

    @Override
    public void setMaxUsed(long usedAtLeast) {
        bytes.writeMaxLong(offset + USED, usedAtLeast);
    }

    @Override
    public long getValueAt(long index) throws BufferUnderflowException {
        return bytes.readLong(VALUES + offset + (index << 3));
    }

    @Override
    public void setValueAt(long index, long value) throws IllegalArgumentException, BufferOverflowException {
        bytes.writeLong(VALUES + offset + (index << 3), value);
    }

    @Override
    public long getVolatileValueAt(long index) throws BufferUnderflowException {
        return bytes.readVolatileLong(VALUES + offset + (index << 3));
    }

    @Override
    public void bindValueAt(int index, @NotNull LongValue value) {
        ((BinaryLongReference) value).bytesStore(bytes, VALUES + offset + (index << 3), 8);
    }

    @Override
    public void setOrderedValueAt(long index, long value) throws IllegalArgumentException, BufferOverflowException {
        bytes.writeOrderedLong(VALUES + offset + (index << 3), value);
    }

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) throws BufferUnderflowException, IllegalArgumentException {
        if (length != peakLength(bytes, offset))
            throw new IllegalArgumentException(length + " != " + peakLength(bytes, offset));
        acceptNewBytesStore(bytes);

        assert (offset & 7) == 0 : "offset=" + offset;
        this.offset = (offset + 7) & ~7;
        this.length = length;
    }

    @Override
    public boolean isNull() {
        return bytes == null;
    }

    @Override
    public void reset() {
        bytes = null;
        offset = 0;
        length = 0;
    }

    @Nullable
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
        return length;
    }

    @NotNull
    public String toString() {
        if (bytes == null)
            return "not set";
        @NotNull StringBuilder sb = new StringBuilder();
        sb.append("used: ");
        long used = getUsed();
        sb.append(used);
        sb.append(", value: ");
        @NotNull String sep = "";
        try {
            int i, max = (int) Math.min(used, Math.min(getCapacity(), MAX_TO_STRING));
            for (i = 0; i < max; i++) {
                long valueAt = getValueAt(i);
                sb.append(sep).append(valueAt);
                sep = ", ";
            }
            if (i < getCapacity())
                sb.append(" ...");

        } catch (BufferUnderflowException e) {
            sb.append(" ").append(e);
        }
        return sb.toString();
    }

    @Override
    public long sizeInBytes(long capacity) {
        return (capacity << 3) + VALUES;
    }

    @Override
    public boolean compareAndSet(long index, long expected, long value) throws IllegalArgumentException, BufferOverflowException {
        if (value == LONG_NOT_COMPLETE && binaryLongArrayReferences != null)
            binaryLongArrayReferences.add(new WeakReference<>(this));
        return bytes.compareAndSwapLong(VALUES + offset + (index << 3), expected, value);
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

