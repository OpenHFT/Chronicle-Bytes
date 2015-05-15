/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
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
