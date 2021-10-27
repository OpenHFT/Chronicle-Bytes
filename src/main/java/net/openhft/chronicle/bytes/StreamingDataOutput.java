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
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.annotation.Java9;
import net.openhft.chronicle.core.util.Histogram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;
import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.checkNonNull;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Position based access.  Once data has been read, the position() moves.
 * <p>The use of this instance is single threaded, though the use of the data
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface StreamingDataOutput<S extends StreamingDataOutput<S>> extends StreamingCommon<S> {
    int JAVA9_STRING_CODER_LATIN = 0;
    int JAVA9_STRING_CODER_UTF16 = 1;

    @NotNull
    S writePosition(long position)
            throws BufferOverflowException;

    @NotNull
    S writeLimit(long limit)
            throws BufferOverflowException;

    /**
     * Skip a number of bytes by moving the writePosition. Must be less than or equal to the writeLimit.
     *
     * @param bytesToSkip bytes to skip.
     * @return this
     * @throws BufferOverflowException if the offset is outside the limits of the Bytes
     */
    @NotNull
    S writeSkip(long bytesToSkip)
            throws BufferOverflowException, IllegalStateException;

    @Deprecated(/* to be removed in x.23*/)
    default S alignBy(int width)
            throws BufferOverflowException, IllegalStateException {
        return writeSkip((-writePosition()) & (width - 1));
    }

    /**
     * obtain the writePosition skipping any padding needed for a header.
     *
     * @param skipPadding optional aligning to 4 bytes
     * @return the write position.
     */
    default long writePositionForHeader(boolean skipPadding) {
        long position = writePosition();
        if (skipPadding)
            return writeSkip(BytesUtil.padOffset(position)).writePosition();
        return position;
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
    default S writeStopBit(long x)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.writeStopBit(this, x);
        return (S) this;
    }

    @NotNull
    default S writeStopBit(char x)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.writeStopBit(this, x);
        return (S) this;
    }

    @NotNull
    default S writeStopBit(double d)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.writeStopBit(this, d);
        return (S) this;
    }

    @NotNull
    default S writeStopBitDecimal(double d)
            throws BufferOverflowException, IllegalStateException {
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
     * @param text the string value to be written. Can be null.
     * @throws BufferOverflowException if there is not enough space left
     */
    @NotNull
    default S writeUtf8(@Nullable CharSequence text)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException, BufferUnderflowException {
        BytesInternal.writeUtf8(this, text);
        return (S) this;
    }

    @NotNull
    default S writeUtf8(@Nullable String text)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException, IllegalArgumentException {
        BytesInternal.writeUtf8(this, text);
        return (S) this;
    }

    @NotNull
    default S write8bit(final @Nullable CharSequence text)
            throws BufferOverflowException, ArithmeticException, IllegalStateException, BufferUnderflowException {
        if (text == null) {
            BytesInternal.writeStopBitNeg1(this);
            return (S) this;
        }

        if (text instanceof BytesStore) {
            final long offset = ((BytesStore) text).readPosition();
            final long readRemaining = Math.min(writeRemaining(), ((BytesStore) text).readLimit() - offset);
            writeStopBit(readRemaining);
            try {
                write((BytesStore) text, offset, readRemaining);
            } catch (BufferUnderflowException | IllegalArgumentException e) {
                throw new AssertionError(e);
            }
            return (S) this;
        }

        if (text instanceof String)
            return write8bit((String) text);

        return write8bit(text, 0, text.length());
    }

    @NotNull
    default S write8bit(@NotNull CharSequence text, int start, int length)
            throws BufferOverflowException, IndexOutOfBoundsException, ArithmeticException, IllegalStateException, BufferUnderflowException {
        checkNonNull(text);
        if (text instanceof String)
            return write8bit((String) text, start, length);

        writeStopBit(length);
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i + start);
            rawWriteByte((byte) Maths.toUInt8((int) c));
        }
        return (S) this;
    }

    @NotNull
    S write8bit(@NotNull String text, int start, int length);

    /**
     * Writes the provided {@code text} to this StreamingDataOutput at the current writePosition().
     *
     * @param text to write
     * @return this StreamingDataOutput
     * @throws BufferOverflowException if the provided {@code text} cannot be accommodated.
     * @throws IllegalStateException if this StreamingDataOutput has been previously released.
     * @throws IllegalArgumentException if the provided {@code text} is {@code null}.
     */
    @NotNull
    default S write(@NotNull CharSequence text)
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException {
        checkNonNull(text);
        if (text instanceof BytesStore) {
            return write((BytesStore) text);
        }
        return write(text, 0, text.length());
    }

    /**
     * Writes the provided {@code text} to this StreamingDataOutput at the current writePosition()
     *
     * @param text to write
     * @param startText offset from where text should be copied from
     * @param length number of characters to write.
     * @return this StreamingDataOutput
     * @throws BufferOverflowException if the provided {@code text} cannot be accommodated.
     * @throws IllegalStateException if this StreamingDataOutput has been previously released.
     * @throws NullPointerException if the provided {@code text} is {@code null}.
     * @throws IllegalArgumentException if the provided {@code startText} or the provided {@code length} is negative.
     */
    @NotNull
    default S write(@NotNull final CharSequence text,
                    final int startText,
                    final int length)
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException {
        checkNonNull(text);
        requireNonNegative(startText);
        requireNonNegative(length);
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i + startText);
            appendUtf8(c);
        }
        return (S) this;
    }

    @NotNull
    default S write8bit(@Nullable String s)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        try {
            if (s == null)
                BytesInternal.writeStopBitNeg1(this);
            else
                write8bit(s, 0, (int) Math.min(writeRemaining(), s.length()));
            return (S) this;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    S writeByte(byte i8)
            throws BufferOverflowException, IllegalStateException;

    default S rawWriteByte(byte i8)
            throws BufferOverflowException, IllegalStateException {
        return writeByte(i8);
    }

    @NotNull
    default S writeUnsignedByte(int i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeByte((byte) Maths.toUInt8(i));
    }

    @NotNull
    default S writeChar(char ch)
            throws BufferOverflowException, IllegalStateException {
        return writeStopBit(ch);
    }

    @NotNull
    S writeShort(short i16)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    default S writeUnsignedShort(int u16)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeShort((short) Maths.toUInt16(u16));
    }

    @NotNull
    default S writeInt24(int i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        writeShort((short) i);
        return writeByte(Maths.toInt8(i >> 16));
    }

    @NotNull
    default S writeUnsignedInt24(int i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        writeShort((short) i);
        return writeUnsignedByte(i >>> 16);
    }

    @NotNull
    S writeInt(int i)
            throws BufferOverflowException, IllegalStateException;

    default S rawWriteInt(int i)
            throws BufferOverflowException, IllegalStateException {
        return writeInt(i);
    }

    @NotNull
    S writeIntAdv(int i, int advance)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    default S writeUnsignedInt(long i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeInt((int) Maths.toUInt32(i));
    }

    /**
     * Write a long
     */
    @NotNull
    S writeLong(long i64)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Write a long without a bounds check
     */
    default S rawWriteLong(long i)
            throws BufferOverflowException, IllegalStateException {
        return writeLong(i);
    }

    @NotNull
    S writeLongAdv(long i64, int advance)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    S writeFloat(float f)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    S writeDouble(double d)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    S writeDoubleAndInt(double d, int i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Write all data or fail.
     * <p>
     * Calling this method will update the cursors of this, but not the bytes we read from.
     * @throws IllegalArgumentException if the provided {@code bytes} is {@code null}
     */
    @NotNull
    default S write(@NotNull RandomDataInput bytes)
            throws BufferOverflowException, IllegalStateException {
        assert bytes != this : "you should not write to yourself !";
        checkNonNull(bytes);

        if (bytes.readRemaining() > writeRemaining())
            throw new BufferOverflowException();
        try {
            return write(bytes, bytes.readPosition(), bytes.readRemaining());
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Writes all the passed BytesStore or it fails.
     * If you want to read only as much as there is use read
     * <p>
     * Calling this method will update the cursors of this, but not the bytes we read from.
     *
     * @param bytes to write
     * @return this
     * @see StreamingDataInput#read(Bytes)
     */
    default S write(@NotNull BytesStore bytes)
            throws BufferOverflowException, IllegalStateException {
        assert bytes != this : "you should not write to yourself !";
        checkNonNull(bytes);

        if (bytes.readRemaining() > writeRemaining())
            throw new BufferOverflowException();
        try {
            return write(bytes, bytes.readPosition(), bytes.readRemaining());
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @return capacity without resize or -1 if closed
     */
    long realCapacity();

    default boolean canWriteDirect(long count) {
        return false;
    }

    /**
     * Write all data available from bytes argument, constrained by how much space available in this.
     * <p>
     * Calling this method will update the cursors of this, but not the bytes we read from.
     */
    @NotNull
    default S writeSome(@NotNull Bytes bytes)
            throws IllegalStateException {
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
     * <p>
     * Calling this method will update the cursors of this, but not the bytes we read from.
     */
    @NotNull
    default S write(@NotNull RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException, IllegalArgumentException {
        BytesInternal.writeFully(bytes, readOffset, length, this);
        return (S) this;
    }

    /**
     * Write all data or fail.
     * <p>
     * Calling this method will update the cursors of this, but not the bytes we read from.
     */
    @NotNull
    default S write(@NotNull BytesStore bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException, IllegalArgumentException {
        if (length + writePosition() > capacity())
            throw new DecoratedBufferOverflowException("Cannot write " + length + " bytes as position is " + writePosition() + " and capacity is " + capacity());
        BytesInternal.writeFully(bytes, readOffset, length, this);
        return (S) this;
    }

    /**
     * Write all data or fail.
     * <p>
     * Calling this method will update the cursors of this.
     * @throws NullPointerException if the provided {@code byteArray } is {@code null}.
     */
    @NotNull
    default S write(@NotNull byte[] byteArray)
            throws BufferOverflowException, IllegalStateException {
        checkNonNull(byteArray);
        try {
            write(byteArray, 0, byteArray.length);
            return (S) this;
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Write all data or fail.
     * <p>
     * Calling this method will update the cursors of this.
     */
    @NotNull
    S write(byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException, ArrayIndexOutOfBoundsException;

    default S unsafeWriteObject(Object o, int length)
            throws BufferOverflowException, IllegalStateException {
        return unsafeWriteObject(o, (o.getClass().isArray() ? 4 : 0) + Jvm.objectHeaderSize(), length);
    }

    default S unsafeWriteObject(Object o, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
        int i = 0;
        for (; i < length - 7; i += 8)
            writeLong(UnsafeMemory.unsafeGetLong(o, (long) offset + i));
        for (; i < length; i++)
            writeByte(UnsafeMemory.unsafeGetByte(o, (long) offset + i));
        return (S) this;
    }

    /**
     * Write raw native memory for a fixed length
     *
     * @return this
     */
    default S unsafeWrite(long address, int length) {
        if (isDirectMemory()) {
            long destAddress = addressForWrite(writePosition());
            writeSkip(length); // blow up if there isn't that much space left
            MEMORY.copyMemory(address, destAddress, length);
        } else {
            int i = 0;
            for (; i < length - 7; i += 8)
                writeLong(UnsafeMemory.unsafeGetLong(address + i));
            for (; i < length; i++)
                writeByte(UnsafeMemory.unsafeGetByte(address + i));
        }
        return (S) this;
    }

    /**
     * Write all data available from buffer, constrained by how much space available in this.
     * <p>
     * Calling this method will update the cursors of this.
     */
    @NotNull
    S writeSome(ByteBuffer buffer)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException;

    @NotNull
    default S writeBoolean(boolean flag)
            throws BufferOverflowException, IllegalStateException {
        return writeByte(flag ? (byte) 'Y' : (byte) 'N');
    }

    @NotNull
    S writeOrderedInt(int i)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    S writeOrderedLong(long i)
            throws BufferOverflowException, IllegalStateException;

    default <E extends Enum<E>> S writeEnum(@NotNull E e)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return write8bit(e.name());
    }

    @NotNull
    default S appendUtf8(@NotNull CharSequence cs)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
        return appendUtf8(cs, 0, cs.length());
    }

    @NotNull
    default S appendUtf8(int codepoint)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.appendUtf8Char(this, codepoint);
        return (S) this;
    }

    @NotNull
    default S appendUtf8(@NotNull char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException, IllegalArgumentException {
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
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException, BufferUnderflowException {
        BytesInternal.appendUtf8(this, cs, offset, length);
        return (S) this;
    }

    // length is number of characters (not bytes)
    @Java9
    @NotNull
    default S appendUtf8(@NotNull byte[] bytes, int offset, int length, byte coder)
            throws BufferOverflowException, IllegalStateException {
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
    default S appendUtf8(@NotNull byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
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

    default void copyFrom(@NotNull InputStream input)
            throws IOException, BufferOverflowException, IllegalStateException {
        BytesInternal.copy(input, this);
    }

    default void writePositionRemaining(long position, long length)
            throws BufferOverflowException {
        writeLimit(position + length);
        writePosition(position);
    }

    default void writeHistogram(@NotNull Histogram histogram)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.writeHistogram(this, histogram);
    }

    default void writeBigDecimal(@NotNull BigDecimal bd)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException {
        writeBigInteger(bd.unscaledValue());
        writeStopBit(bd.scale());
    }

    default void writeBigInteger(@NotNull BigInteger bi)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException {
        byte[] bytes = bi.toByteArray();
        writeStopBit(bytes.length);
        write(bytes);
    }

    default void writeWithLength(@NotNull RandomDataInput bytes)
            throws BufferOverflowException, IllegalStateException {
        writeStopBit(bytes.readRemaining());
        write(bytes);
    }
}
