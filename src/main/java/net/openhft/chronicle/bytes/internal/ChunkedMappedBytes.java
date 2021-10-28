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

package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Bytes to wrap memory mapped data.
 * <p>
 * NOTE These Bytes are single Threaded as are all Bytes.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ChunkedMappedBytes extends CommonMappedBytes {

    // assume the mapped file is reserved already.
    public ChunkedMappedBytes(@NotNull(exception = NullPointerException.class) final MappedFile mappedFile)
            throws IllegalStateException {
        this(mappedFile, "");
    }

    protected ChunkedMappedBytes(@NotNull(exception = NullPointerException.class) final MappedFile mappedFile, final String name)
            throws IllegalStateException {
        super(mappedFile, name);
    }

    public @NotNull ChunkedMappedBytes write(final long offsetInRDO,
                                             final byte[] bytes,
                                             int offset,
                                             final int length)
            throws IllegalStateException, BufferOverflowException {
        requireNonNull(bytes);
        throwExceptionIfClosed();

        long wp = offsetInRDO;
        if ((length + offset) > bytes.length)
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + bytes.length + ", " + "length=" + length + ", offset=" + offset);

        if (length > writeRemaining())
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));

        int remaining = length;

        MappedBytesStore bytesStore = acquireNextByteStore(wp, false);

        while (remaining > 0) {

            long safeCopySize = copySize(wp);

            if (safeCopySize + mappedFile.overlapSize() >= remaining) {
                bytesStore.write(wp, bytes, offset, remaining);
                return this;
            }

            bytesStore.write(wp, bytes, offset, (int) safeCopySize);

            offset += safeCopySize;
            wp += safeCopySize;
            remaining -= safeCopySize;

            // move to the next chunk
            bytesStore = acquireNextByteStore0(wp, false);
        }
        return this;

    }

    public @NotNull ChunkedMappedBytes write(final long writeOffset,
                                             @NotNull(exception = NullPointerException.class) final RandomDataInput bytes,
                                             long readOffset,
                                             final long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        requireNonNull(bytes);
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

    private long copySize(final long writePosition) {
        long size = mappedFile.chunkSize();
        return size - writePosition % size;
    }

    @NotNull
    @Override
    public Bytes<Void> readPositionRemaining(final long position, final long remaining)
            throws BufferUnderflowException, IllegalStateException {
//        throwExceptionIfClosed();

        final long limit = position + remaining;
        acquireNextByteStore(position, true);

        try {
            if (writeLimit < limit)
                writeLimit(limit);

            if (Jvm.isAssertEnabled())
                readLimit(limit);
            else
                uncheckedWritePosition(limit);

            return readPosition(position);
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> readPosition(final long position)
            throws BufferUnderflowException, IllegalStateException {
//        throwExceptionIfClosed();

        if (bytesStore.inside(position)) {
            return super.readPosition(position);
        } else {
            acquireNextByteStore0(position, true);
            return this;
        }
    }

    /**
     * This single-argument version of the call returns an address which is guarateed safe for a contiguous
     * read up to the overlap size.
     * <p>
     * NOTE: If called with an offset which is already in the overlap region this call with therefore
     * prompt a remapping to the new segment, which in turn may unmap the current segment.
     * Any other handles using data in the current segment may therefore result in a memory violation
     * when next used.
     * <p>
     * If manipulating offsets which may reside in the overlap region, always use the 2-argument version below
     */
    @Override
    public long addressForRead(final long offset)
            throws BufferUnderflowException, IllegalStateException {
//        throwExceptionIfClosed();


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
    public long addressForRead(final long offset, final int buffer)
            throws UnsupportedOperationException, BufferUnderflowException, IllegalStateException {
//        throwExceptionIfClosed();


        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, buffer))
            bytesStore = acquireNextByteStore0(offset, true);
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(final long offset)
            throws UnsupportedOperationException, BufferOverflowException, IllegalStateException {
//        throwExceptionIfClosed();


        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset))
            bytesStore = acquireNextByteStore0(offset, true);
        return bytesStore.addressForWrite(offset);
    }

    @Override
    protected void readCheckOffset(final long offset,
                                   final long adding,
                                   final boolean given)
            throws BufferUnderflowException, IllegalStateException {
        final long check = adding >= 0 ? offset : offset + adding;
        //noinspection StatementWithEmptyBody

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(check, adding)) {
            acquireNextByteStore0(offset, false);
        }
        super.readCheckOffset(offset, adding, given);
    }

    @Override
    protected void writeCheckOffset(final long offset, final long adding)
            throws BufferOverflowException, IllegalStateException {

        throwExceptionIfClosed();
        if (offset < 0 || offset > mappedFile.capacity() - adding)
            throw writeBufferOverflowException(offset);
        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, checkSize(adding))) {
            acquireNextByteStore0(offset, false);
        }
