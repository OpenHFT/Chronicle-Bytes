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

import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Position based access.  Once data has been read, the position() moves.
 * <p>The use of this instance is single threaded, though the use of the data
 */
public interface StreamingDataOutput<S extends StreamingDataOutput<S>> extends StreamingCommon<S> {
    default OutputStream outputStream() {
        throw new UnsupportedOperationException();
    }

    default S writeStopBit(long x) {
        BytesUtil.writeStopBit(this, x);
        return (S) this;
    }

    /**
     * Write the same encoding as <code>writeUTF</code> with the following changes.  1) The length is stop bit encoded
     * i.e. one byte longer for short strings, but is not limited in length. 2) The string can be null.
     *
     * @param cs the string value to be written. Can be null.
     * @throws BufferOverflowException if there is not enough space left
     */
    default S writeUTFΔ(CharSequence cs) throws BufferOverflowException {
        BytesUtil.writeUTF(this, cs);
        return (S) this;
    }

    S writeByte(byte i8);

    default S writeUnsignedByte(int i) {
        return writeByte((byte) Maths.toUInt8(i));
    }

    S writeShort(short i16);

    default S writeUnsignedShort(int u16) {
        return writeShort((short) Maths.toUInt16(u16));
    }

    S writeInt(int i);

    default S writeUnsignedInt(long i) {
        return writeInt((int) Maths.toUInt32(i));
    }

    S writeLong(long i64);

    S writeFloat(float f);

    S writeDouble(double d);

    default S write(BytesStore bytes) {
        return write(bytes, bytes.position(), bytes.readLimit() - bytes.position());
    }

    S write(BytesStore buffer, long offset, long length);

    default S write(byte[] bytes) {
        return write(bytes, 0, bytes.length);
    }

    S write(byte[] bytes, int offset, int length);

    S write(ByteBuffer buffer);

    default S writeBoolean(boolean flag) {
        return writeByte(flag ? (byte) 'Y' : 0);
    }

    S writeOrderedInt(int i);

    S writeOrderedLong(long i);

    /**
     * This is an expert level method for writing out data to native memory.
     *
     * @param address to write to.
     * @param size    in bytes.
     */
    void nativeWrite(long address, long size);
}
