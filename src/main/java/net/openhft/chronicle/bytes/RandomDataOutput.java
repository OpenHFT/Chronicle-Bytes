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

import net.openhft.chronicle.core.Maths;

import java.nio.ByteBuffer;

public interface RandomDataOutput<R extends RandomDataOutput<R, A, AT>,
        A extends WriteAccess<AT>, AT> extends RandomCommon<R, A, AT> {
    default R writeByte(long offset, int i) {
        return writeByte(offset, Maths.toInt8(i));
    }

    default R writeUnsignedByte(long offset, int i) {
        return writeByte(offset, (byte) Maths.toUInt8(i));
    }

    default R writeBoolean(long offset, boolean flag) {
        return writeByte(offset, flag ? 'Y' : 0);
    }

    default R writeUnsignedShort(long offset, int i) {
        return writeShort(offset, (short) Maths.toUInt16(i));
    }

    default R writeUnsignedInt(long offset, long i) {
        return writeInt(offset, (int) Maths.toUInt32(i));
    }

    R writeByte(long offset, byte i8);

    R writeShort(long offset, short i);

    R writeInt(long offset, int i);

    R writeOrderedInt(long offset, int i);

    R writeLong(long offset, long i);

    R writeOrderedLong(long offset, long i);

    R writeFloat(long offset, float d);

    R writeDouble(long offset, double d);

    default R write(long offsetInRDO, byte[] bytes) {
        return write(offsetInRDO, bytes, 0, bytes.length);
    }

    R write(long offsetInRDO, byte[] bytes, int offset, int length);

    R write(long offsetInRDO, ByteBuffer bytes, int offset, int length);

    default R write(long offsetInRDO, Bytes bytes) {
        return write(offsetInRDO, bytes, bytes.position(), bytes.remaining());
    }

    R write(long offsetInRDO, Bytes bytes, long offset, long length);

    R zeroOut(long start, long end) ;

    default R append(long offset, long value, int digits) {
        BytesUtil.append(this, offset, value, digits);
        return (R) this;
    }

    // this "needless" override is needed for better erasure while accessing raw Bytes/BytesStore
    @Override
    A access();

    /**
     * expert level method to copy data from native memory into the BytesStore
     *
     * @param address  in native memory to copy from
     * @param position in BytesStore to copy to
     * @param size     in bytes
     */
    void nativeWrite(long address, long position, long size);
}
