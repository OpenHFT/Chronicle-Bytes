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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Position based access.  Once data has been read, the position() moves.
 * <p>The use of this instance is single threaded, though the use of the data
 */
public interface StreamingDataOutput<S extends StreamingDataOutput<S>> extends StreamingCommon<S> {

    S writePosition(long position) throws BufferOverflowException;

    S writeLimit(long limit) throws BufferOverflowException;

    /**
     * Skip a number of bytes by moving the readPosition. Must be less than or equal to the readLimit.
     *
     * @param bytesToSkip bytes to skip.
     * @return this
     * @throws BufferOverflowException if the offset is outside the limits of the Bytes
     */
    S writeSkip(long bytesToSkip) throws BufferOverflowException;

    /**
     * @return Bytes as an OutputStream
     */
    @NotNull
    default OutputStream outputStream() {
        return new StreamingOutputStream(this);
    }

    /**
     * Write a stop bit encoded long
     *
     * @param x long to write
     * @return this.
     */
    @NotNull
    default S writeStopBit(long x) throws BufferOverflowException {
        BytesInternal.writeStopBit(this, x);
        return (S) this;
    }

    default S writeStopBit(double d) throws BufferOverflowException {
        BytesInternal.writeStopBit(this, d);
        return (S) this;
    }

    /**
     * Write the same encoding as <code>writeUTF</code> with the following changes.  1) The length is stop bit encoded
     * i.e. one byte longer for short strings, but is not limited in length. 2) The string can be null.
     *
     * @param cs the string value to be written. Can be null.
     * @throws BufferOverflowException if there is not enough space left
     */
    @NotNull
    default S writeUtf8(CharSequence cs)
            throws BufferOverflowException {
        BytesInternal.writeUtf8(this, cs);
        return (S) this;
    }

    default S writeUtf8(String s)
            throws BufferOverflowException {
        BytesInternal.writeUtf8(this, s);
        return (S) this;
    }

    @Deprecated
    default S writeUTFΔ(CharSequence cs) throws BufferOverflowException {
        return writeUtf8(cs);
    }

    default S write8bit(CharSequence cs)
            throws BufferOverflowException {
        if (cs == null)
            return writeStopBit(-1);

        if (cs instanceof BytesStore)
            return write8bit((BytesStore) cs);

        return write8bit(cs, 0, cs.length());
    }

    default S write8bit(CharSequence s, int start, int length)
            throws BufferOverflowException, IllegalArgumentException, IndexOutOfBoundsException {
        writeStopBit(length);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i + start);
            writeUnsignedByte(c);
        }
        return (S) this;
    }

    default S write(CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IllegalArgumentException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs);
        }
        return write(cs, 0, cs.length());
    }

    default S write(CharSequence s, int start, int length)
            throws BufferOverflowException, IllegalArgumentException, IndexOutOfBoundsException {
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i + start);
            appendUtf8(c);
        }
        return (S) this;
    }

    default S write8bit(@NotNull String s)
            throws BufferOverflowException {
        if (s == null)
            writeStopBit(-1);
        else
            write8bit(s, 0, s.length());
        return (S) this;
    }

    default S write8bit(@NotNull BytesStore sdi)
            throws BufferOverflowException {
        long offset = sdi.readPosition();
        long readRemaining = sdi.readLimit() - offset;
        writeStopBit(readRemaining);
        write(sdi, offset, readRemaining);
        return (S) this;
    }

    @NotNull
    S writeByte(byte i8) throws BufferOverflowException;

    @NotNull
    default S writeUnsignedByte(int i)
            throws BufferOverflowException, IllegalArgumentException {
        return writeByte((byte) Maths.toUInt8(i));
    }

    @NotNull
    S writeShort(short i16) throws BufferOverflowException;

    @NotNull
    default S writeUnsignedShort(int u16)
            throws BufferOverflowException, IllegalArgumentException {
        return writeShort((short) Maths.toUInt16(u16));
    }

    @NotNull
    default S writeInt24(int i) throws BufferOverflowException {
        writeUnsignedShort(i);
        return writeUnsignedByte(i >> 16);
    }

    @NotNull
    S writeInt(int i) throws BufferOverflowException;

    @NotNull
    default S writeUnsignedInt(long i)
            throws BufferOverflowException, IllegalArgumentException {
        return writeInt((int) Maths.toUInt32(i));
    }

    @NotNull
    S writeLong(long i64) throws BufferOverflowException;

    @NotNull
    S writeFloat(float f) throws BufferOverflowException;

    @NotNull
    S writeDouble(double d) throws BufferOverflowException;

    /**
     * Write all data or fail.
     */
    @NotNull
    default S write(@NotNull BytesStore bytes)
            throws BufferOverflowException {
        if (bytes == this)
            throw new IllegalArgumentException("you should not write to your self !");

        return write(bytes, bytes.readPosition(), bytes.readRemaining());
    }

    @NotNull
    default S writeSome(@NotNull Bytes bytes)
            throws BufferOverflowException {
        long length = Math.min(bytes.readRemaining(), writeRemaining());
        write(bytes, bytes.readPosition(), length);
        if (length == bytes.readRemaining()) {
            bytes.clear();
        } else {
            bytes.readSkip(length);
            if (bytes.writePosition() > bytes.realCapacity() / 2)
                bytes.compact();
        }
        return (S) this;
    }

    /**
     * Write all data or fail.
     */
    @NotNull
    default S write(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalArgumentException {
        BytesInternal.writeFully(bytes, offset, length, this);
        return (S) this;
    }

    @NotNull
    default S write(@NotNull byte[] bytes) throws BufferOverflowException {
        write(bytes, 0, bytes.length);
        return (S) this;
    }

    /**
     * Write all data or fail.
     */
    @NotNull
    S write(byte[] bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException;

    @NotNull
    S writeSome(ByteBuffer buffer) throws BufferOverflowException;

    @NotNull
    default S writeBoolean(boolean flag) throws BufferOverflowException {
        return writeByte(flag ? (byte) 'Y' : 0);
    }

    @NotNull
    S writeOrderedInt(int i) throws BufferOverflowException;

    @NotNull
    S writeOrderedLong(long i) throws BufferOverflowException;

    /**
     * This is an expert level method for writing out data to native memory.
     *
     * @param address to write to.
     * @param size    in bytes.
     */
    void nativeWrite(long address, long size)
            throws BufferOverflowException;

    default <E extends Enum<E>> void writeEnum(E e)
            throws BufferOverflowException {
        write8bit(e.name());
    }

    default S appendUtf8(CharSequence cs)
            throws BufferOverflowException {
        return appendUtf8(cs, 0, cs.length());
    }

    default S appendUtf8(int codepoint) throws BufferOverflowException {
        BytesInternal.appendUtf8Char(this, codepoint);
        return (S) this;
    }

    default S appendUtf8(char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException {
        int i;
        ascii:
        {
            for (i = 0; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                writeByte((byte) c);
            }
            return (S) this;
        }
        for (; i < length; i++) {
            char c = chars[offset + i];
            BytesInternal.appendUtf8Char(this, c);
        }
        return (S) this;
    }

    default S appendUtf8(CharSequence cs, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException {
        BytesInternal.appendUtf8(this, cs, offset, length);
        return (S) this;
    }

    default void copyFrom(InputStream input) throws IOException, BufferOverflowException, IllegalArgumentException {
        BytesInternal.copy(input, this);
    }

    default void writePositionRemaining(long position, long length) {
        writeLimit(position + length);
        writePosition(position);
    }
}
