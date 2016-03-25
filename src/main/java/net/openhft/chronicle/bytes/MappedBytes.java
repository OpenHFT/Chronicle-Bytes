/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Bytes to wrap memory mapped data.
 */
public class MappedBytes extends AbstractBytes<Void> {
    public static boolean CHECKING = false;
    private final MappedFile mappedFile;

    // assume the mapped file is reserved already.
    protected MappedBytes(MappedFile mappedFile) throws IllegalStateException {
        super(NoBytesStore.noBytesStore(), NoBytesStore.noBytesStore().writePosition(), NoBytesStore.noBytesStore().writeLimit());
        this.mappedFile = mappedFile;
        clear();
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull String filename, long chunkSize) throws FileNotFoundException, IllegalStateException {
        return mappedBytes(new File(filename), chunkSize);
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull File file, long chunkSize) throws FileNotFoundException, IllegalStateException {
        return mappedBytes(file, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull File file, long chunkSize, long overlapSize) throws FileNotFoundException, IllegalStateException {
        MappedFile rw = MappedFile.of(file, chunkSize, overlapSize);
        return mappedBytes(rw);
    }

    @NotNull
    public static MappedBytes mappedBytes(MappedFile rw) {
        return CHECKING ? new CheckingMappedBytes(rw) : new MappedBytes(rw);
    }

    public void setNewChunkListener(NewChunkListener listener) {
        mappedFile.setNewChunkListener(listener);
    }

    public MappedFile mappedFile() {
        return mappedFile;
    }

    public MappedBytes withSizes(long chunkSize, long overlapSize) {
        MappedFile mappedFile2 = this.mappedFile.withSizes(chunkSize, overlapSize);
        if (mappedFile2 == this.mappedFile)
            return this;
        try {
            return mappedBytes(mappedFile2);
        } finally {
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
        return mappedFile.actualSize();
    }

    @Override
    public long refCount() {
        return Math.max(super.refCount(), mappedFile.refCount());
    }

    @Override
    protected void readCheckOffset(long offset, long adding, boolean given) throws BufferUnderflowException, IORuntimeException {
        if (!bytesStore.inside(offset)) {
            BytesStore oldBS = bytesStore;
            try {
                bytesStore = (BytesStore) mappedFile.acquireByteStore(offset);
                oldBS.release();
            } catch (IOException | IllegalStateException e) {
                throw new IORuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new BufferUnderflowException();
            }
        }
    }

    @Override
    protected void writeCheckOffset(long offset, long adding) throws BufferOverflowException, IORuntimeException {
        if (offset < 0 || offset > capacity() - adding)
            throw new IllegalArgumentException("Offset out of bound " + offset);
        if (!bytesStore.inside(offset)) {
            BytesStore oldBS = bytesStore;
            try {
                bytesStore = (BytesStore) mappedFile.acquireByteStore(offset);
                oldBS.release();
            } catch (IOException | IllegalStateException e) {
                throw new IORuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new BufferOverflowException();
            }
        }
    }// 07721192269

    @Override
    public long start() {
        return 0L;
    }

    @Override
    protected void performRelease() {
        super.performRelease();
        mappedFile.close();
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull BytesStore bytes, long offset, long length)
            throws IORuntimeException, BufferUnderflowException, BufferOverflowException {
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else if (bytes.bytesStore() instanceof NativeBytesStore && length >= 16) {
            rawCopy(bytes, offset, length);

        } else {
            BytesInternal.writeFully(bytes, offset, length, this);
        }
        return this;
    }

    public void rawCopy(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException {
        long len = Math.min(writeRemaining(), Math.min(bytes.readRemaining(), length));
        if (len > 0) {
            OS.memory().copyMemory(bytes.address(offset), address(writePosition()), len);
            writePosition += len;
        }
    }

    public MappedBytes write8bit(CharSequence s, int start, int length) {
        // check the start.
        long pos = writePosition();
        writeCheckOffset(pos, 0);
        if (!(s instanceof String) || pos + length + 5 >= safeLimit()) {
            super.write8bit(s, start, length);
            return this;
        }

        writeStopBit(length);
        char[] chars = StringUtils.extractChars((String) s);
        long address = address(writePosition());
        Memory memory = OS.memory();
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
        return this;
    }

    @Override
    public Bytes<Void> appendUtf8(CharSequence cs, int start, int length) throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        // check the start.
        long pos = writePosition();
        writeCheckOffset(pos, 0);
        if (!(cs instanceof String) || pos + length + 5 >= safeLimit()) {
            super.appendUtf8(cs, start, length);
            return this;
        }

        char[] chars = StringUtils.extractChars((String) cs);
        long address = address(pos);
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
        return this;
    }
}
