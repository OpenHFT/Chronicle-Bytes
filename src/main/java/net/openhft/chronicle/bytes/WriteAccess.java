/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    default <S> void writeFrom(
            T handle, long offset,
            ReadAccess<S> sourceAccess, S source, long sourceOffset, long len) {
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
        int ic;
        char c;
        int i;
        long l;
        switch (b) {
            case 0:
                c = 0;
                i = 0;
                l = 0;
                break;
            case -1:
                c = Character.MAX_VALUE;
                i = -1;
                l = -1;
                break;
            default:
                ic = b | ((b & 0xFF) << 8);
                c = (char) ic;
                i = ic | (ic << 16);
                l = i | ((i & 0xFFFFFFFFL) << 32);
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

}
