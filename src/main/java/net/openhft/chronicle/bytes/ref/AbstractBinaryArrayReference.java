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

@SuppressWarnings("deprecation")
public abstract class AbstractBinaryArrayReference extends AbstractReference implements BytesMarshallable {
    protected static final long CAPACITY = 0;
    protected static final long USED = CAPACITY + Long.BYTES;
    protected static final long VALUES = USED + Long.BYTES;
    protected static final int MAX_TO_STRING = 1024;

    private final int shift;
    protected long length;

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

    public long getCapacity() throws IllegalStateException {
        throwExceptionIfClosed();

        if (bytesStore == null)
            return (length - VALUES) >>> shift;
        return bytesStore.readVolatileLong(offset + CAPACITY);
    }

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

    public boolean isNull() throws IllegalStateException {
        throwExceptionIfClosed();
        return bytesStore == null;
    }

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

    protected abstract void appendContents(StringBuilder sb, long used);

    @FunctionalInterface
    protected interface ValueReader {
        long read(long index) throws BufferUnderflowException;
    }

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

    protected static void lazyWriteArray(Bytes<?> bytes, long capacity, int shift, long maxCapacity) throws BufferOverflowException {
        assert (bytes.writePosition() & 0x7) == 0;
        checkCapacity(capacity, maxCapacity);

        bytes.writeLong(capacity);
        bytes.writeLong(0L); // used
        long sizeToSkip = capacity << shift;
        bytes.writeSkip(sizeToSkip);
    }

    protected static long peakLength(BytesStore<?, ?> bytes, long offset, int shift, long maxCapacity) throws BufferUnderflowException {
        final long capacity = bytes.readLong(offset + CAPACITY);
        assert capacity > 0 : "capacity too small " + capacity;
        checkCapacity(capacity, maxCapacity);
        return (capacity << shift) + VALUES;
    }

    protected static void checkCapacity(long capacity, long maxCapacity) {
        if (capacity < 0)
            throw new IllegalArgumentException("Capacity " + capacity + " is negative");
        if (capacity > maxCapacity)
            throw new IllegalArgumentException("Capacity " + capacity + " is too large > " + maxCapacity);
    }
}