//        super.writeCheckOffset(offset, adding);
    }

    private long checkSize(long adding) {
        if (adding < 0 || adding > MAX_CAPACITY)
            throw new IllegalArgumentException("Invalid size " + adding);
        return adding;
    }

    @Override
    public void ensureCapacity(final long desiredCapacity)
            throws IllegalArgumentException, IllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(writePosition(), checkSize(desiredCapacity))) {
            acquireNextByteStore0(writePosition(), false);
        }
    }

    @Override
    public @NotNull Bytes<Void> writeSkip(long bytesToSkip)
            throws BufferOverflowException, IllegalStateException {
        // only check up to 128 bytes are real.
        writeCheckOffset(writePosition(), Math.min(128, bytesToSkip));
        // the rest can be lazily allocated.
        uncheckedWritePosition(writePosition() + bytesToSkip);
        return this;
    }

    @NotNull
    private BufferOverflowException writeBufferOverflowException(final long offset) {
        BufferOverflowException exception = new BufferOverflowException();
        exception.initCause(new IllegalArgumentException("Offset out of bound " + offset));
        return exception;
    }

    private @NotNull MappedBytesStore acquireNextByteStore(final long offset, final boolean set)
            throws IllegalStateException {
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
    private synchronized @NotNull MappedBytesStore acquireNextByteStore0(final long offset, final boolean set)
            throws IllegalStateException {
        throwExceptionIfClosed();

        @Nullable final BytesStore oldBS = this.bytesStore;
        @NotNull final MappedBytesStore newBS;
        try {
            newBS = mappedFile.acquireByteStore(this, offset, oldBS);
            if (newBS != oldBS) {
                this.bytesStore(newBS);
                if (oldBS != null)
                    oldBS.release(this);
                if (lastActualSize < newBS.maximumLimit)
                    lastActualSize = newBS.maximumLimit;
            }
            assert newBS.reservedBy(this);

        } catch (@NotNull IOException e) {
            throw new IORuntimeException(e);
        }
        if (set) {
            try {
                if (writeLimit() < readPosition)
                    writeLimit(readPosition);
                if (readLimit() < readPosition)
                    readLimit(readPosition);
            } catch (BufferUnderflowException | BufferOverflowException e) {
                throw new AssertionError(e);
            }
            readPosition = offset;
        }
        return newBS;
    }

    @NotNull
    @Override
    public Bytes<Void> readSkip(final long bytesToSkip)
            throws BufferUnderflowException, IllegalStateException {
        // called often so skip this check for performance
        // throwExceptionIfClosed();

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
            throws IllegalStateException {
        long start = 0L;
        readPosition = start;
        uncheckedWritePosition(start);
        writeLimit = mappedFile.capacity();
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> writeByte(final byte i8)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        final long oldPosition = writePosition();
        if (writePosition() < 0 || writePosition() > capacity() - 1)
            throw writeBufferOverflowException(writePosition());
        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(writePosition(), 1)) {
            // already determined we need it
            bytesStore = acquireNextByteStore0(writePosition(), false);
        }
        uncheckedWritePosition(writePosition() + 1);
        bytesStore.writeByte(oldPosition, i8);
        return this;
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull(exception = NullPointerException.class) final BytesStore bytes,
                             final long offset,
                             final long length)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        requireNonNull(bytes);
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

    void rawCopy(final long length, final long fromAddress)
            throws BufferOverflowException, IllegalStateException {
        this.throwExceptionIfReleased();
        OS.memory().copyMemory(fromAddress, addressForWritePosition(), length);
        uncheckedWritePosition(writePosition() + length);
    }

    @Override
    @NotNull
    public Bytes<Void> writeOrderedInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        writeCheckOffset(offset, 4);
        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, 4)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @Override
    public byte readVolatileByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, 1)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, 2)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, 4)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, 8)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileLong(offset);
    }

    @Override
    public int peekUnsignedByte()
            throws IllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(readPosition, 1)) {
            bytesStore = acquireNextByteStore0(readPosition, false);
        }
        try {
            return readPosition >= writePosition() ? -1 : bytesStore.readUnsignedByte(readPosition);
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    public int peekUnsignedByte(final long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, 1)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return offset >= readLimit() ? -1 : bytesStore.peekUnsignedByte(offset);
    }

    @SuppressWarnings("restriction")
    @Override
    public int peekVolatileInt()
            throws IllegalStateException {

        BytesStore bytesStore = this.bytesStore;
        if (!bytesStore.inside(readPosition, 4)) {
            bytesStore = acquireNextByteStore0(readPosition, true);
        }
        MappedBytesStore mbs = (MappedBytesStore) bytesStore;
        long address = mbs.address + mbs.translate(readPosition);
        @Nullable Memory memory = mbs.memory;

        // are we inside a cache line?
        if ((address & 63) <= 60) {
            ObjectUtils.requireNonNull(memory);
            UnsafeMemory.unsafeLoadFence();
            return UnsafeMemory.unsafeGetInt(address);
        } else {
            return memory.readVolatileInt(address);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> appendUtf8(@NotNull(exception = NullPointerException.class) char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        requireNonNull(chars);
        throwExceptionIfClosed();

        if (writePosition() < 0 || writePosition() > capacity() - 1L + length)
            throw writeBufferOverflowException(writePosition());
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
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (offset < 0 || offset > mappedFile.capacity() - 8L)
            throw writeBufferOverflowException(offset);
        // this is correct that it uses the maximumLimit, yes it is different from the method above.
        BytesStore bytesStore = this.bytesStore;
        if (bytesStore.start() > offset || offset + 8L > bytesStore.safeLimit()) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }
}
