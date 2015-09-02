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
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public interface RandomDataOutput<R extends RandomDataOutput<R>> extends RandomCommon {
    default R writeByte(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeByte(offset, Maths.toInt8(i));
    }

    default R writeUnsignedByte(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeByte(offset, (byte) Maths.toUInt8(i));
    }

    default R writeBoolean(long offset, boolean flag)
            throws BufferOverflowException, IORuntimeException {
        try {
            return writeByte(offset, flag ? 'Y' : 0);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    default R writeUnsignedShort(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeShort(offset, (short) Maths.toUInt16(i));
    }

    default R writeUnsignedInt(long offset, long i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeInt(offset, (int) Maths.toUInt32(i));
    }

    R writeByte(long offset, byte i8)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    R writeShort(long offset, short i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    R writeInt(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param i      value to write
     * @return this
     */
    R writeOrderedInt(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param f      value to write
     * @return this
     */
    default R writeOrderedFloat(long offset, float f)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeOrderedInt(offset, Float.floatToRawIntBits(f));
    }

    R writeLong(long offset, long i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param i      value to write
     * @return this
     */
    R writeOrderedLong(long offset, long i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    /**
     * Perform a non stalling write with a store barrier.
     *
     * @param offset to write to
     * @param d      value to write
     * @return this
     */
    default R writeOrderedDouble(long offset, double d)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return writeOrderedLong(offset, Double.doubleToRawLongBits(d));
    }

    R writeFloat(long offset, float d)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    R writeDouble(long offset, double d)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    default R write(long offsetInRDO, @NotNull byte[] bytes)
            throws BufferOverflowException, IORuntimeException {
        try {
            return write(offsetInRDO, bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    R write(long offsetInRDO, byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    void write(long offsetInRDO, ByteBuffer bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    default R write(long offsetInRDO, @NotNull Bytes bytes)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        try {
            return write(offsetInRDO, bytes, bytes.readPosition(), bytes.readRemaining());
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    R write(long offsetInRDO, RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException, BufferUnderflowException;

    R zeroOut(long start, long end) throws IllegalArgumentException, IORuntimeException;

    @NotNull
    default R append(long offset, long value, int digits)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
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
}
