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

public interface RandomDataOutputAccess<R extends RandomDataOutput<R, ?, ?>>
        extends WriteAccess<R> {
    @Override
    default void writeByte(R handle, long offset, int i) {
        handle.writeByte(offset, i);
    }

    @Override
    default void writeUnsignedByte(R handle, long offset, int i) {
        handle.writeUnsignedByte(offset, i);
    }

    @Override
    default void writeBoolean(R handle, long offset, boolean flag) {
        handle.writeBoolean(offset, flag);
    }

    @Override
    default void writeUnsignedShort(R handle, long offset, int i) {
        handle.writeUnsignedShort(offset, i);
    }

    @Override
    default void writeUnsignedInt(R handle, long offset, long i) {
        handle.writeUnsignedInt(offset, i);
    }

    @Override
    default void writeByte(R handle, long offset, byte i8) {
        handle.writeByte(offset, i8);
    }

    @Override
    default void writeShort(R handle, long offset, short i) {
        handle.writeShort(offset, i);
    }

    @Override
    default void writeInt(R handle, long offset, int i) {
        handle.writeInt(offset, i);
    }

    @Override
    default void writeOrderedInt(R handle, long offset, int i) {
        handle.writeOrderedInt(offset, i);
    }

    @Override
    default void writeLong(R handle, long offset, long i) {
        handle.writeLong(offset, i);
    }

    @Override
    default void writeOrderedLong(R handle, long offset, long i) {
        handle.writeOrderedLong(offset, i);
    }

    @Override
    default void writeFloat(R handle, long offset, float d) {
        handle.writeFloat(offset, d);
    }

    @Override
    default void writeDouble(R handle, long offset, double d) {
        handle.writeDouble(offset, d);
    }

    @Override
    default ByteOrder byteOrder(R handle) {
        return handle.byteOrder();
    }
}
