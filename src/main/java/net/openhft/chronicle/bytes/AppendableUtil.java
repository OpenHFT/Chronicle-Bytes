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
import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Created by peter on 30/08/15.
 */
public class AppendableUtil {
    public static void setCharAt(@NotNull Appendable sb, int index, char ch)
            throws IllegalArgumentException, BufferOverflowException, IORuntimeException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).setCharAt(index, ch);
        else if (sb instanceof Bytes)
            ((Bytes) sb).writeByte(index, ch);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    @ForceInline
    public static void setLength(@NotNull Appendable sb, int newLength)
            throws BufferUnderflowException, IllegalArgumentException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).setLength(newLength);
        else if (sb instanceof Bytes)
            ((Bytes) sb).readPosition(newLength);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static void append(@NotNull Appendable sb, double value)
            throws IllegalArgumentException, IORuntimeException, BufferOverflowException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).append(value);
        else if (sb instanceof Bytes)
            ((Bytes) sb).append(value);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static void append(@NotNull Appendable sb, long value)
            throws IllegalArgumentException, IORuntimeException, BufferOverflowException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).append(value);
        else if (sb instanceof Bytes)
            ((Bytes) sb).append(value);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static <ACS extends Appendable & CharSequence> void append(@NotNull ACS sb, String str) {
        try {
            sb.append(str);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static void read8bitAndAppend(@NotNull StreamingDataInput bytes,
                                         @NotNull StringBuilder appendable,
                                         @NotNull StopCharsTester tester) throws IORuntimeException {
        while (true) {
            int c = bytes.readUnsignedByte();
            if (tester.isStopChar(c, bytes.peekUnsignedByte()))
                return;
            appendable.append((char) c);
            if (bytes.readRemaining() == 0)
                return;
        }
    }

    public static void readUTFAndAppend(@NotNull StreamingDataInput bytes,
                                        @NotNull Appendable appendable,
                                        @NotNull StopCharsTester tester)
            throws IOException, BufferUnderflowException {
        readUtf8AndAppend(bytes, appendable, tester);
    }

    public static void readUtf8AndAppend(@NotNull StreamingDataInput bytes,
                                         @NotNull Appendable appendable,
                                         @NotNull StopCharsTester tester)
            throws IOException, BufferUnderflowException {
        try {
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
        } catch (IORuntimeException e) {
            throw new IOException(e);
        }
    }

    public static void parse8bit_SB1(@NotNull Bytes bytes, @NotNull StringBuilder sb, int utflen)
            throws IORuntimeException, BufferUnderflowException {
        if (utflen > bytes.readRemaining())
            throw new BufferUnderflowException();
        NativeBytesStore nbs = (NativeBytesStore) bytes.bytesStore();
        long offset = bytes.readPosition();
        int count = BytesInternal.parse8bit_SB1(offset, nbs, sb, utflen);
        bytes.readSkip(count);
    }

    public static void parse8bit(@NotNull StreamingDataInput bytes, Appendable appendable, int utflen)
            throws IORuntimeException, BufferUnderflowException {
        if (bytes instanceof Bytes
                && ((Bytes) bytes).bytesStore() instanceof NativeBytesStore
                && appendable instanceof StringBuilder) {
            parse8bit_SB1((Bytes) bytes, (StringBuilder) appendable, utflen);
        } else {
            BytesInternal.parse8bit1(bytes, appendable, utflen);
        }
    }

    public static <ACS extends Appendable & CharSequence> void append(ACS a, CharSequence cs, long start, long len) {
        if (a instanceof StringBuilder) {
            if (cs instanceof Bytes)
                ((StringBuilder) a).append(Bytes.toString(((Bytes) cs), start, len));
            else
                ((StringBuilder) a).append(cs.subSequence(Maths.toInt32(start), Maths.toInt32(len)));
        } else if (a instanceof Bytes) {
            ((Bytes) a).appendUtf8(cs, Maths.toInt32(start), Maths.toInt32(len));
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
