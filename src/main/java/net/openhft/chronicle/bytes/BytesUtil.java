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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.StackTrace;
import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static net.openhft.chronicle.core.io.IOTools.*;

/*
 * Created by Peter Lawrey on 30/08/15..
 */
public enum BytesUtil {
    ;

    static final Map<AbstractBytes, Throwable> bytesCreated = Collections.synchronizedMap(new IdentityHashMap<>());

    public static Bytes readFile(@org.jetbrains.annotations.NotNull String name) throws IOException {
        File file = new File(name);
        URL url = null;
        if (!file.exists()) {
            url = urlFor(name);
            file = new File(url.getFile());
        }
        return // name.endsWith(".gz") || !file.exists() || OS.isWindows() ?
                Bytes.wrapForRead(readAsBytes(url == null ? new FileInputStream(file) : open(url)));
        //: MappedFile.readOnly(file).acquireBytesForRead(0);

    }

    public static boolean bytesEqual(
            @org.jetbrains.annotations.NotNull @NotNull RandomDataInput a, long offset,
            @org.jetbrains.annotations.NotNull @NotNull RandomDataInput second, long secondOffset, long len)
            throws BufferUnderflowException {
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

    public static boolean bytesEqual(@Nullable CharSequence cs, @org.jetbrains.annotations.NotNull RandomDataInput bs, long offset, int length) {
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

    public static int asInt(@org.jetbrains.annotations.NotNull @NotNull String str) {
        @org.jetbrains.annotations.NotNull ByteBuffer bb = ByteBuffer.wrap(str.getBytes(StandardCharsets.ISO_8859_1)).order(ByteOrder.nativeOrder());
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

    @org.jetbrains.annotations.NotNull
    public static char[] toCharArray(@org.jetbrains.annotations.NotNull Bytes bytes) {
        @org.jetbrains.annotations.NotNull final char[] chars = new char[Maths.toUInt31(bytes.readRemaining())];

        for (int i = 0; i < bytes.readRemaining(); i++) {
            chars[i] = (char) bytes.readUnsignedByte(i + bytes.readPosition());
        }
        return chars;
    }

    @org.jetbrains.annotations.NotNull
    public static char[] toCharArray(@org.jetbrains.annotations.NotNull Bytes bytes, long position, int length) {
        @org.jetbrains.annotations.NotNull final char[] chars = new char[length];

        int j = 0;
        for (int i = 0; i < length; i++) {
            chars[j++] = (char) bytes.readUnsignedByte(position + i);
        }
        return chars;
    }

    public static long readStopBit(@org.jetbrains.annotations.NotNull StreamingDataInput in) throws IORuntimeException {
        return BytesInternal.readStopBit(in);
    }

    public static void writeStopBit(@org.jetbrains.annotations.NotNull StreamingDataOutput out, long n) {
        BytesInternal.writeStopBit(out, n);
    }

    public static void parseUtf8(
            @org.jetbrains.annotations.NotNull @NotNull StreamingDataInput in, Appendable appendable, int utflen)
            throws UTFDataFormatRuntimeException {
        BytesInternal.parseUtf8(in, appendable, utflen);
    }

    public static void appendUtf8(@org.jetbrains.annotations.NotNull @NotNull StreamingDataOutput out, @org.jetbrains.annotations.NotNull @NotNull CharSequence cs) {
        BytesInternal.appendUtf8(out, cs, 0, cs.length());
    }

    // used by Chronicle FIX.
    public static void appendBytesFromStart(@org.jetbrains.annotations.NotNull Bytes bytes, long startPosition, @org.jetbrains.annotations.NotNull StringBuilder sb) {
        try {
            BytesInternal.parse8bit(startPosition, bytes, sb, (int) (bytes.readPosition() - startPosition));
            sb.append('\u2016');
            sb.append(bytes);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static void readMarshallable(@org.jetbrains.annotations.NotNull ReadBytesMarshallable marshallable, BytesIn bytes) {
        BytesMarshaller.BYTES_MARSHALLER_CL.get(marshallable.getClass())
                .readMarshallable(marshallable, bytes);
    }

    public static void writeMarshallable(@org.jetbrains.annotations.NotNull WriteBytesMarshallable marshallable, BytesOut bytes) {
        BytesMarshaller.BYTES_MARSHALLER_CL.get(marshallable.getClass())
                .writeMarshallable(marshallable, bytes);
    }

    @Deprecated
    public static long utf8Length(@org.jetbrains.annotations.NotNull CharSequence toWrite) {
        return AppendableUtil.findUtf8Length(toWrite);
    }

    public static boolean register(AbstractBytes bytes) {
        bytesCreated.put(bytes, new StackTrace("Created here"));
        return true;
    }

    public static void checkRegisteredBytes() {
        int count = 0;
        for (Map.Entry<AbstractBytes, Throwable> entry : bytesCreated.entrySet()) {
            AbstractBytes key = entry.getKey();
            if (key.refCount() != 0) {
                System.err.println("Bytes " + key.getClass() + " refCount=" + key.refCount());
                entry.getValue().printStackTrace();
                count++;
            }
        }
        bytesCreated.clear();
        if (count != 0)
            throw new IllegalStateException("Bytes not released properly " + count);
    }

    public static boolean unregister(Bytes bytes) {
        bytesCreated.remove(bytes);
        return true;
    }

    public static boolean byteToBoolean(byte b) {
        return b != 0 && b != 'N' && b != 'n';
    }
}
