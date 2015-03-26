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
