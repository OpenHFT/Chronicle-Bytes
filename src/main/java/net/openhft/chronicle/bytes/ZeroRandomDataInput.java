/*
 * Copyright 2015 Higher Frequency Trading
 *
 *  http://www.higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.UnsafeMemory;

public final class ZeroRandomDataInput implements RandomDataInput {
    public static final RandomDataInput INSTANCE = new ZeroRandomDataInput();

    private ZeroRandomDataInput() {}

    @Override
    public byte readByte(long offset) {
        return 0;
    }

    @Override
    public short readShort(long offset) {
        return 0;
    }

    @Override
    public int readInt(long offset) {
        return 0;
    }

    @Override
    public long readLong(long offset) {
        return 0;
    }

    @Override
    public float readFloat(long offset) {
        return 0;
    }

    @Override
    public double readDouble(long offset) {
        return 0;
    }

    @Override
    public void nativeRead(long position, long address, long size) {
        OS.memory().setMemory(address, size, (byte) 0);
    }

    @Override
    public long capacity() {
        return Long.MAX_VALUE;
    }

    @Override
    public long address() throws UnsupportedOperationException {
        return 0;
    }

    @Override
    public Bytes bytesForRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bytes bytesForWrite() {
        throw new UnsupportedOperationException();
    }
}
