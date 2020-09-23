/*
 * Copyright 2016-2020 Chronicle Software
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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.annotation.Java9;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

@SuppressWarnings("rawtypes")
public enum AppendableUtil {
    ;

    public static void setCharAt(@NotNull Appendable sb, int index, char ch)
            throws IllegalArgumentException, BufferOverflowException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).setCharAt(index, ch);
        else if (sb instanceof Bytes)
            ((Bytes) sb).writeByte(index, ch);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static void parseUtf8(@NotNull BytesStore bs, StringBuilder sb, boolean utf, int length) throws UTFDataFormatRuntimeException {
        BytesInternal.parseUtf8(bs, bs.readPosition(), sb, utf, length);
    }

    @ForceInline
    public static void setLength(@NotNull Appendable sb, int newLength)
            throws BufferUnderflowException, IllegalArgumentException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).setLength(newLength);
        else if (sb instanceof Bytes)
            ((Bytes) sb).readPositionRemaining(0, newLength);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static void append(@NotNull Appendable sb, double value)
            throws IllegalArgumentException, BufferOverflowException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).append(value);
        else if (sb instanceof Bytes)
            ((Bytes) sb).append(value);
        else
            throw new IllegalArgumentException("" + sb.getClass());
    }

    public static void append(@NotNull Appendable sb, long value)
            throws IllegalArgumentException, BufferOverflowException {
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
                                         @NotNull StopCharsTester tester) {
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
            throws BufferUnderflowException {
        try {
            readUtf8AndAppend(bytes, appendable, tester);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static void readUtf8AndAppend(@NotNull StreamingDataInput bytes,
                                         @NotNull Appendable appendable,
                                         @NotNull StopCharsTester tester)
            throws BufferUnderflowException, IOException {
        while (true) {
            int c = bytes.readUnsignedByte();
            if (c >= 128) {
                bytes.readSkip(-1);
                break;
            }
            // this is used for array class such as !type byte[]
            if (c == '[' && bytes.peekUnsignedByte() == ']') {
                appendable.append((char) c);
                appendable.append((char) bytes.readUnsignedByte());
                if (bytes.readRemaining() == 0)
                    return;
                continue;
            }

            if (tester.isStopChar(c, bytes.peekUnsignedByte()))
                return;
            appendable.append((char) c);
            if (bytes.readRemaining() == 0)
                return;
        }

        for (int c; (c = bytes.readUnsignedByte()) >= 0; ) {
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
                                "malformed input around byte " + Integer.toHexString(char2));
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

                    if (((char2 & 0xC0) != 0x80))
                        throw new UTFDataFormatException(
                                "malformed input around byte " + Integer.toHexString(char2));
                    if ((char3 & 0xC0) != 0x80)
                        throw new UTFDataFormatException(
                                "malformed input around byte " + Integer.toHexString(char3));
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
                            "malformed input around byte " + Integer.toHexString(c));
            }
        }
    }

    public static void parse8bit_SB1(@NotNull Bytes bytes, @NotNull StringBuilder sb, int length)
            throws BufferUnderflowException {
        if (length > bytes.readRemaining())
            throw new BufferUnderflowException();
        @Nullable NativeBytesStore nbs = (NativeBytesStore) bytes.bytesStore();
        long offset = bytes.readPosition();
        int count = BytesInternal.parse8bit_SB1(offset, nbs, sb, length);
        bytes.readSkip(count);
    }

    public static void parse8bit(@NotNull StreamingDataInput bytes, Appendable appendable, int utflen)
            throws BufferUnderflowException, IOException {
        if (appendable instanceof StringBuilder) {
            @NotNull final StringBuilder sb = (StringBuilder) appendable;
            if (bytes instanceof Bytes && ((Bytes) bytes).bytesStore() instanceof NativeBytesStore) {
                parse8bit_SB1((Bytes) bytes, sb, utflen);
            } else {
                BytesInternal.parse8bit1(bytes, sb, utflen);
            }
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

    public static long findUtf8Length(@NotNull CharSequence str) throws IndexOutOfBoundsException {
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

    @Java9
    public static long findUtf8Length(@NotNull byte[] bytes, byte coder) {
        long utflen;

        if (coder == 0) {
            int strlen = bytes.length;
            utflen = bytes.length;

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < strlen; i++) {
                int b = (bytes[i] & 0xFF);

                if (b > 0x007F) {
                    utflen++;
                }
            }
        } else {
            int strlen = bytes.length;
            utflen = 0;/* use charAt instead of copying String to char array */
            for (int i = 0; i < strlen; i += 2) {
                char c = (char) (((bytes[i + 1] & 0xFF) << 8) | (bytes[i] & 0xFF));

                if (c <= 0x007F) {
                    utflen += 1;
                    continue;
                }
                if (c <= 0x07FF) {
                    utflen += 2;
                } else {
                    utflen += 3;
                }
            }
        }

        return utflen;
    }

    @Java9
    public static long findUtf8Length(@NotNull byte[] chars) {
        long utflen = 0; /* use charAt instead of copying String to char array */
        int strlen = chars.length;
        for (int i = 0; i < strlen; i++) {
            int c = chars[i] & 0xFF; // unsigned byte

            if (c == 0) { // we have hit end of string
                break;
            }

            if (c >= 0xF0) {
                utflen += 4;
                i += 3;
            } else if (c >= 0xE0) {
                utflen += 3;
                i += 2;
            } else if (c >= 0xC0) {
                utflen += 2;
                i += 1;
            } else {
                utflen += 1;
            }
        }
        return utflen;
    }

    public static long findUtf8Length(@NotNull char[] chars) {
        long utflen = chars.length;/* use charAt instead of copying String to char array */
        for (char c : chars) {
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
}
