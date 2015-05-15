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

import java.io.InputStream;
import java.io.ObjectInput;
import java.nio.ByteBuffer;

/**
 * This data input has a a position() and a limit()
 */
public interface StreamingDataInput<S extends StreamingDataInput<S, A, AT>,
        A extends ReadAccess<AT>, AT> extends StreamingCommon<S, A, AT> {
    UnderflowMode underflowMode();

    S underflowMode(UnderflowMode underflowMode);

    default ObjectInput objectInput() {
        throw new UnsupportedOperationException();
    }

    default InputStream inputStream() {
        throw new UnsupportedOperationException();
    }

    default boolean readUTFΔ(StringBuilder sb) throws UTFDataFormatRuntimeException {
        sb.setLength(0);
        long len0 = BytesUtil.readStopBit(this);
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        BytesUtil.parseUTF(this, sb, len);
        return true;
    }

    default long readStopBit() {
        return BytesUtil.readStopBit(this);
    }

    default boolean readBoolean() {
        return readByte() != 0;
    }

    byte readByte();

    default int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    short readShort();

    default int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    int readInt();

    default long readUnsignedInt() {
        return readInt() & 0xFFFFFFFFL;
    }

    long readLong();

    float readFloat();

    double readDouble();

    int peakVolatileInt();

    default String readUTFΔ() {
        return BytesUtil.readUTFΔ(this);
    }

    void read(byte[] bytes);

    void read(ByteBuffer buffer);

    int readVolatileInt();

    long readVolatileLong();

    // this "needless" override is needed for better erasure while accessing raw Bytes/BytesStore
    @Override
    A access();

    int peekUnsignedByte();
}
