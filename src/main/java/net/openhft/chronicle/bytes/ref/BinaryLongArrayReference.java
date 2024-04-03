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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;
import static net.openhft.chronicle.bytes.ref.BinaryLongReference.LONG_NOT_COMPLETE;

/**
 * Represents a binary array of 64-bit long values backed by a {@link BytesStore}.
 * <p>
 * This class provides various operations to access and manipulate an array of 64-bit long integers in binary form.
 * The long integers are stored in a BytesStore, and this class provides methods for reading and writing values at specific indices.
 * <p>
 * Example usage:
 * <pre>
 * BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(32);
 * BinaryLongArrayReference ref = new BinaryLongArrayReference(4); // Creates an array with 4 longs
 * ref.bytesStore(bytesStore, 0, ref.maxSize());
 * ref.setValueAt(0, 1234567890L);
 * long value = ref.getValueAt(0);
 * </pre>
 * <p>
 * Note: This class is not thread-safe. External synchronization may be necessary if instances
 * are shared between threads.
 *
 * @see BytesStore
 * @see BinaryLongReference
 */
@SuppressWarnings("rawtypes")
public class BinaryLongArrayReference extends AbstractReference implements ByteableLongArrayValues, BytesMarshallable {
    public static final int SHIFT = 3;
    private static final long CAPACITY = 0;
    private static final long USED = CAPACITY + Long.BYTES;
    private static final long VALUES = USED + Long.BYTES;
    private static final int MAX_TO_STRING = 1024;
    @Nullable
    private static Set<WeakReference<BinaryLongArrayReference>> binaryLongArrayReferences = null;
    private long length;

    /**
     * Constructs a BinaryLongArrayReference with a default capacity of 0.
     */
    public BinaryLongArrayReference() {
        this(0);
    }

    /**
     * Constructs a BinaryLongArrayReference with the specified default capacity.
     *
     * @param defaultCapacity the initial capacity of the long array in number of elements.
     */
    @SuppressWarnings("this-escape")
    public BinaryLongArrayReference(@NonNegative long defaultCapacity) {
        this.length = (defaultCapacity << SHIFT) + VALUES;
        singleThreadedCheckDisabled(true);
    }

