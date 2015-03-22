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

interface RandomDataInputAccess<S extends RandomDataInput<S>> extends ReadAccess<S> {
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
    default char printable(S handle, long offset) {
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
