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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static java.util.Objects.requireNonNull;
import static net.openhft.chronicle.core.util.StringUtils.extractBytes;
import static net.openhft.chronicle.core.util.StringUtils.extractChars;

/**
 * Fast unchecked version of AbstractBytes
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UncheckedBytes<U>
        extends AbstractBytes<U> {
    Bytes underlyingBytes;

    public UncheckedBytes(@NotNull Bytes underlyingBytes)
            throws IllegalStateException {
        super(requireNonNull(underlyingBytes.bytesStore()),
                underlyingBytes.writePosition(),
                Math.min(underlyingBytes.writeLimit(), underlyingBytes.realCapacity()));
        this.underlyingBytes = underlyingBytes;
        readPosition(underlyingBytes.readPosition());
        if (writeLimit > capacity())
            writeLimit(capacity());
    }

    public void setBytes(@NotNull Bytes bytes)
            throws IllegalStateException {
        requireNonNull(bytes);
        final BytesStore underlying = bytes.bytesStore();
        if (bytesStore != underlying) {
            bytesStore.release(this);
            this.bytesStore(underlying);
            bytesStore.reserve(this);
        }
        readPosition(bytes.readPosition());
        this.uncheckedWritePosition(bytes.writePosition());
        this.writeLimit = bytes.writeLimit();

        this.underlyingBytes = bytes;
    }

    @Override
    public void ensureCapacity(long desiredCapacity)
            throws IllegalArgumentException, IllegalStateException {
        if (desiredCapacity > realCapacity()) {
            underlyingBytes.ensureCapacity(desiredCapacity);
            bytesStore(underlyingBytes.bytesStore());
        }
    }

    @Override
    @NotNull
    public Bytes<U> unchecked(boolean unchecked) {
        throwExceptionIfReleased();
        return this;
    }

    @Override
    public boolean unchecked() {
        return true;
    }

    @Override
    protected void writeCheckOffset(long offset, long adding) {
        // Do nothing
    }

    @Override
    protected void readCheckOffset(long offset, long adding, boolean given) {
        // Do nothing
    }

    @Override
    void prewriteCheckOffset(long offset, long subtracting) {
        // Do nothing
    }

    @NotNull
    @Override
    public Bytes<U> readPosition(long position) {
        readPosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> readLimit(long limit) {
        uncheckedWritePosition(limit);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writePosition(long position) {
        uncheckedWritePosition(position);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> readSkip(long bytesToSkip) {
        readPosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeSkip(long bytesToSkip) {
        uncheckedWritePosition(writePosition() + bytesToSkip);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeLimit(long limit) {
        writeLimit = limit;
        return this;
    }

    @NotNull
    @Override
    public BytesStore<Bytes<U>, U> copy() {
        throwExceptionIfReleased();
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
    protected long prewriteOffsetPositionMoved(long subtracting)
            throws BufferOverflowException {
        readPosition -= subtracting;
        return readPosition;
    }

    @NotNull
    @Override
    public Bytes<U> write(@NotNull RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException, BufferUnderflowException {
        requireNonNull(bytes);
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else {
            super.write(bytes, offset, length);
        }
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> write(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException, BufferUnderflowException {
        requireNonNull(bytes);
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
    public Bytes<U> append8bit(@NotNull CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        requireNonNull(cs);
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

    long rawCopy(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
        requireNonNull(bytes);
        long len = Math.min(writeRemaining(), Math.min(bytes.capacity() - offset, length));
        if (len > 0) {
            writeCheckOffset(writePosition(), len);
            this.throwExceptionIfReleased();
            OS.memory().copyMemory(bytes.addressForRead(offset), addressForWritePosition(), len);
            writeSkip(len);
        }
        return len;
    }

    @NotNull
    @Override
    public Bytes<U> writeByte(byte i8)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(1, 1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeUtf8(@Nullable String text)
            throws BufferOverflowException, IllegalStateException {
        if (text == null) {
            BytesInternal.writeStopBitNeg1(this);
            return this;
        }

        try {
            if (Jvm.isJava9Plus()) {
                byte[] strBytes = extractBytes(text);
                byte coder = StringUtils.getStringCoder(text);
                long utfLength = AppendableUtil.findUtf8Length(strBytes, coder);
                writeStopBit(utfLength);
                appendUtf8(strBytes, 0, text.length(), coder);
            } else {
                char[] chars = extractChars(text);
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

    void append8bit(@NotNull char[] chars)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        requireNonNull(chars);
        long wp = writePosition();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            bytesStore.writeByte(wp++, (byte) c);
        }
        uncheckedWritePosition(wp);
    }

    @NotNull
    @Override
    public Bytes<U> appendUtf8(@NotNull char @NotNull [] chars, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        requireNonNull(chars);
        long wp = writePosition();
        int i;
        ascii:
        {
            for (i = 0; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                bytesStore.writeByte(wp++, (byte) c);
            }
            return this;
        }
        for (; i < length; i++) {
            char c = chars[offset + i];
            BytesInternal.appendUtf8Char(this, c);
        }
        uncheckedWritePosition(wp);
        return this;
    }

}
