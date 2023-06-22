/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.annotation.Java9;
import net.openhft.chronicle.core.annotation.NonNegative;
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

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;
import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * StreamingDataOutput is an interface for classes that support writing data to a stream.
 * Position based access.  Once data has been read, the writePosition() moves.
 *
 * <p>The various write methods in this interface support writing bytes, arrays of bytes, and sequences
 * of bytes from other sources like {@link ByteBuffer} and {@link RandomDataInput}. It also provides
 * methods for writing primitive data types and their boxed counterparts. Additionally, it supports
 * writing complex data structures such as {@link BigDecimal}, {@link BigInteger}, {@link Histogram}
 * and objects in unsafe way.</p>
 *
 * <p>Not all the methods are expected to be implemented by classes. Default methods are provided for
 * common use cases. Classes that extend this interface can override these methods to provide more
 * efficient or custom implementations.</p>
 *
 * <p>Implementations of this interface are expected to handle cases where the buffer may not have
 * enough capacity to handle the write operation. In such cases, methods should throw a {@link BufferOverflowException}.</p>
 *
 * <p>Instances of classes implementing StreamingDataOutput are not guaranteed to be thread safe. If multiple
 * threads interact with the same instance, external synchronization should be used.</p>
 *
 * @see RandomDataOutput
 * @see ByteBuffer
 * @see BufferOverflowException
 * @see BigDecimal
 * @see BigInteger
 * @see Histogram
 */
@SuppressWarnings({"unchecked"})
@DontChain
public interface StreamingDataOutput<S extends StreamingDataOutput<S>> extends StreamingCommon<S> {
    int JAVA9_STRING_CODER_LATIN = 0;
    int JAVA9_STRING_CODER_UTF16 = 1;

    /**
     * Sets the current write position in the data stream. The write position indicates the point
     * where the next write operation will begin. Calling this method does not modify the data in
     * the stream; it only changes the position where future write operations will take place.
     *
     * @param position The new write position. It must be a non-negative number.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if the specified position exceeds the limit of the data buffer.
     */
    @NotNull
    S writePosition(@NonNegative long position)
            throws BufferOverflowException;

    /**
     * Sets the limit for writing to the data stream. No data can be written beyond this limit.
     * If an attempt is made to write data beyond this limit, a BufferOverflowException will be thrown.
     *
     * @param limit The new write limit. It must be a non-negative number.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if the specified limit is less than the current write position.
     */
    @NotNull
    S writeLimit(@NonNegative long limit)
            throws BufferOverflowException;

    /**
     * Skips a specified number of bytes from the current write position in the data stream.
     * This method adjusts the write position either forward or backward based on the value of
     * bytesToSkip. The new position should not exceed the write limit.
     *
     * @param bytesToSkip The number of bytes to skip. This can be a negative number to move the
     *                    position backward.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if the new position calculated by the skip operation falls
     *                                 outside the limits of the data buffer.
     */
    @NotNull
    S writeSkip(long bytesToSkip)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Returns the current write position while optionally skipping any padding needed for a header.
     * The write position can be optionally aligned to a 4-byte boundary.
     *
     * @param skipPadding If true, aligns the write position to the next 4-byte boundary.
     * @return The current write position after optional alignment.
     */
    default long writePositionForHeader(boolean skipPadding) {
        long position = writePosition();
        if (skipPadding)
            return writeSkip(BytesUtil.padOffset(position)).writePosition();
        return position;
    }

    /**
     * Provides a view of this Bytes object as an OutputStream. Any data written to the returned
     * OutputStream will be appended to this Bytes object.
     *
     * @return An OutputStream view of this Bytes object.
     */
    @NotNull
    default OutputStream outputStream() {
        return new StreamingOutputStream(this);
    }

