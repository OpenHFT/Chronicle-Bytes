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
import net.openhft.chronicle.core.annotation.NotNull;

import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Position based access.  Once data has been read, the position() moves.
 * <p>The use of this instance is single threaded, though the use of the data
 */
public interface StreamingDataOutput<S extends StreamingDataOutput<S>> extends StreamingCommon<S> {
    @NotNull
    default OutputStream outputStream() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    default S writeStopBit(long x) {
        BytesUtil.writeStopBit(this, x);
        return (S) this;
    }

    S writePosition(long position);

    S writeLimit(long limit);

    /**
     * Write the same encoding as <code>writeUTF</code> with the following changes.  1) The length is stop bit encoded
     * i.e. one byte longer for short strings, but is not limited in length. 2) The string can be null.
     *
     * @param cs the string value to be written. Can be null.
     * @throws BufferOverflowException if there is not enough space left
     */
    @NotNull
    default S writeUTFΔ(CharSequence cs) throws BufferOverflowException {
        BytesUtil.writeUTF(this, cs);
        return (S) this;
    }

    default S write8bit(CharSequence cs) {
        BytesUtil.write8bit(this, cs);
        return (S) this;
    }

    default S write8bit(@NotNull BytesStore sdi) {
        BytesUtil.write8bit(this, sdi);
        return (S) this;
    }

    @NotNull
    S writeByte(byte i8);

    @NotNull
    default S writeUnsignedByte(int i) {
        return writeByte((byte) Maths.toUInt8(i));
    }

    @NotNull
    S writeShort(short i16);

    @NotNull
    default S writeUnsignedShort(int u16) {
        return writeShort((short) Maths.toUInt16(u16));
    }

    @NotNull
    S writeInt(int i);

    @NotNull
    default S writeUnsignedInt(long i) {
        return writeInt((int) Maths.toUInt32(i));
    }

    @NotNull
    S writeLong(long i64);

    @NotNull
    S writeFloat(float f);

    @NotNull
    S writeDouble(double d);

    @NotNull
    default S write(@NotNull BytesStore bytes) {
        return write(bytes, bytes.readPosition(), bytes.readRemaining());
    }

    @NotNull
    default S write(@NotNull BytesStore bytes, long offset, long length) {
        BytesUtil.write(bytes, offset, length, this);
        return (S) this;
    }

    @NotNull
    default S write(@NotNull byte[] bytes) {
        return write(bytes, 0, bytes.length);
    }

    @NotNull
    S write(byte[] bytes, int offset, int length);

    @NotNull
    S write(ByteBuffer buffer);

    @NotNull
    default S writeBoolean(boolean flag) {
        return writeByte(flag ? (byte) 'Y' : 0);
    }

    @NotNull
    S writeOrderedInt(int i);

    @NotNull
    S writeOrderedLong(long i);

    /**
     * This is an expert level method for writing out data to native memory.
     *
     * @param address to write to.
     * @param size    in bytes.
     */
    void nativeWrite(long address, long size);

    default <E extends Enum<E>> void writeEnum(E e) {
        write8bit(e.name());
    }
}
