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
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.util.StringUtils.extractBytes;
import static net.openhft.chronicle.core.util.StringUtils.extractChars;

/**
 * Fast unchecked version of AbstractBytes
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UncheckedBytes<Underlying> extends AbstractBytes<Underlying> {
    Bytes underlyingBytes;

    public UncheckedBytes(@NotNull Bytes underlyingBytes) throws IllegalStateException {
        super(underlyingBytes.bytesStore(), underlyingBytes.writePosition(), underlyingBytes.writeLimit());
        this.underlyingBytes = underlyingBytes;
        readPosition(underlyingBytes.readPosition());
    }

    public void setBytes(@NotNull Bytes bytes) throws IllegalStateException {
        BytesStore underlyingBytes = bytes.bytesStore();
        if (bytesStore != underlyingBytes) {
            bytesStore.release();
            this.bytesStore = underlyingBytes;
            bytesStore.reserve();
        }
        readPosition(bytes.readPosition());
        this.uncheckedWritePosition(bytes.writePosition());
        this.writeLimit = bytes.writeLimit();

        assert !bytesStore.isDirectMemory() || BytesUtil.register(this);
        this.underlyingBytes = bytes;
    }

    @Override
    public void ensureCapacity(long size) throws IllegalArgumentException {
        if (size > realCapacity()) {
            underlyingBytes.ensureCapacity(size);
            bytesStore = underlyingBytes.bytesStore();
        }
    }

    @Override
    @NotNull
    public Bytes<Underlying> unchecked(boolean unchecked) {
        return this;
    }

    @Override
    public boolean unchecked() {
        return true;
    }

    @Override
    void writeCheckOffset(long offset, long adding) {
    }

    @Override
    void readCheckOffset(long offset, long adding, boolean given) {
    }

    @Override
    void prewriteCheckOffset(long offset, long subtracting) {
    }

    @NotNull
    @Override
    public Bytes<Underlying> readPosition(long position) {
        readPosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readLimit(long limit) {
        uncheckedWritePosition(limit);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writePosition(long position) {
        uncheckedWritePosition(position);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readSkip(long bytesToSkip) {
        readPosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeSkip(long bytesToSkip) {
        uncheckedWritePosition(writePosition() + bytesToSkip);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLimit(long limit) {
        writeLimit = limit;
        return this;
    }

    @NotNull
    @Override
    public BytesStore<Bytes<Underlying>, Underlying> copy() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean isElastic() {
        return false;
    }

    @Override
    protected long readOffsetPositionMoved(long adding) {
        long offset = readPosition;
        readPosition += adding;
        return offset;
    }

    @Override
    protected long writeOffsetPositionMoved(long adding, long advance) {
        long oldPosition = writePosition();
        uncheckedWritePosition(writePosition() + advance);
        return oldPosition;
    }

    @Override
    protected long prewriteOffsetPositionMoved(long subtracting) throws BufferOverflowException {
        return readPosition -= subtracting;
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, IllegalArgumentException {
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else {
            super.write(bytes, offset, length);
        }
        return this;
    }

    @NotNull
    public Bytes<Underlying> write(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, IllegalArgumentException {
        if (length == 8) {
            writeLong(bytes.readLong(offset));
        } else if (bytes.underlyingObject() == null
                && bytesStore
                .isDirectMemory() &&
                length >= 32) {
            rawCopy(bytes, offset, length);

        } else {
            super.write(bytes, offset, length);
        }
        return this;
    }

    @Override
    @NotNull
    public Bytes<Underlying> append8bit(@NotNull CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException {
        if (cs instanceof RandomDataInput) {
            return write((RandomDataInput) cs);
        }

        int length = cs.length();
        long offset = writeOffsetPositionMoved(length);
        for (int i = 0; i < length; i++) {
            char c = cs.charAt(i);
            if (c > 255) c = '?';
            writeByte(offset, (byte) c);
        }
        return this;
    }

    public long rawCopy(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, IllegalArgumentException {
        long len = Math.min(writeRemaining(), Math.min(bytes.capacity() - offset, length));
        if (len > 0) {
            writeCheckOffset(writePosition(), len);
            OS.memory().copyMemory(bytes.addressForRead(offset), addressForWrite(writePosition()), len);
            writeSkip(len);
        }
        return len;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeByte(byte i8) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(1, 1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeUtf8(String s) throws BufferOverflowException {
        if (s == null) {
            writeStopBit(-1);
            return this;
        }

        try {
            if (Jvm.isJava9Plus()) {
                byte[] strBytes = extractBytes(s);
                byte coder = StringUtils.getStringCoder(s);
                long utfLength = AppendableUtil.findUtf8Length(strBytes, coder);
                writeStopBit(utfLength);
                appendUtf8(strBytes, 0, s.length(), coder);
            } else {
                char[] chars = extractChars(s);
                long utfLength = AppendableUtil.findUtf8Length(chars);
                writeStopBit(utfLength);
                if (utfLength == chars.length)
                    append8bit(chars);
                else
                    appendUtf8(chars, 0, chars.length);
            }
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
        return this;
    }

    void append8bit(char[] chars) throws BufferOverflowException, IllegalArgumentException {
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            bytesStore.writeByte(writePosition++, (byte) c);
        }
    }

    @NotNull
    @Override
    public Bytes<Underlying> appendUtf8(char[] chars, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
        int i;
        ascii:
        {
            for (i = 0; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                bytesStore.writeByte(writePosition++, (byte) c);
            }
            return this;
        }
        for (; i < length; i++) {
            char c = chars[offset + i];
            BytesInternal.appendUtf8Char(this, c);
        }
        return this;
    }
}
