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

import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Position based access.  Once data has been read, the position() moves.
 * <p>The use of this instance is single threaded, though the use of the data
 */
public interface StreamingDataOutput<S extends StreamingDataOutput<S, A, AT>,
        A extends WriteAccess<AT>, AT> extends StreamingCommon<S, A, AT> {
    default public ObjectOutput objectStream() {
        throw new UnsupportedOperationException();
    }

    default public OutputStream outputStream() {
        throw new UnsupportedOperationException();
    }

    default S writeStopBit(long x) {
        BytesUtil.writeStopBit(this, x);
        return (S) this;
    }

    default S writeUTFΔ(CharSequence cs) {
        BytesUtil.writeUTF(this, cs);
        return (S) this;
    }

    S writeByte(byte i8);

    default S writeUnsignedByte(int i) {
        return writeByte((byte) Maths.toUInt8(i));
    }

    S writeShort(short i16);

    default S writeUnsignedShort(int u16) {
        return writeShort((short) Maths.toUInt16(u16));
    }

    S writeInt(int i);

    default S writeUnsignedInt(long i) {
        return writeInt((int) Maths.toUInt32(i));
    }

    S writeLong(long i64);

    S writeFloat(float f);

    S writeDouble(double d);

    S write(BytesStore bytes);

    S write(Bytes bytes);

    S write(BytesStore buffer, long offset, long length);

    S write(Bytes buffer, long offset, long length);

    default S write(byte[] bytes) {
        return write(bytes, 0, bytes.length);
    }

    S write(byte[] bytes, int offset, int length);

    S write(ByteBuffer buffer);

    default <T> S write(ReadAccess<T> access, T handle, long offset, long len) {
        long targetOffset = accessPositionOffset();
        skip(len);
        Access.copy(access, handle, offset, access(), accessHandle(), targetOffset, len);
        return (S) this;
    }

    default <T, H> S write(Accessor<T, H, ? extends ReadAccess<H>> accessor,
                           T source, long offset, long len) {
        return write(accessor.access(source), accessor.handle(source),
                accessor.offset(source, offset), accessor.size(len));
    }

    default S writeBoolean(boolean flag) {
        return writeByte(flag ? (byte) 'Y' : 0);
    }

    S writeOrderedInt(int i);

    S writeOrderedLong(long i);

    // this "needless" override is needed for better erasure while accessing raw Bytes/BytesStore
    @Override
    A access();

    /**
     * This is an expert level method for writing out data to native memory.
     *
     * @param address to write to.
     * @param size    in bytes.
     */
    void nativeWrite(long address, long size);
}
