/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Created by peter on 08/03/16.
 */
public class CheckingMappedBytes extends MappedBytes {
    public CheckingMappedBytes(MappedFile mappedFile) {
        super(mappedFile);
    }

    @Override
    protected long writeOffsetPositionMoved(long adding) throws BufferOverflowException, IORuntimeException {
        long wp = writePosition();
        if (adding <= 8) {
            long value = 0;
            switch ((int) adding) {
                case 1:
                    value = readByte(wp);
                    break;
                case 2:
                    value = readShort(wp);
                    break;
                case 3:
                    value = (readUnsignedByte(wp) << 16) | readUnsignedShort(wp + 1);
                    break;
                case 4:
                    value = readInt(wp);
                    break;
                case 5:
                    value = ((long) readUnsignedByte(wp) << 32) | readUnsignedInt(wp + 1);
                    break;
                case 6:
                    value = ((long) readUnsignedShort(wp) << 32) | readUnsignedInt(wp + 2);
                    break;
                case 7:
                    value = ((long) readUnsignedByte(wp) << 48) | ((long) readUnsignedShort(wp + 1) << 32) | readUnsignedInt(wp + 3);
                    break;
                case 8:
                    value = readLong(wp);
                    break;
            }
            if (value != 0)
                throw new IllegalStateException("Attempting to over-write " + value + " at " + wp);
        }
        return super.writeOffsetPositionMoved(adding);
    }

    @NotNull
    @Override
    public Bytes<Void> writeOrderedInt(int i) throws BufferOverflowException, IORuntimeException {
        return super.writeOrderedInt(i);
    }

    @NotNull
    @Override
    public Bytes<Void> writeOrderedInt(long offset, int i) throws BufferOverflowException, IORuntimeException {
        return super.writeOrderedInt(offset, i);
    }

    @NotNull
    @Override
    public Bytes<Void> writeOrderedLong(long i) throws BufferOverflowException, IORuntimeException {
        return super.writeOrderedLong(i);
    }

    @NotNull
    @Override
    public Bytes<Void> writeOrderedLong(long offset, long i) throws BufferOverflowException, IORuntimeException {
        return super.writeOrderedLong(offset, i);
    }

    @Override
    public long writePosition() {
        return super.writePosition();
    }

    @Override
    public Bytes<Void> writePosition(long position) throws BufferOverflowException {
        return super.writePosition(position);
    }

    @NotNull
    @Override
    public Bytes<Void> writeShort(short i16) throws BufferOverflowException, IORuntimeException {
        return super.writeShort(i16);
    }

    @NotNull
    @Override
    public Bytes<Void> writeShort(long offset, short i) throws BufferOverflowException, IORuntimeException {
        return super.writeShort(offset, i);
    }

    @Override
    public Bytes<Void> writeSkip(long bytesToSkip) throws BufferOverflowException, IORuntimeException {
        writePosition(writePosition() + bytesToSkip);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Void> writeSome(@NotNull ByteBuffer buffer) throws BufferOverflowException, IORuntimeException {
        return super.writeSome(buffer);
    }

    @Override
    public Bytes<Void> writeVolatileByte(long offset, byte i8) throws BufferOverflowException, IORuntimeException {
        return super.writeVolatileByte(offset, i8);
    }

    @Override
    public Bytes<Void> writeVolatileInt(long offset, int i32) throws BufferOverflowException, IORuntimeException {
        return super.writeVolatileInt(offset, i32);
    }

    @Override
    public Bytes<Void> writeVolatileLong(long offset, long i64) throws BufferOverflowException, IORuntimeException {
        return super.writeVolatileLong(offset, i64);
    }

    @Override
    public Bytes<Void> writeVolatileShort(long offset, short i16) throws BufferOverflowException, IORuntimeException {
        return super.writeVolatileShort(offset, i16);
    }
}
