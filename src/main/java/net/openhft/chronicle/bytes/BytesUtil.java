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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesFieldInfo;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.internal.ReferenceCountedUtil;
import net.openhft.chronicle.core.*;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;
import static net.openhft.chronicle.core.io.IOTools.*;

@SuppressWarnings("rawtypes")
public enum BytesUtil {
    ; // none
    private static final int[] NO_INTS = {};
    private static final ClassLocal<int[]> TRIVIALLY_COPYABLE = ClassLocal.withInitial(BytesUtil::isTriviallyCopyable0);
    private static final String USER_NAME = Jvm.getProperty("user.name", "unknown");
    private static final String TIME_STAMP_DIR = Jvm.getProperty("timestamp.dir", OS.TMP);
    static final String TIME_STAMP_PATH = Jvm.getProperty("timestamp.path", new File(TIME_STAMP_DIR, ".time-stamp." + USER_NAME + ".dat").getAbsolutePath());

    /**
     * Is the whole class trivially copyable
     *
     * @param clazz to check
     * @return true if the whole class is trivially copyable
     */
    public static boolean isTriviallyCopyable(@NotNull Class<?> clazz) {
        final int[] ints = TRIVIALLY_COPYABLE.get(clazz);
        return ints[1] > 0;
    }

    static int[] isTriviallyCopyable0(@NotNull Class<?> clazz) {
        if (Jvm.isAzulZing())
            throw new UnsupportedOperationException();
        if (clazz.isArray()) {
            Class componentType = clazz.getComponentType();
            if (componentType.isPrimitive())
                return new int[]{MEMORY.arrayBaseOffset(clazz)};
            return NO_INTS;
        }
        List<Field> fields = BytesFieldInfo.fields(clazz);
        return calculateMinMax(fields);
    }

    private static int[] calculateMinMax(final List<Field> fields) {
        int min = 0;
        int max = 0;
        for (Field field : fields) {
            final FieldGroup fieldGroup = field.getAnnotation(FieldGroup.class);
            if (fieldGroup != null && FieldGroup.HEADER.equals(fieldGroup.value()))
                continue;
            int start = (int) MEMORY.objectFieldOffset(field);
            int size = sizeOf(field.getType());
            int end = start + size;
            boolean nonTrivial = !field.getType().isPrimitive();
            if (nonTrivial) {
                if (max > 0)
                    break;
                // otherwise skip them.
            } else {
                if (min == 0)
                    min = start;
                max = end;
            }
        }
        return new int[]{min, max};
    }

    /**
     * Are all the fields in the range given trivially copyable
     *
     * @param clazz  to check
     * @param offset start of field area
     * @param length of the field area
     * @return true if the fields in range are trivially copyable.
     */
    public static boolean isTriviallyCopyable(Class clazz, @NonNegative int offset, @NonNegative int length) {
        int[] ints = TRIVIALLY_COPYABLE.get(clazz);
        if (ints.length == 0)
            return false;
        return offset >= ints[0] && (ints.length == 1 || offset + length <= ints[1]);
    }

    public static int[] triviallyCopyableRange(Class clazz) {
        return TRIVIALLY_COPYABLE.get(clazz);
    }

    /**
     * Return the first byte of trivially copyable fields.
     *
     * @param clazz to examine
     * @return the first byte copyable
     */
    public static int triviallyCopyableStart(Class clazz) {
        return triviallyCopyableRange(clazz)[0];
    }

    /**
     * Return the length of trivially copyable fields, note references are ignored as they are not trivially copyable.
     *
     * @param clazz to examine
     * @return the length of data
     */
    public static int triviallyCopyableLength(Class clazz) {
        final int[] startEnd = triviallyCopyableRange(clazz);
        return startEnd[1] - startEnd[0];
    }

    private static int sizeOf(Class<?> type) {
        return Memory.sizeOf(type);
    }

    public static String findFile(@NotNull String name)
            throws FileNotFoundException {
        File file = new File(name);
        URL url = null;
        if (!file.exists()) {
            url = urlFor(Thread.currentThread().getContextClassLoader(), name);
            String file2 = url.getFile()
                    .replace("target/test-classes", "src/test/resources");
            file = new File(file2);
        }
        if (!file.exists())
            throw new FileNotFoundException(name);
        return file.getAbsolutePath();
    }

