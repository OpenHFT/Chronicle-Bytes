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
import net.openhft.chronicle.core.annotation.NonNegative;
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
 * Represents a binary array of integers, backed by a {@link BytesStore}.
 * <p>
 * This class provides operations to access and manipulate an array of integers in binary form.
 * The integers are stored in a BytesStore, and this class provides various methods to perform
 * atomic operations, read/write values, and manage the state of the array.
 * <p>
 * The BinaryIntArrayReference class also contains the ability to throw exceptions in cases
 * of buffer underflows or illegal states, and to read the array in a volatile fashion,
 * ensuring a happens-before relationship between threads.
 * <p>
 * Example usage:
 * <pre>
 * BytesStore bytesStore = ...
 * BinaryIntArrayReference arrayRef = new BinaryIntArrayReference();
 * arrayRef.bytesStore(bytesStore, 0, bytesStore.capacity());
 * arrayRef.setOrderedValueAt(5, 12345);
 * int value = arrayRef.getValueAt(5);
 * </pre>
 * <p>
 * Note: This class is not thread-safe, and external synchronization may be necessary if instances
 * are shared between threads. The data referenced is thread safe when the appropriate methods are used.
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
        this.length = (defaultCapacity << SHIFT) + VALUES;
    }

    /**
     * Initializes the collection that keeps references to BinaryIntArrayReference instances.
     */
    public static void startCollecting() {
        binaryIntArrayReferences = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * Forces all BinaryIntArrayReferences to a not complete state.
     *
     * @throws IllegalStateException   if an illegal state is encountered.
     * @throws BufferOverflowException if buffer overflows.
     */
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
     * @throws BufferOverflowException  if buffer overflows.
     * @throws IllegalArgumentException if an illegal argument is encountered.
     * @throws IllegalStateException    if an illegal state is encountered.
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
     * Lazily initializes the binary data in the provided Bytes object with the given capacity.
     *
     * @param bytes    the Bytes object to be written.
     * @param capacity the capacity to be set.
     * @throws BufferOverflowException if buffer overflows.
     * @throws IllegalStateException   if an illegal state is encountered.
     */
    public static void lazyWrite(@NotNull Bytes<?> bytes, @NonNegative long capacity)
            throws BufferOverflowException, IllegalStateException {
        assert (bytes.writePosition() & 0x7) == 0;

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        bytes.writeSkip(capacity << SHIFT);
    }

    /**
     * Calculates and returns the peak length from the BytesStore at the given offset.
     *
     * @param bytes  the BytesStore object to read from.
     * @param offset the offset in the BytesStore to start reading from.
     * @return the peak length.
     * @throws BufferUnderflowException if buffer underflows.
     * @throws IllegalStateException    if an illegal state is encountered.
     */
    public static long peakLength(@NotNull BytesStore bytes, @NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        final long capacity = bytes.readLong(offset + CAPACITY);
        assert capacity > 0 : "capacity too small " + capacity;
        return (capacity << SHIFT) + VALUES;
    }

    /**
     * Assigns a new BytesStore to this BinaryIntArrayReference.
     *
     * @param bytes the new BytesStore to be assigned.
     * @throws IllegalStateException if an illegal state is encountered.
     */
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
     * Gets the capacity of the array.
     *
     * @return the capacity.
     * @throws IllegalStateException if an illegal state is encountered.
     */
    @Override
    public long getCapacity()
            throws IllegalStateException {
        throwExceptionIfClosed();

        return (length - VALUES) >>> SHIFT;
    }

    /**
     * Gets the number of used elements in the array.
     *
     * @return the number of used elements.
     * @throws IllegalStateException    if an illegal state is encountered.
     * @throws BufferUnderflowException if buffer underflows.
     */
    @Override
    public long getUsed()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(offset + USED);
    }

    /**
     * Sets the maximum number of used elements in the array.
     *
     * @param usedAtLeast the number of used elements to be set.
     * @throws IllegalStateException    if an illegal state is encountered.
     * @throws BufferUnderflowException if buffer underflows.
     */
    @Override
    public void setMaxUsed(long usedAtLeast)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeMaxLong(offset + USED, usedAtLeast);
    }

    /**
     * Gets the value at the specified index.
     *
     * @param index the index to retrieve the value from.
     * @return the value at the specified index.
     * @throws IllegalStateException    if an illegal state is encountered.
     * @throws BufferUnderflowException if buffer underflows.
     */
    @Override
    public int getValueAt(@NonNegative long index)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readInt(VALUES + offset + (index << SHIFT));
    }

    /**
     * Sets the value at the specified index.
     *
     * @param index the index to set the value at.
     * @param value the value to be set.
     * @throws IllegalStateException   if an illegal state is encountered.
     * @throws BufferOverflowException if buffer overflows.
     */
    @Override
    public void setValueAt(@NonNegative long index, int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeInt(VALUES + offset + (index << SHIFT), value);
    }

    /**
     * Retrieves the value at the specified index with volatile semantics.
     *
     * @param index the index to retrieve the value from.
     * @return the value at the specified index.
     * @throws IllegalStateException    if an illegal state is encountered.
     * @throws BufferUnderflowException if buffer underflows.
     */
    @Override
    public int getVolatileValueAt(@NonNegative long index)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(VALUES + offset + (index << SHIFT));
    }

    /**
     * Binds the value at the specified index to the provided IntValue.
     *
     * @param index the index to bind the value at.
     * @param value the IntValue to be bound.
     * @throws IllegalStateException    if an illegal state is encountered.
     * @throws BufferOverflowException  if buffer overflows.
     * @throws IllegalArgumentException if the arguments are invalid.
     */
    @Override
    public void bindValueAt(@NonNegative long index, @NotNull IntValue value)
            throws IllegalStateException, BufferOverflowException, IllegalArgumentException {
        throwExceptionIfClosed();

        ((BinaryIntReference) value).bytesStore(bytes, VALUES + offset + (index << SHIFT), 8);
    }

    /**
     * Sets the value at the specified index with ordered semantics.
     *
     * @param index the index to set the value at.
     * @param value the value to be set.
     * @throws BufferOverflowException if buffer overflows.
     * @throws IllegalStateException   if an illegal state is encountered.
     */
    @Override
    public void setOrderedValueAt(@NonNegative long index, int value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosedInSetter();

        bytes.writeOrderedInt(VALUES + offset + (index << SHIFT), value);
    }

    /**
     * Stores a bytes sequence into the BinaryIntArrayReference.
     *
     * @param bytes  the bytes sequence to store.
     * @param offset the starting position.
     * @param length the length of bytes sequence.
     * @throws IllegalArgumentException if the length does not match the peak length.
     * @throws IllegalStateException    if an illegal state is encountered.
     * @throws BufferOverflowException  if buffer overflows.
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

    /**
     * Reads and deserializes data from the input stream.
     *
     * @param bytes the input stream.
     * @throws IORuntimeException          if an IO exception occurs.
     * @throws IllegalStateException       if an illegal state occurs.
     * @throws BufferUnderflowException    if buffer underflow occurs.
     */
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

    /**
     * Serializes and writes data to the output stream.
     *
     * @param bytes the output stream.
     * @throws IllegalStateException       if an illegal state occurs.
     * @throws BufferOverflowException     if buffer overflow occurs.
     */
    @Override
    public void writeMarshallable(BytesOut<?> bytes)
            throws IllegalStateException, BufferOverflowException {
        final boolean retainsComments = bytes.retainedHexDumpDescription();
        if (retainsComments)
            bytes.writeHexDumpDescription("BinaryIntArrayReference");
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

    /**
     * Checks if the instance is null.
     *
     * @return true if the instance is null, false otherwise.
     * @throws IllegalStateException if an illegal state occurs.
     */
    @Override
    public boolean isNull()
            throws IllegalStateException {
        throwExceptionIfClosed();

        return bytes == null;
    }

    /**
     * Resets the instance to its initial state.
     *
     * @throws IllegalStateException if an illegal state occurs.
     */
    @Override
    public void reset()
            throws IllegalStateException {
        throwExceptionIfClosedInSetter();

        bytes = null;
        offset = 0;
        length = 0;
    }

    /**
     * Retrieves the BytesStore.
     *
     * @return the BytesStore, or null if not set.
     */
    @Nullable
    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    /**
     * Retrieves the offset position.
     *
     * @return the offset position.
     */
    @Override
    public long offset() {
        return offset;
    }

    /**
     * Retrieves the maximum size.
     *
     * @return the maximum size.
     */
    @Override
    public long maxSize() {
        return length;
    }

    /**
     * Returns a string representation of the BinaryIntArrayReference.
     *
     * @return a string representation.
     */
    @NotNull
    @Override
    public String toString() {
        if (bytes == null) {
            return "not set";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("used: ");
        try {
            long used = getUsed();
            sb.append(used);
            sb.append(", value: ");
            appendContents(sb, used);
            return sb.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Appends the contents to the provided StringBuilder.
     *
     * @param sb   the StringBuilder to append to.
     * @param used the number of used elements.
     */
    private void appendContents(@NotNull StringBuilder sb, long used) {
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

    /**
     * Calculates the size in bytes of the array with the given capacity.
     *
     * @param capacity the capacity of the array.
     * @return the size in bytes.
     * @throws IllegalStateException if an illegal state is encountered.
     */
    @Override
    public long sizeInBytes(@NonNegative long capacity)
            throws IllegalStateException {
        throwExceptionIfClosed();

        return (capacity << SHIFT) + VALUES;
    }

    /**
     * Sets the capacity of the BinaryIntArrayReference.
     *
     * @param arrayLength the desired capacity.
     * @return this BinaryIntArrayReference with the updated capacity.
     * @throws IllegalStateException if an illegal state is encountered.
     */
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

    /**
     * Atomically sets the value at the specified index to the given updated value
     * if the current value equals the expected value.
     *
     * @param index the index of the value to be updated.
     * @param expected the expected value.
     * @param value the new value.
     * @return true if successful. False return indicates that
     * the actual value was not equal to the expected value.
     * @throws BufferOverflowException if buffer overflow occurs.
     * @throws IllegalStateException   if an illegal state occurs.
     */
    @Override
    public boolean compareAndSet(@NonNegative long index, int expected, int value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (value == INT_NOT_COMPLETE && binaryIntArrayReferences != null)
            binaryIntArrayReferences.add(new WeakReference<>(this));
        return bytes.compareAndSwapInt(VALUES + offset + (index << SHIFT), expected, value);
    }
}

