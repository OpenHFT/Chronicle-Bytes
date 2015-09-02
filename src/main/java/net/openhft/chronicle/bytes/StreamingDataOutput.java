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
        return write(s, start, length);
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
            if (c > 255) c = '?';
            writeUnsignedByte(c);
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

    @NotNull
    default S write(@NotNull BytesStore bytes)
            throws BufferOverflowException, IORuntimeException {
        return write(bytes, bytes.readPosition(), bytes.readRemaining());
    }

    @NotNull
    default S write(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalArgumentException, IORuntimeException {
        BytesInternal.write(bytes, offset, length, this);
        return (S) this;
    }

    @NotNull
    default S write(@NotNull byte[] bytes) throws BufferOverflowException, IORuntimeException {
        return write(bytes, 0, bytes.length);
    }

    @NotNull
    S write(byte[] bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    @NotNull
    S write(ByteBuffer buffer) throws BufferOverflowException, IORuntimeException;

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
        BytesInternal.appendUTF(this, codepoint);
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
            BytesInternal.appendUTF(this, c);
        }
        return (S) this;
    }

    default S appendUtf8(CharSequence cs, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        BytesInternal.appendUTF(this, cs, offset, length);
        return (S) this;
    }

    default void copyFrom(InputStream input) throws IOException, BufferOverflowException, IllegalArgumentException {
        BytesInternal.copy(input, this);
    }
}
