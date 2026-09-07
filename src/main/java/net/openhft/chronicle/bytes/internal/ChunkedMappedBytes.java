/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;
import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Bytes to wrap memory-mapped data.
 * <p>
 * NOTE These Bytes are single-threaded as are all Bytes.
 */
@SuppressWarnings("rawtypes")
public class ChunkedMappedBytes extends CommonMappedBytes {

    static final Logger LOG = LoggerFactory.getLogger(ChunkedMappedBytes.class);
    static final boolean DEBUG_CHUNKED_MAPPED_BYTES = Jvm.getBoolean("debug.chunked.mapped.bytes", false);

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

            // as remaining is an int, the min is an int
            int bytesToWrite = (int) Math.min(safeCopySize, remaining);

            bytesStore.write(wp, byteArray, offset, bytesToWrite);

            offset += bytesToWrite;
            wp += bytesToWrite;
            remaining -= bytesToWrite;

            if (bytesToWrite == safeCopySize)
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

    /**
     * Move the read position to {@code position}, loading the correct chunk if needed.
     * <p>
     * Uses the chunk's hard upper bound so we only grow the file when absolutely required.
     *
     * @param position new read position (>= 0)
     * @return this instance
     */
    @NotNull
    @Override
    public Bytes<Void> readPosition(@NonNegative final long position)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {

        // use the real limit of the byteStore rather than the safe limit to minimise resizing
        if (bytesStore.inside(position, 0)) {
            return super.readPosition(position);
        } else {
            acquireNextByteStore0(position, true);
            return this;
        }
    }

    /**
     * Move the write limit to {@code limit}, loading the correct chunk if needed.
     * <p>
     * Uses the chunk's hard upper bound so we only grow the file when absolutely required.
     *
     * @param limit new write limit (>= 0)
     * @return this instance
     */
    @NotNull
    @Override
    public Bytes<Void> writeLimit(long limit) throws BufferOverflowException {
        // use the real limit of the byteStore rather than the safe limit to minimise resizing
        if (limit != capacity() && !bytesStore.inside(limit, 0)) {
            acquireNextByteStore0(limit, false);
        }
        return super.writeLimit(limit);
    }

    /**
     * Move the write position to {@code position}, loading the correct chunk if needed.
     * <p>
     * Uses the chunk's safe upper bound so we can safely write a significant block of data after this without checking the size regularly.
     *
     * @param position new write position (>= 0)
     * @return this instance
     */
    @NotNull
    @Override
    public Bytes<Void> writePosition(long position) throws BufferOverflowException {
        // use the safe limit of the byteStore to ensure we can write something after it
        if (!bytesStore.inside(position)) {
            acquireNextByteStore0(position, false);
        }
        return super.writePosition(position);
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

        BytesStore<?, ?> bytesStore = this.bytesStore;
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
     * of bytes (e.g. returned from a DocumentContext) regardless of whether the handles span the
     * overlap region
     */
    @Override
    public long addressForRead(@NonNegative final long offset, @NonNegative final int bufferSize)
            throws UnsupportedOperationException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {

        BytesStore<?, ?> bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, bufferSize)) {
            bytesStore = acquireNextByteStore0(offset, true);
            //! This overload promises an address safe for bufferSize contiguous bytes. With no overlap, or a range longer
            //! than the overlap, acquiring the chunk cannot make that true, so the range is rejected.
            //! MappedBytesReadAcrossMappingTest#primitiveReadStraddlingTheMappingEndIsRejected covers addressForRead.
            requireInsideAcquiredStore(offset, bufferSize);
        }
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(@NonNegative final long offset)
            throws UnsupportedOperationException, BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNegative(offset);

        BytesStore<?, ?> bytesStore = this.bytesStore;
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

