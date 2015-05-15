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

public interface ReadAccess<T> extends AccessCommon<T> {
    default boolean readBoolean(T handle, long offset) {
        return readByte(handle, offset) != 0;
    }

    byte readByte(T handle, long offset);

    default int readUnsignedByte(T handle, long offset) {
        return readByte(handle, offset) & 0xFF;
    }

    short readShort(T handle, long offset);

    default int readUnsignedShort(T handle, long offset) {
        return readShort(handle, offset) & 0xFFFF;
    }

    default char readChar(T handle, long offset) {
        return (char) readShort(handle, offset);
    }

    int readInt(T handle, long offset);

    default long readUnsignedInt(T handle, long offset) {
        return readInt(handle, offset) & 0xFFFFFFFFL;
    }

    long readLong(T handle, long offset);

    /**
     * Default implementation: {@code Float.intBitsToFloat(readInt(handle, offset))}.
     */
    default float readFloat(T handle, long offset) {
        return Float.intBitsToFloat(readInt(handle, offset));
    }

    /**
     * Default implementation: {@code Double.longBitsToDouble(readLong(handle, offset))}.
     */
    default double readDouble(T handle, long offset) {
        return Double.longBitsToDouble(readLong(handle, offset));
    }

    default char printable(T handle, long offset) {
        int b = readUnsignedByte(handle, offset);
        if (b == 0)
            return '\u0660';
        else if (b < 21)
            return (char) (b + 0x2487);
        else
            return (char) b;
    }

    /**
     * Default implementation: throws {@code UnsupportedOperationException}.
     */
    default int readVolatileInt(T handle, long offset) {
        throw new UnsupportedOperationException();
    }

    /**
     * Default implementation: throws {@code UnsupportedOperationException}.
     */
    default long readVolatileLong(T handle, long offset) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated use {@link Access#equivalent} instead
     */
    @Deprecated
    default <S> boolean compareTo(
            T handle, long offset, ReadAccess<S> sourceAccess, S source, long sourceOffset,
            long len) {
        long i = 0;
        while (len - i >= 8L) {
            if (readLong(handle, offset + i) != sourceAccess.readLong(source, sourceOffset + i))
                return false;
            i += 8L;
        }
        if (len - i >= 4L) {
            if (readInt(handle, offset + i) != sourceAccess.readInt(source, sourceOffset + i))
                return false;
            i += 4L;
        }
        if (len - i >= 2L) {
            if (readShort(handle, offset + i) != sourceAccess.readShort(source, sourceOffset + i))
                return false;
            i += 2L;
        }
        if (i < len)
            if (readByte(handle, offset + i) != sourceAccess.readByte(source, sourceOffset + i))
                return false;
        return true;
    }
}
