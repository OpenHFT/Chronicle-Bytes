/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.jitter;

import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;

public class MemoryMessager {

    public static final int NOT_READY = Integer.MIN_VALUE;
    public static final int HEADER_LENGTH = 20;
    private final MappedBytes bytes;
    private final int padTo;
    private final int padMask;
    private long address;
    private long firstLong;

    public MemoryMessager(MappedBytes bytes, int padTo) {
        this.bytes = bytes;
        address = bytes.addressForRead(bytes.readPosition());
        this.padTo = padTo;
        this.padMask = padTo - 1;
    }

    /**
     * Writes length bytes. First writes a 4 byte header then a 8 byte index (count)
     * and then the remaining number of bytes so that total message is of required length
     */
    public void writeMessage(int length, long count, long firstLong) {
        long pos = bytes.writePosition();
        boolean works = bytes.compareAndSwapInt(pos, 0x0, NOT_READY);

        if (!works) throw new AssertionError();
        Jvm.safepoint();
        bytes.writeSkip(4);
        bytes.writeLong(count);
        bytes.writeLong(firstLong);
        if (padTo != 0) {
            int masked = length & padMask;
            if (masked != 0)
                length += (padTo - masked);
        }
        length -= HEADER_LENGTH;
        int i = 0;
        Jvm.safepoint();
        for (; i < length - 7; i += 8)
            bytes.writeLong(i);
        for (; i < length; i++)
            bytes.writeByte((byte) 0);
        Jvm.safepoint();
        boolean works2 = bytes.compareAndSwapInt(pos, NOT_READY, (int) (bytes.writePosition() - pos));
        if (!works2) throw new AssertionError();
    }

    @SuppressWarnings("restriction")
    public int length() {
        UnsafeMemory.unsafeLoadFence();
        int length = UnsafeMemory.unsafeGetInt(address);
        return length;
    }

    public long consumeBytes() {
        int length = length();
        if (length == 0x0 || length == NOT_READY)
            throw new AssertionError("length: " + length);

        Jvm.safepoint();
        bytes.readSkip(4);
        long ret = bytes.readLong();
        this.firstLong = bytes.readLong();
        length -= HEADER_LENGTH;
        int i = 0;
        Jvm.safepoint();
        for (; i < length - 7; i += 8)
            bytes.readLong();
        for (; i < length; i++)
            bytes.readByte();
        Jvm.safepoint();
        address = bytes.addressForRead(bytes.readPosition(), 4);
        return ret;
    }

    public long firstLong() {
        return firstLong;
    }
}
