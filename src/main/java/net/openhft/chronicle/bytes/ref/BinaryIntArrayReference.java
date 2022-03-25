/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;
import static net.openhft.chronicle.bytes.ref.BinaryIntReference.INT_NOT_COMPLETE;

/**
 * This class acts a Binary array of 64-bit values. c.f. TextLongArrayReference
 */
@SuppressWarnings("rawtypes")
public class BinaryIntArrayReference extends AbstractReference implements ByteableIntArrayValues, BytesMarshallable {
    public static final int SHIFT = 2;
    private static final long CAPACITY = 0;
    private static final long USED = CAPACITY + Long.BYTES;
    private static final long VALUES = USED + Long.BYTES;
    private static final int MAX_TO_STRING = 1024;
    @Nullable
    private static Set<WeakReference<BinaryIntArrayReference>> binaryIntArrayReferences = null;
    private long length;

    public BinaryIntArrayReference() {
        this(0);
    }

    public BinaryIntArrayReference(long defaultCapacity) {
        this.length = (defaultCapacity << SHIFT) + VALUES;
    }

    public static void startCollecting() {
        binaryIntArrayReferences = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public static void forceAllToNotCompleteState()
            throws IllegalStateException, BufferOverflowException {
        if (binaryIntArrayReferences == null)
            return;

        for (WeakReference<BinaryIntArrayReference> x : binaryIntArrayReferences) {
            @Nullable BinaryIntArrayReference binaryLongReference = x.get();
            if (binaryLongReference != null) {
                binaryLongReference.setValueAt(0, INT_NOT_COMPLETE);
            }
        }

        binaryIntArrayReferences = null;
    }

    public static void write(@NotNull Bytes bytes, long capacity)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        assert (bytes.writePosition() & 0x7) == 0;

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        long start = bytes.writePosition();
        bytes.zeroOut(start, start + (capacity << SHIFT));
        bytes.writeSkip(capacity << SHIFT);
    }

    public static void lazyWrite(@NotNull Bytes bytes, long capacity)
            throws BufferOverflowException, IllegalStateException {
        assert (bytes.writePosition() & 0x7) == 0;

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        bytes.writeSkip(capacity << SHIFT);
    }

    public static long peakLength(@NotNull BytesStore bytes, long offset)
            throws BufferUnderflowException, IllegalStateException {
        final long capacity = bytes.readLong(offset + CAPACITY);
        assert capacity > 0 : "capacity too small " + capacity;
        return (capacity << SHIFT) + VALUES;
    }

    @Override
    protected void acceptNewBytesStore(@NotNull final BytesStore bytes)
            throws IllegalStateException {
        if (this.bytes != null) {
            this.bytes.release(this);
        }
        this.bytes = bytes;
        this.bytes.reserve(this);
    }

    @Override
    public long getCapacity()
            throws IllegalStateException {
        throwExceptionIfClosed();

        return (length - VALUES) >>> SHIFT;
    }

    @Override
    public long getUsed()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(offset + USED);
    }

    @Override
    public void setMaxUsed(long usedAtLeast)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeMaxLong(offset + USED, usedAtLeast);
    }

    @Override
    public int getValueAt(long index)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readInt(VALUES + offset + (index << SHIFT));
    }

    @Override
    public void setValueAt(long index, int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeInt(VALUES + offset + (index << SHIFT), value);
    }

    @Override
    public int getVolatileValueAt(long index)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(VALUES + offset + (index << SHIFT));
    }

    @Override
    public void bindValueAt(long index, @NotNull IntValue value)
            throws IllegalStateException, BufferOverflowException, IllegalArgumentException {
        throwExceptionIfClosed();

        ((BinaryIntReference) value).bytesStore(bytes, VALUES + offset + (index << SHIFT), 8);
    }

    @Override
    public void setOrderedValueAt(long index, int value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosedInSetter();

        bytes.writeOrderedInt(VALUES + offset + (index << SHIFT), value);
    }

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length)
            throws IllegalArgumentException, IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        long peakLength;
        try {
            peakLength = peakLength(bytes, offset);
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
        if (length != peakLength)
            throw new IllegalArgumentException(length + " != " + peakLength);
        if (bytes instanceof HexDumpBytes) {
            offset &= MASK;
        }
        assert (offset & 7) == 0 : "offset=" + offset;
        super.bytesStore(bytes, (offset + 7) & ~7, length);
        this.length = length;
    }

    @Override
    public void readMarshallable(BytesIn bytes)
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        long position = bytes.readPosition();
        long capacity = bytes.readLong();
        long used = bytes.readLong();
        if (capacity < 0 || capacity > bytes.readRemaining() >> SHIFT)
            throw new IORuntimeException("Corrupt used capacity");

        if (used < 0 || used > capacity)
            throw new IORuntimeException("Corrupt used value");

        bytes.readSkip(capacity << SHIFT);
        long len = bytes.readPosition() - position;
        try {
            bytesStore((Bytes) bytes, position, len);
        } catch (IllegalArgumentException | BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void writeMarshallable(BytesOut bytes)
            throws IllegalStateException, BufferOverflowException {
        final boolean retainsComments = bytes.retainsComments();
        if (retainsComments)
            bytes.comment("BinaryIntArrayReference");
        BytesStore bytesStore = bytesStore();
        if (bytesStore == null) {
            long capacity = getCapacity();
            if (retainsComments)
                bytes.comment("capacity");
            bytes.writeLong(capacity);
            if (retainsComments)
                bytes.comment("used");
            bytes.writeLong(0);
            if (retainsComments)
                bytes.comment("values");
            bytes.writeSkip(capacity << SHIFT);
        } else {
            try {
                bytes.write(bytesStore, offset, length);
            } catch (BufferUnderflowException | IllegalArgumentException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Override
    public boolean isNull()
            throws IllegalStateException {
        throwExceptionIfClosed();

        return bytes == null;
    }

    @Override
    public void reset()
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

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
    @Override
    public String toString() {
        if (bytes == null) {
            return "not set";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("used: ");
        try {
            final long used = getUsed();
            sb.append(used);
            sb.append(", value: ");
            appendContents(sb, used);
            return sb.toString();
        } catch (IllegalStateException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    private void appendContents(@NotNull final StringBuilder sb,
                                final long used) {
        String sep = "";
        try {
            int i;
            int max = (int) Math.min(used, Math.min(getCapacity(), MAX_TO_STRING));
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
    }

    @Override
    public long sizeInBytes(long capacity)
            throws IllegalStateException {
        throwExceptionIfClosed();

        return (capacity << SHIFT) + VALUES;
    }

    @Override
    public ByteableIntArrayValues capacity(long arrayLength)
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

        BytesStore bytesStore = bytesStore();
        long len = sizeInBytes(arrayLength);
        if (bytesStore == null) {
            this.length = len;
        } else {
            assert this.length == len;
        }
        return this;
    }

    @Override
    public boolean compareAndSet(long index, int expected, int value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (value == INT_NOT_COMPLETE && binaryIntArrayReferences != null)
            binaryIntArrayReferences.add(new WeakReference<>(this));
        return bytes.compareAndSwapInt(VALUES + offset + (index << SHIFT), expected, value);
    }
}

