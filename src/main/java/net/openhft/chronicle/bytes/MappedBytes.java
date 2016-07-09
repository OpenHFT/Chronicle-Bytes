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

import net.openhft.chronicle.core.Jvm;
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

import static net.openhft.chronicle.core.util.StringUtils.extractChars;

/**
 * Bytes to wrap memory mapped data.
 */
public class MappedBytes extends AbstractBytes<Void> {
    public static boolean CHECKING = false;
    private final MappedFile mappedFile;

    // assume the mapped file is reserved already.
    protected MappedBytes(MappedFile mappedFile) throws IllegalStateException {
        this(mappedFile, "");
    }

    protected MappedBytes(MappedFile mappedFile, String name) throws IllegalStateException {
        super(NoBytesStore.noBytesStore(), NoBytesStore.noBytesStore().writePosition(),
                NoBytesStore.noBytesStore().writeLimit(), name);
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
        try {
            return mappedFile.actualSize();

        } catch (IORuntimeException e) {
            Jvm.warn().on(getClass(), "Unable to obtain the real size for " + mappedFile.file(), e);
            return 0;
        }
    }

    @Override
    protected void readCheckOffset(long offset, long adding, boolean given) throws BufferUnderflowException {
        long check = adding >= 0 ? offset : offset + adding;
        if (!bytesStore.inside(check)) {
            BytesStore oldBS = bytesStore;
            try {
                bytesStore = (BytesStore) mappedFile.acquireByteStore(offset);
                oldBS.release();

            } catch (IOException | IllegalStateException | IllegalArgumentException e) {
                BufferUnderflowException bue = new BufferUnderflowException();
                bue.initCause(e);
                throw bue;
            }
        }
        super.readCheckOffset(offset, adding, given);
    }

    @Override
    protected void writeCheckOffset(long offset, long adding) throws BufferOverflowException {
        if (offset < 0 || offset > capacity() - adding)
            throw new IllegalArgumentException("Offset out of bound " + offset);
        if (!bytesStore.inside(offset)) {
            acquireNextByteStore(offset);
        }
        super.writeCheckOffset(offset, adding);
    }

    private void acquireNextByteStore(long offset) {
        BytesStore oldBS = bytesStore;
        try {
            bytesStore = (BytesStore) mappedFile.acquireByteStore(offset);
            oldBS.release();

        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            BufferOverflowException boe = new BufferOverflowException();
            boe.initCause(e);
            throw boe;
        }
    }

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
            throws BufferUnderflowException, BufferOverflowException {
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else if (bytes.isDirectMemory() && length >= 16) {
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
            uncheckedWritePosition(writePosition() + len);
        }
    }

    @Override
    public Bytes<Void> append8bit(@NotNull CharSequence cs, int start, int end) throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException {
        return cs instanceof String
                ? append8bit0((String) cs, start, end - start)
                : super.append8bit(cs, start, end);
    }

    @Override
    public Bytes<Void> writeUtf8(String s) throws BufferOverflowException {
        char[] chars = extractChars(s);
        long utfLength = AppendableUtil.findUtf8Length(chars);
        writeStopBit(utfLength);
        appendUtf8(chars, 0, chars.length);
        return this;
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
        return append8bit0((String) s, start, length);
    }

    @NotNull
    private MappedBytes append8bit0(String s, int start, int length) {
        char[] chars = StringUtils.extractChars(s);
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
    public Bytes<Void> appendUtf8(CharSequence cs, int start, int length) throws BufferOverflowException, IllegalArgumentException {
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

    @Override
    public boolean sharedMemory() {
        return true;
    }

    public Bytes<Void> writeOrderedInt(long offset, int i) throws BufferOverflowException {
        assert writeCheckOffset0(offset, (long) 4);
        if (!bytesStore.inside(offset)) {
            acquireNextByteStore(offset);
        }
        ((NativeBytesStore) bytesStore).writeOrderedInt(offset, i);
        return this;
    }

    @Override
    public int peekVolatileInt() {
        readCheckOffset(readPosition, 4, true);
        MappedBytesStore bytesStore = (MappedBytesStore) (BytesStore) this.bytesStore;
        long address = bytesStore.address + bytesStore.translate(readPosition);
        Memory memory = bytesStore.memory;
        for (int i = 0; i < 128; i++) {
            int value = memory.readVolatileInt(address);
            if (value != 0)
                return value;
        }
        return 0;
    }
}
