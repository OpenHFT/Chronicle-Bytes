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

public interface WriteAccess<T> extends AccessCommon<T> {
    default void writeByte(T handle, long offset, int i) {
        writeByte(handle, offset, Maths.toInt8(i));
    }

    default void writeUnsignedByte(T handle, long offset, int i) {
        writeByte(handle, offset, (byte) Maths.toUInt8(i));
    }

    default void writeBoolean(T handle, long offset, boolean flag) {
        writeByte(handle, offset, flag ? 'Y' : 0);
    }

    default void writeUnsignedShort(T handle, long offset, int i) {
        writeShort(handle, offset, (short) Maths.toUInt16(i));
    }

    default void writeChar(T handle, long offset, char c) {
        writeShort(handle, offset, (short) c);
    }

    default void writeUnsignedInt(T handle, long offset, long i) {
        writeInt(handle, offset, (int) Maths.toUInt32(i));
    }

    void writeByte(T handle, long offset, byte i8);

    void writeShort(T handle, long offset, short i);

    void writeInt(T handle, long offset, int i);

    /**
     * Default implementation: throws {@code UnsupportedOperationException}.
     */
    default void writeOrderedInt(T handle, long offset, int i) {
        throw new UnsupportedOperationException();
    }

    void writeLong(T handle, long offset, long i);

    /**
     * Default implementation: throws {@code UnsupportedOperationException}.
     */
    default void writeOrderedLong(T handle, long offset, long i) {
        throw new UnsupportedOperationException();
    }

    void writeFloat(T handle, long offset, float d);

    void writeDouble(T handle, long offset, double d);

    /**
     * @deprecated use {@link Access#copy} instead
     */
    @Deprecated
    default <S> void writeFrom(
            T handle, long offset,
            ReadAccess<S> sourceAccess, S source, long sourceOffset, long len) {
        if (this == sourceAccess && handle == source && offset == sourceOffset)
            return;
        long i = 0;
        while (len - i >= 8L) {
            writeLong(handle, offset + i, sourceAccess.readLong(source, sourceOffset + i));
            i += 8L;
        }
        if (len - i >= 4L) {
            writeInt(handle, offset + i, sourceAccess.readInt(source, sourceOffset + i));
            i += 4L;
        }
        if (len - i >= 2L) {
            writeShort(handle, offset + i, sourceAccess.readShort(source, sourceOffset + i));
            i += 2L;
        }
        if (i < len)
            writeByte(handle, offset + i, sourceAccess.readByte(source, sourceOffset + i));
    }

    default void writeBytes(T handle, long offset, long len, byte b) {
        char c;
        int i;
        long l;
        switch (b) {
            case 0:
                zeroOut(handle, offset, len);
                return;
            case -1:
                c = Character.MAX_VALUE;
                i = -1;
                l = -1;
                break;
            default:
                int ub = b & 0xFF;
                int ic = ub | (ub << 8);
                c = (char) ic;
                i = ic | (ic << 16);
                long ui = i & 0xFFFFFFFFL;
                l = ui | (ui << 32);
        }
        long index = 0;
        while (len - index >= 8L) {
            writeLong(handle, offset + index, l);
            index += 8L;
        }
        if (len - index >= 4L) {
            writeInt(handle, offset + index, i);
            index += 4L;
        }
        if (len - index >= 2L) {
            writeChar(handle, offset + index, c);
            index += 2L;
        }
        if (index < len)
            writeByte(handle, offset + index, b);
    }

    default void zeroOut(T handle, long offset, long len) {
        long index = 0;
        while (len - index >= 8L) {
            writeLong(handle, offset + index, 0L);
            index += 8L;
        }
        if (len - index >= 4L) {
            writeInt(handle, offset + index, 0);
            index += 4L;
        }
        if (len - index >= 2L) {
            writeChar(handle, offset + index, (char) 0);
            index += 2L;
        }
        if (index < len)
            writeByte(handle, offset + index, (byte) 0);
    }
}