    /**
     * Writes a stop bit encoded long to the data stream. Stop bit encoding is a form of variable-length
     * integer encoding that uses the continuation bit to indicate if there are more bytes to be processed.
     *
     * @param x The long value to be written to the data stream.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeStopBit(long x)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.writeStopBit(this, x);
        return (S) this;
    }

    /**
     * Writes a stop bit encoded char to the data stream. Stop bit encoding is a form of variable-length
     * integer encoding that uses the continuation bit to indicate if there are more bytes to be processed.
     *
     * @param x The char value to be written to the data stream.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeStopBit(char x)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.writeStopBit(this, x);
        return (S) this;
    }

    /**
     * Writes a stop bit encoded double value to the data stream.
     * Stop bit encoding is a form of variable-length integer encoding
     * that uses the continuation bit to indicate if there are more bytes to be processed.
     *
     * @param d The double value to be written to the data stream.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeStopBit(double d)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.writeStopBit(this, d);
        return (S) this;
    }

    /**
     * Writes a double value to the data stream with a stop bit encoded decimal. The encoding used
     * aims to efficiently store values with a small number of significant digits.
     *
     * @param d The double value to be written to the data stream.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
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
     * Writes a UTF-8 encoded string to the data stream, similar to writeUTF, but with a few differences.
     * Firstly, the length is stop bit encoded, meaning that the length encoding may be one byte longer
     * for short strings, but the string's length is not limited. Secondly, the string can be null.
     *
     * @param text The string to be written. Can be null.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeUtf8(@Nullable CharSequence text)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException, BufferUnderflowException {
        BytesInternal.writeUtf8(this, text);
        return (S) this;
    }

    /**
     * Writes a UTF-8 encoded String to the data stream. This method is functionally similar to
     * writeUtf8(CharSequence), but it specifically accepts a String as an argument.
     *
     * @param text The string to be written. Can be null.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeUtf8(@Nullable String text)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException, IllegalArgumentException {
        BytesInternal.writeUtf8(this, text);
        return (S) this;
    }

    /**
     * Writes a CharSequence as an 8-bit string to the data stream. If the CharSequence is null,
     * a stop bit encoded -1 is written. If the CharSequence is an instance of BytesStore, the contents
     * are written directly from the BytesStore. Otherwise, the method delegates to the
     * write8bit(String) or write8bit(CharSequence, int, int) as appropriate.
     *
     * @param text The CharSequence to be written. Can be null.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S write8bit(final @Nullable CharSequence text)
            throws BufferOverflowException, ArithmeticException, IllegalStateException, BufferUnderflowException {
        if (text == null) {
            BytesInternal.writeStopBitNeg1(this);
            return (S) this;
        }

        if (text instanceof BytesStore) {
            long offset = ((BytesStore<?, ?>) text).readPosition();
            long readRemaining = Math.min(writeRemaining(), ((BytesStore<?, ?>) text).readLimit() - offset);
            writeStopBit(readRemaining);
            try {
                write((BytesStore<?, ?>) text, offset, readRemaining);
            } catch (BufferUnderflowException | IllegalArgumentException e) {
                throw new AssertionError(e);
            }
            return (S) this;
        }

        if (text instanceof String)
            return write8bit((String) text);

        return write8bit(text, 0, text.length());
    }

    /**
     * Writes a subsequence of a CharSequence as an 8-bit string to the data stream. If the CharSequence is an
     * instance of String, the method delegates to the write8bit(String, int, int).
     *
     * @param text   The CharSequence to be written.
     * @param start  The index of the first char in the CharSequence to write.
     * @param length The number of chars from the CharSequence to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S write8bit(@NotNull CharSequence text, @NonNegative int start, @NonNegative int length)
            throws BufferOverflowException, IndexOutOfBoundsException, ArithmeticException, IllegalStateException, BufferUnderflowException {
        requireNonNull(text);
        if (text instanceof String)
            return write8bit((String) text, start, length);

        writeStopBit(length);
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i + start);
            rawWriteByte((byte) Maths.toUInt8(c));
        }
        return (S) this;
    }

    /**
     * Writes a subsequence of a String as an 8-bit string to the data stream. This method is to be
     * implemented by concrete subclasses.
     *
     * @param text   The String to be written.
     * @param start  The index of the first char in the String to write.
     * @param length The number of chars from the String to write.
     * @return The current StreamingDataOutput instance.
     */
    @NotNull
    S write8bit(@NotNull String text, @NonNegative int start, @NonNegative int length);

    /**
     * Writes the provided {@code text} to this StreamingDataOutput at the current writePosition().
     *
     * @param text to write
     * @return this StreamingDataOutput
     * @throws BufferOverflowException  if the provided {@code text} cannot be accommodated.
     * @throws IllegalStateException    if this StreamingDataOutput has been previously released.
     * @throws IllegalArgumentException if the provided {@code text} is {@code null}.
     */
    @NotNull
    default S write(@NotNull CharSequence text)
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException {
        requireNonNull(text);
        if (text instanceof BytesStore) {
            return write((BytesStore<?, ?>) text);
        }
        return write(text, 0, text.length());
    }

