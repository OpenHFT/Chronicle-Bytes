/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.openhft.chronicle.core.io.IOTools.*;

@SuppressWarnings("rawtypes")
public enum BytesUtil {
    ;
    private static final int[] NO_INTS = {};
    private static final ClassLocal<int[]> TRIVIALLY_COPYABLE = ClassLocal.withInitial(BytesUtil::isTriviallyCopyable0);

    /**
     * Is the whole class trivially copyable
     *
     * @param clazz to check
     * @return true if the whole class is trivially copyable
     */
    public static boolean isTriviallyCopyable(Class clazz) {
        return TRIVIALLY_COPYABLE.get(clazz).length > 0;
    }

    static int[] isTriviallyCopyable0(Class clazz) {
        if (clazz.isArray()) {
            Class componentType = clazz.getComponentType();
            if (componentType.isPrimitive())
                return new int[]{UnsafeMemory.UNSAFE.arrayBaseOffset(clazz)};
            return NO_INTS;
        }
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            Collections.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers))
                continue;
            long offset2 = UnsafeMemory.UNSAFE.objectFieldOffset(field);
            int size = sizeOf(field.getType());
            min = (int) Math.min(min, offset2);
            max = (int) Math.max(max, offset2 + size);
            if (Modifier.isTransient(modifiers))
                return NO_INTS;
            if (!field.getType().isPrimitive())
                return NO_INTS;
        }
        return new int[]{min, max};
    }

    /**
     * Are all the fields in the range given trivially copyable
     *
     * @param clazz to check
     * @return true if the fields in range are trivially copyable.
     */
    public static boolean isTriviallyCopyable(Class clazz, int offset, int length) {
        int[] ints = TRIVIALLY_COPYABLE.get(clazz);
        if (ints.length == 0)
            return false;
        return offset >= ints[0] && (ints.length == 1 || offset + length <= ints[1]);
    }

    public static int[] triviallyCopyableRange(Class clazz) {
        return TRIVIALLY_COPYABLE.get(clazz);
    }

    private static int sizeOf(Class<?> type) {
        return type == boolean.class || type == byte.class ? 1
                : type == short.class || type == char.class ? 2
                : type == int.class || type == float.class ? 4
                : type == long.class || type == double.class ? 8
                : UnsafeMemory.UNSAFE.ARRAY_OBJECT_INDEX_SCALE;
    }

    public static String findFile(@NotNull String name) throws FileNotFoundException {
        File file = new File(name);
        URL url = null;
        if (!file.exists()) {
            url = urlFor(name);
            String file2 = url.getFile()
                    .replace("target/test-classes", "src/test/resources");
            file = new File(file2);
        }
        if (!file.exists())
            throw new FileNotFoundException(name);
        return file.getAbsolutePath();
    }

    public static Bytes readFile(@NotNull String name) throws IOException {
        if (name.startsWith("=")) {
            return Bytes.from(name.substring(1));
        }
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

    public static void writeFile(String file, Bytes<byte[]> bytes) throws IOException {
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(bytes.underlyingObject());
        }
    }

    public static boolean bytesEqual(
            @NotNull RandomDataInput a, long offset,
            @NotNull RandomDataInput second, long secondOffset, long len)
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
            return a.readByte(offset + i) == second.readByte(secondOffset + i);
        return true;
    }

    public static boolean bytesEqual(@Nullable CharSequence cs, @NotNull RandomDataInput bs, long offset, int length) {
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
        @NotNull ByteBuffer bb = ByteBuffer.wrap(str.getBytes(StandardCharsets.ISO_8859_1)).order(ByteOrder.nativeOrder());
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

    @NotNull
    public static char[] toCharArray(@NotNull Bytes bytes) {
        @NotNull final char[] chars = new char[Maths.toUInt31(bytes.readRemaining())];

        for (int i = 0; i < bytes.readRemaining(); i++) {
            chars[i] = (char) bytes.readUnsignedByte(i + bytes.readPosition());
        }
        return chars;
    }

    @NotNull
    public static char[] toCharArray(@NotNull Bytes bytes, long position, int length) {
        @NotNull final char[] chars = new char[length];

        int j = 0;
        for (int i = 0; i < length; i++) {
            chars[j++] = (char) bytes.readUnsignedByte(position + i);
        }
        return chars;
    }

    public static long readStopBit(@NotNull StreamingDataInput in) throws IORuntimeException {
        return BytesInternal.readStopBit(in);
    }

    public static void writeStopBit(@NotNull StreamingDataOutput out, long n) {
        BytesInternal.writeStopBit(out, n);
    }

    public static void parseUtf8(
            @NotNull StreamingDataInput in, Appendable appendable, int utflen)
            throws UTFDataFormatRuntimeException {
        BytesInternal.parseUtf8(in, appendable, true, utflen);
    }

    public static void appendUtf8(@NotNull StreamingDataOutput out, @NotNull CharSequence cs) {
        BytesInternal.appendUtf8(out, cs, 0, cs.length());
    }

    // used by Chronicle FIX.
    public static void appendBytesFromStart(@NotNull Bytes bytes, long startPosition, @NotNull StringBuilder sb) {
        try {
            BytesInternal.parse8bit(startPosition, bytes, sb, (int) (bytes.readPosition() - startPosition));
            sb.append('\u2016');
            sb.append(bytes);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public static void readMarshallable(@NotNull ReadBytesMarshallable marshallable, BytesIn bytes) {
        BytesMarshaller.BYTES_MARSHALLER_CL.get(marshallable.getClass())
                .readMarshallable(marshallable, bytes);
    }

    public static void writeMarshallable(@NotNull WriteBytesMarshallable marshallable, BytesOut bytes) {
        BytesMarshaller.BYTES_MARSHALLER_CL.get(marshallable.getClass())
                .writeMarshallable(marshallable, bytes);
    }

    @Deprecated
    public static long utf8Length(@NotNull CharSequence toWrite) {
        return AppendableUtil.findUtf8Length(toWrite);
    }

    static String asString(String s, Throwable t) {
        StringWriter sw = new StringWriter();
        sw.append(s).append("\n");
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // calls the BackgroundResourceReleaser and AbstractCloseable.assertCloseableClose first.
    public static void checkRegisteredBytes() {
        AbstractReferenceCounted.assertReferencesReleased();
    }

    public static boolean byteToBoolean(byte b) {
        return b != 0 && b != 'N' && b != 'n';
    }

    public static long roundUpTo64ByteAlign(long x) {
        return (x + 63L) & ~63L;
    }

    public static long roundUpTo8ByteAlign(long x) {
        return (x + 7L) & ~7L;
    }

    public static void read8ByteAlignPadding(Bytes<?> bytes) {
        bytes.readPosition(roundUpTo8ByteAlign(bytes.readPosition()));
    }

    public static void write8ByteAlignPadding(Bytes<?> bytes) {
        long start = bytes.writePosition();
        long end = roundUpTo8ByteAlign(start);
        bytes.writePosition(end);
        bytes.zeroOut(start, end);
    }

    public static String toDebugString(@NotNull RandomDataInput bytes, long start, long maxLength) {
        BytesStore bytes2 = bytes.subBytes(start, maxLength);
        return bytes2.toDebugString(maxLength);
    }

    @Deprecated
    public static boolean unregister(BytesStore bs) {
        IOTools.unmonitor(bs);
        return true;
    }

    static class WarnUncheckedElasticBytes {
        static {
            Jvm.debug().on(WarnUncheckedElasticBytes.class, "Wrapping elastic bytes with unchecked() will require calling ensureCapacity() as needed!");
        }

        static void warn() {
            // static block does the work.
        }
    }
}
