/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
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
 * Array of 32-bit values laid out as
 * {@code [capacity][used][values...]} in little-endian order.
 * Each entry is shifted by {@code SHIFT} bytes from {@code VALUES}.
 * <p>Example usage:</p>
 * <pre>
 * BytesStore bytesStore = ...
 * BinaryIntArrayReference arrayRef = new BinaryIntArrayReference();
 * arrayRef.bytesStore(bytesStore, 0, bytesStore.capacity());
 * arrayRef.setOrderedValueAt(5, 12345);
 * int value = arrayRef.getValueAt(5);
 * </pre>
 * <p>
 * Note: This class is not thread-safe. External synchronisation may be
 * required if instances are shared between threads.
 */
@SuppressWarnings({"rawtypes", "deprecation"})
public class BinaryIntArrayReference extends AbstractBinaryArrayReference implements ByteableIntArrayValues, BytesMarshallable {

    public static final int SHIFT = 2;
    public static final long MAX_CAPACITY = (Long.MAX_VALUE - VALUES) >> SHIFT;

    @Nullable
    private static Set<WeakReference<BinaryIntArrayReference>> binaryIntArrayReferences = null;

    /**
     * Default constructor initializes the BinaryIntArrayReference with a default capacity of 0.
     */
    public BinaryIntArrayReference() {
        this(0);
    }

    /**
     * Constructs a new BinaryIntArrayReference with the specified default capacity.
     *
     * @param defaultCapacity the default capacity of the array.
     */
    public BinaryIntArrayReference(long defaultCapacity) {
        super(SHIFT);
        this.length = (defaultCapacity << SHIFT) + VALUES;
    }

