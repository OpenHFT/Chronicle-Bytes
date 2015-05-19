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

import net.openhft.chronicle.core.OS;

/**
 * This allows random access to the underling bytes.  This instance can be used across threads as it is stateless.
 * The thread safety of the underlying data depends on how the methods are used.
 */
public interface RandomDataInput<S extends RandomDataInput<S, A, AT>, A extends ReadAccess<AT>, AT>
        extends RandomCommon<S, A, AT> {
    default boolean readBoolean(long offset) {
        return readByte(offset) != 0;
    }

    byte readByte(long offset);

    default int readUnsignedByte(long offset) {
        return readByte(offset) & 0xFF;
    }

    short readShort(long offset);

    default int readUnsignedShort(long offset) {
        return readShort(offset) & 0xFFFF;
    }

    int readInt(long offset);

    default long readUnsignedInt(long offset) {
        return readInt(offset) & 0xFFFFFFFFL;
    }

    long readLong(long offset);

    float readFloat(long offset);

    double readDouble(long offset);

    default char printable(long offset) {
        int b = readUnsignedByte(offset);
        if (b == 0)
            return '\u0660';
        else if (b < 21)
            return (char) (b + 0x2487);
        else
            return (char) b;
    }

    default int readVolatileInt(long offset) {
        OS.memory().loadFence();
        return readInt(offset);
    }

    default long readVolatileLong(long offset) {
        OS.memory().loadFence();
        return readLong(offset);
    }

    default long parseLong(long offset) {
        return BytesUtil.parseLong(this, offset);
    }

    // this "needless" override is needed for better erasure while accessing raw Bytes/BytesStore
    @Override
    A access();

    /**
     * expert level method for copying data to native memory.
     *
     * @param position within the ByteStore to copy.
     * @param address  in native memory
     * @param size     in bytes
     */
    void nativeRead(long position, long address, long size);
}
