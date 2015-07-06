/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public enum BytesUtil {
    ;
    static final char[] HEXI_DECIMAL = "0123456789ABCDEF".toCharArray();
    private static final byte[] MIN_VALUE_TEXT = ("" + Long.MIN_VALUE).getBytes();
    private static final StringBuilderPool SBP = new StringBuilderPool();
    private static final StringInterner SI = new StringInterner(1024);
    private static final byte[] Infinity = "Infinity".getBytes();
    private static final byte[] NaN = "NaN".getBytes();
    private static final long MAX_VALUE_DIVIDE_5 = Long.MAX_VALUE / 5;
    private static final ThreadLocal<byte[]> NUMBER_BUFFER = ThreadLocal.withInitial(() -> new byte[20]);
    private static final long MAX_VALUE_DIVIDE_10 = Long.MAX_VALUE / 10;
    private static final Constructor<String> STRING_CONSTRUCTOR;

    static {
        try {
            STRING_CONSTRUCTOR = String.class.getDeclaredConstructor(char[].class, boolean.class);
            STRING_CONSTRUCTOR.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public static boolean contentEqual(BytesStore a, BytesStore b) {
        if (a == null) return b == null;
        if (b == null) return false;
        if (a.start() != b.start() || a.readRemaining() != b.readRemaining())
            return false;
        long aPos = a.readPosition();
        long bPos = b.readPosition();
        long length = a.readRemaining();
        long i;
        for (i = 0; i < length - 7; i += 8) {
            if (a.readLong(aPos + i) != b.readLong(bPos + i))
                return false;
        }
        for (i = 0; i < length; i++) {
            if (a.readByte(aPos + i) != b.readByte(bPos + i))
                return false;
        }
        return true;
    }

    public static boolean bytesEqual(
            RandomDataInput a, long aOffset, RandomDataInput b, long bOffset, long len) {
        return a.bytesEqual(aOffset, b, bOffset, len);
    }

    public static void parseUTF(StreamingDataInput bytes, Appendable appendable, int utflen) throws UTFDataFormatRuntimeException {
        try {
            int count = 0;
            assert bytes.readRemaining() >= utflen;
            while (count < utflen) {
                int c = bytes.readUnsignedByte();
                if (c >= 128) {
                    bytes.readSkip(-1);
                    break;

                } else if (c < 0) {
                    break;
                }
                count++;
                appendable.append((char) c);
            }

            if (utflen > count)
                parseUTF2(bytes, appendable, utflen, count);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    static void parseUTF2(StreamingDataInput bytes, Appendable appendable, int utflen, int count) throws IOException {
        while (count < utflen) {
            int c = bytes.readUnsignedByte();
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                /* 0xxxxxxx */
                    count++;
                    appendable.append((char) c);
                    break;

                case 12:
                case 13: {
                /* 110x xxxx 10xx xxxx */
                    count += 2;
                    if (count > utflen)
                        throw new UTFDataFormatRuntimeException(
                                "malformed input: partial character at end");
                    int char2 = bytes.readUnsignedByte();
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatRuntimeException(
                                "malformed input around byte " + count + " was " + char2);
                    int c2 = (char) (((c & 0x1F) << 6) |
                            (char2 & 0x3F));
                    appendable.append((char) c2);
                    break;
                }

                case 14: {
                /* 1110 xxxx 10xx xxxx 10xx xxxx */
                    count += 3;
                    if (count > utflen)
                        throw new UTFDataFormatRuntimeException(
                                "malformed input: partial character at end");
                    int char2 = bytes.readUnsignedByte();
                    int char3 = bytes.readUnsignedByte();

                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatRuntimeException(
                                "malformed input around byte " + (count - 1) + " was " + char2 + " " + char3);
                    int c3 = (char) (((c & 0x0F) << 12) |
                            ((char2 & 0x3F) << 6) |
                            (char3 & 0x3F));
                    appendable.append((char) c3);
                    break;
                }
                // TODO add code point of characters > 0xFFFF support.
                default:
                /* 10xx xxxx, 1111 xxxx */
                    throw new UTFDataFormatRuntimeException(
                            "malformed input around byte " + count);
            }
        }
    }

    @ForceInline
    public static void writeUTF(StreamingDataOutput bytes, CharSequence str) {
        if (str == null) {
            bytes.writeStopBit(-1);

        } else {
            bytes.writeStopBit(findUTFLength(str));
            appendUTF(bytes, str, 0, str.length());
        }
    }

    private static long findUTFLength(@NotNull CharSequence str) {
        long utflen = 0;/* use charAt instead of copying String to char array */
        for (int i = 0, strlen = str.length(); i < strlen; i++) {
            char c = str.charAt(i);
            if (c <= 0x007F) {
                utflen++;

            } else if (c <= 0x07FF) {
                utflen += 2;

            } else {
                utflen += 3;
            }
        }
        return utflen;
    }

    @NotNull
    public static Bytes asBytes(RandomDataOutput bytes, long position, long limit) {
        Bytes sbytes = bytes.bytesForWrite();
        sbytes.writeLimit(limit);
        sbytes.readLimit(limit);
        sbytes.readPosition(position);
        return sbytes;
    }

    public static void appendUTF(StreamingDataOutput bytes, @NotNull CharSequence str, int offset, int length) {
        if (bytes instanceof VanillaBytes) {
            if (str instanceof VanillaBytes) {
                ((VanillaBytes) bytes).write((VanillaBytes) str, offset, length);
                return;
            }
        }
        int i;
        for (i = 0; i < length; i++) {
            char c = str.charAt(offset + i);
            if (c > 0x007F)
                break;
            bytes.writeByte((byte) c);
        }

        for (; i < length; i++) {
            char c = str.charAt(offset + i);
            appendUTF(bytes, c);
        }
    }

    public static void append8bit(StreamingDataOutput bytes, @NotNull CharSequence str, int offset, int length) {
        if (bytes instanceof VanillaBytes) {
            if (str instanceof VanillaBytes) {
                ((VanillaBytes) bytes).write((VanillaBytes) str, offset, length);
                return;
            }
            if (str instanceof String) {
                ((NativeBytes) bytes).write((String) str, offset, length);
                return;
            }
        }
        for (int i = 0; i < length; i++) {
            char c = str.charAt(offset + i);
            if (c > 255) c = '?';
            bytes.writeUnsignedByte(c);
        }
    }

    public static void appendUTF(StreamingDataOutput bytes, int c) {
        if (c <= 0x007F) {
            bytes.writeByte((byte) c);

        } else if (c <= 0x07FF) {
            bytes.writeByte((byte) (0xC0 | ((c >> 6) & 0x1F)));
            bytes.writeByte((byte) (0x80 | c & 0x3F));

        } else if (c <= 0xFFFF) {
            bytes.writeByte((byte) (0xE0 | ((c >> 12) & 0x0F)));
            bytes.writeByte((byte) (0x80 | ((c >> 6) & 0x3F)));
            bytes.writeByte((byte) (0x80 | (c & 0x3F)));

        } else {
            bytes.writeByte((byte) (0xF0 | ((c >> 18) & 0x07)));
            bytes.writeByte((byte) (0x80 | ((c >> 12) & 0x3F)));
            bytes.writeByte((byte) (0x80 | ((c >> 6) & 0x3F)));
            bytes.writeByte((byte) (0x80 | (c & 0x3F)));
        }
    }

    public static void writeStopBit(StreamingDataOutput out, long n) {
        if ((n & ~0x7F) == 0) {
            out.writeByte((byte) (n & 0x7f));
            return;
        }
        if ((n & ~0x3FFF) == 0) {
            out.writeByte((byte) ((n & 0x7f) | 0x80));
            out.writeByte((byte) (n >> 7));
            return;
        }
        writeStopBit0(out, n);
    }

    public static int stopBitLength(long n) {
        if ((n & ~0x7F) == 0) {
            return 1;
        }
        if ((n & ~0x3FFF) == 0) {
            return 2;
        }
        return stopBitlength0(n);
    }

    static void writeStopBit0(StreamingDataOutput out, long n) {
        boolean neg = false;
        if (n < 0) {
            neg = true;
            n = ~n;
        }

        long n2;
        while ((n2 = n >>> 7) != 0) {
            out.writeByte((byte) (0x80L | n));
            n = n2;
        }
        // final byte
        if (!neg) {
            out.writeByte((byte) n);

        } else {
            out.writeByte((byte) (0x80L | n));
            out.writeByte((byte) 0);
        }
    }

    static int stopBitlength0(long n) {
        int len = 0;
        if (n < 0) {
            len = 1;
            n = ~n;
        }

        while ((n >>>= 7) != 0) len++;
        return len + 1;
    }

    public static String toDebugString(RandomDataInput bytes, long maxLength) {
        StringBuilder sb = new StringBuilder(200);
        long position = bytes.readPosition();
        sb.append("[")
                .append("pos: ").append(position)
                .append(", rlim: ").append(bytes.readLimit())
                .append(", wlim: ").append(asSize(bytes.writeLimit()))
                .append(", cap: ").append(asSize(bytes.capacity()))
                .append(" ] ");
        toString(bytes, sb, position - maxLength, position, position + maxLength);

        return sb.toString();
    }

    public static Object asSize(long size) {
        return size == Bytes.MAX_CAPACITY ? "8EiB" : size;
    }

    public static String to8bitString(BytesStore bytes) {
        long pos = bytes.readPosition();
        int len = Maths.toInt32(bytes.readRemaining());
        char[] chars = new char[len];
        if (bytes instanceof VanillaBytes) {
            ((VanillaBytes) bytes).read8Bit(chars, len);
        } else {
            for (int i = 0; i < len; i++)
                chars[i] = (char) bytes.readUnsignedByte(pos + i);
        }
        return newString(chars);
    }

    private static String newString(char[] chars) {
        try {
            return STRING_CONSTRUCTOR.newInstance(chars, true);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static String toString(RandomDataInput bytes) {
        StringBuilder sb = new StringBuilder(200);
        toString(bytes, sb);
        return sb.toString();
    }

    private static void toString(RandomDataInput bytes, Appendable sb, long start, long position, long end) {
        try {
            // before
            if (start < 0) start = 0;
            if (position > start) {
                long last = Math.min(position, bytes.readLimit());
                for (long i = start; i < last; i++) {
                    sb.append(bytes.printable(i));
                }
                sb.append('\u2016');
                if (position >= bytes.readLimit()) {
                    return;
                }
            }
            if (end > bytes.readLimit())
                end = bytes.readLimit();
            // after
            for (long i = position; i < end; i++) {
                sb.append(bytes.printable(i));
            }
        } catch (IOException e) {
            try {
                sb.append(e.toString());
            } catch (IOException e1) {
                throw new AssertionError(e);
            }
        }
    }

    private static void toString(RandomDataInput bytes, StringBuilder sb) {
        for (long i = bytes.readPosition(); i < bytes.readLimit(); i++) {
            sb.append((char) bytes.readUnsignedByte(i));
        }
    }

    @ForceInline
    public static long readStopBit(StreamingDataInput in) {
        long l;
        if ((l = in.readByte()) >= 0)
            return l;
        return readStopBit0(in, l);
    }

    static long readStopBit0(StreamingDataInput in, long l) {
        l &= 0x7FL;
        long b;
        int count = 7;
        while ((b = in.readByte()) < 0) {
            l |= (b & 0x7FL) << count;
            count += 7;
        }
        if (b != 0) {
            if (count > 56)
                throw new IllegalStateException(
                        "Cannot read more than 9 stop bits of positive value");
            return l | (b << count);

        } else {
            if (count > 63)
                throw new IllegalStateException(
                        "Cannot read more than 10 stop bits of negative value");
            return ~l;
        }
    }

    public static <S extends ByteStringAppender> void append(S out, long num) {
        if (num < 0) {
            if (num == Long.MIN_VALUE) {
                out.write(MIN_VALUE_TEXT);
                return;
            }
            out.writeByte((byte) '-');
            num = -num;
        }
        if (num == 0) {
            out.writeByte((byte) '0');

        } else {
            appendLong0(out, num);
        }
    }

    /**
     * The length of the number must be fixed otherwise short numbers will not overwrite longer numbers
     */
    public static void append(RandomDataOutput out, long offset, long num, int digits) {
        boolean negative = num < 0;
        num = Math.abs(num);

        for (int i = digits - 1; i > 0; i--) {
            out.writeByte(offset + i, (byte) (num % 10 + '0'));
            num /= 10;
        }
        if (negative) {
            if (num != 0)
                numberTooLarge(digits);
            out.writeByte(offset, '-');

        } else {
            if (num > 9)
                numberTooLarge(digits);
            out.writeByte(offset, (byte) (num % 10 + '0'));
        }
    }

    private static void numberTooLarge(int digits) {
        throw new IllegalArgumentException("Number too large for " + digits + "digits");
    }

    private static void appendLong0(StreamingDataOutput out, long num) {
        byte[] numberBuffer = NUMBER_BUFFER.get();
        // Extract digits into the end of the numberBuffer
        int endIndex = appendLong1(numberBuffer, num);

        // Bulk copy the digits into the front of the buffer
        out.write(numberBuffer, endIndex, numberBuffer.length - endIndex);
    }

    private static int appendLong1(byte[] numberBuffer, long num) {
        numberBuffer[19] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 19;
        numberBuffer[18] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 18;
        numberBuffer[17] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 17;
        numberBuffer[16] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 16;
        numberBuffer[15] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 15;
        numberBuffer[14] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 14;
        numberBuffer[13] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 13;
        numberBuffer[12] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 12;
        numberBuffer[11] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 11;
        numberBuffer[10] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 10;
        numberBuffer[9] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 9;
        numberBuffer[8] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 8;
        numberBuffer[7] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 7;
        numberBuffer[6] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 6;
        numberBuffer[5] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 5;
        numberBuffer[4] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 4;
        numberBuffer[3] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 3;
        numberBuffer[2] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0)
            return 2;
        numberBuffer[1] = (byte) (num % 10L + '0');
        return 1;
    }

    public static void append(StreamingDataOutput out, double d) {
        long val = Double.doubleToRawLongBits(d);
        int sign = (int) (val >>> 63);
        int exp = (int) ((val >>> 52) & 2047);
        long mantissa = val & ((1L << 52) - 1);
        if (sign != 0) {
            out.writeByte((byte) '-');
        }
        if (exp == 0 && mantissa == 0) {
            out.writeByte((byte) '0');
            return;

        } else if (exp == 2047) {
            if (mantissa == 0) {
                out.write(Infinity);

            } else {
                out.write(NaN);
            }
            return;

        } else if (exp > 0) {
            mantissa += 1L << 52;
        }
        final int shift = (1023 + 52) - exp;
        if (shift > 0) {
            // integer and faction
            if (shift < 53) {
                long intValue = mantissa >> shift;
                appendLong0(out, intValue);
                mantissa -= intValue << shift;
                if (mantissa > 0) {
                    out.writeByte((byte) '.');
                    mantissa <<= 1;
                    mantissa++;
                    int precision = shift + 1;
                    long error = 1;

                    long value = intValue;
                    int decimalPlaces = 0;
                    while (mantissa > error) {
                        // times 5*2 = 10
                        mantissa *= 5;
                        error *= 5;
                        precision--;
                        long num = (mantissa >> precision);
                        value = value * 10 + num;
                        out.writeByte((byte) ('0' + num));
                        mantissa -= num << precision;

                        final double parsedValue = asDouble(value, 0, sign != 0, ++decimalPlaces);
                        if (parsedValue == d)
                            break;
                    }
                }
                return;

            } else {
                // faction.
                out.writeByte((byte) '0');
                out.writeByte((byte) '.');
                mantissa <<= 6;
                mantissa += (1 << 5);
                int precision = shift + 6;

                long error = (1 << 5);

                long value = 0;
                int decimalPlaces = 0;
                while (mantissa > error) {
                    while (mantissa > MAX_VALUE_DIVIDE_5) {
                        mantissa >>>= 1;
                        error = (error + 1) >>> 1;
                        precision--;
                    }
                    // times 5*2 = 10
                    mantissa *= 5;
                    error *= 5;
                    precision--;
                    if (precision >= 64) {
                        decimalPlaces++;
                        out.writeByte((byte) '0');
                        continue;
                    }
                    long num = (mantissa >>> precision);
                    value = value * 10 + num;
                    final char c = (char) ('0' + num);
                    assert !(c < '0' || c > '9');
                    out.writeByte((byte) c);
                    mantissa -= num << precision;
                    final double parsedValue = asDouble(value, 0, sign != 0, ++decimalPlaces);
                    if (parsedValue == d)
                        break;
                }
                return;
            }
        }
        // large number
        mantissa <<= 10;
        int precision = -10 - shift;
        int digits = 0;
        while ((precision > 53 || mantissa > Long.MAX_VALUE >> precision) && precision > 0) {
            digits++;
            precision--;
            long mod = mantissa % 5;
            mantissa /= 5;
            int modDiv = 1;
            while (mantissa < MAX_VALUE_DIVIDE_5 && precision > 1) {
                precision -= 1;
                mantissa <<= 1;
                modDiv <<= 1;
            }
            mantissa += modDiv * mod / 5;
        }
        long val2 = precision > 0 ? mantissa << precision : mantissa >>> -precision;

        appendLong0(out, val2);
        for (int i = 0; i < digits; i++)
            out.writeByte((byte) '0');
    }

    private static double asDouble(long value, int exp, boolean negative, int decimalPlaces) {
        if (decimalPlaces > 0 && value < Long.MAX_VALUE / 2) {
            if (value < Long.MAX_VALUE / (1L << 32)) {
                exp -= 32;
                value <<= 32;
            }
            if (value < Long.MAX_VALUE / (1L << 16)) {
                exp -= 16;
                value <<= 16;
            }
            if (value < Long.MAX_VALUE / (1L << 8)) {
                exp -= 8;
                value <<= 8;
            }
            if (value < Long.MAX_VALUE / (1L << 4)) {
                exp -= 4;
                value <<= 4;
            }
            if (value < Long.MAX_VALUE / (1L << 2)) {
                exp -= 2;
                value <<= 2;
            }
            if (value < Long.MAX_VALUE / (1L << 1)) {
                exp -= 1;
                value <<= 1;
            }
        }
        for (; decimalPlaces > 0; decimalPlaces--) {
            exp--;
            long mod = value % 5;
            value /= 5;
            int modDiv = 1;
            if (value < Long.MAX_VALUE / (1L << 4)) {
                exp -= 4;
                value <<= 4;
                modDiv <<= 4;
            }
            if (value < Long.MAX_VALUE / (1L << 2)) {
                exp -= 2;
                value <<= 2;
                modDiv <<= 2;
            }
            if (value < Long.MAX_VALUE / (1L << 1)) {
                exp -= 1;
                value <<= 1;
                modDiv <<= 1;
            }
            if (decimalPlaces > 1)
                value += modDiv * mod / 5;
            else
                value += (modDiv * mod + 4) / 5;
        }
        final double d = Math.scalb((double) value, exp);
        return negative ? -d : d;
    }

    @ForceInline
    public static String readUTFΔ(StreamingDataInput in) {
        StringBuilder sb = SBP.acquireStringBuilder();
        return in.readUTFΔ(sb) ? SI.intern(sb) : null;
    }

    @NotNull
    @ForceInline
    public static String parseUTF(StreamingDataInput bytes, @NotNull StopCharTester tester) {
        StringBuilder utfReader = SBP.acquireStringBuilder();
        parseUTF(bytes, utfReader, tester);
        return SI.intern(utfReader);
    }

    @ForceInline
    public static void parseUTF(StreamingDataInput bytes, @NotNull Appendable builder, @NotNull StopCharTester tester) {
        setLength(builder, 0);
        try {
            readUTF0(bytes, builder, tester);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void readUTF0(StreamingDataInput bytes, @NotNull Appendable appendable, @NotNull StopCharTester tester) throws IOException {
        int len = Maths.toInt32(bytes.readRemaining());
        while (len-- > 0) {
            int c = bytes.readUnsignedByte();
            if (c >= 128) {
                bytes.readSkip(-1);
                break;
            }
            if (tester.isStopChar(c))
                return;
            appendable.append((char) c);
        }
        if (len <= 0)
            return;

        while (true) {
            int c = bytes.readUnsignedByte();
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                /* 0xxxxxxx */
                    if (tester.isStopChar(c))
                        return;
                    appendable.append((char) c);
                    break;

                case 12:
                case 13: {
                /* 110x xxxx 10xx xxxx */
                    int char2 = bytes.readUnsignedByte();
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatException(
                                "malformed input around byte");
                    int c2 = (char) (((c & 0x1F) << 6) |
                            (char2 & 0x3F));
                    if (tester.isStopChar(c2))
                        return;
                    appendable.append((char) c2);
                    break;
                }

                case 14: {
                /* 1110 xxxx 10xx xxxx 10xx xxxx */

                    int char2 = bytes.readUnsignedByte();
                    int char3 = bytes.readUnsignedByte();

                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatException(
                                "malformed input around byte ");
                    int c3 = (char) (((c & 0x0F) << 12) |
                            ((char2 & 0x3F) << 6) |
                            (char3 & 0x3F));
                    if (tester.isStopChar(c3))
                        return;
                    appendable.append((char) c3);
                    break;
                }

                default:
                /* 10xx xxxx, 1111 xxxx */
                    throw new UTFDataFormatException(
                            "malformed input around byte ");
            }
        }
    }

    @ForceInline
    public static void parseUTF(StreamingDataInput bytes, @NotNull Appendable builder, @NotNull StopCharsTester tester) {
        setLength(builder, 0);
        try {
            readUTF0(bytes, builder, tester);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void readUTF0(StreamingDataInput bytes, @NotNull Appendable appendable, @NotNull StopCharsTester tester) throws IOException {
        while (true) {
            int c = bytes.readUnsignedByte();
            if (c >= 128) {
                bytes.readSkip(-1);
                break;
            }
            if (tester.isStopChar(c, bytes.peekUnsignedByte()))
                return;
            appendable.append((char) c);
            if (bytes.readRemaining() == 0)
                return;
        }

        while (true) {
            int c = bytes.readUnsignedByte();
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                /* 0xxxxxxx */
                    if (tester.isStopChar(c, bytes.peekUnsignedByte()))
                        return;
                    appendable.append((char) c);
                    break;

                case 12:
                case 13: {
                /* 110x xxxx 10xx xxxx */
                    int char2 = bytes.readUnsignedByte();
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatException(
                                "malformed input around byte");
                    int c2 = (char) (((c & 0x1F) << 6) |
                            (char2 & 0x3F));
                    if (tester.isStopChar(c2, bytes.peekUnsignedByte()))
                        return;
                    appendable.append((char) c2);
                    break;
                }

                case 14: {
                /* 1110 xxxx 10xx xxxx 10xx xxxx */

                    int char2 = bytes.readUnsignedByte();
                    int char3 = bytes.readUnsignedByte();

                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatException(
                                "malformed input around byte ");
                    int c3 = (char) (((c & 0x0F) << 12) |
                            ((char2 & 0x3F) << 6) |
                            (char3 & 0x3F));
                    if (tester.isStopChar(c3, bytes.peekUnsignedByte()))
                        return;
                    appendable.append((char) c3);
                    break;
                }

                default:
                /* 10xx xxxx, 1111 xxxx */
                    throw new UTFDataFormatException(
                            "malformed input around byte ");
            }
        }
    }

    @ForceInline
    public static void parse8bit(StreamingDataInput bytes, @NotNull StringBuilder builder, @NotNull StopCharsTester tester) {
        builder.setLength(0);
        read8bit0(bytes, builder, tester);
    }

    @ForceInline
    public static void parse8bit(StreamingDataInput bytes, @NotNull Bytes builder, @NotNull StopCharsTester tester) {
        builder.readPosition(0);

        read8bit0(bytes, builder, tester);
    }

    private static void read8bit0(StreamingDataInput bytes, @NotNull StringBuilder appendable, @NotNull StopCharsTester tester) {
        while (true) {
            int c = bytes.readUnsignedByte();
            if (tester.isStopChar(c, bytes.peekUnsignedByte()))
                return;
            appendable.append((char) c);
            if (bytes.readRemaining() == 0)
                return;
        }
    }

    private static void read8bit0(StreamingDataInput bytes, @NotNull Bytes bytes2, @NotNull StopCharsTester tester) {
        int ch = bytes.readUnsignedByte();
        do {
            int next = bytes.readUnsignedByte();
            if (tester.isStopChar(ch, next)) {
                bytes.readSkip(-1);
                return;
            }
            bytes2.writeUnsignedByte(ch);
            ch = next;
        } while (bytes.readRemaining() > 1);

        if (tester.isStopChar(ch, -1)) {
            bytes.readSkip(-1);
            return;
        }
        bytes2.writeUnsignedByte(ch);
    }

    public static double parseDouble(StreamingDataInput in) {
        long value = 0;
        int exp = 0;
        boolean negative = false;
        int decimalPlaces = Integer.MIN_VALUE;
        int ch = in.readUnsignedByte();
        switch (ch) {
            case 'N':
                if (compareRest(in, "aN"))
                    return Double.NaN;
                in.readSkip(-1);
                return Double.NaN;
            case 'I':
                //noinspection SpellCheckingInspection
                if (compareRest(in, "nfinity"))
                    return Double.POSITIVE_INFINITY;
                in.readSkip(-1);
                return Double.NaN;
            case '-':
                if (compareRest(in, "Infinity"))
                    return Double.NEGATIVE_INFINITY;
                negative = true;
                ch = in.readUnsignedByte();
                break;
        }
        while (true) {
            if (ch >= '0' && ch <= '9') {
                while (value >= MAX_VALUE_DIVIDE_10) {
                    value >>>= 1;
                    exp++;
                }
                value = value * 10 + (ch - '0');
                decimalPlaces++;

            } else if (ch == '.') {
                decimalPlaces = 0;

            } else {
                break;
            }
            if (in.readRemaining() == 0)
                break;
            ch = in.readUnsignedByte();
        }

        return asDouble(value, exp, negative, decimalPlaces);
    }

    static boolean compareRest(StreamingDataInput in, String s) {
        if (s.length() > in.readRemaining())
            return false;
        long position = in.readPosition();
        for (int i = 0; i < s.length(); i++) {
            if (in.readUnsignedByte() != s.charAt(i)) {
                in.readPosition(position);
                return false;
            }
        }
        return true;
    }

    @ForceInline
    public static long parseLong(StreamingDataInput in) {
        long num = 0;
        boolean negative = false;
        while (in.readRemaining() > 0) {
            int b = in.readUnsignedByte();
            // if (b >= '0' && b <= '9')
            if ((b - ('0' + Integer.MIN_VALUE)) <= 9 + Integer.MIN_VALUE) {
                num = num * 10 + b - '0';
            } else if (b == '-') {
                negative = true;
            } else if (b == ']' || b == '}') {
                in.readSkip(-1);
                break;
            } else {
                break;
            }
        }
        return negative ? -num : num;
    }

    public static long parseLong(RandomDataInput in, long offset) {
        long num = 0;
        boolean negative = false;
        while (true) {
            int b = in.readUnsignedByte(offset++);
            // if (b >= '0' && b <= '9')
            if ((b - ('0' + Integer.MIN_VALUE)) <= 9 + Integer.MIN_VALUE)
                num = num * 10 + b - '0';
            else if (b == '-')
                negative = true;
            else
                break;
        }
        return negative ? -num : num;
    }

    public static boolean skipTo(ByteStringParser parser, StopCharTester tester) {
        while (parser.readRemaining() > 0) {
            int ch = parser.readUnsignedByte();
            if (tester.isStopChar(ch))
                return true;
        }
        return false;
    }

    public static int getAndAddInt(BytesStore in, long offset, int adding) {
        for (; ; ) {
            int value = in.readVolatileInt(offset);
            if (in.compareAndSwapInt(offset, value, value + adding))
                return value;
        }
    }

    public static long getAndAddLong(BytesStore in, long offset, long adding) {
        for (; ; ) {
            long value = in.readVolatileLong(offset);
            if (in.compareAndSwapLong(offset, value, value + adding))
                return value;
        }
    }

    public static int asInt(@NotNull String str) {
        ByteBuffer bb = ByteBuffer.wrap(str.getBytes(StandardCharsets.ISO_8859_1)).order(ByteOrder.nativeOrder());
        return bb.getInt();
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param bytes the buffer you wish to toString()
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    public static String toHexString(@NotNull final Bytes bytes, long offset, long len) {
        if (len == 0)
            return "";

        int width = 16;
        int[] lastLine = new int[width];
        String sep = "";
        long position = bytes.readPosition();
        long limit = bytes.readLimit();

        try {

            bytes.readLimit(offset + len);
            bytes.readPosition(offset);

            final StringBuilder builder = new StringBuilder();
            long start = offset / width * width;
            long end = (offset + len + width - 1) / width * width;
            for (long i = start; i < end; i += width) {
                // check for duplicate rows
                if (i == start) {
                    for (int j = 0; j < width && i + j < offset + len; j++) {
                        int ch = bytes.readUnsignedByte(i + j);
                        lastLine[j] = ch;
                    }
                } else if (i + width < end) {
                    boolean same = true;

                    for (int j = 0; j < width && i + j < offset + len; j++) {
                        int ch = bytes.readUnsignedByte(i + j);
                        same &= (ch == lastLine[j]);
                        lastLine[j] = ch;
                    }
                    if (same) {
                        sep = "........\n";
                        continue;
                    }
                }
                builder.append(sep);
                sep = "";
                String str = Long.toHexString(i);
                for (int j = str.length(); j < 8; j++)
                    builder.append('0');
                builder.append(str);
                for (int j = 0; j < width; j++) {
                    if (j == width / 2)
                        builder.append(' ');
                    if (i + j < start || i + j >= offset + len) {
                        builder.append("   ");

                    } else {
                        builder.append(' ');
                        int ch = bytes.readUnsignedByte(i + j);
                        builder.append(HEXI_DECIMAL[ch >> 4]);
                        builder.append(HEXI_DECIMAL[ch & 15]);
                    }
                }
                builder.append(' ');
                for (int j = 0; j < width; j++) {
                    if (j == width / 2)
                        builder.append(' ');
                    if (i + j < start || i + j >= offset + len) {
                        builder.append(' ');

                    } else {
                        int ch = bytes.readUnsignedByte(i + j);
                        if (ch < ' ' || ch > 126)
                            ch = '\u00B7';
                        builder.append((char) ch);
                    }
                }
                builder.append("\n");
            }
            return builder.toString();
        } finally {
            bytes.readLimit(limit);
            bytes.readPosition(position);
        }
    }

    public static void setCharAt(@NotNull Appendable sb, int index, char ch) {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).setCharAt(index, ch);
        else if (sb instanceof Bytes)
            ((Bytes) sb).writeByte(index, ch);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    @ForceInline
    public static void setLength(@NotNull Appendable sb, int newLength) {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).setLength(newLength);
        else if (sb instanceof Bytes)
            ((Bytes) sb).readPosition(newLength);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static void append(@NotNull Appendable sb, double value) {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).append(value);
        else if (sb instanceof Bytes)
            ((Bytes) sb).append(value);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static void append(@NotNull Appendable sb, long value) {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).append(value);
        else if (sb instanceof Bytes)
            ((Bytes) sb).append(value);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static <ACS extends Appendable & CharSequence> void append(ACS sb, String str) {
        try {
            sb.append(str);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static <V> boolean equals(V a, V b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a instanceof CharSequence && b instanceof CharSequence)
            return a.toString().equals(b.toString());
        return Objects.equals(a, b);
    }

    public static void appendTimeMillis(ByteStringAppender b, long timeInMS) {
        int hours = (int) (timeInMS / (60 * 60 * 1000));
        if (hours > 99) {
            b.append(hours); // can have over 24 hours.
        } else {
            b.writeByte((byte) (hours / 10 + '0'));
            b.writeByte((byte) (hours % 10 + '0'));
        }
        b.writeByte((byte) ':');
        int minutes = (int) ((timeInMS / (60 * 1000)) % 60);
        b.writeByte((byte) (minutes / 10 + '0'));
        b.writeByte((byte) (minutes % 10 + '0'));
        b.writeByte((byte) ':');
        int seconds = (int) ((timeInMS / 1000) % 60);
        b.writeByte((byte) (seconds / 10 + '0'));
        b.writeByte((byte) (seconds % 10 + '0'));
        b.writeByte((byte) '.');
        int millis = (int) (timeInMS % 1000);
        b.writeByte((byte) (millis / 100 + '0'));
        b.writeByte((byte) (millis / 10 % 10 + '0'));
        b.writeByte((byte) (millis % 10 + '0'));
    }

    private static final ThreadLocal<DateCache> dateCacheTL = new ThreadLocal<DateCache>();

    public static boolean equalBytesAny(BytesStore b1, BytesStore b2, long remaining) {
        BytesStore bs1 = b1.bytesStore();
        BytesStore bs2 = b2.bytesStore();
        long i = 0;
        for (; i < remaining - 7; i++) {
            long l1 = bs1.readLong(b1.readPosition() + i);
            long l2 = bs2.readLong(b2.readPosition() + i);
            if (l1 != l2)
                return false;
        }
        if (i < remaining - 3) {
            int i1 = bs1.readInt(b1.readPosition() + i);
            int i2 = bs2.readInt(b2.readPosition() + i);
            if (i1 != i2)
                return false;
            i += 4;
        }
        for (; i < remaining; i++) {
            byte i1 = bs1.readByte(b1.readPosition() + i);
            byte i2 = bs2.readByte(b2.readPosition() + i);
            if (i1 != i2)
                return false;
        }
        return true;
    }

    static class DateCache {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        private long lastDay = Long.MIN_VALUE;
        @Nullable
        private byte[] lastDateStr = null;

        DateCache() {
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }

    public static void appendDateMillis(ByteStringAppender b, long timeInMS) {
        DateCache dateCache = dateCacheTL.get();
        if (dateCache == null) {
            dateCacheTL.set(dateCache = new DateCache());
        }
        long date = timeInMS / 86400000;
        if (dateCache.lastDay != date) {
            dateCache.lastDateStr = dateCache.dateFormat.format(new Date(timeInMS)).getBytes(StandardCharsets.ISO_8859_1);
            dateCache.lastDay = date;

        } else {
            assert dateCache.lastDateStr != null;
        }
        b.write(dateCache.lastDateStr);
    }
}
