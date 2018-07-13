/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MappedBytes extends AbstractBytes<Void> implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappedBytes.class);
    private static final boolean ENFORCE_SINGLE_THREADED_ACCESS =
            Boolean.getBoolean("chronicle.bytes.enforceSingleThreadedAccess");
    @NotNull
    private final MappedFile mappedFile;
    private final boolean backingFileIsReadOnly;
    private volatile Thread lastAccessedThread;
    private volatile RuntimeException writeStack;

    // assume the mapped file is reserved already.
    protected MappedBytes(@NotNull MappedFile mappedFile) throws IllegalStateException {
        this(mappedFile, "");
    }

    protected MappedBytes(@NotNull MappedFile mappedFile, String name) throws IllegalStateException {
        super(NoBytesStore.noBytesStore(), NoBytesStore.noBytesStore().writePosition(),
                NoBytesStore.noBytesStore().writeLimit(), name);

        this.mappedFile = reserve(mappedFile);
        this.backingFileIsReadOnly = !mappedFile.file().canWrite();
        assert !mappedFile.isClosed();
        clear();
    }

    @NotNull
    private static MappedFile reserve(@NotNull MappedFile mappedFile) {
        mappedFile.reserve();
        return mappedFile;
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull String filename, long chunkSize)
            throws FileNotFoundException, IllegalStateException {
        return mappedBytes(new File(filename), chunkSize);
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull File file, long chunkSize)
            throws FileNotFoundException, IllegalStateException {
        return mappedBytes(file, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull File file, long chunkSize, long overlapSize)
            throws FileNotFoundException, IllegalStateException {
        @NotNull MappedFile rw = MappedFile.of(file, chunkSize, overlapSize, false);
        try {
            return mappedBytes(rw);
        } finally {
            rw.release();
        }
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull File file,
                                          long chunkSize,
                                          long overlapSize,
                                          boolean readOnly) throws FileNotFoundException,
            IllegalStateException {
        @NotNull MappedFile rw = MappedFile.of(file, chunkSize, overlapSize, readOnly);
        try {
            return mappedBytes(rw);
        } finally {
            rw.release();
        }
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull MappedFile rw) {
        return new MappedBytes(rw);
    }

    @NotNull
    public static MappedBytes readOnly(@NotNull File file) throws FileNotFoundException {
        return new MappedBytes(MappedFile.readOnly(file));
    }

    public MappedBytes write(byte[] bytes, int offset, int length) {
        write(writePosition, bytes, offset, length);
        writePosition += Math.min(length, bytes.length - offset);
        return this;
    }

    public MappedBytes write(long offsetInRDO, byte[] bytes, int offset, int length) {

        long wp = offsetInRDO;
        if ((length + offset) > bytes.length)
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + bytes.length + ", " + "length=" + length + ", offset=" + offset);

        if (length > writeRemaining())
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));

        int remaining = length;

        acquireNextByteStore(wp, false);

        while (remaining > 0) {

            long copySize = copySize(wp);

            // remaining is an int and safeCopySize is >= 0.
            int copy = (int) Math.min(remaining, copySize); // copy 64 KB at a time.

            bytesStore.write(wp, bytes, offset, copy);
            offset += copy;
            wp += copy;
            remaining -= copy;

            if (remaining == 0)
                return this;

            // move to the next chunk
            acquireNextByteStore0(wp, false);
        }
        return this;

    }

    public MappedBytes write(long writeOffset, RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException {
        if (readOffset + length <= bytes.realCapacity() && length <= 80)
            writeLittle(writeOffset, bytes, readOffset, length);
        else
            write0(writeOffset, bytes, readOffset, length);
        return this;
    }

    private void writeLittle(long writeOffset, RandomDataInput bytes, long readOffset, long length) {
        writeCheckOffset(writeOffset, (length + 7) & ~7);
        while (length > 0) {
            long read = bytes.readLong(readOffset);
            bytesStore.writeLong(writeOffset, read);
            length -= 8;
            readOffset += 8;
            writeOffset += 8;
        }
    }

    public MappedBytes write0(long writeOffset, RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException {

        long wp = writeOffset;

        if (length > writeRemaining())
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));

        long remaining = length;

        acquireNextByteStore(wp, false);

        while (remaining > 0) {

            long safeCopySize = copySize(wp);
            long copy = Math.min(remaining, safeCopySize); // copy 64 KB at a time.

            bytesStore.write(wp, bytes, readOffset, copy);

            readOffset += copy;
            wp += copy;
            remaining -= copy;

            if (remaining == 0)
                return this;

            // move to the next chunk
            acquireNextByteStore0(wp, false);
        }
        return this;
    }

    @NotNull
    @Override
    public MappedBytes write(@NotNull BytesStore bytes)
            throws BufferOverflowException {
        assert singleThreadedAccess();
        assert bytes != this : "you should not write to yourself !";
        long remaining = bytes.readRemaining();
        write(writePosition, bytes);
        writePosition += remaining;
        return this;
    }

    @NotNull
    public MappedBytes write(long offsetInRDO, @NotNull BytesStore bytes)
            throws BufferOverflowException {
        write(offsetInRDO, bytes, bytes.readPosition(), bytes.readRemaining());
        return this;
    }

    private long copySize(long writePosition) {
        return bytesStore.capacity() - writePosition % bytesStore.capacity();
    }

    public void setNewChunkListener(NewChunkListener listener) {
        mappedFile.setNewChunkListener(listener);
    }

    @NotNull
    public MappedFile mappedFile() {
        return mappedFile;
    }

    @NotNull
    public MappedBytes withSizes(long chunkSize, long overlapSize) {
        @NotNull MappedFile mappedFile2 = this.mappedFile.withSizes(chunkSize, overlapSize);
        if (mappedFile2 == this.mappedFile)
            return this;
        try {
            return mappedBytes(mappedFile2);
        } finally {
            mappedFile2.release();
            release();
        }
    }

    @Override
    public BytesStore<Bytes<Void>, Void> copy() {
        return NativeBytes.copyOf(this);
    }

    @Override
    public long capacity() {
        return mappedFile == null ? 0L : mappedFile.capacity();
    }

    @Override
    public long realCapacity() {
        try {
            return mappedFile.actualSize();

        } catch (IORuntimeException e) {
            Jvm.warn().on(getClass(), "Unable to obtain the real size for " + mappedFile.file(), e);
            return 0;
        }
    }

    @NotNull
    @Override
    public Bytes<Void> readPositionRemaining(long position, long remaining) throws BufferUnderflowException {
        long limit = position + remaining;
        if (!bytesStore.inside(position, Math.toIntExact(remaining))) {
            acquireNextByteStore(position, true);
        } else if (!bytesStore.inside(limit)) {
            acquireNextByteStore(limit, false);
        }

        if (writeLimit < limit)
            writeLimit(limit);

        boolean debug = false;
        assert debug = true;
        if (debug)
            readLimit(limit);
        else
            uncheckedWritePosition(limit);

        return readPosition(position);
    }

    @NotNull
    @Override
    public Bytes<Void> readPosition(long position) throws BufferUnderflowException {
        if (bytesStore.inside(position)) {
            return super.readPosition(position);
        } else {
            acquireNextByteStore(position, true);
            return this;
        }
    }

    @Override
    public long addressForRead(long offset) throws BufferUnderflowException {
        if (!bytesStore.inside(offset))
            acquireNextByteStore(offset, true);
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForRead(long offset, int buffer) throws UnsupportedOperationException, BufferUnderflowException {
        if (!bytesStore.inside(offset, buffer))
            acquireNextByteStore(offset, true);
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(long offset) throws UnsupportedOperationException, BufferOverflowException {
        if (!bytesStore.inside(offset))
            acquireNextByteStore(offset, true);
        return bytesStore.addressForWrite(offset);
    }

    @Override
    protected void readCheckOffset(long offset, long adding, boolean given) throws BufferUnderflowException {
        long check = adding >= 0 ? offset : offset + adding;
        if (adding < 1 << 10
                ? !bytesStore.inside(check, (int) adding)
                : !bytesStore.inside(check)) {
            acquireNextByteStore(offset, false);
        }
        super.readCheckOffset(offset, adding, given);
    }

    @Nullable
    @Override
    public String read8bit() throws IORuntimeException, BufferUnderflowException {
        return BytesInternal.read8bit(this);
    }

    @Override
    protected void writeCheckOffset(long offset, long adding) throws BufferOverflowException {
        assert singleThreadedAccess();
        if (offset < 0 || offset > mappedFile.capacity() - adding)
            throw writeBufferOverflowException(offset);
        if (!bytesStore.inside(offset, Math.toIntExact(adding))) {
            acquireNextByteStore(offset, false);
        }
//        super.writeCheckOffset(offset, adding);
    }

    @NotNull
    private BufferOverflowException writeBufferOverflowException(long offset) {
        BufferOverflowException exception = new BufferOverflowException();
        exception.initCause(new IllegalArgumentException("Offset out of bound " + offset));
        return exception;
    }

    private void acquireNextByteStore(long offset, boolean set) throws BufferOverflowException {
        if (bytesStore.inside(offset))
            return;

        acquireNextByteStore0(offset, set);
    }

    // DON'T call this directly.
    // TODO Check whether we need synchronized; original comment; require protection from concurrent mutation to bytesStore field
    private synchronized void acquireNextByteStore0(final long offset, final boolean set) {
        @Nullable BytesStore oldBS = bytesStore;
        try {
            @Nullable BytesStore newBS = mappedFile.acquireByteStore(offset);
            bytesStore = newBS;
            oldBS.release();

        } catch (@NotNull IOException | IllegalStateException | IllegalArgumentException e) {
            @NotNull BufferOverflowException boe = new BufferOverflowException();
            boe.initCause(e);
            throw boe;
        }
        if (set) {
            if (writeLimit() < readPosition)
                writeLimit(readPosition);
            if (readLimit() < readPosition)
                readLimit(readPosition);
            readPosition = offset;
        }
    }

    @NotNull
    @Override
    public Bytes<Void> readSkip(long bytesToSkip)
            throws BufferUnderflowException {
        if (readPosition + bytesToSkip > readLimit()) throw new BufferUnderflowException();
        long check = bytesToSkip >= 0 ? this.readPosition : this.readPosition + bytesToSkip;
        if (bytesStore instanceof NoBytesStore ||
                bytesToSkip != (int) bytesToSkip ||
                !((MappedBytesStore) (BytesStore) bytesStore).inside(readPosition, (int) bytesToSkip)) {
            acquireNextByteStore(check, false);
        }
        this.readPosition += bytesToSkip;
        return this;
    }

    @Nullable
    @Override
    public MappedBytesStore bytesStore() {
        return (MappedBytesStore) super.bytesStore();
    }

    @Override
    public long start() {
        return 0L;
    }

    @NotNull
    @Override
    public Bytes<Void> writePosition(long position) throws BufferOverflowException {
        assert singleThreadedAccess();
        if (position > writeLimit)
            throw new BufferOverflowException();
        if (position < 0L)
            throw new BufferUnderflowException();
        if (position < readPosition)
            this.readPosition = position;
        this.writePosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> clear() {
        long start = 0L;
        readPosition = start;
        this.writePosition = start;
        writeLimit = mappedFile.capacity();
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> writeByte(byte i8) throws BufferOverflowException {
        assert singleThreadedAccess();
        long oldPosition = writePosition;
        if (writePosition < 0 || writePosition > capacity() - (long) 1)
            throw writeBufferOverflowException(writePosition);
        if (!bytesStore.inside(writePosition, 1)) {
            acquireNextByteStore(writePosition, false);
        }
        this.writePosition = writePosition + (long) 1;
        bytesStore.writeByte(oldPosition, i8);
        return this;
    }

    @Override
    protected void performRelease() throws IllegalStateException {
        super.performRelease();
        mappedFile.release();
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    public boolean isBackingFileReadOnly() {
        return backingFileIsReadOnly;
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull BytesStore bytes, long offset, long length)
            throws BufferUnderflowException, BufferOverflowException {
        assert singleThreadedAccess();
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else if (bytes.isDirectMemory() && length <= Math.min(writeRemaining(), safeCopySize())) {
            rawCopy(bytes, offset, length);

        } else if (length > 0) {
            BytesInternal.writeFully(bytes, offset, length, this);
        }
        return this;
    }

    public void rawCopy(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException {
        long len = Math.min(writeRemaining(), Math.min(bytes.readRemaining(), length));
        if (len > 0) {
            OS.memory().copyMemory(bytes.addressForRead(offset), addressForWrite(writePosition()), len);
            uncheckedWritePosition(writePosition() + len);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> append8bit(@NotNull CharSequence cs, int start, int end)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException,
            IndexOutOfBoundsException {
        assert singleThreadedAccess();
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
    public MappedBytes write8bit(CharSequence s, int start, int length) {
        assert singleThreadedAccess();
        if (s == null) {
            writeStopBit(-1);
            return this;
        }
        // check the start.
        long pos = writePosition();
        writeCheckOffset(pos, 0);
        if (!(s instanceof String) || pos + length * 3 + 5 >= safeLimit()) {
            super.write8bit(s, start, length);
            return this;
        }

        writeStopBit(length);
        return append8bit0((String) s, start, length);
    }

    @NotNull
    private MappedBytes append8bit0(@NotNull String s, int start, int length) throws BufferOverflowException {
        assert singleThreadedAccess();
        if (Jvm.isJava9Plus()) {
            byte[] bytes = extractBytes(s);
            long address = addressForWrite(writePosition());
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
            long address = addressForWrite(writePosition());
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
    public Bytes<Void> appendUtf8(CharSequence cs, int start, int length)
            throws BufferOverflowException, IllegalArgumentException {
        assert singleThreadedAccess();
        // check the start.
        long pos = writePosition();
        writeCheckOffset(pos, 0);
        if (!(cs instanceof String) || pos + length * 3 + 5 >= safeLimit()) {
            super.appendUtf8(cs, start, length);
            return this;
        }

        if (Jvm.isJava9Plus()) {
            byte[] bytes = extractBytes((String) cs);
            long address = addressForWrite(pos);
            Memory memory = OS.memory();
            int i = 0;
            non_ascii:
            {
                for (; i < length; i++) {
                    byte c = bytes[i + start];
                    if (c > 127) {
                        writeSkip(i);
                        break non_ascii;
                    }
                    memory.writeByte(address++, c);
                }
                writeSkip(length);
                return this;
            }
            for (; i < length; i++) {
                byte c = bytes[i + start];
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
    public Bytes<Void> writeOrderedInt(long offset, int i) throws BufferOverflowException {
        assert singleThreadedAccess();
        assert writeCheckOffset0(offset, (long) 4);
        if (!bytesStore.inside(offset, 4)) {
            acquireNextByteStore(offset, false);
        }
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @Override
    public byte readVolatileByte(long offset) throws BufferUnderflowException {
        if (!bytesStore.inside(offset, 1)) {
            acquireNextByteStore(offset, false);
        }
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset) throws BufferUnderflowException {
        if (!bytesStore.inside(offset, 2)) {
            acquireNextByteStore(offset, false);
        }
        return bytesStore.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset) throws BufferUnderflowException {
        if (!bytesStore.inside(offset, 4)) {
            acquireNextByteStore(offset, false);
        }
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset) throws BufferUnderflowException {
        if (!bytesStore.inside(offset, 8)) {
            acquireNextByteStore(offset, false);
        }
        return bytesStore.readVolatileLong(offset);
    }

    @Override
    public int peekVolatileInt() {
        if (!bytesStore.inside(readPosition, 4)) {
            acquireNextByteStore(readPosition, true);
        }

        @Nullable MappedBytesStore bytesStore = (MappedBytesStore) (BytesStore) this.bytesStore;
        long address = bytesStore.address + bytesStore.translate(readPosition);
        @Nullable Memory memory = bytesStore.memory;

        // are we inside a cache line?
        if ((address & 63) <= 60) {
            // if (memory == null) throw new NullPointerException();
            memory.getClass(); // faster check for null.
            return UnsafeMemory.UNSAFE.getIntVolatile(null, address);

        } else {
            return memory.readVolatileInt(address);
        }
    }

    @Override
    public void close() {
        this.release();
    }

    @Override
    public boolean isClosed() {
        return this.refCount() <= 0;
    }


    @NotNull
    @Override
    public Bytes<Void> writeUtf8(CharSequence str) throws BufferOverflowException {
        assert singleThreadedAccess();
        if (str instanceof String) {
            writeUtf8((String) str);
            return this;
        }
        if (str == null) {
            this.writeStopBit(-1);

        } else {
            try {
                long utfLength = AppendableUtil.findUtf8Length(str);
                this.writeStopBit(utfLength);
                BytesInternal.appendUtf8(this, str, 0, str.length());
            } catch (IndexOutOfBoundsException e) {
                throw new AssertionError(e);
            }
        }
        return this;
    }

    @Override
    public Bytes<Void> writeUtf8(String str) throws BufferOverflowException {
        assert singleThreadedAccess();
        if (str == null) {
            writeStopBit(-1);
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
    public Bytes<Void> appendUtf8(char[] chars, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
        assert singleThreadedAccess();
        if (writePosition < 0 || writePosition > capacity() - (long) 1 + length)
            throw writeBufferOverflowException(writePosition);
        int i;
        ascii:
        {
            for (i = 0; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                long oldPosition = writePosition;
                if ((writePosition & 0xff) == 0 && !bytesStore.inside(writePosition, (length - i) * 3)) {
                    acquireNextByteStore(writePosition, false);
                }
                this.writePosition = writePosition + (long) 1;
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
    public long readStopBit() throws IORuntimeException {
        long offset = readOffsetPositionMoved(1);
        byte l = bytesStore.readByte(offset);

        if (l >= 0)
            return l;
        return BytesInternal.readStopBit0(this, l);
    }

    @Override
    public char readStopBitChar() throws IORuntimeException {
        long offset = readOffsetPositionMoved(1);
        byte l = bytesStore.readByte(offset);

        if (l >= 0)
            return (char) l;
        return (char) BytesInternal.readStopBit0(this, l);
    }

    @NotNull
    @Override
    public Bytes<Void> writeStopBit(long n) throws BufferOverflowException {
        assert singleThreadedAccess();
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
    public Bytes<Void> writeStopBit(char n) throws BufferOverflowException {
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

    private boolean singleThreadedAccess() {
        if (!ENFORCE_SINGLE_THREADED_ACCESS) {
            return true;
        }
        if (lastAccessedThread == null) {
            lastAccessedThread = Thread.currentThread();
        }
        final boolean isSingleThreaded = lastAccessedThread == Thread.currentThread();
        if (!isSingleThreaded) {
            LOGGER.warn("Detected multi-threaded write access. Initial write stack:", writeStack);
            LOGGER.warn("Current write stack: ", new RuntimeException());
        }
        if (writeStack == null) {
            writeStack = new RuntimeException();
        }
        return isSingleThreaded;
    }

    @Override
    public boolean isDirectMemory() {
        return true;
    }

    public MappedBytes write8bit(@Nullable BytesStore bs)
            throws BufferOverflowException {
        if (bs == null) {
            writeStopBit(-1);
        } else {
            long offset = bs.readPosition();
            long readRemaining = Math.min(writeRemaining(), bs.readLimit() - offset);
            writeStopBit(readRemaining);
            write(bs, offset, readRemaining);
        }
        return this;
    }
}