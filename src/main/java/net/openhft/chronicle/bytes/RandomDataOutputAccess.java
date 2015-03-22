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

import java.nio.ByteOrder;

public interface RandomDataOutputAccess<R extends RandomDataOutput<R>> extends WriteAccess<R> {
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
