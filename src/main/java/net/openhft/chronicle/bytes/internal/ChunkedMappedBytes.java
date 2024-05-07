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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Bytes to wrap memory mapped data.
 * <p>
 * NOTE These Bytes are single Threaded as are all Bytes.
 */
@SuppressWarnings({"rawtypes"})
public class ChunkedMappedBytes extends CommonMappedBytes {

    // assume the mapped file is reserved already.
    public ChunkedMappedBytes(@NotNull final MappedFile mappedFile)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        this(mappedFile, "");
    }

    protected ChunkedMappedBytes(@NotNull final MappedFile mappedFile, final String name)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        super(mappedFile, name);
    }

    @Override
    public @NotNull ChunkedMappedBytes write(@NonNegative final long offsetInRDO,
                                             final byte[] byteArray,
                                             @NonNegative int offset,
                                             @NonNegative final int length) throws ClosedIllegalStateException, BufferOverflowException, ThreadingIllegalStateException {
        requireNonNegative(offsetInRDO);
        requireNonNull(byteArray);
        requireNonNegative(offset);
        requireNonNegative(length);
        throwExceptionIfClosed();

        long wp = offsetInRDO;
        if ((length + offset) > byteArray.length)
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + byteArray.length + ", " + "length=" + length + ", offset=" + offset);

        if (length > writeRemaining())
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));

        int remaining = length;

        MappedBytesStore bytesStore = acquireNextByteStore(wp, false);

        while (remaining > 0) {

            long safeCopySize = copySize(wp);

            if (safeCopySize + mappedFile.overlapSize() >= remaining) {
                bytesStore.write(wp, byteArray, offset, remaining);
                return this;
            }

            bytesStore.write(wp, byteArray, offset, (int) safeCopySize);

            offset += safeCopySize;
            wp += safeCopySize;
            remaining -= safeCopySize;

            // move to the next chunk
            bytesStore = acquireNextByteStore0(wp, false);
        }
        return this;

    }

    @Override
    public @NotNull ChunkedMappedBytes write(@NonNegative final long writeOffset,
                                             @NotNull final RandomDataInput bytes,
                                             @NonNegative long readOffset,
                                             @NonNegative final long length)
            throws BufferOverflowException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNegative(writeOffset);
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        requireNonNegative(readOffset);
        requireNonNegative(length);
        throwExceptionIfClosed();

        long wp = writeOffset;

        if (length > writeRemaining())
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));

        long remaining = length;

        MappedBytesStore bytesStore = acquireNextByteStore(wp, false);

        while (remaining > 0) {

            long safeCopySize = copySize(wp);

            if (safeCopySize + mappedFile.overlapSize() >= remaining) {
                bytesStore.write(wp, bytes, readOffset, remaining);
                return this;
            }

            bytesStore.write(wp, bytes, readOffset, safeCopySize);

            readOffset += safeCopySize;
            wp += safeCopySize;
            remaining -= safeCopySize;

            // move to the next chunk
            bytesStore = acquireNextByteStore0(wp, false);
        }
        return this;
    }

    private long copySize(@NonNegative final long writePosition) {
        long size = mappedFile.chunkSize();
        return size - writePosition % size;
    }

    @NotNull
    @Override
    public Bytes<Void> readPositionRemaining(@NonNegative final long position, @NonNegative final long remaining)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {

        final long limit = position + remaining;
        acquireNextByteStore(position, true);

        if (writeLimit < limit)
            writeLimit(limit);

        if (Jvm.isAssertEnabled())
            readLimit(limit);
        else
            uncheckedWritePosition(limit);

        return readPosition(position);
    }

    @NotNull
    @Override
    public Bytes<Void> readPosition(@NonNegative final long position)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {

        if (bytesStore.inside(position)) {
            return super.readPosition(position);
        } else {
            acquireNextByteStore0(position, true);
            return this;
        }
    }

    /**
     * This single-argument version of the call returns an address which is guaranteed safe for a contiguous
     * read up to the overlap size.
     * <p>
     * NOTE: If called with an offset which is already in the overlap region this call will therefore
     * prompt a remapping to the new segment, which in turn may unmap the current segment.
     * Any other handles using data in the current segment may therefore result in a memory violation
     * when next used.
     * <p>
     * If manipulating offsets which may reside in the overlap region, always use the 2-argument version below
     */
    @Override
    public long addressForRead(@NonNegative final long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNegative(offset);

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset))
            bytesStore = acquireNextByteStore0(offset, true);
        return bytesStore.addressForRead(offset);
    }

    /**
     * This two-argument version of the call returns an address which is guaranteed safe for a contiguous
     * read up to the requested buffer size.
     * <p>
     * NOTE: In contrast to the single-argument version this call will not prompt a remapping if
     * called within the overlap region (provided the full extent remains in the overlap region)
     * <p>
     * This version is therefore safe to use cooperatively with other handles in a defined sequence
     * of bytes (eg returned from a DocumentContext) regardless of whether the handles span the
     * overlap region
     */
    @Override
    public long addressForRead(@NonNegative final long offset, @NonNegative final int bufferSize)
            throws UnsupportedOperationException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, bufferSize))
            bytesStore = acquireNextByteStore0(offset, true);
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(@NonNegative final long offset)
            throws UnsupportedOperationException, BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNegative(offset);

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset)) {
            bytesStore = acquireNextByteStore0(offset, true);
        }
        return bytesStore.addressForWrite(offset);
    }

    @Override
    protected void readCheckOffset(@NonNegative final long offset,
                                   final long adding,
                                   final boolean given)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        final long check = adding >= 0 ? offset : offset + adding;

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(check, adding)) {
            acquireNextByteStore0(offset, false);
        }
        super.readCheckOffset(offset, adding, given);
    }

    @Override
    protected void writeCheckOffset(final @NonNegative long offset, final @NonNegative long adding)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {

        throwExceptionIfClosed();
        if (offset + adding < start() || offset > mappedFile.capacity() - adding)
            throw writeBufferOverflowException0(offset);
        BytesStore bytesStore = this.bytesStore;
        if (adding > 0 && !bytesStore.inside(offset, checkSize0(adding))) {
            acquireNextByteStore0(offset, false);

            if (!this.bytesStore.inside(offset, checkSize0(adding)))
                throw new DecoratedBufferUnderflowException(String.format("Acquired the next BytesStore, but still not room to add %d when realCapacity %d", adding, this.bytesStore.realCapacity()));
        }
    }

    private long checkSize0(long adding) {
        if (adding < 0 || adding > MAX_CAPACITY)
            throw new IllegalArgumentException("Invalid size " + adding);
        return adding;
    }

    @Override
    public void ensureCapacity(@NonNegative final long desiredCapacity)
            throws IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();
        // TODO: should not accept desiredCapacity == 0
        BytesStore<?, ?> bytesStore = this.bytesStore;
        if (desiredCapacity > capacity())
            throw new DecoratedBufferOverflowException("Cannot extend capacity beyond " + capacity());
        // we deliberately check writePosition here - the javadoc of this method explicitly references
        // growing an elastic Bytes and it feels like the least surprising behaviour is to do this
        if (desiredCapacity > writePosition()) {
            long adding = desiredCapacity - writePosition();
            if (!bytesStore.inside(writePosition(), checkSize0(adding))) {
                acquireNextByteStore0(writePosition() + adding, false);
            }
        }
    }

    @Override
    public @NotNull Bytes<Void> writeSkip(long bytesToSkip)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        // only check up to 128 bytes are real.
        writeCheckOffset(writePosition(), Math.min(128, bytesToSkip));
        // the rest can be lazily allocated.
        uncheckedWritePosition(writePosition() + bytesToSkip);
        return this;
    }

    @NotNull
    private BufferOverflowException writeBufferOverflowException0(final long offset) {
        BufferOverflowException exception = new BufferOverflowException();
        exception.initCause(new IllegalArgumentException("Offset out of bound " + offset));
        return exception;
    }

    private @NotNull MappedBytesStore acquireNextByteStore(final long offset, final boolean set)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        // if in the same chunk, can continue even if closed, but not released.

        BytesStore bytesStore = this.bytesStore;
        if (bytesStore.inside(offset))
            return (MappedBytesStore) bytesStore;

        // not allowed if closed.
        throwExceptionIfReleased();

        return acquireNextByteStore0(offset, set);
    }

    // DON'T call this directly.
    // TODO Check whether we need synchronized; original comment; require protection from concurrent mutation to bytesStore field
    private synchronized @NotNull MappedBytesStore acquireNextByteStore0(@NonNegative final long offset, final boolean set)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        @Nullable final BytesStore oldBS = this.bytesStore;
        @NotNull final MappedBytesStore newBS;
        try {
            newBS = mappedFile.acquireByteStore(this, offset, oldBS);
            if (newBS != oldBS) {
                this.bytesStore((BytesStore<Bytes<Void>, Void>) (BytesStore<?, Void>) newBS);
                if (oldBS != null)
                    oldBS.release(this);
                if (lastActualSize < newBS.maximumLimit)
                    lastActualSize = newBS.maximumLimit;
            }
            assert newBS.reservedBy(this);

        } catch (@NotNull IOException e) {
            throw new IORuntimeException(String.format("Failed to acquireByteStore start: 0x%X offset: 0x%X safeLimit: 0x%X", start(), offset, safeLimit()), e);
        }
        if (set) {
            if (writeLimit() < readPosition)
                writeLimit(readPosition);
            if (readLimit() < readPosition)
                readLimit(readPosition);
            readPosition = offset;
        }
        return newBS;
    }

    @NotNull
    @Override
    public Bytes<Void> readSkip(final long bytesToSkip)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        if (bytesToSkip == 0)
            return this;

        if (readPosition + bytesToSkip > readLimit()) throw new BufferUnderflowException();
        long check = bytesToSkip >= 0 ? this.readPosition : this.readPosition + bytesToSkip;
        BytesStore bytesStore = this.bytesStore;
        if (bytesToSkip != (int) bytesToSkip || !bytesStore.inside(readPosition, (int) bytesToSkip)) {
            acquireNextByteStore0(check, false);
        }
        this.readPosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> clear()
            throws ClosedIllegalStateException {
        readPosition = 0L;
        uncheckedWritePosition(0L);
        writeLimit = mappedFile.capacity();
        if (writeLimit == 16843020)
            throw new AssertionError();
        return this;
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull final BytesStore bytes,
                             @NonNegative final long offset,
                             @NonNegative final long length)
            throws BufferUnderflowException, BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNull(bytes);
        requireNonNegative(offset);
        requireNonNegative(length);
        throwExceptionIfClosed();

        if (length == 8) {
            writeLong(bytes.readLong(offset));
        } else if (length > 0) {
            if (bytes.isDirectMemory()) {
                // need to check this to pull in the right bytesStore()
                long fromAddress = bytes.addressForRead(offset);
                if (length <= bytes.bytesStore().realCapacity() - offset) {
                    this.acquireNextByteStore(writePosition(), false);
                    // can we do a direct copy of raw memory?
                    if (bytesStore.realCapacity() - writePosition() >= length) {
                        rawCopy(length, fromAddress);
                        return this;
                    }
                }
            }
            BytesInternal.writeFully(bytes, offset, length, this);
        }

        return this;
    }

    void rawCopy(@NonNegative final long length, final long fromAddress)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        this.throwExceptionIfReleased();
        final long offset = writePosition();
        writeCheckOffset(offset, length);
        long address = bytesStore.addressForWrite(offset);
        OS.memory().copyMemory(fromAddress, address, length);
        uncheckedWritePosition(writePosition() + length);
    }

    @Override
    public byte readVolatileByte(@NonNegative long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, Byte.BYTES)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(@NonNegative long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, Short.BYTES)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(@NonNegative long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, Integer.BYTES)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(@NonNegative long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, Long.BYTES)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileLong(offset);
    }

    @Override
    public int peekUnsignedByte()
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(readPosition, Byte.BYTES)) {
            bytesStore = acquireNextByteStore0(readPosition, false);
        }
        try {
            return readPosition >= writePosition() ? -1 : bytesStore.readUnsignedByte(readPosition);
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    public int peekUnsignedByte(@NonNegative final long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, Byte.BYTES)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return offset < start() || readLimit() <= offset ? -1 : bytesStore.peekUnsignedByte(offset);
    }

    @SuppressWarnings("restriction")
    @Override
    public int peekVolatileInt()
            throws ClosedIllegalStateException, ThreadingIllegalStateException {

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(readPosition, Integer.BYTES)) {
            bytesStore = acquireNextByteStore0(readPosition, true);
        }
        MappedBytesStore mbs = (MappedBytesStore) bytesStore;
        long address = mbs.address + mbs.translate(readPosition);
        @Nullable Memory memory = mbs.memory;

        return memory.readVolatileInt(address);
    }

    @NotNull
    @Override
    public Bytes<Void> appendUtf8(char[] chars, @NonNegative int offset, @NonNegative int length)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNull(chars);
        throwExceptionIfClosed();

        if (writePosition() < 0 || writePosition() > capacity() - 1L + length)
            throw writeBufferOverflowException0(writePosition());
        int i;
        ascii:
        {
            for (i = 0; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                long oldPosition = writePosition();
                BytesStore bytesStore = this.bytesStore;
                if ((writePosition() & 0xff) == 0 && !bytesStore.inside(writePosition(), (length - i) * 3L)) {
                    bytesStore = acquireNextByteStore0(writePosition(), false);
                }
                uncheckedWritePosition(writePosition() + 1);
                bytesStore.writeByte(oldPosition, (byte) c);
            }
            return this;
        }
        for (; i < length; i++) {
            char c = chars[offset + i];
            BytesInternal.appendUtf8Char(this, c);
        }
        return this;
    }

    // used by the Pretoucher, don't change this without considering the impact.
    @Override
    public boolean compareAndSwapLong(@NonNegative long offset, long expected, long value)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        if (offset < 0 || offset > mappedFile.capacity() - 8L)
            throw writeBufferOverflowException0(offset);
        // this is correct that it uses the maximumLimit, yes it is different from the method above.
        BytesStore bytesStore = this.bytesStore;
        if (bytesStore.start() > offset || offset + 8L > bytesStore.safeLimit()) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }
}
