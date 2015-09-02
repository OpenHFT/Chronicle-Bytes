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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.core.annotation.Nullable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.EnumInterner;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static net.openhft.chronicle.core.util.StringUtils.extractChars;
import static net.openhft.chronicle.core.util.StringUtils.setCount;

/**
 * Utility methods to support common functionality in this package.
 * This is not intended to be accessed directly.
 */
enum BytesInternal {
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
    private static final ThreadLocal<DateCache> dateCacheTL = new ThreadLocal<>();

    static {
        try {
            ClassAliasPool.CLASS_ALIASES.addAlias(BytesStore.class, "!binary");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static boolean contentEqual(@Nullable BytesStore a, @Nullable BytesStore b) throws IORuntimeException {
        if (a == null) return b == null;
        if (b == null) return false;
        if (a.readRemaining() != b.readRemaining())
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
            @NotNull RandomDataInput a,
            long offset, @org.jetbrains.annotations.NotNull RandomDataInput second, long secondOffset, long len) throws IORuntimeException, BufferUnderflowException {
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


    public static void parseUTF(@NotNull StreamingDataInput bytes, Appendable appendable, int utflen) throws UTFDataFormatRuntimeException, BufferUnderflowException {
        if (bytes instanceof Bytes
                && ((Bytes) bytes).bytesStore() instanceof NativeBytesStore
                && appendable instanceof StringBuilder) {
            parseUTF_SB1((Bytes) bytes, (StringBuilder) appendable, utflen);
        } else {
            parseUTF1(bytes, appendable, utflen);
        }
    }

    public static void parse8bit(long offset, @NotNull RandomDataInput bytesStore, Appendable appendable, int utflen) throws UTFDataFormatRuntimeException, BufferUnderflowException {
        if (bytesStore instanceof NativeBytesStore
                && appendable instanceof StringBuilder) {
            parse8bit_SB1(offset, (NativeBytesStore) bytesStore, (StringBuilder) appendable, utflen);
        } else {
            parse8bit1(offset, bytesStore, appendable, utflen);
        }
    }

    public static void parseUTF1(@NotNull StreamingDataInput bytes, @NotNull Appendable appendable, int utflen) throws UTFDataFormatRuntimeException, BufferUnderflowException {
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

    public static void parse8bit1(@NotNull StreamingDataInput bytes, @NotNull Appendable appendable, int utflen) throws UTFDataFormatRuntimeException {
        try {
            assert bytes.readRemaining() >= utflen;
            for (int count = 0; count < utflen; count++) {
                int c = bytes.readUnsignedByte();
                appendable.append((char) c);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static void parse8bit1(long offset, @NotNull RandomDataInput bytes, @NotNull Appendable appendable, int utflen) throws UTFDataFormatRuntimeException, BufferUnderflowException {
        try {
            assert bytes.readRemaining() >= utflen;
            for (int count = 0; count < utflen; count++) {
                int c = bytes.readUnsignedByte(offset + count);
                appendable.append((char) c);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static void parseUTF_SB1(@NotNull Bytes bytes, @NotNull StringBuilder sb, int utflen) throws UTFDataFormatRuntimeException, BufferUnderflowException {
        try {
            int count = 0;
            if (utflen > bytes.readRemaining())
                throw new BufferUnderflowException();
            NativeBytesStore nbs = (NativeBytesStore) bytes.bytesStore();
            long address = nbs.address + nbs.translate(bytes.readPosition());
            Memory memory = nbs.memory;
            sb.ensureCapacity(utflen);
            char[] chars = extractChars(sb);
            while (count < utflen) {
                int c = memory.readByte(address + count);
                if (c < 0)
                    break;
                chars[count++] = (char) c;
            }
            bytes.readSkip(count);
            setCount(sb, count);
            if (count < utflen)
                parseUTF2(bytes, sb, utflen, count);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static int parse8bit_SB1(long offset, NativeBytesStore nbs, @NotNull StringBuilder sb, int utflen) {
            long address = nbs.address + nbs.translate(offset);
            Memory memory = nbs.memory;
            sb.ensureCapacity(utflen);
        char[] chars = extractChars(sb);
            int count = 0;
            while (count < utflen) {
                int c = memory.readByte(address + count) & 0xFF;
                chars[count++] = (char) c;
            }
        setCount(sb, count);
            return count;
    }

    static void parseUTF2(@NotNull StreamingDataInput bytes, @NotNull Appendable appendable, int utflen, int count) throws IOException {
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

    public static void writeUtf8(@NotNull StreamingDataOutput bytes, @Nullable String str) throws IORuntimeException, IllegalArgumentException, BufferOverflowException {
        char[] chars = extractChars(str);
        long utfLength = findUTFLength(chars);
        bytes.writeStopBit(utfLength);
        bytes.appendUTF(chars, 0, chars.length);
    }

    @ForceInline
    public static void writeUtf8(@NotNull StreamingDataOutput bytes, @Nullable CharSequence str) throws BufferOverflowException, IllegalArgumentException, IORuntimeException, IndexOutOfBoundsException {
        if (str instanceof String) {
            writeUtf8(bytes, (String) str);
            return;
        }
        if (str == null) {
            bytes.writeStopBit(-1);

        } else {
            long utfLength = findUTFLength(str);
            bytes.writeStopBit(utfLength);
            appendUTF(bytes, str, 0, str.length());
        }
    }

    private static long findUTFLength(@NotNull CharSequence str) throws IndexOutOfBoundsException {
        int strlen = str.length();
        long utflen = strlen;/* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            char c = str.charAt(i);
            if (c <= 0x007F) {
                continue;
            }
            if (c <= 0x07FF) {
                utflen++;

            } else {
                utflen += 2;
            }
        }
        return utflen;
    }

    private static long findUTFLength(@NotNull char[] chars) {
        int strlen = chars.length;
        long utflen = strlen;/* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            char c = chars[i];
            if (c <= 0x007F) {
                continue;
            }
            if (c <= 0x07FF) {
                utflen++;

            } else {
                utflen += 2;
            }
        }
        return utflen;
    }

    @NotNull
    public static Bytes asBytes(@NotNull RandomDataOutput bytes, long position, long limit) throws IllegalStateException, BufferOverflowException, BufferUnderflowException {
        Bytes sbytes = bytes.bytesForWrite();
        sbytes.writeLimit(limit);
        sbytes.readLimit(limit);
        sbytes.readPosition(position);
        return sbytes;
    }

    public static void appendUTF(@NotNull StreamingDataOutput bytes, @NotNull CharSequence str, int offset, int length) throws IndexOutOfBoundsException, IORuntimeException, BufferOverflowException {
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

    public static void append8bit(long offsetInRDO, RandomDataOutput bytes, @NotNull CharSequence str, int offset, int length) throws IllegalArgumentException, BufferOverflowException, IORuntimeException, BufferUnderflowException, IndexOutOfBoundsException {
        if (bytes instanceof VanillaBytes) {
            VanillaBytes vb = (VanillaBytes) bytes;
            if (str instanceof RandomDataInput) {
                vb.write(offsetInRDO, (RandomDataInput) str, offset, length);
                return;
            }
            if (str instanceof String) {
                vb.write(offsetInRDO, str, offset, length);
                return;
            }
        }
        for (int i = 0; i < length; i++) {
            char c = str.charAt(offset + i);
            if (c > 255) c = '?';
            bytes.writeUnsignedByte(offsetInRDO + i, c);
        }
    }

    public static void appendUTF(@NotNull StreamingDataOutput bytes, int c) throws IORuntimeException, BufferOverflowException {
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

    public static void writeStopBit(@NotNull StreamingDataOutput out, long n) throws IORuntimeException, BufferOverflowException {
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

    @SuppressWarnings("ShiftOutOfRange")
    public static void writeStopBit(@NotNull StreamingDataOutput out, double d) throws IORuntimeException, BufferOverflowException {
        long n = Double.doubleToRawLongBits(d);
        while ((n & (~0L >>> 7)) != 0) {
            out.writeByte((byte) (((n >>> -7) & 0x7F) | 0x80));
            n <<= 7;
        }
        out.writeByte((byte) ((n >>> -7) & 0x7F));
    }

    public static double readStopBitDouble(@NotNull StreamingDataInput in) throws IORuntimeException {
        long n = 0;
        int shift = 64 - 7;
        int b;
        do {
            b = in.readByte();
            n |= shift > 0 ? (long) (b & 0x7F) << shift : b >> -shift;
            shift -= 7;
        } while (b < 0);
        return Double.longBitsToDouble(n);
    }

    static void writeStopBit0(@NotNull StreamingDataOutput out, long n) throws IORuntimeException, BufferOverflowException {
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

    public static String toDebugString(@NotNull RandomDataInput bytes, long maxLength) {
        StringBuilder sb = new StringBuilder(200);
        long position = bytes.readPosition();
        sb.append("[")
                .append("pos: ").append(position)
                .append(", rlim: ").append(bytes.readLimit())
                .append(", wlim: ").append(asSize(bytes.writeLimit()))
                .append(", cap: ").append(asSize(bytes.capacity()))
                .append(" ] ");
        try {
            toString(bytes, sb, position - maxLength, position, position + maxLength);
        } catch (Exception e) {
            sb.append(' ').append(e);
        }

        return sb.toString();
    }

    @NotNull
    public static Object asSize(long size) {
        return size == Bytes.MAX_CAPACITY ? "8EiB" : size;
    }

    public static String to8bitString(@NotNull BytesStore bytes) throws IllegalArgumentException, IORuntimeException {
        long pos = bytes.readPosition();
        int len = Maths.toInt32(bytes.readRemaining());
        char[] chars = new char[len];
        if (bytes instanceof VanillaBytes) {
            ((VanillaBytes) bytes).read8Bit(chars, len);
        } else {
            for (int i = 0; i < len; i++)
                try {
                    chars[i] = (char) bytes.readUnsignedByte(pos + i);
                } catch (Exception e) {
                    return new String(chars, 0, len) + ' ' + e;
                }
        }
        return StringUtils.newString(chars);
    }

    public static String toString(@NotNull RandomDataInput bytes) throws IllegalStateException, IORuntimeException {
        StringBuilder sb = new StringBuilder(200);
        toString(bytes, sb);
        return sb.toString();
    }

    private static void toString(@NotNull RandomDataInput bytes, @NotNull Appendable sb, long start, long position, long end) throws BufferUnderflowException {
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

    private static void toString(@NotNull RandomDataInput bytes, @NotNull StringBuilder sb) throws IllegalStateException, IORuntimeException {
        bytes.reserve();
        assert bytes.start() <= bytes.readPosition();
        assert bytes.readPosition() <= bytes.readLimit();
        assert bytes.readLimit() <= bytes.realCapacity();

        try {
        for (long i = bytes.readPosition(); i < bytes.readLimit(); i++) {
            sb.append((char) bytes.readUnsignedByte(i));
        }
        } catch (BufferUnderflowException e) {
            sb.append(' ').append(e);
        }

        bytes.release();
    }

    @ForceInline
    public static long readStopBit(@NotNull StreamingDataInput in) throws IORuntimeException {
        long l;
        if ((l = in.readByte()) >= 0)
            return l;
        return readStopBit0(in, l);
    }

    static long readStopBit0(@NotNull StreamingDataInput in, long l) throws IORuntimeException {
        l &= 0x7FL;
        long b;
        int count = 7;
        while ((b = in.readByte()) < 0) {
            l |= (b & 0x7FL) << count;
            count += 7;
        }
        if (b != 0) {
            if (count > 56)
                throw new IORuntimeException(
                        "Cannot read more than 9 stop bits of positive value");
            return l | (b << count);

        } else {
            if (count > 63)
                throw new IORuntimeException(
                        "Cannot read more than 10 stop bits of negative value");
            return ~l;
        }
    }

    public static <S extends ByteStringAppender> void append(@NotNull S out, long num) throws IORuntimeException, IllegalArgumentException, BufferOverflowException {
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
    public static void append(@NotNull RandomDataOutput out, long offset, long num, int digits) throws IORuntimeException, IllegalArgumentException, BufferOverflowException {
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

    private static void numberTooLarge(int digits) throws IllegalArgumentException {
        throw new IllegalArgumentException("Number too large for " + digits + "digits");
    }

    private static void appendLong0(@NotNull StreamingDataOutput out, long num) throws IORuntimeException, IllegalArgumentException, BufferOverflowException {
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

    public static void append(@NotNull StreamingDataOutput out, double d) throws IORuntimeException, BufferOverflowException, IllegalArgumentException {
        long val = Double.doubleToRawLongBits(d);
        int sign = (int) (val >>> 63);
        int exp = (int) ((val >>> 52) & 2047);
        long mantissa = val & ((1L << 52) - 1);
        if (sign != 0) {
            out.writeByte((byte) '-');
        }
        if (exp == 0 && mantissa == 0) {
            out.writeByte((byte) '0');
            out.writeByte((byte) '.');
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
                } else {
                    out.writeByte((byte) '.');
                    out.writeByte((byte) '0');
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

    @Nullable
    @ForceInline
    public static String readUtf8(@NotNull StreamingDataInput in) throws BufferUnderflowException, IORuntimeException, IllegalArgumentException {
        StringBuilder sb = acquireStringBuilder();
        return in.readUtf8(sb) ? SI.intern(sb) : null;
    }

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    @Nullable
    @ForceInline
    public static String read8bit(@NotNull StreamingDataInput in) throws BufferUnderflowException, IORuntimeException {
        StringBuilder sb = acquireStringBuilder();
        return in.read8bit(sb) ? SI.intern(sb) : null;
    }

    @NotNull
    @ForceInline
    public static String parseUTF(@NotNull StreamingDataInput bytes, @NotNull StopCharTester tester) {
        StringBuilder utfReader = acquireStringBuilder();
        parseUTF(bytes, utfReader, tester);
        return SI.intern(utfReader);
    }

    @ForceInline
    public static void parseUTF(@NotNull StreamingDataInput bytes, @NotNull Appendable builder, @NotNull StopCharTester tester) throws BufferUnderflowException, IllegalStateException {
        try {
            if (builder instanceof StringBuilder
                    && ((Bytes) bytes).bytesStore() instanceof NativeBytesStore) {
                Bytes vb = (Bytes) bytes;
                StringBuilder sb = (StringBuilder) builder;
                sb.setLength(0);
                readUTF_SB1(vb, sb, tester);
            } else {
                AppendableUtil.setLength(builder, 0);
                readUTF1(bytes, builder, tester);
            }
        } catch (IOException | IllegalArgumentException e) {
            throw Jvm.rethrow(e);
        }
    }

    private static void readUTF_SB1(@NotNull Bytes bytes, @NotNull StringBuilder appendable, @NotNull StopCharTester tester) throws IOException, IllegalArgumentException, IllegalStateException, BufferUnderflowException {
        NativeBytesStore nb = (NativeBytesStore) bytes.bytesStore();
        int i = 0, len = Maths.toInt32(bytes.readRemaining());
        long address = nb.address + nb.translate(bytes.readPosition());

        Memory memory = nb.memory;
        for (; i < len; i++) {
            int c = memory.readByte(address + i);
            if (c < 0)
                break;
            if (tester.isStopChar(c)) {
                bytes.readSkip(i + 1);
                return;
            }
            appendable.append((char) c);
        }
        bytes.readSkip(i);
        if (i < len) {
            readUTF_SB2(bytes, appendable, tester);
        }
    }

    private static void readUTF_SB2(@NotNull StreamingDataInput bytes, @NotNull StringBuilder appendable, @NotNull StopCharTester tester) throws UTFDataFormatException, IORuntimeException {
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

    private static void readUTF1(@NotNull StreamingDataInput bytes, @NotNull Appendable appendable, @NotNull StopCharTester tester) throws IOException, IllegalArgumentException, BufferUnderflowException {
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

        readUTF2(bytes, appendable, tester);
    }

    private static void readUTF2(@NotNull StreamingDataInput bytes, @NotNull Appendable appendable, @NotNull StopCharTester tester) throws IOException {
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
    public static void parseUTF(@NotNull StreamingDataInput bytes, @NotNull Appendable builder, @NotNull StopCharsTester tester) throws IllegalArgumentException, BufferUnderflowException, IORuntimeException {
        AppendableUtil.setLength(builder, 0);
        try {
            AppendableUtil.readUTFAndAppend(bytes, builder, tester);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @ForceInline
    public static void parse8bit(@NotNull StreamingDataInput bytes, @NotNull StringBuilder builder, @NotNull StopCharsTester tester) throws IORuntimeException {
        builder.setLength(0);
        AppendableUtil.read8bitAndAppend(bytes, builder, tester);
    }

    @ForceInline
    public static void parse8bit(@NotNull StreamingDataInput bytes, @NotNull Bytes builder, @NotNull StopCharsTester tester) throws BufferUnderflowException, BufferOverflowException, IllegalArgumentException, IORuntimeException {
        builder.readPosition(0);

        read8bitAndAppend(bytes, builder, tester);
    }

    @ForceInline
    public static void parse8bit(@NotNull StreamingDataInput bytes, @NotNull StringBuilder builder, @NotNull StopCharTester tester) throws IORuntimeException {
        builder.setLength(0);
        read8bitAndAppend(bytes, builder, tester);
    }

    @ForceInline
    public static void parse8bit(@NotNull StreamingDataInput bytes, @NotNull Bytes builder, @NotNull StopCharTester tester) throws BufferUnderflowException, BufferOverflowException, IllegalArgumentException, IORuntimeException {
        builder.readPosition(0);

        read8bitAndAppend(bytes, builder, tester);
    }

    private static void read8bitAndAppend(@NotNull StreamingDataInput bytes, @NotNull StringBuilder appendable, @NotNull StopCharTester tester) throws IORuntimeException {
        while (true) {
            int c = bytes.readUnsignedByte();
            if (tester.isStopChar(c))
                return;
            appendable.append((char) c);
            if (bytes.readRemaining() == 0)
                return;
        }
    }

    private static void read8bitAndAppend(@NotNull StreamingDataInput bytes, @NotNull Bytes bytes2, @NotNull StopCharTester tester) throws IORuntimeException, BufferUnderflowException, IllegalArgumentException, BufferOverflowException {
        int ch = bytes.readUnsignedByte();
        do {
            if (tester.isStopChar(ch)) {
                bytes.readSkip(-1);
                return;
            }
            bytes2.writeUnsignedByte(ch);
            int next = bytes.readUnsignedByte();
            ch = next;
        } while (bytes.readRemaining() > 1);

        if (tester.isStopChar(ch)) {
            bytes.readSkip(-1);
            return;
        }
        bytes2.writeUnsignedByte(ch);
    }

    private static void read8bitAndAppend(@NotNull StreamingDataInput bytes, @NotNull Bytes bytes2, @NotNull StopCharsTester tester) throws IORuntimeException, BufferUnderflowException, IllegalArgumentException, BufferOverflowException {
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

    public static double parseDouble(@NotNull StreamingDataInput in) throws IORuntimeException, BufferUnderflowException {
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

    static boolean compareRest(@NotNull StreamingDataInput in, @NotNull String s) throws IORuntimeException, BufferUnderflowException {
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
    public static long parseLong(@NotNull StreamingDataInput in) throws IORuntimeException, BufferUnderflowException {
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
            } else if (b == '_') {
                // ignore
            } else {
                break;
            }
        }
        return negative ? -num : num;
    }

    public static long parseLong(@NotNull RandomDataInput in, long offset) throws IORuntimeException, BufferUnderflowException {
        long num = 0;
        boolean negative = false;
        while (true) {
            int b = in.readUnsignedByte(offset++);
            // if (b >= '0' && b <= '9')
            if ((b - ('0' + Integer.MIN_VALUE)) <= 9 + Integer.MIN_VALUE)
                num = num * 10 + b - '0';
            else if (b == '-')
                negative = true;
            else if (b != '_')
                break;
        }
        return negative ? -num : num;
    }

    public static boolean skipTo(@NotNull ByteStringParser parser, @NotNull StopCharTester tester) throws IORuntimeException {
        while (parser.readRemaining() > 0) {
            int ch = parser.readUnsignedByte();
            if (tester.isStopChar(ch))
                return true;
        }
        return false;
    }

    public static float addAndGetFloat(@NotNull RandomDataInput in, long offset, float adding) throws IORuntimeException, BufferUnderflowException, IllegalArgumentException, BufferOverflowException {
        for (; ; ) {
            int value = in.readVolatileInt(offset);
            float value1 = Float.intBitsToFloat(value) + adding;
            int value2 = Float.floatToRawIntBits(value1);
            if (in.compareAndSwapInt(offset, value, value2))
                return value1;
        }
    }

    public static double addAndGetDouble(@NotNull RandomDataInput in, long offset, double adding) throws IORuntimeException, BufferUnderflowException, IllegalArgumentException, BufferOverflowException {
        for (; ; ) {
            long value = in.readVolatileLong(offset);
            double value1 = Double.longBitsToDouble(value) + adding;
            long value2 = Double.doubleToRawLongBits(value1);
            if (in.compareAndSwapLong(offset, value, value2))
                return value1;
        }
    }

    public static int addAndGetInt(@NotNull RandomDataInput in, long offset, int adding) throws IORuntimeException, BufferUnderflowException, IllegalArgumentException, BufferOverflowException {
        for (; ; ) {
            int value = in.readVolatileInt(offset);
            int value2 = value + adding;
            if (in.compareAndSwapInt(offset, value, value2))
                return value2;
        }
    }

    public static long addAndGetLong(@NotNull RandomDataInput in, long offset, long adding) throws IORuntimeException, BufferUnderflowException, IllegalArgumentException, BufferOverflowException {
        for (; ; ) {
            long value = in.readVolatileLong(offset);
            long value2 = value + adding;
            if (in.compareAndSwapLong(offset, value, value2))
                return value2;
        }
    }

    public static String toHexString(@NotNull final Bytes bytes) throws IORuntimeException, BufferUnderflowException {
        return toHexString(bytes, bytes.readPosition(), bytes.readRemaining());
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param bytes the buffer you wish to toString()
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    public static String toHexString(@NotNull final Bytes bytes, long offset, long len) throws BufferUnderflowException, IORuntimeException {
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
                if (i + width < end) {
                    boolean same = true;

                    for (int j = 0; j < width && i + j < offset + len; j++) {
                        int ch = bytes.readUnsignedByte(i + j);
                        same &= (ch == lastLine[j]);
                        lastLine[j] = ch;
                    }
                    if (i > start && same) {
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

    public static void appendTimeMillis(@NotNull ByteStringAppender b, long timeInMS) throws IORuntimeException, BufferOverflowException, IllegalArgumentException {
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

    public static boolean equalBytesAny(@NotNull BytesStore b1, @NotNull BytesStore b2, long remaining) throws IORuntimeException, BufferUnderflowException {
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

    public static void appendDateMillis(@NotNull ByteStringAppender b, long timeInMS) throws IORuntimeException, BufferOverflowException {
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

    public static <E extends Enum<E>, S extends StreamingDataInput<S>> E readEnum(StreamingDataInput input, Class<E> eClass) throws BufferUnderflowException, IORuntimeException {
        StringBuilder sb = acquireStringBuilder();
        input.read8bit(sb);

        return (E) EnumInterner.ENUM_INTERNER.get(eClass).intern(sb);
    }

    public static void write(@NotNull BytesStore bytes, long offset, long length, StreamingDataOutput sdo) throws IORuntimeException, BufferUnderflowException, BufferOverflowException {
        long i = 0;
        for (; i < length - 7; i += 8)
            sdo.writeLong(bytes.readLong(offset + i));
        if (i < length - 3) {
            sdo.writeInt(bytes.readInt(offset + i));
            i += 4;
        }
        for (; i < length; i++)
            sdo.writeByte(bytes.readByte(offset + i));
    }

    public static byte[] toByteArray(StreamingDataInput in) throws IllegalArgumentException, IORuntimeException {
        int len = Maths.toInt32(in.readRemaining());
        byte[] bytes = new byte[len];
        in.read(bytes);
        return bytes;
    }

    public static void copy(StreamingDataInput input, OutputStream output) throws IOException {
        byte[] bytes = new byte[512];
        for (int len; (len = input.read(bytes)) > 0; ) {
            output.write(bytes, 0, len);
        }
    }

    public static void copy(InputStream input, StreamingDataOutput output) throws IOException, IllegalArgumentException, BufferOverflowException {
        byte[] bytes = new byte[512];
        for (int len; (len = input.read(bytes)) > 0; ) {
            output.write(bytes, 0, len);
        }
    }

    public static Boolean parseBoolean(ByteStringParser parser, StopCharTester tester) {
        StringBuilder sb = acquireStringBuilder();
        parseUTF(parser, sb, tester);
        if (sb.length() == 0)
            return null;
        switch (sb.charAt(0)) {
            case 't':
            case 'T':
                return sb.length() == 1 || StringUtils.equalsCaseIgnore(sb, "true") ? true : null;
            case 'y':
            case 'Y':
                return sb.length() == 1 || StringUtils.equalsCaseIgnore(sb, "yes") ? true : null;
            case '0':
                return sb.length() == 1 ? false : null;
            case '1':
                return sb.length() == 1 ? true : null;
            case 'f':
            case 'F':
                return sb.length() == 1 || StringUtils.equalsCaseIgnore(sb, "false") ? false : null;
            case 'n':
            case 'N':
                return sb.length() == 1 || StringUtils.equalsCaseIgnore(sb, "no") ? false : null;
        }
        return null;
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
}
