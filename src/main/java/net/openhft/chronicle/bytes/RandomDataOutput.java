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
import net.openhft.chronicle.core.Maths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

@SuppressWarnings({"rawtypes", "unchecked"})
public interface RandomDataOutput<R extends RandomDataOutput<R>> extends RandomCommon {
    /**
     * Write a byte at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException  if the capacity was exceeded
     * @throws IllegalArgumentException if the value cannot be cast to the type without loss.
     */
    @NotNull
    default R writeByte(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, ArithmeticException, IllegalStateException {
        return writeByte(offset, Maths.toInt8(i));
    }

    /**
     * Write an unsigned byte at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException  if the capacity was exceeded
     * @throws IllegalArgumentException if the value cannot be cast to the type without loss.
     */
    @NotNull
    default R writeUnsignedByte(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, ArithmeticException, IllegalStateException {
        return writeByte(offset, (byte) Maths.toUInt8(i));
    }

    /**
     * Write a boolean at an offset.
     *
     * @param offset to write to
     * @param flag   the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    default R writeBoolean(long offset, boolean flag)
            throws BufferOverflowException, IllegalStateException {
        try {
            return writeByte(offset, flag ? 'Y' : 'N');

        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Write an unsigned byte at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     * @throws ArithmeticException     if the value cannot be cast to the type without loss.
     */
    @NotNull
    default R writeUnsignedShort(long offset, int i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeShort(offset, (short) Maths.toUInt16(i));
    }

    /**
     * Write an unsigned byte at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     * @throws ArithmeticException     if the value cannot be cast to the type without loss.
     */
    @NotNull
    default R writeUnsignedInt(long offset, long i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeInt(offset, (int) Maths.toUInt32(i));
    }

    /**
     * Write an unsigned byte at an offset.
     *
     * @param offset to write to
     * @param i8     the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    R writeByte(long offset, byte i8)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Write a short at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    R writeShort(long offset, short i)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    default R writeInt24(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        writeShort(offset, (short) i);
        return writeByte(offset + 2, (byte) (i >> 16));
    }

    /**
     * Write an int at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    R writeInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param i      value to write
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    R writeOrderedInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param f      value to write
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    default R writeOrderedFloat(long offset, float f)
            throws BufferOverflowException, IllegalStateException {
        return writeOrderedInt(offset, Float.floatToRawIntBits(f));
    }

    /**
     * Write a long at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    R writeLong(long offset, long i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param i      value to write
     * @return this
     */
    @NotNull
    R writeOrderedLong(long offset, long i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param d      value to write
     * @return this
     */
    @NotNull
    default R writeOrderedDouble(long offset, double d)
            throws BufferOverflowException, IllegalStateException {
        return writeOrderedLong(offset, Double.doubleToRawLongBits(d));
    }

    /**
     * Write a float at an offset.
     *
     * @param offset to write to
     * @param d      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    R writeFloat(long offset, float d)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Write a double at an offset.
     *
     * @param offset to write to
     * @param d      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    R writeDouble(long offset, double d)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    R writeVolatileByte(long offset, byte i8)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    R writeVolatileShort(long offset, short i16)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    R writeVolatileInt(long offset, int i32)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    R writeVolatileLong(long offset, long i64)
            throws BufferOverflowException, IllegalStateException;

    @NotNull
    default R writeVolatileFloat(long offset, float f)
            throws BufferOverflowException, IllegalStateException {
        return writeVolatileInt(offset, Float.floatToRawIntBits(f));
    }

    @NotNull
    default R writeVolatileDouble(long offset, double d)
            throws BufferOverflowException, IllegalStateException {
        return writeVolatileLong(offset, Double.doubleToRawLongBits(d));
    }

    /**
     * Copies whole byte[] into this. See {@link #write(long, byte[], int, int)}
     */
    @NotNull
    default R write(long offsetInRDO, @NotNull(exception = NullPointerException.class) byte[] bytes)
            throws BufferOverflowException, IllegalStateException {
        requireNonNull(bytes);
        return write(offsetInRDO, bytes, 0, bytes.length);
    }

    /**
     * Copy from byte[] into this.
     * <p>
     * Does not update cursors e.g. {@link #writePosition}
     *
     * @param writeOffset offset to write to
     * @param bytes       copy from bytes
     * @param readOffset  copy from offset
     * @param length
     * @return this
     * @throws BufferOverflowException
     * @throws IllegalStateException
     */
    @NotNull
    R write(long writeOffset, @NotNull(exception = NullPointerException.class) byte[] bytes, int readOffset, int length)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Copy from ByteBuffer into this.
     * <p>
     * Does not update cursors e.g. {@link #writePosition}
     *
     * @param writeOffset offset to write to
     * @param bytes       copy from bytes
     * @param readOffset  copy from offset
     * @param length
     * @return this
     * @throws BufferOverflowException
     * @throws IllegalStateException
     */
    void write(long writeOffset, @NotNull(exception = NullPointerException.class) ByteBuffer bytes, int readOffset, int length)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Copies whole BytesStore into this - see {@link #write(long, RandomDataInput, long, long)}
     */
    @NotNull
    default R write(long offsetInRDO, @NotNull(exception = NullPointerException.class) BytesStore bytes)
            throws BufferOverflowException, IllegalStateException {
        requireNonNull(bytes);
        try {
            return write(offsetInRDO, bytes, bytes.readPosition(), bytes.readRemaining());

        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Copy from RandomDataInput into this. Does not bump {@link #writePosition} nor {@link RandomDataInput#readPosition()}
     *
     * @param writeOffset offset to write to
     * @param bytes       copy from bytes
     * @param readOffset  copy from offset
     * @param length
     * @return this
     * @throws BufferOverflowException
     * @throws IllegalStateException
     */
    @NotNull
    R write(long writeOffset, @NotNull(exception = NullPointerException.class) RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException;

    /**
     * Zero out the bytes between the start and the end.
     *
     * @param start index of first byte inclusive
     * @param end   index of last byte exclusive
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    @NotNull
    R zeroOut(long start, long end)
            throws IllegalStateException;

    @NotNull
    default R append(long offset, long value, int digits)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        BytesInternal.append(this, offset, value, digits);
        return (R) this;
    }

    @NotNull
    default R append(long offset, double value, int decimalPlaces, int digits)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException, ArithmeticException {
        if (decimalPlaces < 20) {
            double d2 = value * Maths.tens(decimalPlaces);
            if (d2 <= Long.MAX_VALUE && d2 >= Long.MIN_VALUE) {
                BytesInternal.appendDecimal(this, Math.round(d2), offset, decimalPlaces, digits);
                return (R) this;
            }
        }
        BytesInternal.append((StreamingDataOutput) this, value);
        return (R) this;
    }

    /**
     * expert level method to copy data from native memory into the BytesStore
     *
     * @param address  in native memory to copy from
     * @param position in BytesStore to copy to
     * @param size     in bytes
     */
    void nativeWrite(long address, long position, long size)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes the provided {@code text} into this {@code RandomDataOutput} writing at the given {@code writeOffset},
     * in Utf8 format. Returns the new write position after writing the provided {@code text}.
     *
     * @param writeOffset the writeOffset to write char sequence from
     * @param text        the char sequence to write, could be {@code null}
     * @return the writeOffset after the char sequence written, in this {@code RandomDataOutput}
     * @see RandomDataInput#readUtf8(long, Appendable)
     */
    default long writeUtf8(long writeOffset, @Nullable CharSequence text)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.writeUtf8(this, writeOffset, text);
    }

    /**
     * Writes the given {@code text} to this {@code RandomDataOutput} writing at the provided {@code writeOffset},
     * in Utf8 format, checking that the utf8 encoding size of the given char sequence is less or
     * equal to the provided {@code maxUtf8Len}, otherwise {@code IllegalArgumentException} is thrown,
     * and no bytes of this {@code RandomDataOutput} are overwritten. Returns the new write position after
     * writing the provided {@code text}
     *
     * @param writeOffset  the writeOffset to write char sequence from
     * @param text         the char sequence to write, could be {@code null}
     * @param maxUtf8Len   the maximum allowed length (in Utf8 encoding) of the given char sequence
     * @return the writeOffset after the char sequence written, in this {@code RandomDataOutput}
     * @throws IllegalArgumentException if the given char sequence size in Utf8 encoding exceeds
     *                                  maxUtf8Len
     * @see RandomDataInput#readUtf8Limited(long, Appendable, int)
     * @see RandomDataInput#readUtf8Limited(long, int)
     */
    default long writeUtf8Limited(long writeOffset, @Nullable CharSequence text, int maxUtf8Len)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.writeUtf8(this, writeOffset, text, maxUtf8Len);
    }

    /**
     * Write the stop bit length and copy the BytesStore
     *
     * @param position to write
     * @param bs       to copy.
     * @return the offset after the char sequence written, in this {@code RandomDataOutput}
     */
    long write8bit(long position, @NotNull(exception = NullPointerException.class) BytesStore bs);

    long write8bit(long position, @NotNull(exception = NullPointerException.class) String s, int start, int length);

}
