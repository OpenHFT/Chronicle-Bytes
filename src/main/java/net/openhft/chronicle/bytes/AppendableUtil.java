/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.Java9;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Utility methods for manipulating {@link Appendable} implementations such as
 * {@link StringBuilder} and {@link Bytes}. These helpers are used throughout
 * the text parsing and formatting code paths.
 */
@SuppressWarnings("rawtypes")
public enum AppendableUtil {

    ; // Enum with no instances signifies a utility class.

    private static final String MALFORMED_INPUT_AROUND_BYTE = "malformed input around byte ";

    /**
     * Writes {@code ch} at {@code index} in the supplied {@code Appendable}.
     * Only {@link StringBuilder} and {@link Bytes} are supported.
     *
     * @throws IllegalArgumentException if {@code sb} is not a supported type
     * @throws BufferOverflowException  if {@code index} exceeds the capacity of the target
     */
    public static void setCharAt(@NotNull Appendable sb, @NonNegative int index, char ch)
            throws IllegalArgumentException, BufferOverflowException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).setCharAt(index, ch);
        else if (sb instanceof Bytes)
            ((Bytes) sb).writeByte(index, ch);
        else
            throw new IllegalArgumentException(String.valueOf(sb.getClass()));
    }

    /**
     * Decodes {@code length} bytes from {@code bs} as either UTF-8 or ISO-8859-1
     * and appends the text to {@code sb}.
     *
     * @param utf when {@code true} treat the bytes as UTF-8, otherwise ISO-8859-1
     * @throws UTFDataFormatRuntimeException if the data is malformed UTF-8
     */
    public static void parseUtf8(@NotNull BytesStore<?, ?> bs, StringBuilder sb, boolean utf, @NonNegative int length)
            throws UTFDataFormatRuntimeException, BufferUnderflowException, ClosedIllegalStateException {
        BytesInternal.parseUtf8(bs, bs.readPosition(), sb, utf, length);
    }

    /**
     * Adjusts the logical length of {@code sb}. For {@link Bytes} this moves the
     * write position to {@code newLength}.
     */
    public static void setLength(@NotNull Appendable sb, @NonNegative int newLength)
            throws IllegalArgumentException, ClosedIllegalStateException, BufferUnderflowException {
        requireNonNull(sb);
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).setLength(newLength);
        else if (sb instanceof Bytes)
            ((Bytes) sb).readPositionRemaining(0, newLength);
        else
            throw new IllegalArgumentException(String.valueOf(sb.getClass()));
    }

    /**
     * Appends {@code value} to {@code sb}. For {@link Bytes} the configured
     * {@link net.openhft.chronicle.bytes.render.Decimaliser} is used.
     */
    public static void append(@NotNull Appendable sb, double value)
            throws IllegalArgumentException, BufferOverflowException, ClosedIllegalStateException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).append(value);
        else if (sb instanceof Bytes)
            ((Bytes) sb).append(value);
        else
            throw new IllegalArgumentException(String.valueOf(sb.getClass()));
    }

    /**
     * Appends {@code value} in decimal form to {@code sb}.
     */
    public static void append(@NotNull Appendable sb, long value)
            throws IllegalArgumentException, BufferOverflowException, ClosedIllegalStateException {
        if (sb instanceof StringBuilder)
            ((StringBuilder) sb).append(value);
        else if (sb instanceof Bytes)
            ((Bytes) sb).append(value);
        else
            throw new IllegalArgumentException(String.valueOf(sb.getClass()));
    }

    /**
     * Appends a string to an Appendable that also implements CharSequence.
     *
     * @param sb  the Appendable to append to
     * @param str the String to append
     */
    public static <C extends Appendable & CharSequence> void append(@NotNull C sb, String str) {
        try {
            sb.append(str);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Reads 8-bit characters from {@code bytes} appending each to
     * {@code appendable} until {@code tester} signals a stop or the input ends.
     */
    public static void read8bitAndAppend(@NotNull StreamingDataInput bytes,
                                         @NotNull StringBuilder appendable,
                                         @NotNull StopCharsTester tester)
            throws ClosedIllegalStateException {
        while (true) {
            int c = bytes.readUnsignedByte();
            if (tester.isStopChar(c, bytes.peekUnsignedByte()))
                return;
            appendable.append((char) c);
            if (bytes.readRemaining() == 0)
                return;
        }
    }

    /**
     * Reads 8-bit characters and appends them to {@code appendable} until
     * {@code tester} requests a stop.
     */
    public static void readUTFAndAppend(@NotNull StreamingDataInput bytes,
                                        @NotNull Appendable appendable,
                                        @NotNull StopCharsTester tester)
            throws BufferUnderflowException, ClosedIllegalStateException {
        try {
            readUtf8AndAppend(bytes, appendable, tester);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Reads UTF-8 characters from {@code bytes} and appends them to
     * {@code appendable} until {@code tester} signals a stop or the input ends.
     */
    public static void readUtf8AndAppend(@NotNull StreamingDataInput bytes,
                                         @NotNull Appendable appendable,
                                         @NotNull StopCharsTester tester)
            throws BufferUnderflowException, IOException, ClosedIllegalStateException {
        while (true) {
            int c = bytes.readUnsignedByte();
            // If the character read is a multi-byte UTF-8 character, rewind and break the loop.
            if (c >= 128) {
                bytes.readSkip(-1);
                break;
            }

            // Special handling for array classes like byte[] which are denoted as '[B'.
            if (c == '[' && bytes.peekUnsignedByte() == ']') {
                appendable.append((char) c);
                appendable.append((char) bytes.readUnsignedByte());
                if (bytes.readRemaining() == 0)
                    return;
                continue;
            }

            // If the stop character is encountered, return.
            if (tester.isStopChar(c, bytes.peekUnsignedByte()))
                return;
            appendable.append((char) c);
            if (bytes.readRemaining() == 0)
                return;
        }

        // Handle multi-byte UTF-8 characters
        for (int c; (c = bytes.readUnsignedByte()) >= 0; ) {
            switch (c >> 4) {
                // If the character is a 1-byte UTF-8 character (0xxxxxxx), append it as is.
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

                // If the character is a 2-byte UTF-8 character (110x xxxx 10xx xxxx), decode and append it.
                case 12:
                case 13: {
                    /* 110x xxxx 10xx xxxx */
                    int char2 = bytes.readUnsignedByte();
                    if ((char2 & 0xC0) != 0x80)
                        throw newUTFDataFormatException(char2);
                    int c2 = (char) (((c & 0x1F) << 6) |
                            (char2 & 0x3F));
                    if (tester.isStopChar(c2, bytes.peekUnsignedByte()))
                        return;
                    appendable.append((char) c2);
                    break;
                }

                // If the character is a 3-byte UTF-8 character (1110 xxxx 10xx xxxx 10xx xxxx), decode and append it.
                case 14: {
                    /* 1110 xxxx 10xx xxxx 10xx xxxx */
                    int char2 = bytes.readUnsignedByte();
                    int char3 = bytes.readUnsignedByte();

                    if (((char2 & 0xC0) != 0x80))
                        throw newUTFDataFormatException(char2);
                    if ((char3 & 0xC0) != 0x80)
                        throw newUTFDataFormatException(char3);
                    int c3 = (char) (((c & 0x0F) << 12) |
                            ((char2 & 0x3F) << 6) |
                            (char3 & 0x3F));
                    if (tester.isStopChar(c3, bytes.peekUnsignedByte()))
                        return;
                    appendable.append((char) c3);
                    break;
                }

                default:
                    // If the character does not match any valid UTF-8 pattern, throw an exception.
                    /* 10xx xxxx, 1111 xxxx */
                    throw newUTFDataFormatException(c);
            }
        }
    }

    private static UTFDataFormatException newUTFDataFormatException(final int c) {
        return new UTFDataFormatException(MALFORMED_INPUT_AROUND_BYTE + Integer.toHexString(c));
    }

    /**
     * Parses a sequence of 8-bit characters from the given Bytes input and appends them to a StringBuilder.
     *
     * @param bytes  the input Bytes to read from
     * @param sb     the StringBuilder to append the characters to
     * @param length the number of characters to read
     * @throws BufferUnderflowException    If there are not enough characters available in the input
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     */
    public static void parse8bit_SB1(@NotNull Bytes<?> bytes, @NotNull StringBuilder sb, @NonNegative int length)
            throws BufferUnderflowException, ClosedIllegalStateException {
        if (length > bytes.readRemaining())
            throw new BufferUnderflowException();
        @Nullable NativeBytesStore nbs = (NativeBytesStore) bytes.bytesStore();
        long offset = bytes.readPosition();
        int count = BytesInternal.parse8bit_SB1(offset, nbs, sb, length);
        bytes.readSkip(count);
    }

    /**
     * Parses a sequence of 8-bit characters from the given StreamingDataInput and appends them to an Appendable.
     *
     * @param bytes      the input StreamingDataInput to read from
     * @param appendable the Appendable to append the characters to
     * @param utflen     the number of characters to read
     * @throws BufferUnderflowException    If there are not enough characters available in the input
     * @throws IOException                 If an I/O error occurs
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     */
    public static void parse8bit(@NotNull StreamingDataInput bytes, Appendable appendable, @NonNegative int utflen)
            throws BufferUnderflowException, IOException, ClosedIllegalStateException {
        if (appendable instanceof StringBuilder && Jvm.maxDirectMemory() > 0) {
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

    /**
     * Appends a subsequence of the specified CharSequence to an Appendable.
     *
     * @param a     the Appendable to append the characters to
     * @param cs    the CharSequence to read characters from
     * @param start the starting index of the subsequence
     * @param len   the number of characters in the subsequence
     * @throws ArithmeticException         If an arithmetic error occurs
     * @throws BufferUnderflowException    If there are not enough characters available in the CharSequence
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws BufferOverflowException     If the Appendable cannot accept more characters
     */
    public static <C extends Appendable & CharSequence> void append(C a, CharSequence cs, @NonNegative long start, @NonNegative long len)
            throws ArithmeticException, BufferUnderflowException, ClosedIllegalStateException, BufferOverflowException {
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

    /**
     * Determines how many bytes {@code str} will occupy when encoded as UTF-8.
     */
    public static long findUtf8Length(@NotNull CharSequence str)
            throws IndexOutOfBoundsException {
        int strlen = str.length();
        long utflen = strlen;/* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            char c = str.charAt(i);
            if (c <= 0x007F) {
                continue;
            }
            utflen += (c <= 0x07FF) ? 1 : 2;
        }
        return utflen;
    }

    /**
     * Returns the number of bytes required to UTF-8 encode the supplied
     * {@code bytes} using the given {@code coder} representation.
     */
    @Java9
    public static long findUtf8Length(byte[] bytes, byte coder) {
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

    /**
     * Computes the UTF-8 byte length of the provided 8-bit encoded character array.
     */
    @Java9
    public static long findUtf8Length(byte[] chars) {
        int strlen = chars.length;
        long utflen = strlen; /* use charAt instead of copying String to char array */
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

    /**
     * Returns the UTF-8 byte length of a portion of {@code chars}.
     */
    public static long findUtf8Length(char[] chars, @NonNegative int offset, @NonNegative int length) {
        requireNonNull(chars);
        long utflen = length;
        for (int i = offset, end = offset + length; i < end; i++) {
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

    /**
     * Convenience overload of {@link #findUtf8Length(char[], int, int)} for the
     * whole array.
     */
    public static long findUtf8Length(char[] chars) {
        return findUtf8Length(chars, 0, chars.length);
    }
}
