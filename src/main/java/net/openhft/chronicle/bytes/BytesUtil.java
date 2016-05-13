/*
 * Copyright 2016 higherfrequencytrading.com
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

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.StringUtils;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Created by peter on 30/08/15.
 */
public enum BytesUtil {
    ;

    public static boolean bytesEqual(
            @NotNull RandomDataInput a, long offset,
            @NotNull RandomDataInput second, long secondOffset, long len)
            throws IORuntimeException, BufferUnderflowException {
        long i = 0;
        while (len - i >= 8L) {
            if (a.readLong(offset + i) != second.readLong(secondOffset + i))
                return false;
            i += 8L;
        }
        if (len - i >= 4L) {
            if (a.readInt(offset + i) != second.readInt(secondOffset + i))
                return false;
            i += 4L;
        }
        if (len - i >= 2L) {
            if (a.readShort(offset + i) != second.readShort(secondOffset + i))
                return false;
            i += 2L;
        }
        if (i < len)
            if (a.readByte(offset + i) != second.readByte(secondOffset + i))
                return false;
        return true;
    }

    public static boolean bytesEqual(CharSequence cs, RandomDataInput bs, long offset, int length) {
        if (cs == null || cs.length() != length)
            return false;
        for (int i = 0; i < length; i++) {
            if (cs.charAt(i) != bs.readUnsignedByte(offset + i))
                return false;
        }
        return true;
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 == o2) return true;
        if (o1 instanceof CharSequence && o2 instanceof CharSequence)
            return StringUtils.isEqual((CharSequence) o1, (CharSequence) o2);
        return o1 != null && o1.equals(o2);
    }

    public static int asInt(@NotNull String str) {
        ByteBuffer bb = ByteBuffer.wrap(str.getBytes(StandardCharsets.ISO_8859_1)).order(ByteOrder.nativeOrder());
        return bb.getInt();
    }

    public static int stopBitLength(long n) {
        if ((n & ~0x7F) == 0) {
            return 1;
        }
        if ((n & ~0x3FFF) == 0) {
            return 2;
        }
        return BytesInternal.stopBitLength0(n);
    }

    public static char[] toCharArray(Bytes bytes) {
        final char[] chars = new char[(int) bytes.readRemaining()];

        for (int i = 0; i < bytes.readRemaining(); i++) {
            chars[i] = (char) bytes.readUnsignedByte(i + bytes.readPosition());
        }
        return chars;
    }

    public static char[] toCharArray(Bytes bytes, long position, int length) {
        final char[] chars = new char[length];

        int j = 0;
        for (long i = 0; i < length; i++) {
            chars[j++] = (char) bytes.readUnsignedByte(position + i);
        }
        return chars;
    }

    public static long readStopBit(StreamingDataInput in) {
        return BytesInternal.readStopBit(in);
    }

    public static void writeStopBit(StreamingDataOutput out, long n) {
        BytesInternal.writeStopBit(out, n);
    }

    public static long utf8Length(CharSequence cs) {
        return AppendableUtil.findUtf8Length(cs);
    }

    public static void parseUtf8(
            @NotNull StreamingDataInput in, Appendable appendable, int utflen) {
        BytesInternal.parseUtf8(in, appendable, utflen);
    }

    public static void appendUtf8(@NotNull StreamingDataOutput out, @NotNull CharSequence cs) {
        BytesInternal.appendUtf8(out, cs, 0, cs.length());
    }

    public static void appendBytesFromStart(Bytes bytes, long startPosition, StringBuilder sb) {
        BytesInternal.parse8bit(startPosition, bytes, sb, (int) (bytes.readPosition() - startPosition));
        sb.append('\u2016');
        sb.append(bytes);
    }

    public static void readMarshallable(ReadBytesMarshallable marshallable, BytesIn bytes) {
        BytesMarshaller.BYTES_MARSHALLER_CL.get(marshallable.getClass())
                .readMarshallable(marshallable, bytes);
    }

    public static void writeMarshallable(WriteBytesMarshallable marshallable, BytesOut bytes) {
        BytesMarshaller.BYTES_MARSHALLER_CL.get(marshallable.getClass())
                .writeMarshallable(marshallable, bytes);
    }
}
