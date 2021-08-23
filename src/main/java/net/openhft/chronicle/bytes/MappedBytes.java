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

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.util.StringUtils.*;

/**
 * Bytes to wrap memory mapped data.
 * <p>
 * NOTE These Bytes are single Threaded as are all Bytes.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MappedBytes extends AbstractBytes<Void> implements Closeable, ManagedCloseable {

    private static final boolean TRACE = Jvm.getBoolean("trace.mapped.bytes");
    private final MappedFile mappedFile;
    private final boolean backingFileIsReadOnly;
    private MappedBytesStore bytesStore;
    private long lastActualSize = 0;
    private boolean disableThreadSafetyCheck;

    private final AbstractCloseable closeable = new AbstractCloseable() {
        @Override
        protected void performClose() throws IllegalStateException {
            MappedBytes.this.performClose();
        }

        @Override
        protected boolean threadSafetyCheck(boolean isUsed) throws IllegalStateException {
            return disableThreadSafetyCheck || super.threadSafetyCheck(isUsed);
        }
    };

    @Override
    public void clearUsedByThread() {
        super.clearUsedByThread();
        closeable.clearUsedByThread();
    }

    // assume the mapped file is reserved already.
    protected MappedBytes(@NotNull final MappedFile mappedFile)
            throws IllegalStateException {
        this(mappedFile, "");
    }

    protected MappedBytes(@NotNull final MappedFile mappedFile, final String name)
            throws IllegalStateException {
        super(NoBytesStore.noBytesStore(),
                NoBytesStore.noBytesStore().writePosition(),
                NoBytesStore.noBytesStore().writeLimit(),
                name);

        assert mappedFile != null;
        this.mappedFile = mappedFile;
        mappedFile.reserve(this);
        this.backingFileIsReadOnly = !mappedFile.file().canWrite();
        assert !mappedFile.isClosed();
        clear();
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final String filename, final long chunkSize)
            throws FileNotFoundException, IllegalStateException {
        return mappedBytes(new File(filename), chunkSize);
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file, final long chunkSize)
            throws FileNotFoundException, IllegalStateException {
        return mappedBytes(file, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file, final long chunkSize, final long overlapSize)
            throws FileNotFoundException, IllegalStateException {
        @NotNull final MappedFile rw = MappedFile.of(file, chunkSize, overlapSize, false);
        try {
            return mappedBytes(rw);
        } finally {
            rw.release(INIT);
        }
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file,
                                          final long chunkSize,
                                          final long overlapSize,
                                          final boolean readOnly)
            throws FileNotFoundException,
            IllegalStateException {
        @NotNull final MappedFile rw = MappedFile.of(file, chunkSize, overlapSize, readOnly);
        try {
            return mappedBytes(rw);
        } finally {
            rw.release(INIT);
        }
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final MappedFile rw)
            throws IllegalStateException {
        return new MappedBytes(rw);
    }

    @NotNull
    public static MappedBytes readOnly(@NotNull final File file)
            throws FileNotFoundException {
        MappedFile mappedFile = MappedFile.readOnly(file);
        try {
            try {
                return new MappedBytes(mappedFile);
            } finally {
                mappedFile.release(INIT);
            }
        } catch (IllegalStateException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected void bytesStore(BytesStore bytesStore) {
        super.bytesStore(bytesStore);
        if (bytesStore instanceof MappedBytesStore)
            this.bytesStore = (MappedBytesStore) bytesStore;
        else
            this.bytesStore = null;
    }

    @NotNull
    @Override
    public String toString() {
        if (!TRACE)
            return super.toString();
        return "MappedBytes{" + "\n" +
                "refCount=" + refCount() + ",\n" +
                "mappedFile=" + mappedFile.file().getAbsolutePath() + ",\n" +
                "mappedFileRefCount=" + mappedFile.refCount() + ",\n" +
                "mappedFileIsClosed=" + mappedFile.isClosed() + ",\n" +
                "mappedFileRafIsClosed=" + Jvm.getValue(mappedFile.raf(), "closed") + ",\n" +
                "mappedFileRafChannelIsClosed=" + !mappedFile.raf().getChannel().isOpen() + ",\n" +
                "isClosed=" + isClosed() +
                '}';
    }

    public @NotNull MappedBytes write(@NotNull final byte[] bytes,
                                      final int offset,
                                      final int length)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        write(writePosition(), bytes, offset, length);
        uncheckedWritePosition(writePosition() + Math.min(length, bytes.length - offset));
        return this;
    }

    public @NotNull MappedBytes write(final long offsetInRDO,
                                      final byte[] bytes,
                                      int offset,
                                      final int length)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        long wp = offsetInRDO;
        if ((length + offset) > bytes.length)
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + bytes.length + ", " + "length=" + length + ", offset=" + offset);

        if (length > writeRemaining())
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));

        int remaining = length;

        acquireNextByteStore(wp, false);

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
            acquireNextByteStore0(wp, false);

        }
        return this;

    }

    public @NotNull MappedBytes write(final long writeOffset,
                                      final RandomDataInput bytes,
                                      long readOffset,
                                      final long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        long wp = writeOffset;

        if (length > writeRemaining())
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));

        long remaining = length;

        acquireNextByteStore(wp, false);

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
            acquireNextByteStore0(wp, false);
        }
        return this;
    }

    @NotNull
    @Override
    public MappedBytes write(@NotNull final RandomDataInput bytes)
            throws IllegalStateException, BufferOverflowException {
        throwExceptionIfClosed();

        assert bytes != this : "you should not write to yourself !";
        final long remaining = bytes.readRemaining();
        write(writePosition(), bytes);
        uncheckedWritePosition(writePosition() + remaining);
        return this;
    }

    @NotNull
    public MappedBytes write(final long offsetInRDO, @NotNull final RandomDataInput bytes)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        try {
            write(offsetInRDO, bytes, bytes.readPosition(), bytes.readRemaining());
            return this;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    private long copySize(final long writePosition) {
        long size = mappedFile.chunkSize();
        return size - writePosition % size;
    }

    public void setNewChunkListener(final NewChunkListener listener) {
        mappedFile.setNewChunkListener(listener);
    }

    @NotNull
    public MappedFile mappedFile() {
        return mappedFile;
    }

    @Override
    public BytesStore<Bytes<Void>, Void> copy()
            throws IllegalStateException {
        return NativeBytes.copyOf(this);
    }

    @Override
    public long capacity() {
        return mappedFile.capacity();
    }

    @Override
    public Bytes<Void> readLimitToCapacity() {
        uncheckedWritePosition(mappedFile.capacity());
        return this;
    }

    @Override
    public long realReadRemaining() {
        long limit = readLimit();
        if (limit > lastActualSize)
            limit = Math.min(realCapacity(), limit);
        return limit - readPosition();
    }

    @Override
    public long realCapacity() {

        try {
            return lastActualSize = mappedFile.actualSize();

        } catch (Exception e) {
            Jvm.warn().on(getClass(), "Unable to obtain the real size for " + mappedFile.file(), e);
            return lastActualSize = 0;
        }
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

        if (bytesStore != null && bytesStore.inside(position)) {
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

        if (bytesStore == null || !bytesStore.inside(offset))
            acquireNextByteStore0(offset, true);
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

        if (bytesStore == null || !bytesStore.inside(offset, buffer))
            acquireNextByteStore0(offset, true);
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(final long offset)
            throws UnsupportedOperationException, BufferOverflowException, IllegalStateException {
//        throwExceptionIfClosed();

        if (bytesStore == null || !bytesStore.inside(offset))
            acquireNextByteStore0(offset, true);
        return bytesStore.addressForWrite(offset);
    }

    @Override
    protected void readCheckOffset(final long offset,
                                   final long adding,
                                   final boolean given)
            throws BufferUnderflowException, IllegalStateException {
        final long check = adding >= 0 ? offset : offset + adding;
        //noinspection StatementWithEmptyBody
        if (bytesStore != null && bytesStore.inside(check, adding)) {
            // nothing.
        } else {
            acquireNextByteStore0(offset, false);
        }
        super.readCheckOffset(offset, adding, given);
    }

    @Nullable
    @Override
    public String read8bit()
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, ArithmeticException {
//        throwExceptionIfClosed();

        return BytesInternal.read8bit(this);
    }

    @Override
    protected void writeCheckOffset(final long offset, final long adding)
            throws BufferOverflowException, IllegalStateException {

        throwExceptionIfClosed();
        if (offset < 0 || offset > mappedFile.capacity() - adding)
            throw writeBufferOverflowException(offset);
        if (bytesStore == null || !bytesStore.inside(offset, checkSize(adding))) {
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

        if (bytesStore == null || !bytesStore.inside(writePosition(), checkSize(desiredCapacity))) {
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

    private void acquireNextByteStore(final long offset, final boolean set)
            throws IllegalStateException {
        // if in the same chunk, can continue even if closed, but not released.
        if (bytesStore != null && bytesStore.inside(offset))
            return;

        // not allowed if closed.
        throwExceptionIfReleased();

        acquireNextByteStore0(offset, set);
    }

    // DON'T call this directly.
    // TODO Check whether we need synchronized; original comment; require protection from concurrent mutation to bytesStore field
    private synchronized void acquireNextByteStore0(final long offset, final boolean set)
            throws IllegalStateException {
        throwExceptionIfClosed();

        @Nullable final BytesStore oldBS = this.bytesStore;
        try {
            @NotNull final MappedBytesStore newBS = mappedFile.acquireByteStore(this, offset, oldBS);
            if (newBS != oldBS) {
                this.bytesStore(newBS);
                if (oldBS != null)
                    oldBS.release(this);
                if (lastActualSize < newBS.maximumLimit)
                    lastActualSize = newBS.maximumLimit;
            }
            assert this.bytesStore.reservedBy(this);

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
    }

    @NotNull
    @Override
    public Bytes<Void> readSkip(final long bytesToSkip)
            throws BufferUnderflowException, IllegalStateException {
        // called often so skip this check for performance
        // throwExceptionIfClosed();

        if (readPosition + bytesToSkip > readLimit()) throw new BufferUnderflowException();
        long check = bytesToSkip >= 0 ? this.readPosition : this.readPosition + bytesToSkip;
        if (bytesStore == null ||
                bytesToSkip != (int) bytesToSkip ||
                !bytesStore.inside(readPosition, (int) bytesToSkip)) {
            acquireNextByteStore0(check, false);
        }
        this.readPosition += bytesToSkip;
        return this;
    }

    @Override
    public @NotNull MappedBytesStore bytesStore() {
//        throwExceptionIfClosed();

        return (MappedBytesStore) super.bytesStore();
    }

    @Override
    public long start() {
//        throwExceptionIfClosed();

        return 0L;
    }

    @NotNull
    @Override
    public Bytes<Void> writePosition(final long position)
            throws BufferOverflowException {
//        throwExceptionIfClosed();

        if (position > writeLimit)
            throw new BufferOverflowException();
        if (position < 0L)
            throw new BufferOverflowException();
        if (position < readPosition)
            this.readPosition = position;
        uncheckedWritePosition(position);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> clear()
            throws IllegalStateException {
        // typically only used at the start of an operation so reject if closed.
        throwExceptionIfClosed();

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
        if (bytesStore == null || !bytesStore.inside(writePosition(), 1)) {
            // already determined we need it
            acquireNextByteStore0(writePosition(), false);
        }
        uncheckedWritePosition(writePosition() + 1);
        bytesStore.writeByte(oldPosition, i8);
        return this;
    }

    @Override
    protected void performRelease() {
        super.performRelease();
        try {
            if (mappedFile.refCount() > 0)
                mappedFile.release(this);
        } catch (IllegalStateException e) {
            Jvm.warn().on(getClass(), e);
        }
    }

    @Override
    protected boolean performReleaseInBackground() {
        return true;
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    public boolean isBackingFileReadOnly() {
//        throwExceptionIfClosed();

        return backingFileIsReadOnly;
    }

    @Override
    @NotNull
    public Bytes<Void> write(@NotNull final RandomDataInput bytes,
                             final long offset,
                             final long length)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (bytes instanceof BytesStore)
            write((BytesStore) bytes, offset, length);
        else if (length == 8)
            writeLong(bytes.readLong(offset));
        else if (length > 0)
            BytesInternal.writeFully(bytes, offset, length, this);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull final BytesStore bytes,
                             final long offset,
                             final long length)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
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

    @NotNull
    @Override
    public Bytes<Void> append8bit(@NotNull CharSequence cs, int start, int end)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException,
            IndexOutOfBoundsException, IllegalStateException {
        throwExceptionIfClosed();

        // check the start.
        long pos = writePosition();
        writeCheckOffset(pos, 0);
        if (!(cs instanceof String) || pos + (end - start) * 3 + 5 >= safeLimit()) {
            return super.append8bit(cs, start, end);
        }
        return append8bit0((String) cs, start, end - start);
    }

    @Override
    @NotNull
    public MappedBytes write8bit(@NotNull CharSequence s, int start, int length)
            throws IllegalStateException, BufferUnderflowException, BufferOverflowException, ArithmeticException, IndexOutOfBoundsException {
        throwExceptionIfClosed();

        ObjectUtils.requireNonNull(s);

        // check the start.
        long pos = writePosition();
        writeCheckOffset(pos, 0);
        if (!(s instanceof String) || pos + length * 3L + 5 >= safeLimit()) {
            super.write8bit(s, start, length);
            return this;
        }

        writeStopBit(length);
        return append8bit0((String) s, start, length);
    }

    @NotNull
    private MappedBytes append8bit0(@NotNull String s, int start, int length)
            throws BufferOverflowException, IllegalStateException {

        if (Jvm.isJava9Plus()) {
            byte[] bytes = extractBytes(s);
            long address = addressForWritePosition();
            Memory memory = bytesStore().memory;
            int i = 0;
            for (; i < length - 3; i += 4) {
                int c0 = bytes[i + start] & 0xff;
                int c1 = bytes[i + start + 1] & 0xff;
                int c2 = bytes[i + start + 2] & 0xff;
                int c3 = bytes[i + start + 3] & 0xff;
                memory.writeInt(address, (c3 << 24) | (c2 << 16) | (c1 << 8) | c0);
                address += 4;
            }
            for (; i < length; i++) {
                byte c = bytes[i + start];
                memory.writeByte(address++, c);
            }
            writeSkip(length);
        } else {
            char[] chars = extractChars(s);
            long address = addressForWritePosition();
            Memory memory = bytesStore().memory;
            int i = 0;
            for (; i < length - 3; i += 4) {
                int c0 = chars[i + start] & 0xff;
                int c1 = chars[i + start + 1] & 0xff;
                int c2 = chars[i + start + 2] & 0xff;
                int c3 = chars[i + start + 3] & 0xff;
                memory.writeInt(address, (c3 << 24) | (c2 << 16) | (c1 << 8) | c0);
                address += 4;
            }
            for (; i < length; i++) {
                char c = chars[i + start];
                memory.writeByte(address++, (byte) c);
            }
            writeSkip(length);
        }
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> appendUtf8(@NotNull CharSequence cs, int start, int length)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException, IndexOutOfBoundsException {
        throwExceptionIfClosed();

        // check the start.
        long pos = writePosition();
        writeCheckOffset(pos, 0);
        if (!(cs instanceof String) || pos + length * 3L + 5 >= safeLimit()) {
            super.appendUtf8(cs, start, length);
            return this;
        }

        if (Jvm.isJava9Plus()) {
            // byte[] bytes = extractBytes((String) cs);
            final String str = (String) cs;
            long address = addressForWrite(pos);
            Memory memory = OS.memory();
            int i = 0;
            non_ascii:
            {
                for (; i < length; i++) {
                    char c = str.charAt(i + start);
                    //byte c = bytes[i + start];
                    if (c > 127) {
                        writeSkip(i);
                        break non_ascii;
                    }
                    memory.writeByte(address++, (byte) c);
                }
                writeSkip(length);
                return this;
            }
            for (; i < length; i++) {
                char c = str.charAt(i + start);
                appendUtf8(c);
            }
        } else {
            char[] chars = extractChars((String) cs);
            long address = addressForWrite(pos);
            Memory memory = OS.memory();
            int i = 0;
            non_ascii:
            {
                for (; i < length; i++) {
                    char c = chars[i + start];
                    if (c > 127) {
                        writeSkip(i);
                        break non_ascii;
                    }
                    memory.writeByte(address++, (byte) c);
                }
                writeSkip(length);
                return this;
            }
            for (; i < length; i++) {
                char c = chars[i + start];
                appendUtf8(c);
            }
        }
        return this;
    }

    @Override
    public boolean sharedMemory() {
        return true;
    }

    @Override
    @NotNull
    public Bytes<Void> writeOrderedInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        writeCheckOffset(offset, 4);
        if (bytesStore == null || !bytesStore.inside(offset, 4)) {
            acquireNextByteStore0(offset, false);
        }
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @Override
    public byte readVolatileByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (bytesStore == null || !bytesStore.inside(offset, 1)) {
            acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (bytesStore == null || !bytesStore.inside(offset, 2)) {
            acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (bytesStore == null || !bytesStore.inside(offset, 4)) {
            acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (bytesStore == null || !bytesStore.inside(offset, 8)) {
            acquireNextByteStore0(offset, false);
        }
        return bytesStore.readVolatileLong(offset);
    }

    @Override
    public int peekUnsignedByte()
            throws IllegalStateException {
        throwExceptionIfClosed();

        if (bytesStore == null || !bytesStore.inside(readPosition, 1)) {
            acquireNextByteStore0(readPosition, false);
        }
        return super.peekUnsignedByte();
    }

    @Override
    public int peekUnsignedByte(final long offset)
            throws BufferUnderflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (bytesStore == null || !bytesStore.inside(offset, 1)) {
            acquireNextByteStore0(offset, false);
        }
        return super.peekUnsignedByte(offset);
    }

    @SuppressWarnings("restriction")
    @Override
    public int peekVolatileInt()
            throws IllegalStateException {

        if (bytesStore == null || !bytesStore.inside(readPosition, 4)) {
            acquireNextByteStore0(readPosition, true);
        }

        @Nullable MappedBytesStore bytesStore = this.bytesStore;
        long address = bytesStore.address + bytesStore.translate(readPosition);
        @Nullable Memory memory = bytesStore.memory;

        // are we inside a cache line?
        if ((address & 63) <= 60) {
            ObjectUtils.requireNonNull(memory);
            UnsafeMemory.unsafeLoadFence();
            return UnsafeMemory.unsafeGetInt(address);
        } else {
            return memory.readVolatileInt(address);
        }
    }

    @Override
    public void release(ReferenceOwner id)
            throws IllegalStateException {
        super.release(id);
        if (refCount() <= 0)
            closeable.close();
    }

    @Override
    public void releaseLast(ReferenceOwner id)
            throws IllegalStateException {
        super.releaseLast(id);
        closeable.close();
    }

    @Override
    public void close() {
        closeable.close();
    }

    void performClose() {
        try {
            if (refCount() > 0)
                release(INIT);
        } catch (IllegalStateException e) {
            Jvm.warn().on(getClass(), e);
        }
    }

    @Override
    public boolean isClosed() {
        return closeable.isClosed();
    }

    @Override
    public void warnAndCloseIfNotClosed() {
        closeable.warnAndCloseIfNotClosed();
    }

    @Override
    public void throwExceptionIfClosed() throws IllegalStateException {
        closeable.throwExceptionIfClosed();
    }

    @NotNull
    @Override
    public Bytes<Void> writeUtf8(CharSequence str)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (str instanceof String) {
            writeUtf8((String) str);
            return this;
        }
        if (str == null) {
            BytesInternal.writeStopBitNeg1(this);

        } else {
            long utfLength = AppendableUtil.findUtf8Length(str);
            this.writeStopBit(utfLength);
            BytesInternal.appendUtf8(this, str, 0, str.length());
        }
        return this;
    }

    @Override
    public @NotNull Bytes<Void> writeUtf8(String str)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (str == null) {
            BytesInternal.writeStopBitNeg1(this);
            return this;
        }

        try {
            if (Jvm.isJava9Plus()) {
                byte[] strBytes = extractBytes(str);
                byte coder = getStringCoder(str);
                long utfLength = AppendableUtil.findUtf8Length(strBytes, coder);
                writeStopBit(utfLength);
                appendUtf8(strBytes, 0, str.length(), coder);
            } else {
                char[] chars = extractChars(str);
                long utfLength = AppendableUtil.findUtf8Length(chars);
                writeStopBit(utfLength);
                appendUtf8(chars, 0, chars.length);
            }
            return this;
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> appendUtf8(char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        throwExceptionIfClosed();

        if (writePosition() < 0 || writePosition() > capacity() - (long) 1 + length)
            throw writeBufferOverflowException(writePosition());
        int i;
        ascii:
        {
            for (i = 0; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                long oldPosition = writePosition();
                if (bytesStore == null || ((writePosition() & 0xff) == 0 && !bytesStore.inside(writePosition(), (length - i) * 3L))) {
                    acquireNextByteStore0(writePosition(), false);
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

    @Override
    public long readStopBit()
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        long offset = readOffsetPositionMoved(1);
        byte l = bytesStore.readByte(offset);

        if (l >= 0)
            return l;
        return BytesInternal.readStopBit0(this, l);
    }

    @Override
    public char readStopBitChar()
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        long offset = readOffsetPositionMoved(1);
        byte l = bytesStore.readByte(offset);

        if (l >= 0)
            return (char) l;
        return (char) BytesInternal.readStopBit0(this, l);
    }

    @NotNull
    @Override
    public Bytes<Void> writeStopBit(long n)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if ((n & ~0x7F) == 0) {
            writeByte((byte) (n & 0x7f));
            return this;
        }
        if ((~n & ~0x7F) == 0) {
            writeByte((byte) (0x80L | ~n));
            writeByte((byte) 0);
            return this;
        }

        if ((n & ~0x3FFF) == 0) {
            writeByte((byte) ((n & 0x7f) | 0x80));
            writeByte((byte) (n >> 7));
            return this;
        }
        BytesInternal.writeStopBit0(this, n);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> writeStopBit(char n)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if ((n & ~0x7F) == 0) {
            writeByte((byte) (n & 0x7f));
            return this;
        }

        if ((n & ~0x3FFF) == 0) {
            writeByte((byte) ((n & 0x7f) | 0x80));
            writeByte((byte) (n >> 7));
            return this;
        }
        BytesInternal.writeStopBit0(this, n);
        return this;
    }

    @Override
    public boolean isDirectMemory() {
        return true;
    }

    // used by the Pretoucher, don't change this without considering the impact.
    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws BufferOverflowException, IllegalStateException {
        throwExceptionIfClosed();

        if (offset < 0 || offset > mappedFile.capacity() - (long) 8)
            throw writeBufferOverflowException(offset);
        if (bytesStore == null || bytesStore.start() > offset || offset + 8 >= bytesStore.safeLimit()) {
            acquireNextByteStore0(offset, false);
        }
//        super.writeCheckOffset(offset, adding);
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    public MappedBytes disableThreadSafetyCheck(boolean disableThreadSafetyCheck) {
        this.disableThreadSafetyCheck = disableThreadSafetyCheck;
        return this;
    }
}