    /**
     * Writes the provided {@code text} to this StreamingDataOutput at the current writePosition()
     *
     * @param text      to write
     * @param startText offset from where text should be copied from
     * @param length    number of characters to write.
     * @return this StreamingDataOutput
     * @throws BufferOverflowException  if the provided {@code text} cannot be accommodated.
     * @throws IllegalStateException    if this StreamingDataOutput has been previously released.
     * @throws NullPointerException     if the provided {@code text} is {@code null}.
     * @throws IllegalArgumentException if the provided {@code startText} or the provided {@code length} is negative.
     */
    @NotNull
    default S write(@NotNull final CharSequence text,
                    @NonNegative final int startText,
                    @NonNegative final int length)
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException {
        requireNonNull(text);
        requireNonNegative(startText);
        requireNonNegative(length);
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i + startText);
            appendUtf8(c);
        }
        return (S) this;
    }

    /**
     * Writes a String as an 8-bit string to the data stream. If the string is null,
     * a stop bit encoded -1 is written. Otherwise, the method delegates to the
     * write8bit(String, int, int) method.
     *
     * @param s The String to be written. Can be null.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S write8bit(@Nullable String s)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        try {
            if (s == null)
                BytesInternal.writeStopBitNeg1(this);
            else {
                long rem = writeRemaining();
                if (rem < 0)
                    throw new IllegalStateException("rem: " + rem);
                write8bit(s, 0, s.length());
            }
            return (S) this;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Writes a byte to the data stream.
     *
     * @param i8 The byte to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    S writeByte(byte i8)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a byte to the data stream without any additional checks or transformations.
     *
     * @param i8 The byte to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    default S rawWriteByte(byte i8)
            throws BufferOverflowException, IllegalStateException {
        return writeByte(i8);
    }

    /**
     * Writes an unsigned byte to the data stream. The input integer is converted to an unsigned byte
     * using Maths.toUInt8(int) before being written.
     *
     * @param i The integer to be written as an unsigned byte.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeUnsignedByte(int i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeByte((byte) Maths.toUInt8(i));
    }

    /**
     * Writes a char to the data stream as a stop bit encoded value.
     *
     * @param ch The char to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeChar(char ch)
            throws BufferOverflowException, IllegalStateException {
        return writeStopBit(ch);
    }

    /**
     * Writes a short integer to the data stream.
     *
     * @param i16 The short integer to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    S writeShort(short i16)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes an unsigned short integer to the data stream. The input integer is converted to an unsigned short
     * using Maths.toUInt16(int) before being written.
     *
     * @param u16 The integer to be written as an unsigned short.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeUnsignedShort(int u16)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeShort((short) Maths.toUInt16(u16));
    }

    /**
     * Writes a 24-bit integer to the data stream. The integer is split into a 16-bit short and an 8-bit byte.
     *
     * @param i The integer to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeInt24(int i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        writeShort((short) i);
        return writeByte(Maths.toInt8(i >> 16));
    }

    /**
     * Writes an unsigned 24-bit integer to the data stream. The integer is split into a 16-bit short and an 8-bit byte.
     *
     * @param i The integer to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeUnsignedInt24(int i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        writeShort((short) i);
        return writeUnsignedByte(i >>> 16);
    }

    /**
     * Writes an integer to the data stream.
     *
     * @param i The integer to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    S writeInt(int i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes an integer to the data stream without any additional checks or transformations.
     *
     * @param i The integer to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    default S rawWriteInt(int i)
            throws BufferOverflowException, IllegalStateException {
        return writeInt(i);
    }

    /**
     * Writes an integer to the data stream, then advances the write position by the specified amount.
     *
     * @param i       The integer to be written.
     * @param advance The number of bytes to advance the write position after the integer has been written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    S writeIntAdv(int i, @NonNegative int advance)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes an unsigned integer to the data stream. The input long is converted to an unsigned integer
     * using Maths.toUInt32(long) before being written.
     *
     * @param i The long to be written as an unsigned integer.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     */
    @NotNull
    default S writeUnsignedInt(long i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeInt((int) Maths.toUInt32(i));
    }

    /**
     * Writes a long integer to the data stream.
     *
     * @param i64 The long integer to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    S writeLong(long i64)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a long integer to the data stream without performing a bounds check.
     *
     * @param i The long integer to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    default S rawWriteLong(long i)
            throws BufferOverflowException, IllegalStateException {
        return writeLong(i);
    }

    /**
     * Writes a long integer to the data stream, then advances the write position by the specified amount.
     *
     * @param i64     The long integer to be written.
     * @param advance The number of bytes to advance the write position after the long integer has been written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    S writeLongAdv(long i64, @NonNegative int advance)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a floating-point number to the data stream.
     *
     * @param f The floating-point number to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    S writeFloat(float f)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a double-precision floating-point number to the data stream.
     *
     * @param d The double-precision floating-point number to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    S writeDouble(double d)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a double-precision floating-point number followed by an integer to the data stream.
     *
     * @param d The double-precision floating-point number to be written.
     * @param i The integer to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is insufficient space in the buffer.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    S writeDoubleAndInt(double d, int i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes all available data from the specified {@code RandomDataInput} into the output stream.
     * The position of this output stream is updated accordingly, but the read position of the input data is not changed.
     * The operation will fail if there is not enough space left in the output stream.
     *
     * @param bytes the {@code RandomDataInput} from which data is read.
     * @return The current StreamingDataOutput instance.
     * @throws IllegalArgumentException if the provided {@code bytes} is {@code null}.
     * @throws BufferOverflowException  if there is not enough space left in the output stream.
     * @throws IllegalStateException    if the buffer is in an invalid state.
     */
    @NotNull
    default S write(@NotNull RandomDataInput bytes)
            throws BufferOverflowException, IllegalStateException {
        assert bytes != this : "you should not write to yourself !";
        requireNonNull(bytes);

        if (bytes.readRemaining() > writeRemaining())
            throw new BufferOverflowException();
        try {
            return write(bytes, bytes.readPosition(), bytes.readRemaining());
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Writes all available data from the specified {@code BytesStore} into the output stream.
     * The position of this output stream is updated accordingly, but the read position of the input data is not changed.
     * The operation will fail if there is not enough space left in the output stream.
     *
     * @param bytes the {@code BytesStore} from which data is read.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     * @see StreamingDataInput#read(Bytes)
     */
    default S write(@NotNull BytesStore<?, ?> bytes)
            throws BufferOverflowException, IllegalStateException {
        assert bytes != this : "you should not write to yourself !";
        requireNonNull(bytes);

        ensureCapacity(bytes.readRemaining() + readPosition());
        try {
            return write(bytes, bytes.readPosition(), bytes.readRemaining());
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Ensures that the buffer has the desired capacity. If the buffer is elastic, it will grow to accommodate
     * the requested capacity. If the buffer is not elastic and there is insufficient space, a
     * {@link DecoratedBufferOverflowException} will be thrown.
     *
     * @param desiredCapacity the required capacity.
     * @throws IllegalStateException            if the buffer is closed and needs to be resized.
     * @throws DecoratedBufferOverflowException if the buffer is not elastic and lacks sufficient space, or
     *                                          if the provided {@code desiredCapacity} is negative.
     */
    void ensureCapacity(@NonNegative long desiredCapacity)
            throws DecoratedBufferOverflowException, IllegalStateException;

    /**
     * Returns the actual capacity of the buffer without resize. If the buffer is closed, this method returns -1.
     *
     * @return the actual capacity of the buffer or -1 if the buffer is closed.
     */
    @Override
    @NonNegative
    long realCapacity();

    /**
     * Determines if the buffer can write the specified count of bytes directly. The default implementation always
     * returns false. Override this method in sub-classes to provide specific implementation.
     *
     * @param count the number of bytes to write.
     * @return true if the buffer can write the specified count of bytes directly, false otherwise.
     */
    default boolean canWriteDirect(long count) {
        return false;
    }

    /**
     * Writes data from the specified {@code Bytes} object into the output stream. The amount of data written is
     * the minimum of the remaining data in the {@code Bytes} object and the remaining space in the output stream.
     * The position of this output stream is updated accordingly, but the read position of the input data is not changed.
     *
     * @param bytes the {@code Bytes} object from which data is read.
     * @return The current StreamingDataOutput instance.
     * @throws IllegalStateException if the buffer is in an invalid state.
     * @deprecated use write(bytes) instead and alter the bytes as you intended
     */
    @Deprecated(/* to be removed in x.25 */)
    @NotNull
    default S writeSome(@NotNull Bytes<?> bytes)
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
     * Writes the specified number of bytes from the provided {@code RandomDataInput} object into the output stream,
     * starting from the given read offset.
     * The position of this output stream is updated accordingly, but the read position of the input data is not changed.
     *
     * @param bytes      the {@code RandomDataInput} from which data is read.
     * @param readOffset the offset at which reading from the {@code RandomDataInput} starts.
     * @param length     the number of bytes to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException  if there is not enough space left in the output stream.
     * @throws BufferUnderflowException if there is not enough data available in the input.
     * @throws IllegalStateException    if the buffer is in an invalid state.
     * @throws IllegalArgumentException if the {@code readOffset} or {@code length} are invalid.
     */
    @NotNull
    default S write(@NotNull RandomDataInput bytes, @NonNegative long readOffset, @NonNegative long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException, IllegalArgumentException {
        BytesInternal.writeFully(bytes, readOffset, length, this);
        return (S) this;
    }

    /**
     * Writes the specified number of bytes from the provided {@code BytesStore} into the output stream,
     * starting from the given read offset. It ensures that the output stream has enough capacity
     * to accommodate the incoming bytes. The position of this output stream is updated accordingly,
     * but the read position of the input data is not changed.
     *
     * @param bytes      the {@code BytesStore} from which data is read.
     * @param readOffset the offset at which reading from the {@code BytesStore} starts.
     * @param length     the number of bytes to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException  if there is not enough space left in the output stream.
     * @throws BufferUnderflowException if there is not enough data available in the input.
     * @throws IllegalStateException    if the buffer is in an invalid state.
     * @throws IllegalArgumentException if the {@code readOffset} or {@code length} are invalid.
     * @throws NullPointerException     if the provided {@code bytes} object is {@code null}.
     */
    @NotNull
    default S write(@NotNull BytesStore<?, ?> bytes, @NonNegative long readOffset, @NonNegative long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException, IllegalArgumentException {
        requireNonNull(bytes);
        requireNonNegative(readOffset);
        requireNonNegative(length);

        ensureCapacity(length + writePosition());
        BytesInternal.writeFully(bytes, readOffset, length, this);
        return (S) this;
    }

    /**
     * Writes all the bytes from the given {@code byteArray} into the output stream. The position of the output stream
     * is updated according to the number of bytes written.
     *
     * @param byteArray the array of bytes to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     * @throws NullPointerException    if the provided {@code byteArray } is {@code null}.
     */
    @NotNull
    default S write(final byte[] byteArray)
            throws BufferOverflowException, IllegalStateException {
        requireNonNull(byteArray);
        try {
            write(byteArray, 0, byteArray.length);
            return (S) this;
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Writes the specified number of bytes from the given {@code byteArray} into the output stream, starting from the
     * given offset. The position of the output stream is updated according to the number of bytes written.
     *
     * @param byteArray the array of bytes to be written.
     * @param offset    the start index in the array from where to start writing bytes.
     * @param length    the number of bytes to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException        if there is not enough space left in the output stream.
     * @throws IllegalStateException          if the buffer is in an invalid state.
     * @throws IllegalArgumentException       if the provided {@code offset} or {@code length} is negative
     * @throws NullPointerException           if the provided {@code byteArray} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the provided {@code offset} and {@code length} combination is invalid.
     */
    @NotNull
    S write(final byte[] byteArray,
            @NonNegative final int offset,
            @NonNegative final int length) throws BufferOverflowException, IllegalStateException, IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /**
     * Writes the memory content of an object to this output stream.
     *
     * @param o      The object whose memory content is to be written.
     * @param length The length (in bytes) of the object's content to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    default S unsafeWriteObject(Object o, @NonNegative int length)
            throws BufferOverflowException, IllegalStateException {
        return unsafeWriteObject(o, (o.getClass().isArray() ? 4 : 0) + Jvm.objectHeaderSize(), length);
    }

    /**
     * Writes the memory content of an object to this output stream from a specific offset.
     *
     * @param o      The object whose memory content is to be written.
     * @param offset The offset in bytes from the start of the object's content to begin writing from.
     * @param length The length (in bytes) of the object's content to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    default S unsafeWriteObject(Object o, @NonNegative int offset, @NonNegative int length)
            throws BufferOverflowException, IllegalStateException {
        if (this.isDirectMemory()) {
            writeSkip(length); // blow up here if this isn't going to work
            final long dest = addressForWrite(writePosition() - length);
            UnsafeMemory.MEMORY.copyMemory(o, offset, dest, length);
            return (S) this;
        }
        int i = 0;
        for (; i < length - 7; i += 8)
            writeLong(UnsafeMemory.unsafeGetLong(o, (long) offset + i));
        for (; i < length; i++)
            writeByte(UnsafeMemory.unsafeGetByte(o, (long) offset + i));
        return (S) this;
    }

    /**
     * Writes raw native memory to this output stream.
     *
     * @param address The address of the raw memory to write.
     * @param length  The length (in bytes) of the raw memory to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    default S unsafeWrite(long address, @NonNegative int length) {
        if (isDirectMemory()) {
            writeSkip(length); // blow up if there isn't that much space left
            long destAddress = addressForWrite(writePosition() - length);
            UnsafeMemory.copyMemory(address, destAddress, length);
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
     * Writes the available data from the provided {@code ByteBuffer} into this Bytes object.
     * The number of bytes written is constrained by the space available in this Bytes object.
     * The position of this Bytes object is updated according to the number of bytes written.
     *
     * @param buffer the ByteBuffer from which data is read.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException  if there is not enough space left in the output stream.
     * @throws IllegalStateException    if the buffer is in an invalid state.
     * @throws BufferUnderflowException if there is not enough data available in the input ByteBuffer.
     * @throws NullPointerException     if the provided {@code buffer} is {@code null}.
     */
    @NotNull
    S writeSome(@NotNull ByteBuffer buffer)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException;

    /**
     * Writes a boolean value to this output stream.
     *
     * @param flag The boolean value to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    default S writeBoolean(boolean flag)
            throws BufferOverflowException, IllegalStateException {
        return writeByte(flag ? (byte) 'Y' : (byte) 'N');
    }

    /**
     * Writes an integer value to this output stream using an ordered-writing mechanism.
     * Ordered writing ensures that the integer is written with a write memory barrier.
     *
     * @param i The integer value to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    S writeOrderedInt(int i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a long value to this output stream using an ordered-writing mechanism.
     * Ordered writing ensures that the integer is written with a write memory barrier.
     *
     * @param i The long value to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    S writeOrderedLong(long i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes an enum value to this output stream by writing the enum's name.
     *
     * @param e The enum value to be written.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws ArithmeticException     if a numeric error occurs.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    default <E extends Enum<E>> S writeEnum(@NotNull E e)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return write8bit(e.name());
    }

    /**
     * Appends a UTF-8 encoded CharSequence to this output stream.
     *
     * @param cs The CharSequence to be appended.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException  if there is not enough space left in the output stream.
     * @throws IllegalStateException    if the buffer is in an invalid state.
     * @throws BufferUnderflowException if there is not enough data available in the input CharSequence.
     */
    @NotNull
    default S appendUtf8(@NotNull CharSequence cs)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
        return appendUtf8(cs, 0, cs.length());
    }

    /**
     * Appends a UTF-8 encoded character to this output stream.
     *
     * @param codepoint The Unicode code point of the character to be appended.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @NotNull
    default S appendUtf8(int codepoint)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.appendUtf8Char(this, codepoint);
        return (S) this;
    }

    /**
     * Appends a UTF-8 encoded CharSequence to this output stream from a specific offset and length.
     *
     * @param cs     The CharSequence to be appended.
     * @param offset The offset from which to start writing the CharSequence.
     * @param length The number of characters from the CharSequence to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException   if there is not enough space left in the output stream.
     * @throws IndexOutOfBoundsException if the offset or length is out of bounds for the given CharSequence.
     * @throws IllegalStateException     if the buffer is in an invalid state.
     * @throws BufferUnderflowException  if there is not enough data available in the input CharSequence.
     */
    @NotNull
    default S appendUtf8(char[] chars, @NonNegative int offset, @NonNegative int length)
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

    /**
     * Appends a UTF-8 encoded CharSequence to this output stream from a specific offset and length.
     *
     * @param cs     The CharSequence to be appended.
     * @param offset The offset from which to start writing the CharSequence.
     * @param length The number of characters from the CharSequence to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException   if there is not enough space left in the output stream.
     * @throws IndexOutOfBoundsException if the offset or length is out of bounds for the given CharSequence.
     * @throws IllegalStateException     if the buffer is in an invalid state.
     * @throws BufferUnderflowException  if there is not enough data available in the input CharSequence.
     */
    @NotNull
    default S appendUtf8(@NotNull CharSequence cs, @NonNegative int offset, @NonNegative int length)
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException, BufferUnderflowException {
        BytesInternal.appendUtf8(this, cs, offset, length);
        return (S) this;
    }

    /**
     * Appends a UTF-8 encoded byte array to this output stream, taking into account the provided offset, length, and coder.
     *
     * @param bytes  The byte array to be appended.
     * @param offset The offset from which to start writing the byte array.
     * @param length The number of characters (not bytes) from the byte array to write.
     * @param coder  The coder indicating the encoding of the byte array.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @Java9
    @NotNull
    default S appendUtf8(final byte[] bytes, @NonNegative int offset, @NonNegative int length, byte coder)
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

    /**
     * Appends a UTF-8 encoded byte array to this output stream, taking into account the provided offset and length.
     *
     * @param bytes  The byte array to be appended.
     * @param offset The offset from which to start writing the byte array.
     * @param length The length of the byte array to write.
     * @return The current StreamingDataOutput instance.
     * @throws BufferOverflowException if there is not enough space left in the output stream.
     * @throws IllegalStateException   if the buffer is in an invalid state.
     */
    @Java9
    @NotNull
    default S appendUtf8(final byte[] bytes, @NonNegative int offset, @NonNegative int length)
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

    /**
     * Copies data from the provided InputStream into this Bytes object.
     *
     * @param input The InputStream from which data should be copied.
     * @throws IOException             if an I/O error occurs when reading from the InputStream.
     * @throws BufferOverflowException if there is not enough space in this Bytes object to store the incoming data.
     * @throws IllegalStateException   if this Bytes object is in an invalid state.
     */
    default void copyFrom(@NotNull InputStream input)
            throws IOException, BufferOverflowException, IllegalStateException {
        BytesInternal.copy(input, this);
    }

    /**
     * Sets the write position and the remaining length of writable bytes in this Bytes object.
     *
     * @param position The new write position.
     * @param length   The new length of writable bytes remaining.
     * @throws BufferOverflowException if the provided position and length exceeds the size of the buffer.
     */
    default void writePositionRemaining(@NonNegative long position, @NonNegative long length)
            throws BufferOverflowException {
        requireNonNegative(position);
        requireNonNegative(length);
        writeLimit(position + length);
        writePosition(position);
    }

    /**
     * Writes the given Histogram object into this Bytes object.
     *
     * @param histogram The Histogram object to be written.
     * @throws BufferOverflowException if there is not enough space in this Bytes object to store the Histogram.
     * @throws IllegalStateException   if this Bytes object is in an invalid state.
     */
    default void writeHistogram(@NotNull Histogram histogram)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.writeHistogram(this, histogram);
    }

    /**
     * Writes the given BigDecimal into this Bytes object.
     *
     * @param bd The BigDecimal object to be written.
     * @throws BufferOverflowException  if there is not enough space in this Bytes object to store the BigDecimal.
     * @throws IllegalStateException    if this Bytes object is in an invalid state.
     * @throws IllegalArgumentException if the BigDecimal cannot be written.
     */
    default void writeBigDecimal(@NotNull BigDecimal bd)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException {
        writeBigInteger(bd.unscaledValue());
        writeStopBit(bd.scale());
    }

    /**
     * Writes the given BigInteger into this Bytes object.
     *
     * @param bi The BigInteger object to be written.
     * @throws BufferOverflowException  if there is not enough space in this Bytes object to store the BigInteger.
     * @throws IllegalStateException    if this Bytes object is in an invalid state.
     * @throws IllegalArgumentException if the BigInteger cannot be written.
     */
    default void writeBigInteger(@NotNull BigInteger bi)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException {
        byte[] bytes = bi.toByteArray();
        throwExceptionIfReleased(this);
        writeStopBit(bytes.length);
        write(bytes);
    }

    /**
     * Writes data from the provided RandomDataInput into this Bytes object with prefixed length.
     *
     * @param bytes The RandomDataInput source of data to be written.
     * @throws BufferOverflowException if there is not enough space in this Bytes object to store the incoming data.
     * @throws IllegalStateException   if this Bytes object is in an invalid state.
     */
    default void writeWithLength(@NotNull RandomDataInput bytes)
            throws BufferOverflowException, IllegalStateException {
        writeStopBit(bytes.readRemaining());
        write(bytes);
    }

    // internal buffer for a byte[]
    byte[] internalNumberBuffer();
}