        BytesStore<?, ?> bytesStore = this.bytesStore;
        if (!bytesStore.inside(check, adding)) {
            acquireNextByteStore0(offset, false);
            super.readCheckOffset(offset, adding, given);
            //! Acquiring the chunk for an offset cannot make a range fit when the mapping has no overlap or the range is
            //! longer than the overlap, and the read then went on from raw memory beyond it: equalBytes crashed the JVM
            //! through readLong(offset) on a zero-overlap mapping. The read-limit check runs first so its exception is
            //! unchanged; then the range is rejected as writeCheckOffset rejects the matching write. Byte-wise reads
            //! still cross chunks. MappedBytesReadAcrossMappingTest#primitiveReadStraddlingTheMappingEndIsRejected.
            requireInsideAcquiredStore(check, adding);
            return;
        }
        super.readCheckOffset(offset, adding, given);
    }

    /**
     * Rejects a read that still does not fit the chunk acquired for its offset, as {@link #writeCheckOffset} rejects
     * the matching write: no single mapping covers a range that straddles a chunk end without enough overlap.
     */
    private void requireInsideAcquiredStore(final long offset, final long size) throws DecoratedBufferUnderflowException {
        if (size > 0 && !bytesStore.inside(offset, size))
            throw new DecoratedBufferUnderflowException(String.format(
                    "Acquired the next BytesStore, but a read of %d bytes at %d still straddles its end at %d",
                    size, offset, bytesStore.realCapacity()));
    }

    /** Copies from the read position in pieces that each stay inside one chunk mapping. */
    @Override
    public int read(byte[] bytes, @NonNegative int off, @NonNegative int len)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNull(bytes);
        final long remaining = readRemaining();
        if (remaining <= 0)
            return -1;
        //! AbstractBytes.read copies 64 KiB at a time whatever the mapping, so a batch crossing a chunk end beyond the
        //! overlap came back as garbage, or with the check above an exception. Bounding each batch by its mapping lets
        //! the copy continue in the next chunk. MappedBytesReadAcrossMappingTest#bulkReadAcrossTheMappingEnd fails without this.
        final int total = (int) Math.min(len, remaining);
        int copied = 0;
        while (copied < total) {
            final long position = readPosition;
            final int batch = Math.min(total - copied, mappedBatchSize(position));
            readOffsetPositionMoved(batch);
            bytesStore.read(position, bytes, off + copied, batch);
            copied += batch;
        }
        return total;
    }

    /** Random-access copy in chunk-sized pieces; {@code copyTo(byte[])} reads through this method. */
    @Override
    public long read(@NonNegative long offsetInRDI, byte[] bytes, @NonNegative int offset, @NonNegative int length)
            throws ClosedIllegalStateException {
        requireNonNull(bytes);
        //! The inherited copy read from the current chunk's store at an absolute offset with neither acquisition nor
        //! range check, so a copy from another chunk or across a chunk end read outside the mapping.
        //! MappedBytesReadAcrossMappingTest#copyToAcrossTheMappingEnd fails without this override.
        final int len = Maths.toUInt31(Math.min(length, requireNonNegative(readLimit() - offsetInRDI)));
        long position = offsetInRDI;
        int copied = 0;
        while (copied < len) {
            final int batch = Math.min(len - copied, mappedBatchSize(position));
            readCheckOffset(position, batch, true);
            bytesStore.read(position, bytes, offset + copied, batch);
            position += batch;
            copied += batch;
        }
        return len;
    }

    /** @return the largest copy from {@code position} inside the chunk acquired for it, at most {@link #safeCopySize()} */
    private int mappedBatchSize(final long position) {
        BytesStore<?, ?> store = this.bytesStore;
        if (!store.inside(position, 1))
            store = acquireNextByteStore0(position, false);
        return (int) Math.min(safeCopySize(), store.realCapacity() - position);
    }

    @Override
    protected void writeCheckOffset(final @NonNegative long offset, final @NonNegative long adding)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {

        throwExceptionIfClosed();
        if (offset + adding < start() || offset > mappedFile.capacity() - adding)
            throw writeBufferOverflowException0(offset);
        BytesStore<?, ?> bytesStore = this.bytesStore;
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
        // we deliberately check writePosition here - the Javadoc of this method explicitly references
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

        BytesStore<?, ?> bytesStore = this.bytesStore;
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
        if (DEBUG_CHUNKED_MAPPED_BYTES && LOG.isDebugEnabled())
            Jvm.debug().on(LOG, Integer.toHexString(System.identityHashCode(this)) + ", file: " + mappedFile.file().getName() + ", offset: 0x" + Long.toHexString(offset) + ", read: " + set);

        @Nullable final BytesStore<?, ?> oldBS = this.bytesStore;
        @NotNull final MappedBytesStore newBS;
        try {
            newBS = mappedFile.acquireByteStore(this, offset, oldBS);
            if (newBS != oldBS) {
                this.bytesStore(uncheckedCast(newBS));
                if (oldBS != null)
                    oldBS.release(this);
                if (lastActualSize < newBS.maximumLimit)
                    lastActualSize = newBS.maximumLimit;
            }

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
        BytesStore<?, ?> bytesStore = this.bytesStore;
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
    public Bytes<Void> write(@NotNull final BytesStore<?, ?> bytes,
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

        BytesStore<?, ?> bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, Byte.BYTES)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(@NonNegative long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();
        //! See storeFor. MappedBytesReadAcrossMappingTest#volatileShortStraddlingTheMappingEndIsRejected fails without it.
        return storeFor(offset, Short.BYTES)
                .readVolatileShort(offset);
    }

    /** The store holding {@code size} bytes at {@code offset}: acquired if needed, rejected if they still do not fit. */
    //! The volatile reads bypass readCheckOffset and read raw memory from the acquired chunk, and Core does not require
    //! alignment for them, so a misaligned value at the last bytes of a zero-overlap chunk read past the mapping. One
    //! helper serves the three reads. MappedBytesReadAcrossMappingTest#volatileShortStraddlingTheMappingEndIsRejected,
    //! #volatileIntStraddlingTheMappingEndIsRejected and #primitiveReadStraddlingTheMappingEndIsRejected fail without it.
    private BytesStore<?, ?> storeFor(final long offset, final int size)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesStore<?, ?> bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, size)) {
            bytesStore = acquireNextByteStore0(offset, false);
            requireInsideAcquiredStore(offset, size);
        }
        return bytesStore;
    }

    @Override
    public int readVolatileInt(@NonNegative long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();
        //! See storeFor. MappedBytesReadAcrossMappingTest#volatileIntStraddlingTheMappingEndIsRejected fails without it.
        return storeFor(offset, Integer.BYTES)
                .readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(@NonNegative long offset)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();
        //! See storeFor. MappedBytesReadAcrossMappingTest#primitiveReadStraddlingTheMappingEndIsRejected covers this read.
        return storeFor(offset, Long.BYTES)
                .readVolatileLong(offset);
    }

    @Override
    public int peekUnsignedByte()
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        BytesStore<?, ?> bytesStore = this.bytesStore;
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

        BytesStore<?, ?> bytesStore = this.bytesStore;
        if (!bytesStore.inside(offset, Byte.BYTES)) {
            bytesStore = acquireNextByteStore0(offset, false);
        }
        return offset < start() || readLimit() <= offset ? -1 : bytesStore.peekUnsignedByte(offset);
    }

    @SuppressWarnings("restriction")
    @Override
    public int peekVolatileInt()
            throws ClosedIllegalStateException, ThreadingIllegalStateException {

        BytesStore<?, ?> bytesStore = this.bytesStore;
        if (!bytesStore.inside(readPosition, Integer.BYTES)) {
            bytesStore = acquireNextByteStore0(readPosition, true);
            //! peekVolatileInt reads through the raw address of the acquired chunk; a misaligned read position at the
            //! last bytes of a zero-overlap chunk would read past the mapping.
            //! MappedBytesReadAcrossMappingTest#volatileIntStraddlingTheMappingEndIsRejected covers the peek.
            requireInsideAcquiredStore(readPosition, Integer.BYTES);
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
                BytesStore<?, ?> bytesStore = this.bytesStore;
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
        BytesStore<?, ?> bytesStore = this.bytesStore;
        if (bytesStore.start() > offset || offset + 8L > bytesStore.safeLimit()) {
            bytesStore = acquireNextByteStore0(offset, false);
            //! Core already throws MisAlignedAssertionError for a swap that crosses a cache line, which every swap straddling
            //! a page-aligned chunk end does, so this check adds no memory safety; it exists so that every acquired-range
            //! rejection throws the same type. Kept as a decision: dropping it is the smaller change.
            //! MappedBytesReadAcrossMappingTest#compareAndSwapLongStraddlingTheMappingEndIsRejected pins the type.
            requireInsideAcquiredStore(offset, Long.BYTES);
        }
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }
}
