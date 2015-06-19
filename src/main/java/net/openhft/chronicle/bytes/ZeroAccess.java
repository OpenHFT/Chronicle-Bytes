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

import java.nio.ByteOrder;

enum ZeroAccess implements ReadAccess<Void> {
    INSTANCE;

    @Override
    public boolean readBoolean(Void handle, long offset) {
        return false;
    }

    @Override
    public byte readByte(Void handle, long offset) {
        return 0;
    }

    @Override
    public int readUnsignedByte(Void handle, long offset) {
        return 0;
    }

    @Override
    public short readShort(Void handle, long offset) {
        return 0;
    }

    @Override
    public int readUnsignedShort(Void handle, long offset) {
        return 0;
    }

    @Override
    public char readChar(Void handle, long offset) {
        return 0;
    }

    @Override
    public int readInt(Void handle, long offset) {
        return 0;
    }

    @Override
    public long readUnsignedInt(Void handle, long offset) {
        return 0;
    }

    @Override
    public long readLong(Void handle, long offset) {
        return 0;
    }

    @Override
    public float readFloat(Void handle, long offset) {
        return 0;
    }

    @Override
    public double readDouble(Void handle, long offset) {
        return 0;
    }

    @Override
    public char printable(Void handle, long offset) {
        return 0;
    }

    @Override
    public int readVolatileInt(Void handle, long offset) {
        return 0;
    }

    @Override
    public long readVolatileLong(Void handle, long offset) {
        return 0;
    }

    @Override
    public ByteOrder byteOrder(Void handle) {
        return ByteOrder.nativeOrder();
    }
}