    /**
     * Enables collection of BinaryLongArrayReference instances.
     * <p>
     * This method is used for debugging and monitoring. It should not be used in production environments.
     */
    public static void startCollecting() {
        binaryLongArrayReferences = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * Sets all values in the BinaryLongArrayReference instances to the "not complete" state.
     * <p>
     * This method is used for debugging and monitoring. It should not be used in production environments.
     *
     * @throws BufferOverflowException If the bytes cannot be written.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public static void forceAllToNotCompleteState()
            throws IllegalStateException, BufferOverflowException {
        if (binaryLongArrayReferences == null)
            return;

        for (WeakReference<BinaryLongArrayReference> x : binaryLongArrayReferences) {
            @Nullable BinaryLongArrayReference binaryLongReference = x.get();
            if (binaryLongReference != null) {
                binaryLongReference.setValueAt(0, LONG_NOT_COMPLETE);
            }
        }
        binaryLongArrayReferences = null;
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

    /**
     * Writes to the provided Bytes object with the given capacity.
     * The method asserts that the write position is correctly aligned,
     * then writes the capacity, followed by a long value of 0 (representing the "used" space),
     * and finally zeros out the subsequent space defined by the capacity.
     *
     * @param bytes    the Bytes object to write to.
     * @param capacity the capacity to be written and used for subsequent zeroing.
     * @throws BufferOverflowException  If there is insufficient space in the buffer.
     * @throws IllegalArgumentException If arguments violate precondition constraints.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public static void write(@NotNull Bytes<?> bytes, @NonNegative long capacity)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        assert (bytes.writePosition() & 0x7) == 0;

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        long start = bytes.writePosition();
        bytes.zeroOut(start, start + (capacity << SHIFT));
        bytes.writeSkip(capacity << SHIFT);
    }

    /**
     * Lazily writes to the provided Bytes object with the given capacity.
     * Unlike the write method, this method does not zero out the subsequent space.
     * It just updates the write position after writing the capacity and "used" space.
     *
     * @param bytes    the Bytes object to write to.
     * @param capacity the capacity to be written.
     * @throws BufferOverflowException If there is insufficient space in the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public static void lazyWrite(@NotNull Bytes<?> bytes, @NonNegative long capacity)
            throws BufferOverflowException, IllegalStateException {
        assert (bytes.writePosition() & 0x7) == 0;

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        bytes.writeSkip(capacity << SHIFT);
    }

    /**
     * Returns the capacity from the BytesStore object and adding the fixed values size to get a length.
     * It asserts that the capacity is greater than 0.
     *
     * @param bytes  the BytesStore object to read from.
     * @param offset the offset at which to start reading.
     * @return the calculated peak length.
     * @throws BufferUnderflowException If there is not enough remaining data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public static long peakLength(@NotNull BytesStore bytes, @NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        long capacity = bytes.readLong(offset + CAPACITY);
        assert capacity > 0 : "capacity too small " + capacity;
        return (capacity << SHIFT) + VALUES;
    }

    /**
     * Returns the capacity from the BytesStore object, adding the fixed values size to get a length
     * If the read capacity is 0, the method writes the capacityHint at the offset and
     * updates the capacity with the capacityHint.
     * It asserts that the capacity is greater than 0.
     *
     * @param bytes        the BytesStore object to read from.
     * @param offset       the offset at which to start reading.
     * @param capacityHint the capacity to be used if the initial capacity is 0.
     * @return the calculated peak length.
     * @throws BufferUnderflowException If there is not enough remaining data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public static long peakLength(@NotNull BytesStore bytes, @NonNegative long offset, long capacityHint)
            throws BufferUnderflowException, IllegalStateException {
        long capacity = bytes.readLong(offset + CAPACITY);
        if (capacity == 0) {
            bytes.writeLong(offset + CAPACITY, capacityHint);
            capacity = capacityHint;
        }
        assert capacity > 0 : "capacity too small " + capacity;
        return (capacity << SHIFT) + VALUES;
    }

    @Override
    public long getCapacity()
            throws IllegalStateException {
        throwExceptionIfClosed();

        if (bytes == null)
            return (length - VALUES) >>> SHIFT;
        return bytes.readVolatileLong(offset + CAPACITY);
    }

    @Override
    public long getUsed()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileLong(offset + USED);
    }

    @Override
    public void setMaxUsed(long usedAtLeast)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeMaxLong(offset + USED, usedAtLeast);
    }

    @Override
    public void setUsed(long used) throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeVolatileLong(offset + USED, used);
    }

    @Override
    public long getValueAt(@NonNegative long index)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        return bytes.readLong(VALUES + offset + (index << SHIFT));
    }

    @Override
    public void setValueAt(@NonNegative long index, long value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosedInSetter();

        bytes.writeLong(VALUES + offset + (index << SHIFT), value);
    }

    @Override
    public long getVolatileValueAt(long index)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        return bytes.readVolatileLong(VALUES + offset + (index << SHIFT));
    }

    @Override
    public void bindValueAt(@NonNegative long index, @NotNull LongValue value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        ((BinaryLongReference) value).bytesStore(bytes, VALUES + offset + (index << SHIFT), 8);
    }

    @Override
    public void setOrderedValueAt(@NonNegative long index, long value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosedInSetter();

        bytes.writeOrderedLong(VALUES + offset + (index << SHIFT), value);
    }

    @Override
    public void bytesStore(@NotNull BytesStore bytes, @NonNegative long offset, @NonNegative long length)
            throws IllegalArgumentException, IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        BytesStore bytesStore = bytes.bytesStore();
        try {
            long peakLength = peakLength(bytesStore, offset, (length - VALUES) >>> 3);
            if (length != peakLength)
                throw new IllegalArgumentException(length + " != " + peakLength);
        } catch (BufferUnderflowException e) {
            throw new DecoratedBufferOverflowException(e.toString());
        }

        if (bytes instanceof HexDumpBytes) {
            offset &= MASK;
        }
        assert (offset & 7) == 0 : "offset=" + offset;
        super.bytesStore(bytesStore, (offset + 7) & ~7, length);
        this.length = length;
    }

    @Override
    public void readMarshallable(BytesIn<?> bytes)
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
        bytesStore((Bytes) bytes, position, len);
    }

    @Override
    public void writeMarshallable(BytesOut<?> bytes)
            throws IllegalStateException, BufferOverflowException, BufferUnderflowException {
        boolean retainsComments = bytes.retainedHexDumpDescription();
        if (retainsComments)
            bytes.writeHexDumpDescription("BinaryLongArrayReference");
        BytesStore bytesStore = bytesStore();
        if (bytesStore == null) {
            long capacity = getCapacity();
            if (retainsComments)
                bytes.writeHexDumpDescription("capacity");
            bytes.writeLong(capacity);
            if (retainsComments)
                bytes.writeHexDumpDescription("used");
            bytes.writeLong(0);
            if (retainsComments)
                bytes.writeHexDumpDescription("values");
            bytes.writeSkip(capacity << SHIFT);
        } else {
            bytes.write(bytesStore, offset, length);
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
        if (bytes == null)
            return "not set";
        @NotNull StringBuilder sb = new StringBuilder();
        sb.append("used: ");
        try {
            long used = getUsed();
            sb.append(used);
            sb.append(", value: ");
            @NotNull String sep = "";
            int i;
            int max = (int) Math.min(used, Math.min(getCapacity(), MAX_TO_STRING));
            for (i = 0; i < max; i++) {
                long valueAt = getValueAt(i);
                sb.append(sep).append(valueAt);
                sep = ", ";
            }
            if (i < getCapacity())
                sb.append(" ...");

        } catch (Throwable e) {
            sb.append(" ").append(e);
        }
        return sb.toString();
    }

    @Override
    public long sizeInBytes(@NonNegative long capacity)
            throws IllegalStateException {
        throwExceptionIfClosed();

        return (capacity << SHIFT) + VALUES;
    }

    @Override
    public ByteableLongArrayValues capacity(@NonNegative long arrayLength)
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
    public boolean compareAndSet(@NonNegative long index, long expected, long value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (value == LONG_NOT_COMPLETE && binaryLongArrayReferences != null)
            binaryLongArrayReferences.add(new WeakReference<>(this));
        return bytes.compareAndSwapLong(VALUES + offset + (index << SHIFT), expected, value);
    }
}

