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
import net.openhft.chronicle.core.io.IORuntimeException;

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
     * @throws IORuntimeException      if an error occurred trying to obtain the data.
     */
    S writeSkip(long bytesToSkip) throws BufferOverflowException, IORuntimeException;

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
    default S writeStopBit(long x) throws BufferOverflowException, IORuntimeException {
        BytesInternal.writeStopBit(this, x);
        return (S) this;
    }

    default S writeStopBit(double d) throws BufferOverflowException, IORuntimeException {
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
            throws BufferOverflowException, IORuntimeException {
        BytesInternal.writeUtf8(this, cs);
        return (S) this;
    }

    @Deprecated
    default S writeUTFΔ(CharSequence cs) throws BufferOverflowException, IORuntimeException {
        return writeUtf8(cs);
    }

    default S write8bit(CharSequence cs)
            throws BufferOverflowException, IORuntimeException {
        if (cs == null)
            return writeStopBit(-1);

        if (cs instanceof BytesStore)
            return write8bit((BytesStore) cs);

        return write8bit(cs, 0, cs.length());
    }

    default S write8bit(CharSequence s, int start, int length)
            throws BufferOverflowException, IllegalArgumentException, IndexOutOfBoundsException, IORuntimeException {
        writeStopBit(length);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i + start);
            writeUnsignedByte(c);
        }
        return (S) this;
    }

    default S write(CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IllegalArgumentException, IORuntimeException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs);
        }
        return write(cs, 0, cs.length());
    }

    default S write(CharSequence s, int start, int length)
            throws BufferOverflowException, IllegalArgumentException, IndexOutOfBoundsException, IORuntimeException {
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i + start);
            appendUtf8(c);
        }
        return (S) this;
    }

    default S write8bit(@NotNull String s)
            throws BufferOverflowException, IORuntimeException {
        write8bit(s, 0, s.length());
        return (S) this;
    }

    default S write8bit(@NotNull BytesStore sdi)
            throws BufferOverflowException, IORuntimeException {
        long offset = sdi.readPosition();
        long readRemaining = sdi.readLimit() - offset;
        writeStopBit(readRemaining);
        write(sdi, offset, readRemaining);
        return (S) this;
    }

    @NotNull
    S writeByte(byte i8) throws BufferOverflowException, IORuntimeException;

    @NotNull
    default S writeUnsignedByte(int i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeByte((byte) Maths.toUInt8(i));
    }

    @NotNull
    S writeShort(short i16) throws BufferOverflowException, IORuntimeException;

    @NotNull
    default S writeUnsignedShort(int u16)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeShort((short) Maths.toUInt16(u16));
    }

    @NotNull
    default S writeInt24(int i) throws BufferOverflowException, IORuntimeException {
        writeUnsignedShort(i);
        return writeUnsignedByte(i >> 16);
    }

    @NotNull
    S writeInt(int i) throws BufferOverflowException, IORuntimeException;

    @NotNull
    default S writeUnsignedInt(long i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeInt((int) Maths.toUInt32(i));
    }

    @NotNull
    S writeLong(long i64) throws BufferOverflowException, IORuntimeException;

    @NotNull
    S writeFloat(float f) throws BufferOverflowException, IORuntimeException;

    @NotNull
    S writeDouble(double d) throws BufferOverflowException, IORuntimeException;

    /**
     * Write all data or fail.
     */
    @NotNull
    default S write(@NotNull BytesStore bytes)
            throws BufferOverflowException, IORuntimeException {
        return write(bytes, bytes.readPosition(), bytes.readRemaining());
    }

    @NotNull
    default S writeSome(@NotNull Bytes bytes)
            throws BufferOverflowException, IORuntimeException {
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
            throws BufferOverflowException, BufferUnderflowException, IllegalArgumentException, IORuntimeException {
        BytesInternal.writeFully(bytes, offset, length, this);
        return (S) this;
    }

    @NotNull
    default S write(@NotNull byte[] bytes) throws BufferOverflowException, IORuntimeException {
        write(bytes, 0, bytes.length);
        return (S) this;
    }

    /**
     * Write all data or fail.
     */
    @NotNull
    S write(byte[] bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    @NotNull
    S writeSome(ByteBuffer buffer) throws BufferOverflowException, IORuntimeException;

    @NotNull
    default S writeBoolean(boolean flag) throws BufferOverflowException, IORuntimeException {
        return writeByte(flag ? (byte) 'Y' : 0);
    }

    @NotNull
    S writeOrderedInt(int i) throws BufferOverflowException, IORuntimeException;

    @NotNull
    S writeOrderedLong(long i) throws BufferOverflowException, IORuntimeException;

    /**
     * This is an expert level method for writing out data to native memory.
     *
     * @param address to write to.
     * @param size    in bytes.
     */
    void nativeWrite(long address, long size)
            throws BufferOverflowException, IORuntimeException;

    default <E extends Enum<E>> void writeEnum(E e)
            throws BufferOverflowException, IORuntimeException {
        write8bit(e.name());
    }

    default S appendUtf8(CharSequence cs)
            throws BufferOverflowException, IORuntimeException {
        return appendUtf8(cs, 0, cs.length());
    }

    default S appendUtf8(int codepoint) throws BufferOverflowException, IORuntimeException {
        BytesInternal.appendUtf8Char(this, codepoint);
        return (S) this;
    }

    default S appendUtf8(char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
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
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
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
