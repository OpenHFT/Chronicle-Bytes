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

import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Position based access.  Once data has been read, the position() moves.
 * <p>The use of this instance is single threaded, though the use of the data
 */
public interface StreamingDataOutput<S extends StreamingDataOutput<S, A, AT>,
        A extends WriteAccess<AT>, AT> extends StreamingCommon<S, A, AT> {
    default ObjectOutput objectStream() {
        throw new UnsupportedOperationException();
    }

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

    S write(BytesStore bytes);

    S write(Bytes bytes);

    S write(BytesStore buffer, long offset, long length);

    S write(Bytes buffer, long offset, long length);

    default S write(byte[] bytes) {
        return write(bytes, 0, bytes.length);
    }

    S write(byte[] bytes, int offset, int length);

    S write(ByteBuffer buffer);

    default <T> S write(ReadAccess<T> access, T handle, long offset, long len) {
        long targetOffset = accessPositionOffset();
        skip(len);
        Access.copy(access, handle, offset, access(), accessHandle(), targetOffset, len);
        return (S) this;
    }

    default <T, H> S write(Accessor<T, H, ? extends ReadAccess<H>> accessor,
                           T source, long offset, long len) {
        return write(accessor.access(source), accessor.handle(source),
                accessor.offset(source, offset), accessor.size(len));
    }

    default S writeBoolean(boolean flag) {
        return writeByte(flag ? (byte) 'Y' : 0);
    }

    S writeOrderedInt(int i);

    S writeOrderedLong(long i);

    // this "needless" override is needed for better erasure while accessing raw Bytes/BytesStore
    @Override
    A access();

    /**
     * This is an expert level method for writing out data to native memory.
     *
     * @param address to write to.
     * @param size    in bytes.
     */
    void nativeWrite(long address, long size);
}
