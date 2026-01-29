/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Base support for array-like binary references backed by a BytesStore.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractBinaryArrayReference extends AbstractReference implements BytesMarshallable {
    /** Offset of capacity field within the backing store. */
    protected static final long CAPACITY = 0;
    /** Offset of used counter within the backing store. */
    protected static final long USED = CAPACITY + Long.BYTES;
    /** Offset where values start within the backing store. */
    protected static final long VALUES = USED + Long.BYTES;
    /** Maximum number of characters to include in {@link #toString()}. */
    protected static final int MAX_TO_STRING = 1024;

    /** Log2 of element size, used for shifting. */
    private final int shift;
    /** Length in bytes of the mapped region. */
    protected long length;

    /**
     * Creates a binary array reference with the given element-size shift.
     *
     * @param shift number of bits to shift counts by to get byte length
     */
    protected AbstractBinaryArrayReference(int shift) {
        this.shift = shift;
    }

    @Override
    protected void acceptNewBytesStore(@NotNull final BytesStore<?, ?> bytes)
            throws IllegalStateException {
        if (this.bytesStore != null) {
            this.bytesStore.release(this);
        }
        this.bytesStore = bytes;
        this.bytesStore.reserve(this);
    }

    /**
     * Returns the declared capacity (number of elements) for this array.
     *
     * @return capacity in elements
     * @throws IllegalStateException if closed
     */
    public long getCapacity() throws IllegalStateException {
        throwExceptionIfClosed();

        if (bytesStore == null)
            return (length - VALUES) >>> shift;
        return bytesStore.readVolatileLong(offset + CAPACITY);
    }

    /**
     * Returns the number of elements currently in use.
     *
     * @return used element count
     * @throws IllegalStateException    if closed
     * @throws BufferUnderflowException if backing data is corrupt
     */
    public abstract long getUsed() throws IllegalStateException, BufferUnderflowException;

    @Override
    public void readMarshallable(BytesIn<?> bytes)
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        long position = bytes.readPosition();
        long capacity = bytes.readLong();
        long used = bytes.readLong();
        if (capacity < 0 || capacity > bytes.readRemaining() >> shift)
            throw new IORuntimeException("Corrupt used capacity");

        if (used < 0 || used > capacity)
            throw new IORuntimeException("Corrupt used value");

        long sizeToSkip = capacity << shift;
        bytes.readSkip(sizeToSkip);
        long len = bytes.readPosition() - position;
        bytesStore((Bytes) bytes, position, len);
    }

    @Override
    public void writeMarshallable(BytesOut<?> bytes)
            throws IllegalStateException, BufferOverflowException {
        final boolean retainsComments = bytes.retainedHexDumpDescription();
        if (retainsComments)
            bytes.writeHexDumpDescription(getClass().getSimpleName());
        BytesStore<?, ?> bytesStore = bytesStore();
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
            bytes.writeSkip(capacity << shift);
        } else {
            bytes.write(bytesStore, offset, length);
        }
    }

    /**
     * Returns {@code true} if no backing store is currently mapped.
     *
     * @return {@code true} when unset
     * @throws IllegalStateException if closed
     */
    public boolean isNull() throws IllegalStateException {
        throwExceptionIfClosed();
        return bytesStore == null;
    }

    /**
     * Clears the current mapping and resets offsets/length.
     *
     * @throws IllegalStateException if closed
     */
    public void reset() throws IllegalStateException {
        throwExceptionIfClosedInSetter();
        bytesStore = null;
        offset = 0;
        length = 0;
    }

    @Override
    public long maxSize() {
        return length;
    }

    @Override
    public String toString() {
        if (bytesStore == null) {
            return "not set";
        }
        StringBuilder sb = new StringBuilder();
        try {
            long used = getUsed();
            sb.append("used: ").append(used).append(", value: ");
            appendContents(sb, used);
            return sb.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Appends a textual representation of up to {@code used} elements to {@code sb}.
     *
     * @param sb   destination buffer
     * @param used number of elements to include
     * @throws IllegalStateException    if closed
     * @throws BufferUnderflowException if backing data is corrupt
     */
    protected abstract void appendContents(@NotNull StringBuilder sb, long used)
            throws IllegalStateException, BufferUnderflowException;

    /**
     * Strategy for reading a single element value from the underlying storage.
     */
    @FunctionalInterface
    protected interface ValueReader {
        /**
         * Reads a single element from the underlying binary array storage at the specified index.
         * The value is returned as a long to accommodate both int and long array types.
         *
         * @param index zero-based element index within the array
         * @return value at that index, widened to long
         * @throws BufferUnderflowException if the index is out of bounds
         */
        long read(long index) throws BufferUnderflowException;
    }

    /**
     * Writes a comma-separated preview of up to {@code used} values into {@code sb}.
     *
     * @param sb       destination buffer
     * @param used     number of elements currently in use
     * @param capacity total capacity in elements
     * @param reader   callback that supplies values by index
     * @throws BufferUnderflowException if the reader encounters invalid data
     */
    protected void appendContents(StringBuilder sb, long used, long capacity, ValueReader reader) {
        String sep = "";
        try {
            int max = (int) Math.min(used, Math.min(capacity, MAX_TO_STRING));
            for (int i = 0; i < max; i++) {
                sb.append(sep).append(reader.read(i));
                sep = ", ";
            }
            if (max < capacity)
                sb.append(" ...");

        } catch (Exception e) {
            sb.append(' ').append(e);
        }
    }

    /**
     * Writes an initialised header and zeroed value region into {@code bytes}.
     *
     * @param bytes       destination to write to
     * @param capacity    number of elements to reserve
     * @param shift       element-size shift
     * @param maxCapacity maximum allowed capacity
     * @throws BufferOverflowException if the destination cannot accommodate the array
     */
    protected static void writeArray(Bytes<?> bytes, long capacity, int shift, long maxCapacity) throws BufferOverflowException {
        assert (bytes.writePosition() & 0x7) == 0;
        checkCapacity(capacity, maxCapacity);

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        long start = bytes.writePosition();
        long sizeToSkip = capacity << shift;
        bytes.zeroOut(start, start + sizeToSkip);
        bytes.writeSkip(sizeToSkip);
    }

    /**
     * Writes a header and skips over the value region without zeroing.
     *
     * @param bytes       destination to write to
     * @param capacity    number of elements to reserve
     * @param shift       element-size shift
     * @param maxCapacity maximum allowed capacity
     * @throws BufferOverflowException if the destination cannot accommodate the array
     */
    protected static void lazyWriteArray(Bytes<?> bytes, long capacity, int shift, long maxCapacity) throws BufferOverflowException {
        assert (bytes.writePosition() & 0x7) == 0;
        checkCapacity(capacity, maxCapacity);

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        long sizeToSkip = capacity << shift;
        bytes.writeSkip(sizeToSkip);
    }

    /**
     * Calculates the total byte length for the array starting at {@code offset}.
     *
     * @param bytes       backing store
     * @param offset      starting offset
     * @param shift       element-size shift
     * @param maxCapacity maximum allowed capacity
     * @return total byte length including header
     * @throws BufferUnderflowException if the header cannot be read safely
     */
    protected static long peakLength(BytesStore<?, ?> bytes, long offset, int shift, long maxCapacity) throws BufferUnderflowException {
        final long capacity = bytes.readLong(offset + CAPACITY);
        assert capacity > 0 : "capacity too small " + capacity;
        checkCapacity(capacity, maxCapacity);
        return (capacity << shift) + VALUES;
    }

    /**
     * Validates that the requested capacity is within valid bounds (0 to maxCapacity inclusive).
     * Throws IllegalArgumentException if capacity is negative or exceeds the allowed maximum.
     *
     * @param capacity    requested capacity in elements
     * @param maxCapacity maximum allowed capacity in elements
     */
    protected static void checkCapacity(long capacity, long maxCapacity) {
        if (capacity < 0)
            throw new IllegalArgumentException("Capacity " + capacity + " is negative");
        if (capacity > maxCapacity)
            throw new IllegalArgumentException("Capacity " + capacity + " is too large > " + maxCapacity);
    }
}
