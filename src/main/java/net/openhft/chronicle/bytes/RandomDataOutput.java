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

import java.nio.ByteBuffer;

public interface RandomDataOutput<R extends RandomDataOutput<R>> extends RandomCommon {
    default R writeByte(long offset, int i) {
        return writeByte(offset, Maths.toInt8(i));
    }

    default R writeUnsignedByte(long offset, int i) {
        return writeByte(offset, (byte) Maths.toUInt8(i));
    }

    default R writeBoolean(long offset, boolean flag) {
        return writeByte(offset, flag ? 'Y' : 0);
    }

    default R writeUnsignedShort(long offset, int i) {
        return writeShort(offset, (short) Maths.toUInt16(i));
    }

    default R writeUnsignedInt(long offset, long i) {
        return writeInt(offset, (int) Maths.toUInt32(i));
    }

    R writeByte(long offset, byte i8);

    R writeShort(long offset, short i);

    R writeInt(long offset, int i);

    R writeOrderedInt(long offset, int i);

    R writeLong(long offset, long i);

    R writeOrderedLong(long offset, long i);

    R writeFloat(long offset, float d);

    R writeDouble(long offset, double d);

    default R write(long offsetInRDO, byte[] bytes) {
        return write(offsetInRDO, bytes, 0, bytes.length);
    }

    R write(long offsetInRDO, byte[] bytes, int offset, int length);

    void write(long offsetInRDO, ByteBuffer bytes, int offset, int length);

    default R write(long offsetInRDO, Bytes bytes) {
        return write(offsetInRDO, bytes, bytes.readPosition(), bytes.readRemaining());
    }

    R write(long offsetInRDO, RandomDataInput bytes, long offset, long length);

    R zeroOut(long start, long end) ;

    default R append(long offset, long value, int digits) {
        BytesUtil.append(this, offset, value, digits);
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
