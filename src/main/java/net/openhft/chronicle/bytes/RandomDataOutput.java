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
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public interface RandomDataOutput<R extends RandomDataOutput<R, A, AT>,
        A extends WriteAccess<AT>, AT> extends RandomCommon<R, A, AT> {
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

    R write(long offsetInRDO, ByteBuffer bytes, int offset, int length);

    default R write(long offsetInRDO, Bytes bytes) {
        return write(offsetInRDO, bytes, bytes.position(), bytes.remaining());
    }

    R write(long offsetInRDO, Bytes bytes, long offset, long length);

    R zeroOut(long start, long end) ;

    default R append(long offset, long value, int digits) {
        BytesUtil.append(this, offset, value, digits);
        return (R) this;
    }

    /**
     * Write the same encoding as <code>writeUTF</code> with the following changes.  1) The length is stop bit encoded
     * i.e. one byte longer for short strings, but is not limited in length. 2) The string can be null.
     *
     * @param offset  to write to
     * @param maxSize maximum number of bytes to use
     * @param s       the string value to be written. Can be null.
     * @throws IllegalStateException if the size is too large.
     */
    default void writeUTFΔ(long offset, int maxSize, @Nullable CharSequence s) throws BufferOverflowException {
        // todo optimise this.
        Bytes bytes = BytesUtil.asBytes(this, offset, offset + maxSize);
        if (s == null) {
            bytes.writeStopBit(-1);
            return;
        }
        bytes.writeStopBit(s.length());

        BytesUtil.writeUTF(bytes.position(), (int) (maxSize - (bytes.position() - offset)), this, s, 0, s.length());
    }

    // this "needless" override is needed for better erasure while accessing raw Bytes/BytesStore
    @Override
    A access();

    /**
     * expert level method to copy data from native memory into the BytesStore
     *
     * @param address  in native memory to copy from
     * @param position in BytesStore to copy to
     * @param size     in bytes
     */
    void nativeWrite(long address, long position, long size);
}