    /**
     * Initializes the collection that keeps references to BinaryIntArrayReference instances.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void startCollecting() {
        binaryIntArrayReferences = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * Forces all BinaryIntArrayReferences to a not complete state.
     *
     * @throws BufferOverflowException        If buffer overflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Deprecated(/* to be removed in 2027 */)
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

    /**
     * Initializes the binary data in the provided Bytes object with the given capacity.
     *
     * @param bytes    the Bytes object to be written.
     * @param capacity the capacity to be set.
     * @throws BufferOverflowException        If buffer overflows.
     * @throws IllegalArgumentException       If an illegal argument is encountered.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public static void write(@NotNull Bytes<?> bytes, @NonNegative long capacity)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        assert (bytes.writePosition() & 0x7) == 0;

        checkCapacity(capacity);

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        long start = bytes.writePosition();
        long sizeToSkip = capacity << SHIFT;
        bytes.zeroOut(start, start + sizeToSkip);
        bytes.writeSkip(sizeToSkip);
    }

    private static void checkCapacity(long capacity) {
        if (capacity > MAX_CAPACITY)
            throw new IllegalArgumentException("Capacity " + capacity + " is too large > " + MAX_CAPACITY);
    }

    /**
     * Lazily initializes the binary data in the provided Bytes object with the given capacity.
     *
     * @param bytes    the Bytes object to be written.
     * @param capacity the capacity to be set.
     * @throws BufferOverflowException        If buffer overflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Deprecated(/* to be removed in 2027 */)
    public static void lazyWrite(@NotNull Bytes<?> bytes, @NonNegative long capacity)
            throws BufferOverflowException, IllegalStateException {
        assert (bytes.writePosition() & 0x7) == 0;

        checkCapacity(capacity);

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        long sizeToSkip = capacity << SHIFT;
        bytes.writeSkip(sizeToSkip);
    }

    /**
     * Calculates and returns the peak length from the BytesStore at the given offset.
     *
     * @param bytes  the BytesStore object to read from.
     * @param offset the offset in the BytesStore to start reading from.
     * @return the peak length.
     * @throws BufferUnderflowException       If buffer underflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    public static long peakLength(@NotNull BytesStore<?, ?> bytes, @NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        final long capacity = bytes.readLong(offset + CAPACITY);
        assert capacity > 0 : "capacity too small " + capacity;
        checkCapacity(capacity);
        return (capacity << SHIFT) + VALUES;
    }

    /**
     * Gets the number of used elements in the array.
     *
     * @return the number of used elements.
     * @throws BufferUnderflowException       If buffer underflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public long getUsed()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore.readVolatileInt(offset + USED);
    }

    /**
     * Sets the maximum number of used elements in the array.
     *
     * @param usedAtLeast the number of used elements to be set.
     * @throws BufferUnderflowException       If buffer underflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setMaxUsed(long usedAtLeast)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        bytesStore.writeMaxLong(offset + USED, usedAtLeast);
    }

    /**
     * Gets the value at the specified index.
     *
     * @param index the index to retrieve the value from.
     * @return the value at the specified index.
     * @throws BufferUnderflowException       If buffer underflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public int getValueAt(@NonNegative long index)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore.readInt(VALUES + offset + (index << SHIFT));
    }

    /**
     * Sets the value at the specified index.
     *
     * @param index the index to set the value at.
     * @param value the value to be set.
     * @throws BufferOverflowException        If buffer overflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setValueAt(@NonNegative long index, int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytesStore.writeInt(VALUES + offset + (index << SHIFT), value);
    }

    /**
     * Retrieves the value at the specified index with volatile semantics.
     *
     * @param index the index to retrieve the value from.
     * @return the value at the specified index.
     * @throws BufferUnderflowException       If buffer underflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public int getVolatileValueAt(@NonNegative long index)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore.readVolatileInt(VALUES + offset + (index << SHIFT));
    }

    /**
     * Binds the value at the specified index to the provided IntValue.
     *
     * @param index the index to bind the value at.
     * @param value the IntValue to be bound.
     * @throws BufferOverflowException        If buffer overflows.
     * @throws IllegalArgumentException       If the arguments are invalid.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void bindValueAt(@NonNegative long index, @NotNull IntValue value)
            throws IllegalStateException, BufferOverflowException, IllegalArgumentException {
        throwExceptionIfClosed();

        if (!(value instanceof BinaryIntReference)) {
            throw new IllegalArgumentException("Expected BinaryIntReference but got " + value.getClass().getName());
        }
        BinaryIntReference intRef = (BinaryIntReference) value;
        intRef.bytesStore(bytesStore, VALUES + offset + (index << SHIFT), 8);
    }

    /**
     * Sets the value at the specified index with ordered semantics.
     *
     * @param index the index to set the value at.
     * @param value the value to be set.
     * @throws BufferOverflowException        If buffer overflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void setOrderedValueAt(@NonNegative long index, int value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosedInSetter();

        bytesStore.writeOrderedInt(VALUES + offset + (index << SHIFT), value);
    }

    /**
     * Stores a bytes sequence into the BinaryIntArrayReference.
     *
     * @param bytes  the bytes sequence to store.
     * @param offset the starting position.
     * @param length the length of bytes sequence.
     * @throws IllegalArgumentException If the length does not match the peak length.
     * @throws BufferOverflowException  If buffer overflows.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public void bytesStore(@NotNull BytesStore bytes, @NonNegative long offset, @NonNegative long length)
            throws IllegalArgumentException, IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        long peakLength = peakLength(bytes, offset);
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
    protected void appendContents(@NotNull StringBuilder sb, long used) {
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
            sb.append(' ').append(e);
        }
    }

    /**
     * Calculates the size in bytes of the array with the given capacity.
     *
     * @param capacity the capacity of the array.
     * @return the size in bytes.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public long sizeInBytes(@NonNegative long capacity)
            throws IllegalStateException {
        throwExceptionIfClosed();

        checkCapacity(capacity);
        return (capacity << SHIFT) + VALUES;
    }

    /**
     * Sets the capacity of the BinaryIntArrayReference.
     *
     * @param arrayLength the desired capacity.
     * @return this BinaryIntArrayReference with the updated capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public ByteableIntArrayValues capacity(long arrayLength)
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

        BytesStore<?, ?> bytesStore = bytesStore();
        long len = sizeInBytes(arrayLength);
        if (bytesStore == null) {
            this.length = len;
        } else {
            assert this.length == len;
        }
        return this;
    }

    /**
     * Atomically sets the value at the specified index to the given updated value
     * if the current value equals the expected value.
     *
     * @param index    the index of the value to be updated.
     * @param expected the expected value.
     * @param value    the new value.
     * @return true if successful. False return indicates that
     * the actual value was not equal to the expected value.
     * @throws BufferOverflowException        If buffer overflow occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public boolean compareAndSet(@NonNegative long index, int expected, int value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (value == INT_NOT_COMPLETE && binaryIntArrayReferences != null)
            binaryIntArrayReferences.add(new WeakReference<>(this));
        return bytesStore.compareAndSwapInt(VALUES + offset + (index << SHIFT), expected, value);
    }
}
