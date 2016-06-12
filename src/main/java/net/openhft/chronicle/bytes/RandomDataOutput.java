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
import net.openhft.chronicle.core.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

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
    default R writeByte(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException {
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
    default R writeUnsignedByte(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException {
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
    default R writeBoolean(long offset, boolean flag)
            throws BufferOverflowException {
        try {
            return writeByte(offset, flag ? 'Y' : 0);

        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
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
    default R writeUnsignedShort(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException {
        return writeShort(offset, (short) Maths.toUInt16(i));
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
    default R writeUnsignedInt(long offset, long i)
            throws BufferOverflowException, IllegalArgumentException {
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
    R writeByte(long offset, byte i8) throws BufferOverflowException;

    /**
     * Write a short at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    R writeShort(long offset, short i) throws BufferOverflowException;

    default R writeInt24(long offset, int i) throws BufferOverflowException {
        writeUnsignedShort(offset, i);
        return writeUnsignedByte(offset + 2, i >> 16);
    }

    /**
     * Write an int at an offset.
     *
     * @param offset to write to
     * @param i      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    R writeInt(long offset, int i) throws BufferOverflowException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param i      value to write
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    R writeOrderedInt(long offset, int i) throws BufferOverflowException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param f      value to write
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    default R writeOrderedFloat(long offset, float f) throws BufferOverflowException {
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
    R writeLong(long offset, long i) throws BufferOverflowException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param i      value to write
     * @return this
     */
    R writeOrderedLong(long offset, long i) throws BufferOverflowException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param d      value to write
     * @return this
     */
    default R writeOrderedDouble(long offset, double d) throws BufferOverflowException {
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
    R writeFloat(long offset, float d) throws BufferOverflowException;

    /**
     * Write a double at an offset.
     *
     * @param offset to write to
     * @param d      the value
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    R writeDouble(long offset, double d) throws BufferOverflowException;

    R writeVolatileByte(long offset, byte i8) throws BufferOverflowException;

    R writeVolatileShort(long offset, short i16) throws BufferOverflowException;

    R writeVolatileInt(long offset, int i32) throws BufferOverflowException;

    R writeVolatileLong(long offset, long i64) throws BufferOverflowException;

    default R writeVolatileFloat(long offset, float f) throws BufferOverflowException {
        return writeVolatileInt(offset, Float.floatToRawIntBits(f));
    }

    default R writeVolatileDouble(long offset, double d) throws BufferOverflowException {
        return writeVolatileLong(offset, Double.doubleToRawLongBits(d));
    }

    default R write(long offsetInRDO, @NotNull byte[] bytes) throws BufferOverflowException {
        try {
            return write(offsetInRDO, bytes, 0, bytes.length);

        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    R write(long offsetInRDO, byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException;

    void write(long offsetInRDO, ByteBuffer bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException;

    default R write(long offsetInRDO, @NotNull BytesStore bytes)
            throws BufferOverflowException {
        try {
            return write(offsetInRDO, bytes, bytes.readPosition(), bytes.readRemaining());

        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    R write(long offsetInRDO, RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, IllegalArgumentException, BufferUnderflowException;

    /**
     * Zero out the bytes between the start and the end.
     *
     * @param start index of first byte inclusive
     * @param end   index of last byte exclusive
     * @return this
     * @throws BufferOverflowException if the capacity was exceeded
     */
    R zeroOut(long start, long end) throws IllegalArgumentException;

    @NotNull
    default R append(long offset, long value, int digits)
            throws BufferOverflowException, IllegalArgumentException {
        BytesInternal.append(this, offset, value, digits);
        return (R) this;
    }

    /**
     * expert level method to copy data from native memory into the BytesStore
     *
     * @param address  in native memory to copy from
     * @param position in BytesStore to copy to
     * @param size     in bytes
     */
    void nativeWrite(long address, long position, long size);

    /**
     * Writes the given {@code cs} to this {@code RandomDataOutput} from the given {@code offset},
     * in Utf8 format. Returns the offset after the written char sequence.
     *
     * @param offset the offset to write char sequence from
     * @param cs     the char sequence to write, could be {@code null}
     * @return the offset after the char sequence written, in this {@code RandomDataOutput}
     * @see RandomDataInput#readUtf8(long, Appendable)
     */
    default long writeUtf8(long offset, @Nullable CharSequence cs) throws BufferOverflowException {
        return BytesInternal.writeUtf8(this, offset, cs);
    }

    /**
     * Writes the given {@code cs} to this {@code RandomDataOutput} from the given {@code offset},
     * in Utf8 format, checking that the utf8 encoding size of the given char sequence is less or
     * equal to the given {@code maxUtf8Len}, otherwise {@code IllegalArgumentException} is thrown,
     * and no bytes of this {@code RandomDataOutput} are overwritten. Returns the offset after the
     * written char sequence.
     *
     * @param offset     the offset to write char sequence from
     * @param cs         the char sequence to write, could be {@code null}
     * @param maxUtf8Len the maximum allowed length (in Utf8 encoding) of the given char sequence
     * @return the offset after the char sequence written, in this {@code RandomDataOutput}
     * @throws IllegalArgumentException if the given char sequence size in Utf8 encoding exceeds
     *                                  maxUtf8Len
     * @see RandomDataInput#readUtf8Limited(long, Appendable, int)
     * @see RandomDataInput#readUtf8Limited(long, int)
     */
    default long writeUtf8Limited(long offset, @Nullable CharSequence cs, int maxUtf8Len)
            throws BufferOverflowException, IllegalArgumentException {
        return BytesInternal.writeUtf8(this, offset, cs, maxUtf8Len);
    }
}
