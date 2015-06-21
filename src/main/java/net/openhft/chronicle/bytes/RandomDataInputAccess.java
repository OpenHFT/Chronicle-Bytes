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

interface RandomDataInputAccess<S extends RandomDataInput<S, ?, ?>> extends ReadAccess<S> {
    @Override
    default boolean readBoolean(S handle, long offset) {
        return handle.readBoolean(offset);
    }

    @Override
    default byte readByte(S handle, long offset) {
        return handle.readByte(offset);
    }

    @Override
    default int readUnsignedByte(S handle, long offset) {
        return handle.readUnsignedByte(offset);
    }

    @Override
    default short readShort(S handle, long offset) {
        return handle.readShort(offset);
    }

    @Override
    default int readUnsignedShort(S handle, long offset) {
        return handle.readUnsignedShort(offset);
    }

    @Override
    default int readInt(S handle, long offset) {
        return handle.readInt(offset);
    }

    @Override
    default long readUnsignedInt(S handle, long offset) {
        return handle.readUnsignedInt(offset);
    }

    @Override
    default long readLong(S handle, long offset) {
        return handle.readLong(offset);
    }

    @Override
    default float readFloat(S handle, long offset) {
        return handle.readFloat(offset);
    }

    @Override
    default double readDouble(S handle, long offset) {
        return handle.readDouble(offset);
    }

    @Override
    default String printable(S handle, long offset) {
        return handle.printable(offset);
    }

    @Override
    default int readVolatileInt(S handle, long offset) {
        return handle.readVolatileInt(offset);
    }

    @Override
    default long readVolatileLong(S handle, long offset) {
        return handle.readVolatileLong(offset);
    }

    @Override
    default ByteOrder byteOrder(S handle) {
        return handle.byteOrder();
    }
}
