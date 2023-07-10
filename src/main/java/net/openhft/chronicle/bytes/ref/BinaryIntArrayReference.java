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
 * This class represents a binary array of 64-bit integer values, which can be accessed and manipulated.
 * It supports features such as reading and writing to the binary array, and handling of reference counts.
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
     * Constructor initializes the BinaryIntArrayReference with a specified default capacity.
     *
     * @param defaultCapacity the default capacity of the binary array.
     */
    public BinaryIntArrayReference(long defaultCapacity) {
        this.length = (defaultCapacity << SHIFT) + VALUES;
    }

    /**
     * Initializes the internal set used to collect BinaryIntArrayReferences.
     */
    public static void startCollecting() {
        binaryIntArrayReferences = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * Forces all BinaryIntArrayReferences to an incomplete state.
     *
     * @throws IllegalStateException       if an illegal state occurs.
     * @throws BufferOverflowException     if buffer overflow occurs.
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
     * Writes the specified number of bytes to the provided BytesStore.
     *
     * @param bytes    the BytesStore to write to.
     * @param capacity the number of bytes to be written.
     * @throws BufferOverflowException     if buffer overflow occurs.
     * @throws IllegalArgumentException    if an illegal argument is provided.
     * @throws IllegalStateException       if an illegal state occurs.
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

    public static void lazyWrite(@NotNull Bytes<?> bytes, @NonNegative long capacity)
            throws BufferOverflowException, IllegalStateException {
        assert (bytes.writePosition() & 0x7) == 0;

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        bytes.writeSkip(capacity << SHIFT);
    }

    public static long peakLength(@NotNull BytesStore bytes, @NonNegative long offset)
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

    /**
     * Sets the maximum value used in the array.
     *
     * @param usedAtLeast the maximum value to be set.
     * @throws IllegalStateException       if an illegal state occurs.
     * @throws BufferUnderflowException    if buffer underflow occurs.
     */
    @Override
    public void setMaxUsed(long usedAtLeast)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeMaxLong(offset + USED, usedAtLeast);
    }

    /**
     * Returns the integer value at the specified index.
     *
     * @param index the index of the value to retrieve.
     * @return the value at the specified index.
     * @throws IllegalStateException       if an illegal state occurs.
     * @throws BufferUnderflowException    if buffer underflow occurs.
     */
    @Override
    public int getValueAt(@NonNegative long index)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readInt(VALUES + offset + (index << SHIFT));
    }

    /**
     * Sets the integer value at the specified index.
     *
     * @param index the index at which the value is to be set.
     * @param value the value to be set.
     * @throws IllegalStateException       if an illegal state occurs.
     * @throws BufferOverflowException     if buffer overflow occurs.
     */
    @Override
    public void setValueAt(@NonNegative long index, int value)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        bytes.writeInt(VALUES + offset + (index << SHIFT), value);
    }

    /**
     * Returns the volatile integer value at the specified index.
     *
     * @param index the index of the value to retrieve.
     * @return the value at the specified index.
     * @throws IllegalStateException       if an illegal state occurs.
     * @throws BufferUnderflowException    if buffer underflow occurs.
     */
    @Override
    public int getVolatileValueAt(@NonNegative long index)
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytes.readVolatileInt(VALUES + offset + (index << SHIFT));
    }

    @Override
    public void bindValueAt(@NonNegative long index, @NotNull IntValue value)
            throws IllegalStateException, BufferOverflowException, IllegalArgumentException {
        throwExceptionIfClosed();

        ((BinaryIntReference) value).bytesStore(bytes, VALUES + offset + (index << SHIFT), 8);
    }

    /**
     * Sets the integer value at the specified index in an ordered or atomic manner.
     *
     * @param index the index at which the value is to be set.
     * @param value the value to be set.
     * @throws BufferOverflowException if buffer overflow occurs.
     * @throws IllegalStateException   if an illegal state occurs.
     */
    @Override
    public void setOrderedValueAt(@NonNegative long index, int value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosedInSetter();

        bytes.writeOrderedInt(VALUES + offset + (index << SHIFT), value);
    }

    /**
     * Stores the given bytes from the specified offset for a specified length.
     *
     * @param bytes  the BytesStore to write to.
     * @param offset the offset at which to start writing.
     * @param length the number of bytes to be written.
     * @throws IllegalArgumentException    if an illegal argument is provided.
     * @throws IllegalStateException       if an illegal state occurs.
     * @throws BufferOverflowException     if buffer overflow occurs.
     */
    @Override
    public void bytesStore(@NotNull BytesStore bytes, @NonNegative long offset, @NonNegative long length)
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
        try {
            bytesStore((Bytes) bytes, position, len);
        } catch (IllegalArgumentException | BufferOverflowException e) {
            throw new AssertionError(e);
        }
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
            try {
                bytes.write(bytesStore, offset, length);
            } catch (BufferUnderflowException | IllegalArgumentException e) {
                throw new AssertionError(e);
            }
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
     * Returns the BytesStore instance associated with this object.
     *
     * @return the BytesStore instance.
     */
    @Nullable
    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    /**
     * Returns the offset where the data is stored.
     *
     * @return the offset.
     */
    @Override
    public long offset() {
        return offset;
    }

    /**
     * Returns the maximum size of the data that can be stored.
     *
     * @return the maximum size.
     */
    @Override
    public long maxSize() {
        return length;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
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
        } catch (IllegalStateException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

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
     * Calculates the size in bytes based on the provided capacity.
     *
     * @param capacity the capacity.
     * @return the size in bytes.
     * @throws IllegalStateException if an illegal state occurs.
     */
    @Override
    public long sizeInBytes(@NonNegative long capacity)
            throws IllegalStateException {
        throwExceptionIfClosed();

        return (capacity << SHIFT) + VALUES;
    }

    /**
     * Sets the capacity of the ByteableIntArrayValues.
     *
     * @param arrayLength the length of the array.
     * @return the updated ByteableIntArrayValues.
     * @throws IllegalStateException if an illegal state occurs.
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