    public static Bytes<?> readFile(@NotNull String name)
            throws IOException {
        if (name.startsWith("=")) {
            return Bytes.from(name.substring(1));
        }
        File file = new File(name);
        URL url = null;
        if (!file.exists()) {
            url = urlFor(Thread.currentThread().getContextClassLoader(), name);
            file = new File(url.getFile());
        }
        return Bytes.wrapForRead(readAsBytes(url == null ? new FileInputStream(file) : open(url)));

    }

    public static void writeFile(String file, Bytes<byte[]> bytes)
            throws IOException {
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(bytes.underlyingObject());
        }
    }

    public static boolean bytesEqual(
            @NotNull RandomDataInput a, long offset,
            @NotNull RandomDataInput second, long secondOffset, long len)
            throws BufferUnderflowException, IllegalStateException {
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

    public static boolean bytesEqual(@Nullable CharSequence cs, @NotNull RandomDataInput bs, @NonNegative long offset, @NonNegative int length)
            throws IllegalStateException, BufferUnderflowException {
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
    public static char[] toCharArray(@NotNull Bytes<?> bytes)
            throws ArithmeticException, IllegalStateException, BufferUnderflowException {
        @NotNull final char[] chars = new char[Maths.toUInt31(bytes.readRemaining())];

        for (int i = 0; i < bytes.readRemaining(); i++) {
            chars[i] = (char) bytes.readUnsignedByte(i + bytes.readPosition());
        }
        return chars;
    }

    @NotNull
    public static char[] toCharArray(@NotNull Bytes<?> bytes, @NonNegative long position, @NonNegative int length)
            throws IllegalStateException, BufferUnderflowException {
        @NotNull final char[] chars = new char[length];

        int j = 0;
        for (int i = 0; i < length; i++) {
            chars[j++] = (char) bytes.readUnsignedByte(position + i);
        }
        return chars;
    }

    public static long readStopBit(@NotNull StreamingDataInput in)
            throws IORuntimeException, IllegalStateException {
        return BytesInternal.readStopBit(in);
    }

    public static void writeStopBit(@NotNull StreamingDataOutput out, long n)
            throws IllegalStateException, BufferOverflowException {
        BytesInternal.writeStopBit(out, n);
    }

    /**
     * @return the resulting offset
     */
    public static long writeStopBit(BytesStore bs, @NonNegative long offset, @NonNegative long n)
            throws IllegalStateException, BufferOverflowException {
        return BytesInternal.writeStopBit(bs, offset, n);
    }

    /**
     * @return the resulting address
     */
    public static long writeStopBit(long addr, long n)
            throws IllegalStateException, BufferOverflowException {
        return BytesInternal.writeStopBit(addr, n);
    }

    public static void parseUtf8(
            @NotNull StreamingDataInput in, Appendable appendable, @NonNegative int utflen)
            throws UTFDataFormatRuntimeException, IllegalStateException, BufferUnderflowException {
        BytesInternal.parseUtf8(in, appendable, true, utflen);
    }

    public static void appendUtf8(@NotNull StreamingDataOutput out, @NotNull CharSequence cs)
            throws IndexOutOfBoundsException {
        BytesInternal.appendUtf8(out, cs, 0, cs.length());
    }

    // used by Chronicle FIX.
    public static void appendBytesFromStart(@NotNull Bytes<?> bytes, @NonNegative long startPosition, @NotNull StringBuilder sb)
            throws IllegalStateException {
        try {
            BytesInternal.parse8bit(startPosition, bytes, sb, (int) (bytes.readPosition() - startPosition));
            sb.append('\u2016');
            sb.append(bytes);
        } catch (IOException | BufferUnderflowException e) {
            throw new IORuntimeException(e);
        }
    }

    public static void readMarshallable(@NotNull ReadBytesMarshallable marshallable, BytesIn<?> bytes) {
        BytesMarshaller.BYTES_MARSHALLER_CL.get(marshallable.getClass())
                .readMarshallable(marshallable, bytes);
    }

    public static void writeMarshallable(@NotNull WriteBytesMarshallable marshallable, BytesOut<?> bytes)
            throws IllegalStateException, BufferOverflowException, ArithmeticException, BufferUnderflowException {
        try {
            BytesMarshaller.BYTES_MARSHALLER_CL.get(marshallable.getClass())
                    .writeMarshallable(marshallable, bytes);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    static String asString(String s, Throwable t) {
        StringWriter sw = new StringWriter();
        sw.append(s).append("\n");
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
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

    public static void read8ByteAlignPadding(Bytes<?> bytes)
            throws IllegalStateException, BufferUnderflowException {
        bytes.readPosition(roundUpTo8ByteAlign(bytes.readPosition()));
    }

    public static void write8ByteAlignPadding(Bytes<?> bytes)
            throws BufferOverflowException, IllegalStateException {
        long start = bytes.writePosition();
        long end = roundUpTo8ByteAlign(start);
        bytes.writePosition(end);
        bytes.zeroOut(start, end);
    }

    public static String toDebugString(@NotNull RandomDataInput bytes, @NonNegative long start, @NonNegative long maxLength)
            throws IllegalStateException, BufferUnderflowException, ArithmeticException {
        BytesStore bytes2 = bytes.subBytes(start, maxLength);
        return bytes2.toDebugString(maxLength);
    }

    public static void copy8bit(BytesStore bs, long addressForWrite, @NonNegative long length) {
        BytesInternal.copy8bit(bs, addressForWrite, length);
    }

    public static void reverse(Bytes<?> text, @NonNegative int start) {
        long rp = text.readPosition();
        int end = text.length() - 1;
        int mid = (start + end + 1) / 2;

        for (int i = 0; i < mid - start; ++i) {
            char ch = text.charAt(start + i);
            text.writeUnsignedByte(rp + start + i, text.charAt(end - i));
            text.writeUnsignedByte(rp + end - i, ch);
        }

    }

    // Based on Maths.roundNup
    public static long roundNup(double d, long factor) {
        boolean neg = d < 0;
        d = Math.abs(d);
        final double df = d * factor;
        long ldf = (long) df;
        final double residual = df - ldf + Math.ulp(d) * (factor * 0.983);
        if (residual >= 0.5)
            ldf++;
        if (neg)
            ldf = -ldf;
        return ldf;
    }

    public static long padOffset(long from) {
        return (-from) & 0x3L;
    }

    /**
     * If the last two characters were a newline, rewind one character so there is only one newline.
     *
     * @param bytes to check and trim as needed.
     */
    public static void combineDoubleNewline(Bytes<?> bytes) {
        long wp = bytes.writePosition();
        long delta = wp - bytes.start();
        final int ch1 = delta >= 1 ? bytes.peekUnsignedByte(wp - 1) : '\0';
        switch (ch1) {
            case '\n': {
                final int ch2 = delta >= 2 ? bytes.peekUnsignedByte(wp - 2) : '\0';
                switch (ch2) {
                    case '\n':
                        bytes.writePosition(wp - 1);
                        return;

                    case ' ':
                        bytes.writePosition(wp - 2);
                        bytes.append('\n');
                        return;

                    default:
                        return;
                }
            }
            case ' ': {
                final int ch2 = delta >= 2 ? bytes.peekUnsignedByte(wp - 2) : '\0';
                switch (ch2) {
                    case ' ':
                        final int ch3 = delta >= 3 ? bytes.peekUnsignedByte(wp - 3) : '\0';
                        if (ch3 > ' ') {
                            bytes.writePosition(wp - 1);
                        }
                        return;

                    default:
                        return;
                }
            }
        }
    }

    static boolean isControlSpace(int ch) {
        return 0 <= ch && ch <= ' ';
    }

    public static BytesStore<Bytes<Void>, Void> copyOf(@NotNull final Bytes<?> bytes)
            throws IllegalStateException {
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        final long remaining = bytes.readRemaining();
        if (remaining == 0)
            return BytesStore.empty();
        final long position = bytes.readPosition();

        try {
            final Bytes<Void> bytes2 = Bytes.allocateDirect(remaining);
            bytes2.write(bytes, position, remaining);
            return bytes2;
        } catch (IllegalArgumentException | BufferOverflowException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    static final class WarnUncheckedElasticBytes {
        static {
            Jvm.debug().on(WarnUncheckedElasticBytes.class, "Wrapping elastic bytes with unchecked() will require calling ensureCapacity() as needed!");
        }

        private WarnUncheckedElasticBytes() {
        }

        static void warn() {
            // static block does the work.
        }
    }
}
