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
import net.openhft.chronicle.core.annotation.Java9;
import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.core.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Position based access.  Once data has been read, the position() moves.
 * <p>The use of this instance is single threaded, though the use of the data
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface StreamingDataOutput<S extends StreamingDataOutput<S>> extends StreamingCommon<S> {
    int JAVA9_STRING_CODER_LATIN = 0;
    int JAVA9_STRING_CODER_UTF16 = 1;

    @NotNull
    S writePosition(long position) throws BufferOverflowException;

    @NotNull
    S writeLimit(long limit) throws BufferOverflowException;

    /**
     * Skip a number of bytes by moving the writePosition. Must be less than or equal to the writeLimit.
     *
     * @param bytesToSkip bytes to skip.
     * @return this
     * @throws BufferOverflowException if the offset is outside the limits of the Bytes
     */
    @NotNull
    S writeSkip(long bytesToSkip) throws BufferOverflowException;

    default S alignBy(int width) {
        return writeSkip((-writePosition()) & (width - 1));
    }

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

    @NotNull
    default S writeStopBit(char x) throws BufferOverflowException {
        BytesInternal.writeStopBit(this, x);
        return (S) this;
    }

    @NotNull
    default S writeStopBit(double d) throws BufferOverflowException {
        BytesInternal.writeStopBit(this, d);
        return (S) this;
    }

    @NotNull
    default S writeStopBitDecimal(double d) throws BufferOverflowException {
        boolean negative = d < 0;
        double ad = Math.abs(d);
        long value;
        int scale = 0;
        if ((long) ad == ad) {
            value = (long) ad * 10;

        } else {
            double factor = 1;
            while (scale < 9) {
                double v = ad * factor;
                if (v >= 1e14 || (long) v == v)
                    break;
                factor *= 10;
                scale++;
            }
            value = Math.round(ad * factor);
            while (scale > 0 && value % 10 == 0) {
                value /= 10;
                scale--;
            }
            value = value * 10 + scale;
        }
        if (negative)
            value = -value;
        BytesInternal.writeStopBit(this, value);
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

    @NotNull
    default S writeUtf8(String s)
            throws BufferOverflowException {
        BytesInternal.writeUtf8(this, s);
        return (S) this;
    }

    @NotNull
    @Deprecated
    default S writeUTFΔ(CharSequence cs) throws BufferOverflowException {
        return writeUtf8(cs);
    }

    @NotNull
    default S write8bit(@Nullable CharSequence cs)
            throws BufferOverflowException {
        if (cs == null)
            return writeStopBit(-1);

        if (cs instanceof BytesStore)
            return write8bit((BytesStore) cs);

        if (cs instanceof String)
            return write8bit((String) cs);

        return write8bit(cs, 0, cs.length());
    }

    @NotNull
    default S write8bit(@NotNull CharSequence s, int start, int length)
            throws BufferOverflowException, IllegalArgumentException, IndexOutOfBoundsException {
        writeStopBit(length);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i + start);
            writeUnsignedByte(c);
        }
        return (S) this;
    }

    @NotNull
    default S write(CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IllegalArgumentException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs);
        }
        return write(cs, 0, cs.length());
    }

    @NotNull
    default S write(@NotNull CharSequence s, int start, int length)
            throws BufferOverflowException, IllegalArgumentException, IndexOutOfBoundsException {
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i + start);
            appendUtf8(c);
        }
        return (S) this;
    }

    @NotNull
    default S write8bit(@Nullable String s)
            throws BufferOverflowException {
        if (s == null)
            writeStopBit(-1);
        else
            write8bit(s, 0, (int) Math.min(writeRemaining(), s.length()));
        return (S) this;
    }

    @NotNull
    default S write8bit(@Nullable BytesStore bs)
            throws BufferOverflowException {
        if (bs == null) {
            writeStopBit(-1);
        } else {
            long offset = bs.readPosition();
            long readRemaining = Math.min(writeRemaining(), bs.readLimit() - offset);
            writeStopBit(readRemaining);
            write(bs, offset, readRemaining);
        }
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
        writeUnsignedShort((short) i);
        return writeUnsignedByte((i >>> 16) & 0xFF);
    }

    @NotNull
    default S writeUnsignedInt24(int i) throws BufferOverflowException {
        writeUnsignedShort((short) i);
        return writeUnsignedByte(i >>> 16);
    }

    @NotNull
    S writeInt(int i) throws BufferOverflowException;

    @NotNull
    S writeIntAdv(int i, int advance) throws BufferOverflowException;

    @NotNull
    default S writeUnsignedInt(long i)
            throws BufferOverflowException, IllegalArgumentException {
        return writeInt((int) Maths.toUInt32(i));
    }

    @NotNull
    S writeLong(long i64) throws BufferOverflowException;

    @NotNull
    S writeLongAdv(long i64, int advance) throws BufferOverflowException;

    @NotNull
    S writeFloat(float f) throws BufferOverflowException;

    @NotNull
    S writeDouble(double d) throws BufferOverflowException;

    @NotNull
    S writeDoubleAndInt(double d, int i) throws BufferOverflowException;

    /**
     * Write all data or fail.
     */
    @NotNull
    default S write(@NotNull RandomDataInput bytes) {
        assert bytes != this : "you should not write to yourself !";

        try {
            return write(bytes, bytes.readPosition(), Math.min(writeRemaining(), bytes.readRemaining()));
        } catch (BufferOverflowException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    default S write(@NotNull BytesStore bytes) {
        assert bytes != this : "you should not write to yourself !";

        try {
            return write(bytes, bytes.readPosition(), Math.min(writeRemaining(), bytes.readRemaining()));
        } catch (BufferOverflowException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    long realCapacity();

    @NotNull
    default S writeSome(@NotNull Bytes bytes) {
        try {
            long length = Math.min(bytes.readRemaining(), writeRemaining());
            if (length + writePosition() >= 1 << 20)
                length = Math.min(bytes.readRemaining(), realCapacity() - writePosition());
            write(bytes, bytes.readPosition(), length);
            if (length == bytes.readRemaining()) {
                bytes.clear();
            } else {
                bytes.readSkip(length);
                if (bytes.writePosition() > bytes.realCapacity() / 2)
                    bytes.compact();
            }
            return (S) this;
        } catch (BufferOverflowException | BufferUnderflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Write all data or fail.
     */
    @NotNull
    default S write(@NotNull RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException {
        BytesInternal.writeFully(bytes, offset, length, this);
        return (S) this;
    }

    /**
     * Write all data or fail.
     */
    @NotNull
    default S write(@NotNull BytesStore bytes, long offset, long
            length)
            throws BufferOverflowException, BufferUnderflowException {
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
    S write(byte[] bytes, int offset, int length) throws BufferOverflowException;

    @NotNull
    S writeSome(ByteBuffer buffer) throws BufferOverflowException;

    @NotNull
    default S writeBoolean(boolean flag) throws BufferOverflowException {
        return writeByte(flag ? (byte) 'Y' : (byte) 'N');
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
    @Deprecated(/*to be removed in 1.13*/)
    void nativeWrite(long address, long size)
            throws BufferOverflowException;

    default <E extends Enum<E>> void writeEnum(@NotNull E e)
            throws BufferOverflowException {
        write8bit(e.name());
    }

    @NotNull
    default S appendUtf8(@NotNull CharSequence cs)
            throws BufferOverflowException {
        return appendUtf8(cs, 0, cs.length());
    }

    @NotNull
    default S appendUtf8(int codepoint) throws BufferOverflowException {
        BytesInternal.appendUtf8Char(this, codepoint);
        return (S) this;
    }

    @NotNull
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

    @NotNull
    default S appendUtf8(@NotNull CharSequence cs, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException {
        BytesInternal.appendUtf8(this, cs, offset, length);
        return (S) this;
    }

    // length is number of characters (not bytes)
    @Java9
    @NotNull
    default S appendUtf8(byte[] bytes, int offset, int length, byte coder)
            throws BufferOverflowException, IllegalArgumentException {
        if (coder == JAVA9_STRING_CODER_LATIN) {
            for (int i = 0; i < length; i++) {
                byte b = bytes[offset + i];
                int b2 = (b & 0xFF);
                BytesInternal.appendUtf8Char(this, b2);
            }
        } else {
            assert coder == JAVA9_STRING_CODER_UTF16;
            for (int i = 0; i < 2 * length; i += 2) {
                byte b1 = bytes[2 * offset + i];
                byte b2 = bytes[2 * offset + i + 1];

                int uBE = ((b2 & 0xFF) << 8) | b1 & 0xFF;
                BytesInternal.appendUtf8Char(this, uBE);
            }
        }
        return (S) this;
    }

    @Java9
    @NotNull
    default S appendUtf8(byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException {
        for (int i = 0; i < length; i++) {
            int b = bytes[offset + i] & 0xFF; // unsigned byte

            if (b >= 0xF0) {
                int b2 = bytes[offset + i + 1] & 0xFF; // unsigned byte
                int b3 = bytes[offset + i + 2] & 0xFF; // unsigned byte
                int b4 = bytes[offset + i + 3] & 0xFF; // unsigned byte
                this.writeByte((byte) b4);
                this.writeByte((byte) b3);
                this.writeByte((byte) b2);
                this.writeByte((byte) b);

                i += 3;
            } else if (b >= 0xE0) {
                int b2 = bytes[offset + i + 1] & 0xFF; // unsigned byte
                int b3 = bytes[offset + i + 2] & 0xFF; // unsigned byte
                this.writeByte((byte) b3);
                this.writeByte((byte) b2);
                this.writeByte((byte) b);

                i += 2;
            } else if (b >= 0xC0) {
                int b2 = bytes[offset + i + 1] & 0xFF; // unsigned byte
                this.writeByte((byte) b2);
                this.writeByte((byte) b);

                i += 1;
            } else {
                this.writeByte((byte) b);
            }
        }
        return (S) this;
    }

    default void copyFrom(@NotNull InputStream input) throws IOException, BufferOverflowException, IllegalArgumentException {
        BytesInternal.copy(input, this);
    }

    default void writePositionRemaining(long position, long length) {
        writeLimit(position + length);
        writePosition(position);
    }

    default void writeHistogram(@NotNull Histogram histogram) {
        BytesInternal.writeHistogram(this, histogram);
    }

    default void writeBigDecimal(@NotNull BigDecimal bd) {
        writeBigInteger(bd.unscaledValue());
        writeStopBit(bd.scale());
    }

    default void writeBigInteger(@NotNull BigInteger bi) {
        byte[] bytes = bi.toByteArray();
        writeStopBit(bytes.length);
        write(bytes);
    }

    default void writeWithLength(RandomDataInput bytes) {
        writeStopBit(bytes.readRemaining());
        write(bytes);
    }
}
