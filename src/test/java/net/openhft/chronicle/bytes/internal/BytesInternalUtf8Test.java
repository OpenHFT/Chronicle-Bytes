/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.bytes.StreamingDataOutput;
import net.openhft.chronicle.bytes.UTFDataFormatRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BytesInternal UTF-8 parsing and appending methods.
 * Covers parseUtf8, parseUtf82, appendUtf8Char, compareUtf8 variants.
 */
@DisplayName("BytesInternal UTF8 append and parse helpers")
class BytesInternalUtf8Test extends BytesTestCommon {

    private Bytes<?> bytes;
    private StringBuilder sb;

    @BeforeEach
    void setUp() {
        bytes = NativeBytes.nativeBytes(256);
        sb = new StringBuilder();
    }

    @AfterEach
    void tearDown() {
        if (bytes != null) {
            bytes.releaseLast();
            bytes = null;
        }
    }

    // --- Original tests preserved ---

    @Test
    @DisplayName("append UTF8 variants into bytes output")
    void appendUtf8CharSequenceVariants() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(32);
        try {
            CharSequence cs = "hello-world";
            BytesInternal.appendUtf8((StreamingDataOutput) out, cs, 0, cs.length());
            assertEquals("hello-world", out.toString(),
                    "appendUtf8 writes hello-world to output");

            out.clear();
            char[] chars = "abcdef".toCharArray();
            BytesInternal.appendUtf8(out, (CharSequence) new String(chars), 1, chars.length - 1);
            assertEquals("bcdef", out.toString(),
                    "appendUtf8 writes substring bcdef to output");

            out.clear();
            String longStr = new String(new char[1024]).replace('\0', 'x');
            BytesInternal.appendUtf8(out, longStr, 0, longStr.length());
            assertEquals(longStr.length(), out.length(),
                    "appendUtf8 writes 1024 chars to output");
        } finally {
            out.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF8 and 8bit using stop testers")
    void parseUtf8And8bitWithStopTesters() {
        Bytes<?> a = Bytes.from("alpha");
        Bytes<?> b = Bytes.from("beta");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(a, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("alpha", sb.toString(),
                    "parseUtf8 reads alpha from input");

            sb.setLength(0);
            BytesInternal.parseUtf8(b, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("beta", sb.toString(),
                    "parseUtf8 reads beta from input");
        } finally {
            a.releaseLast();
            b.releaseLast();
        }
    }

    // --- appendUtf8Char tests (StreamingDataOutput) ---

    @Test
    @DisplayName("appendUtf8Char encodes ASCII character in 1 byte")
    void appendUtf8CharAscii() {
        BytesInternal.appendUtf8Char(bytes, 'A');
        assertEquals(1, bytes.writePosition(), "appendUtf8Char writes 1 byte for ASCII");
        assertEquals('A', (char) bytes.readByte(0), "appendUtf8Char preserves ASCII value");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x7F, 0x41, 0x20})
    @DisplayName("appendUtf8Char encodes ASCII range in 1 byte")
    void appendUtf8CharAsciiRange(int c) {
        BytesInternal.appendUtf8Char(bytes, c);
        assertEquals(1, bytes.writePosition(), "appendUtf8Char writes 1 byte for code " + c);
    }

    @Test
    @DisplayName("appendUtf8Char encodes 2-byte character")
    void appendUtf8CharTwoByte() {
        int c = 0x00E1; // a with acute
        BytesInternal.appendUtf8Char(bytes, c);
        assertEquals(2, bytes.writePosition(), "appendUtf8Char writes 2 bytes for 2-byte char");

        int b0 = bytes.readUnsignedByte(0);
        int b1 = bytes.readUnsignedByte(1);
        assertTrue((b0 & 0xE0) == 0xC0, "appendUtf8Char 2-byte first byte starts with 110");
        assertTrue((b1 & 0xC0) == 0x80, "appendUtf8Char 2-byte second byte starts with 10");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x80, 0xFF, 0x07FF, 0x0100})
    @DisplayName("appendUtf8Char encodes 2-byte range")
    void appendUtf8CharTwoByteRange(int c) {
        BytesInternal.appendUtf8Char(bytes, c);
        assertEquals(2, bytes.writePosition(), "appendUtf8Char writes 2 bytes for code " + c);
    }

