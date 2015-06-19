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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ByteBufferAccess implements Access<ByteBuffer> {
    public static final ByteBufferAccess INSTANCE = new ByteBufferAccess();

    private ByteBufferAccess() {}

    @Override
    public byte readByte(ByteBuffer buffer, long offset) {
        return buffer.get((int) offset);
    }

    @Override
    public short readShort(ByteBuffer buffer, long offset) {
        return buffer.getShort((int) offset);
    }

    @Override
    public char readChar(ByteBuffer buffer, long offset) {
        return buffer.getChar((int) offset);
    }

    @Override
    public int readInt(ByteBuffer buffer, long offset) {
        return buffer.getInt((int) offset);
    }

    @Override
    public long readLong(ByteBuffer buffer, long offset) {
        return buffer.getLong((int) offset);
    }

    @Override
    public float readFloat(ByteBuffer buffer, long offset) {
        return buffer.getFloat((int) offset);
    }

    @Override
    public double readDouble(ByteBuffer buffer, long offset) {
        return buffer.getDouble((int) offset);
    }

    @Override
    public void writeByte(ByteBuffer buffer, long offset, byte i8) {
        buffer.put((int) offset, i8);
    }

    @Override
    public void writeShort(ByteBuffer buffer, long offset, short i) {
        buffer.putShort((int) offset, i);
    }

    @Override
    public void writeChar(ByteBuffer buffer, long offset, char c) {
        buffer.putChar((int) offset, c);
    }

    @Override
    public void writeInt(ByteBuffer buffer, long offset, int i) {
        buffer.putInt((int) offset, i);
    }

    @Override
    public void writeLong(ByteBuffer buffer, long offset, long i) {
        buffer.putLong((int) offset, i);
    }

    @Override
    public void writeFloat(ByteBuffer buffer, long offset, float d) {
        buffer.putFloat((int) offset, d);
    }

    @Override
    public void writeDouble(ByteBuffer buffer, long offset, double d) {
        buffer.putDouble((int) offset, d);
    }

    @Override
    public ByteOrder byteOrder(ByteBuffer buffer) {
        return buffer.order();
    }
}