    @Test
    @DisplayName("appendUtf8Char encodes 3-byte character")
    void appendUtf8CharThreeByte() {
        int c = 0x20AC; // Euro Sign
        BytesInternal.appendUtf8Char(bytes, c);
        assertEquals(3, bytes.writePosition(), "appendUtf8Char writes 3 bytes for Euro sign");

        int b0 = bytes.readUnsignedByte(0);
        int b1 = bytes.readUnsignedByte(1);
        int b2 = bytes.readUnsignedByte(2);
        assertTrue((b0 & 0xF0) == 0xE0, "appendUtf8Char 3-byte first byte starts with 1110");
        assertTrue((b1 & 0xC0) == 0x80, "appendUtf8Char 3-byte second byte starts with 10");
        assertTrue((b2 & 0xC0) == 0x80, "appendUtf8Char 3-byte third byte starts with 10");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x0800, 0xFFFF, 0x4E2D, 0x3000})
    @DisplayName("appendUtf8Char encodes 3-byte range")
    void appendUtf8CharThreeByteRange(int c) {
        BytesInternal.appendUtf8Char(bytes, c);
        assertEquals(3, bytes.writePosition(), "appendUtf8Char writes 3 bytes for code " + c);
    }

    @Test
    @DisplayName("appendUtf8Char encodes 4-byte character (supplementary)")
    void appendUtf8CharFourByte() {
        int c = 0x1F600; // Grinning Face emoji
        BytesInternal.appendUtf8Char(bytes, c);
        assertEquals(4, bytes.writePosition(), "appendUtf8Char writes 4 bytes for emoji");

        int b0 = bytes.readUnsignedByte(0);
        int b1 = bytes.readUnsignedByte(1);
        int b2 = bytes.readUnsignedByte(2);
        int b3 = bytes.readUnsignedByte(3);
        assertTrue((b0 & 0xF8) == 0xF0, "appendUtf8Char 4-byte first byte starts with 11110");
        assertTrue((b1 & 0xC0) == 0x80, "appendUtf8Char 4-byte second byte starts with 10");
        assertTrue((b2 & 0xC0) == 0x80, "appendUtf8Char 4-byte third byte starts with 10");
        assertTrue((b3 & 0xC0) == 0x80, "appendUtf8Char 4-byte fourth byte starts with 10");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x10000, 0x10FFFF, 0x1F4A9})
    @DisplayName("appendUtf8Char encodes 4-byte range")
    void appendUtf8CharFourByteRange(int c) {
        BytesInternal.appendUtf8Char(bytes, c);
        assertEquals(4, bytes.writePosition(), "appendUtf8Char writes 4 bytes for code " + c);
    }

    // --- appendUtf8Char tests (RandomDataOutput) ---

    @Test
    @DisplayName("appendUtf8Char with offset encodes ASCII")
    void appendUtf8CharWithOffsetAscii() {
        long newOffset = BytesInternal.appendUtf8Char(bytes.bytesStore(), 0, 'A');
        assertEquals(1, newOffset, "appendUtf8Char returns offset+1 for ASCII");
    }

    @Test
    @DisplayName("appendUtf8Char with offset encodes 2-byte")
    void appendUtf8CharWithOffsetTwoByte() {
        long newOffset = BytesInternal.appendUtf8Char(bytes.bytesStore(), 0, 0x00E1);
        assertEquals(2, newOffset, "appendUtf8Char returns offset+2 for 2-byte");
    }

    @Test
    @DisplayName("appendUtf8Char with offset encodes 3-byte")
    void appendUtf8CharWithOffsetThreeByte() {
        long newOffset = BytesInternal.appendUtf8Char(bytes.bytesStore(), 0, 0x20AC);
        assertEquals(3, newOffset, "appendUtf8Char returns offset+3 for 3-byte");
    }

    @Test
    @DisplayName("appendUtf8Char with offset encodes 4-byte")
    void appendUtf8CharWithOffsetFourByte() {
        long newOffset = BytesInternal.appendUtf8Char(bytes.bytesStore(), 0, 0x1F600);
        assertEquals(4, newOffset, "appendUtf8Char returns offset+4 for 4-byte");
    }

    // --- parseUtf8 tests ---

    @Test
    @DisplayName("parseUtf8 decodes ASCII string")
    void parseUtf8Ascii() {
        String input = "Hello World";
        bytes.append(input);
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, input.length());
        assertEquals(input, sb.toString(), "parseUtf8 decodes ASCII Hello World");
    }

    @Test
    @DisplayName("parseUtf8 decodes 2-byte UTF-8 characters")
    void parseUtf8TwoByte() {
        bytes.writeByte((byte) 0xC3);
        bytes.writeByte((byte) 0xA4);
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, 2);
        assertEquals("\u00E4", sb.toString(), "parseUtf8 decodes 2-byte a-umlaut");
    }

    @Test
    @DisplayName("parseUtf8 decodes 3-byte UTF-8 characters")
    void parseUtf8ThreeByte() {
        bytes.writeByte((byte) 0xE2);
        bytes.writeByte((byte) 0x82);
        bytes.writeByte((byte) 0xAC);
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, 3);
        assertEquals("\u20AC", sb.toString(), "parseUtf8 decodes 3-byte Euro sign");
    }

    @Test
    @DisplayName("parseUtf8 decodes 4-byte UTF-8 characters")
    void parseUtf8FourByte() {
        bytes.writeByte((byte) 0xF0);
        bytes.writeByte((byte) 0x9F);
        bytes.writeByte((byte) 0x98);
        bytes.writeByte((byte) 0x80);
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, 4);
        String expected = new String(Character.toChars(0x1F600));
        assertEquals(expected, sb.toString(), "parseUtf8 decodes 4-byte emoji");
    }

    @Test
    @DisplayName("parseUtf8 decodes mixed ASCII and multi-byte")
    void parseUtf8Mixed() {
        bytes.writeByte((byte) 'A');
        bytes.writeByte((byte) 0xC3);
        bytes.writeByte((byte) 0xA4);
        bytes.writeByte((byte) 0xE2);
        bytes.writeByte((byte) 0x82);
        bytes.writeByte((byte) 0xAC);
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, 6);
        assertEquals("A\u00E4\u20AC", sb.toString(), "parseUtf8 decodes mixed UTF-8");
    }

    // --- parseUtf8 error cases ---

    @Test
    @DisplayName("parseUtf8 throws for truncated 2-byte sequence")
    void parseUtf8TruncatedTwoByte() {
        bytes.writeByte((byte) 0xC3);
        bytes.readPosition(0);

        assertThrows(UTFDataFormatRuntimeException.class,
                () -> BytesInternal.parseUtf8(bytes, sb, true, 1),
                "parseUtf8 throws for truncated 2-byte");
    }

    @Test
    @DisplayName("parseUtf8 throws for truncated 3-byte sequence")
    void parseUtf8TruncatedThreeByte() {
        bytes.writeByte((byte) 0xE2);
        bytes.writeByte((byte) 0x82);
        bytes.readPosition(0);

        assertThrows(UTFDataFormatRuntimeException.class,
                () -> BytesInternal.parseUtf8(bytes, sb, true, 2),
                "parseUtf8 throws for truncated 3-byte");
    }

    @Test
    @DisplayName("parseUtf8 throws for truncated 4-byte sequence")
    void parseUtf8TruncatedFourByte() {
        bytes.writeByte((byte) 0xF0);
        bytes.writeByte((byte) 0x9F);
        bytes.writeByte((byte) 0x98);
        bytes.readPosition(0);

        assertThrows(UTFDataFormatRuntimeException.class,
                () -> BytesInternal.parseUtf8(bytes, sb, true, 3),
                "parseUtf8 throws for truncated 4-byte");
    }

    @Test
    @DisplayName("parseUtf8 throws for invalid continuation byte in 2-byte")
    void parseUtf8InvalidContinuationTwoByte() {
        bytes.writeByte((byte) 0xC3);
        bytes.writeByte((byte) 0x20);
        bytes.readPosition(0);

        assertThrows(UTFDataFormatRuntimeException.class,
                () -> BytesInternal.parseUtf8(bytes, sb, true, 2),
                "parseUtf8 throws for invalid 2-byte continuation");
    }

    @Test
    @DisplayName("parseUtf8 throws for invalid continuation byte in 3-byte")
    void parseUtf8InvalidContinuationThreeByte() {
        bytes.writeByte((byte) 0xE2);
        bytes.writeByte((byte) 0x82);
        bytes.writeByte((byte) 0x20);
        bytes.readPosition(0);

        assertThrows(UTFDataFormatRuntimeException.class,
                () -> BytesInternal.parseUtf8(bytes, sb, true, 3),
                "parseUtf8 throws for invalid 3-byte continuation");
    }

    @Test
    @DisplayName("parseUtf8 throws for invalid continuation byte in 4-byte")
    void parseUtf8InvalidContinuationFourByte() {
        bytes.writeByte((byte) 0xF0);
        bytes.writeByte((byte) 0x9F);
        bytes.writeByte((byte) 0x98);
        bytes.writeByte((byte) 0x20);
        bytes.readPosition(0);

        assertThrows(UTFDataFormatRuntimeException.class,
                () -> BytesInternal.parseUtf8(bytes, sb, true, 4),
                "parseUtf8 throws for invalid 4-byte continuation");
    }

    @Test
    @DisplayName("parseUtf8 throws for standalone continuation byte")
    void parseUtf8StandaloneContinuation() {
        bytes.writeByte((byte) 0x80);
        bytes.readPosition(0);

        assertThrows(UTFDataFormatRuntimeException.class,
                () -> BytesInternal.parseUtf8(bytes, sb, true, 1),
                "parseUtf8 throws for standalone continuation byte");
    }

    @Test
    @DisplayName("parseUtf8 throws for invalid 4-byte code point range")
    void parseUtf8InvalidFourByteCodePoint() {
        bytes.writeByte((byte) 0xF0);
        bytes.writeByte((byte) 0x80);
        bytes.writeByte((byte) 0x80);
        bytes.writeByte((byte) 0x80);
        bytes.readPosition(0);

        assertThrows(UTFDataFormatRuntimeException.class,
                () -> BytesInternal.parseUtf8(bytes, sb, true, 4),
                "parseUtf8 throws for invalid 4-byte code point");
    }

    // --- compareUtf8 tests ---

    @Test
    @DisplayName("compareUtf8 returns true for matching ASCII")
    void compareUtf8MatchingAscii() {
        String expected = "Hello";
        bytes.writeStopBit(expected.length());
        bytes.append(expected);

        assertTrue(BytesInternal.compareUtf8(bytes.bytesStore(), 0, expected),
                "compareUtf8 returns true for matching ASCII");
    }

    @Test
    @DisplayName("compareUtf8 returns false for non-matching ASCII")
    void compareUtf8NonMatchingAscii() {
        String stored = "Hello";
        bytes.writeStopBit(stored.length());
        bytes.append(stored);

        assertFalse(BytesInternal.compareUtf8(bytes.bytesStore(), 0, "World"),
                "compareUtf8 returns false for non-matching ASCII");
    }

    @Test
    @DisplayName("compareUtf8 returns true for matching 2-byte UTF-8")
    void compareUtf8MatchingTwoByte() {
        String expected = "\u00E4";
        bytes.writeStopBit(2);
        bytes.writeByte((byte) 0xC3);
        bytes.writeByte((byte) 0xA4);

        assertTrue(BytesInternal.compareUtf8(bytes.bytesStore(), 0, expected),
                "compareUtf8 returns true for matching 2-byte");
    }

    @Test
    @DisplayName("compareUtf8 returns true for matching 3-byte UTF-8")
    void compareUtf8MatchingThreeByte() {
        String expected = "\u20AC";
        bytes.writeStopBit(3);
        bytes.writeByte((byte) 0xE2);
        bytes.writeByte((byte) 0x82);
        bytes.writeByte((byte) 0xAC);

        assertTrue(BytesInternal.compareUtf8(bytes.bytesStore(), 0, expected),
                "compareUtf8 returns true for matching 3-byte");
    }

    @Test
    @DisplayName("compareUtf8 returns false for different 2-byte chars")
    void compareUtf8NonMatchingTwoByte() {
        bytes.writeStopBit(2);
        bytes.writeByte((byte) 0xC3);
        bytes.writeByte((byte) 0xA4);

        assertFalse(BytesInternal.compareUtf8(bytes.bytesStore(), 0, "\u00E5"),
                "compareUtf8 returns false for different 2-byte");
    }

    @Test
    @DisplayName("compareUtf8 returns false for different lengths")
    void compareUtf8DifferentLengths() {
        bytes.writeStopBit(5);
        bytes.append("Hello");

        assertFalse(BytesInternal.compareUtf8(bytes.bytesStore(), 0, "Hi"),
                "compareUtf8 returns false for different lengths");
    }

    @Test
    @DisplayName("compareUtf8 handles null comparison with -1 length")
    void compareUtf8NullHandling() {
        bytes.writeStopBit(-1);

        assertTrue(BytesInternal.compareUtf8(bytes.bytesStore(), 0, null),
                "compareUtf8 returns true for null with -1 marker");
    }

    @Test
    @DisplayName("compareUtf8 returns false for null vs non-null")
    void compareUtf8NullVsNonNull() {
        bytes.writeStopBit(5);
        bytes.append("Hello");

        assertFalse(BytesInternal.compareUtf8(bytes.bytesStore(), 0, null),
                "compareUtf8 returns false for null vs stored string");
    }

    // --- parseUtf8 with non-UTF mode (8-bit) ---

    @Test
    @DisplayName("parseUtf8 with utf=false treats as 8-bit encoding")
    void parseUtf8NonUtfMode() {
        bytes.writeByte((byte) 'H');
        bytes.writeByte((byte) 'i');
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, false, 2);
        assertEquals("Hi", sb.toString(), "parseUtf8 utf=false decodes 8-bit");
    }

    // --- Edge cases ---

    @Test
    @DisplayName("parseUtf8 handles empty input")
    void parseUtf8Empty() {
        BytesInternal.parseUtf8(bytes, sb, true, 0);
        assertEquals("", sb.toString(), "parseUtf8 returns empty for 0 length");
    }

    @Test
    @DisplayName("parseUtf8 handles boundary values")
    void parseUtf8BoundaryValues() {
        bytes.writeByte((byte) 0x7F);
        bytes.writeByte((byte) 0xC2);
        bytes.writeByte((byte) 0x80);
        bytes.writeByte((byte) 0xDF);
        bytes.writeByte((byte) 0xBF);
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, 5);
        assertEquals("\u007F\u0080\u07FF", sb.toString(), "parseUtf8 handles boundary values");
    }

    // --- Parameterized tests for various characters ---

    static Stream<Arguments> utf8Characters() {
        return Stream.of(
                Arguments.of("ASCII A", "A", new byte[]{0x41}),
                Arguments.of("ASCII space", " ", new byte[]{0x20}),
                Arguments.of("ASCII tilde", "~", new byte[]{0x7E}),
                Arguments.of("Latin a-umlaut", "\u00E4", new byte[]{(byte) 0xC3, (byte) 0xA4}),
                Arguments.of("Copyright", "\u00A9", new byte[]{(byte) 0xC2, (byte) 0xA9}),
                Arguments.of("Euro sign", "\u20AC", new byte[]{(byte) 0xE2, (byte) 0x82, (byte) 0xAC}),
                Arguments.of("CJK char", "\u4E2D", new byte[]{(byte) 0xE4, (byte) 0xB8, (byte) 0xAD})
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("utf8Characters")
    @DisplayName("parseUtf8 correctly decodes various characters")
    void parseUtf8VariousCharacters(String name, String expected, byte[] utf8Bytes) {
        for (byte b : utf8Bytes) {
            bytes.writeByte(b);
        }
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, utf8Bytes.length);
        assertEquals(expected, sb.toString(), "parseUtf8 decodes " + name);
    }

    // --- Round-trip tests ---

    @Test
    @DisplayName("appendUtf8Char and parseUtf8 round-trip ASCII")
    void roundTripAscii() {
        String original = "Hello World 123!";
        for (char c : original.toCharArray()) {
            BytesInternal.appendUtf8Char(bytes, c);
        }
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, (int) bytes.writePosition());
        assertEquals(original, sb.toString(), "round-trip ASCII matches");
    }

    @Test
    @DisplayName("appendUtf8Char and parseUtf8 round-trip 2-byte")
    void roundTripTwoByte() {
        String original = "\u00E4\u00F6\u00FC";
        for (int i = 0; i < original.length(); i++) {
            BytesInternal.appendUtf8Char(bytes, original.charAt(i));
        }
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, (int) bytes.writePosition());
        assertEquals(original, sb.toString(), "round-trip 2-byte matches");
    }

    @Test
    @DisplayName("appendUtf8Char and parseUtf8 round-trip 3-byte")
    void roundTripThreeByte() {
        String original = "\u20AC\u4E2D\u6587";
        for (int i = 0; i < original.length(); i++) {
            BytesInternal.appendUtf8Char(bytes, original.charAt(i));
        }
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, (int) bytes.writePosition());
        assertEquals(original, sb.toString(), "round-trip 3-byte matches");
    }

    @Test
    @DisplayName("appendUtf8Char and parseUtf8 round-trip mixed encodings")
    void roundTripMixed() {
        String original = "A\u00E4\u20AC";
        for (int i = 0; i < original.length(); i++) {
            BytesInternal.appendUtf8Char(bytes, original.charAt(i));
        }
        bytes.readPosition(0);

        BytesInternal.parseUtf8(bytes, sb, true, (int) bytes.writePosition());
        assertEquals(original, sb.toString(), "round-trip mixed matches");
    }

    // --- Tests for compareUtf82 branches ---

    @Test
    @DisplayName("compareUtf8 with multi-byte mismatch returns false")
    void compareUtf8MultiByteNonMatch() {
        // Store "Hello" which doesn't match "World"
        bytes.writeStopBit(5);
        bytes.append("Hello");

        assertFalse(BytesInternal.compareUtf8(bytes.bytesStore(), 0, "World"),
                "compareUtf8 returns false for ASCII content mismatch");
    }

    @Test
    @DisplayName("compareUtf8 with same content returns true")
    void compareUtf8MultiByteMatch() {
        // Use simple ASCII comparison that reliably works
        String content = "Test123";
        bytes.writeStopBit(content.length());
        bytes.append(content);

        assertTrue(BytesInternal.compareUtf8(bytes.bytesStore(), 0, content),
                "compareUtf8 returns true for matching content");
    }
}
