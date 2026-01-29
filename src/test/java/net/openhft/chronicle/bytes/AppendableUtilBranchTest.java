/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch coverage tests for AppendableUtil formatting, parsing, UTF-8 handling,
 * and unsupported Appendable errors because correct handling of edge cases is required
 * to avoid data corruption and exceptions in production text processing.
 *
 * <p>Exercises setCharAt, setLength, and parseUtf8 in order to verify all branches
 * are covered, to avoid silent failures when edge-case inputs are provided.
 */
@SuppressWarnings("deprecation")
@DisplayName("AppendableUtilBranch - branch coverage for formatting, parsing, and UTF-8")
class AppendableUtilBranchTest extends BytesTestCommon {

    @Test
    @DisplayName("setCharAt with StringBuilder should modify character at index due to direct access")
    void setCharAtStringBuilder() {
        // StringBuilder supports setCharAt natively
        StringBuilder sb = new StringBuilder("Hello");
        AppendableUtil.setCharAt(sb, 0, 'J');
        assertEquals("Jello", sb.toString(),
                "setCharAt should modify the first character in StringBuilder");
    }

    @Test
    @DisplayName("setCharAt with Bytes should modify underlying storage despite wrapper abstraction")
    void setCharAtBytes() {
            // Elastic heap allocation to verify in-place modification
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
            try {
                bytes.append("Hello");
                AppendableUtil.setCharAt(bytes, 0, 'J');
                assertEquals((byte) 'J', bytes.readByte(0),
                        "setCharAt should modify the underlying storage at the given index");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("setCharAt with unsupported type should throw despite valid index")
    void setCharAtUnsupportedType() {
        // StringWriter does not support random access modification
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> AppendableUtil.setCharAt(writer, 0, 'X'),
                "setCharAt should reject unsupported Appendable types");
    }

    @Test
    @DisplayName("setLength with StringBuilder should truncate or extend")
    void setLengthStringBuilder() {
        StringBuilder sb = new StringBuilder("Hello World");
        AppendableUtil.setLength(sb, 5);
        assertEquals("Hello", sb.toString(),
                "setLength should truncate StringBuilder to the new length");
    }

    @Test
    @DisplayName("setLength with Bytes should adjust read remaining")
    void setLengthBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
            AppendableUtil.setLength(bytes, 5);
            assertEquals(5, bytes.readRemaining(),
                    "setLength should set read remaining to 5 bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("setLength with unsupported type should throw IllegalArgumentException")
    void setLengthUnsupportedType() {
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> AppendableUtil.setLength(writer, 5),
                "setLength should reject unsupported Appendable types");
    }

    @Test
    @DisplayName("append(double) with StringBuilder should format number")
    void appendDoubleStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        String result = sb.toString();
        assertTrue(result.contains("3.14"),
                "append(double) should include 3.14 in StringBuilder: " + result);
    }

    @Test
    @DisplayName("append(double) with Bytes should format number")
    void appendDoubleBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            AppendableUtil.append(bytes, 3.14);
            String result = bytes.toString();
            assertTrue(result.contains("3.14"),
                    "append(double) should include 3.14 in Bytes output: " + result);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append(double) with unsupported type should throw IllegalArgumentException")
    void appendDoubleUnsupportedType() {
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> AppendableUtil.append(writer, 3.14),
                "append(double) should reject unsupported Appendable types");
    }

    @Test
    @DisplayName("append(long) with StringBuilder should format number")
    void appendLongStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 12345L);
        assertEquals("12345", sb.toString(),
                "append(long) should format 12345 in StringBuilder");
    }

    @Test
    @DisplayName("append(long) with Bytes should format number")
    void appendLongBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            AppendableUtil.append(bytes, 12345L);
            String result = bytes.toString();
            assertEquals("12345", result,
                    "append(long) should format 12345 in Bytes output");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append(long) with unsupported type should throw IllegalArgumentException")
    void appendLongUnsupportedType() {
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> AppendableUtil.append(writer, 12345L),
                "append(long) should reject unsupported Appendable types");
    }

    @Test
    @DisplayName("append(String) with StringBuilder should append literal Hello text")
    void appendStringToStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, "Hello");
        assertEquals("Hello", sb.toString(),
                "append(String) should append Hello to StringBuilder");
    }

    @Test
    @DisplayName("read8bitAndAppend should read until stop character")
    void read8bitAndAppendStopsAtStopChar() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.write("Hello World".getBytes());
            StringBuilder sb = new StringBuilder();
            bytes.readPosition(0);
            AppendableUtil.read8bitAndAppend(bytes, sb, (ch, next) -> ch == ' ');
            assertEquals("Hello", sb.toString(),
                    "read8bitAndAppend should stop at the space character");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("read8bitAndAppend should read to end if no stop character")
    void read8bitAndAppendReadsToEnd() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.write("Hello".getBytes());
            StringBuilder sb = new StringBuilder();
            bytes.readPosition(0);
            AppendableUtil.read8bitAndAppend(bytes, sb, (ch, next) -> false);
            assertEquals("Hello", sb.toString(),
                    "read8bitAndAppend should read entire content without a stop");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readUTFAndAppend should copy ASCII characters into StringBuilder")
    void readUTFAndAppendAscii() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.write("Hello".getBytes());
            StringBuilder sb = new StringBuilder();
            bytes.readPosition(0);
            AppendableUtil.readUTFAndAppend(bytes, sb, (ch, next) -> false);
            assertEquals("Hello", sb.toString(),
                    "readUTFAndAppend should read ASCII characters correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readUtf8AndAppend should handle array class notation []")
    void readUtf8AndAppendArrayClass() throws IOException {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.write(new byte[]{'[', ']', 'B'});
            StringBuilder sb = new StringBuilder();
            bytes.readPosition(0);
            AppendableUtil.readUtf8AndAppend(bytes, sb, (ch, next) -> ch == 'B');
            assertEquals("[]", sb.toString(),
                    "readUtf8AndAppend should read array class notation");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readUtf8AndAppend should handle 2-byte UTF-8")
    void readUtf8AndAppendTwoByteUtf8() throws IOException {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            // UTF-8 encoding of 'e' (U+00E9, requires 2 bytes: C3 A9)
            bytes.write(new byte[]{(byte) 0xC3, (byte) 0xA9});
            StringBuilder sb = new StringBuilder();
            bytes.readPosition(0);
            AppendableUtil.readUtf8AndAppend(bytes, sb, (ch, next) -> false);
            assertEquals("\u00E9", sb.toString(),
                    "readUtf8AndAppend should decode 2-byte UTF-8 sequence");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readUtf8AndAppend should handle 3-byte UTF-8")
    void readUtf8AndAppendThreeByteUtf8() throws IOException {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            // UTF-8 encoding of Euro sign U+20AC (3 bytes: E2 82 AC)
            bytes.write(new byte[]{(byte) 0xE2, (byte) 0x82, (byte) 0xAC});
            StringBuilder sb = new StringBuilder();
            bytes.readPosition(0);
            AppendableUtil.readUtf8AndAppend(bytes, sb, (ch, next) -> false);
            assertEquals("\u20AC", sb.toString(),
                    "readUtf8AndAppend should decode 3-byte UTF-8 sequence");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("findUtf8Length(CharSequence) should calculate ASCII length")
    void findUtf8LengthAscii() {
        assertEquals(5, AppendableUtil.findUtf8Length("Hello"),
                "findUtf8Length should count ASCII characters as 1 byte each");
    }

    @Test
    @DisplayName("findUtf8Length(CharSequence) should calculate 2-byte char length")
    void findUtf8LengthTwoByte() {
        // U+00E9 requires 2 bytes in UTF-8
        assertEquals(2, AppendableUtil.findUtf8Length("\u00E9"),
                "findUtf8Length should count U+00E9 as 2 bytes");
    }

    @Test
    @DisplayName("findUtf8Length(CharSequence) should calculate 3-byte char length")
    void findUtf8LengthThreeByte() {
        // U+20AC (Euro) requires 3 bytes in UTF-8
        assertEquals(3, AppendableUtil.findUtf8Length("\u20AC"),
                "findUtf8Length should count U+20AC as 3 bytes");
    }

    @Test
    @DisplayName("findUtf8Length(CharSequence) should handle surrogate pairs")
    void findUtf8LengthSurrogatePair() {
        // U+1F600 (grinning face emoji) requires 4 bytes in UTF-8
        String emoji = "\uD83D\uDE00"; // Surrogate pair
        assertEquals(4, AppendableUtil.findUtf8Length(emoji),
                "findUtf8Length should count surrogate pairs as 4 bytes");
    }

    @ParameterizedTest
    @CsvSource({
            "'Hello', 5",
            "'', 0",
            "'Test123', 7"
    })
    @DisplayName("findUtf8Length(CharSequence) should handle various ASCII strings")
    void findUtf8LengthVariousAscii(String input, int expected) {
        assertEquals(expected, AppendableUtil.findUtf8Length(input),
                "findUtf8Length should match expected length for input");
    }

    @Test
    @DisplayName("findUtf8Length(byte[], coder) with coder 0 should count bytes")
    void findUtf8LengthBytesCoderZero() {
        byte[] bytes = new byte[]{'H', 'e', 'l', 'l', 'o'};
        assertEquals(5, AppendableUtil.findUtf8Length(bytes, (byte) 0),
                "findUtf8Length should count ASCII bytes as 1 byte each");
    }

    @Test
    @DisplayName("findUtf8Length(byte[], coder) with coder 0 and high bytes")
    void findUtf8LengthBytesCoderZeroHighBytes() {
        // Bytes > 0x7F require 2 bytes in UTF-8
        byte[] bytes = new byte[]{(byte) 0x80, (byte) 0x81};
        assertEquals(4, AppendableUtil.findUtf8Length(bytes, (byte) 0),
                "findUtf8Length should count high bytes as 2 UTF-8 bytes");
    }

    @Test
    @DisplayName("findUtf8Length(byte[], coder) with coder 1 should decode UTF-16")
    void findUtf8LengthBytesCoderOne() {
        // UTF-16LE encoding of 'AB' (2 chars = 4 bytes: 41 00 42 00)
        byte[] bytes = new byte[]{0x41, 0x00, 0x42, 0x00};
        assertEquals(2, AppendableUtil.findUtf8Length(bytes, (byte) 1),
                "findUtf8Length should map two ASCII chars to 2 UTF-8 bytes");
    }

    @Test
    @DisplayName("findUtf8Length(byte[], coder) with coder 1 and 2-byte chars")
    void findUtf8LengthBytesCoderOneTwoByte() {
        // UTF-16LE encoding of U+00E9 (E9 00)
        byte[] bytes = new byte[]{(byte) 0xE9, 0x00};
        assertEquals(2, AppendableUtil.findUtf8Length(bytes, (byte) 1),
                "findUtf8Length should count U+00E9 as 2 UTF-8 bytes");
    }

    @Test
    @DisplayName("findUtf8Length(byte[], coder) with coder 1 and 3-byte chars")
    void findUtf8LengthBytesCoderOneThreeByte() {
        // UTF-16LE encoding of U+20AC (AC 20)
        byte[] bytes = new byte[]{(byte) 0xAC, 0x20};
        assertEquals(3, AppendableUtil.findUtf8Length(bytes, (byte) 1),
                "findUtf8Length should count U+20AC as 3 UTF-8 bytes");
    }

    @Test
    @DisplayName("findUtf8Length(byte[]) should handle ASCII bytes")
    void findUtf8LengthSingleByteArrayAscii() {
        byte[] bytes = new byte[]{'H', 'e', 'l', 'l', 'o'};
        long length = AppendableUtil.findUtf8Length(bytes);
        assertTrue(length >= 5, "findUtf8Length(byte[]) should be >= 5, was " + length);
    }

    @Test
    @DisplayName("findUtf8Length(byte[]) should stop at the first null byte terminator")
    void findUtf8LengthSingleByteArrayWithNull() {
        byte[] bytes = new byte[]{'H', 'e', 0, 'l', 'o'};
        long length = AppendableUtil.findUtf8Length(bytes);
        assertTrue(length >= 2, "findUtf8Length(byte[]) should count bytes before null, was " + length);
    }

    @Test
    @DisplayName("findUtf8Length(byte[]) should handle multi-byte sequences")
    void findUtf8LengthSingleByteArrayMultiByte() {
        // Simulate various UTF-8 lead bytes
        byte[] bytes = new byte[]{(byte) 0xC0, 0x41}; // 2-byte sequence start
        long length = AppendableUtil.findUtf8Length(bytes);
        assertTrue(length > 0, "findUtf8Length(byte[]) should be positive for multibyte input, was " + length);
    }

    @Test
    @DisplayName("findUtf8Length(char[]) should handle ASCII chars")
    void findUtf8LengthCharArrayAscii() {
        char[] chars = {'H', 'e', 'l', 'l', 'o'};
        assertEquals(5, AppendableUtil.findUtf8Length(chars),
                "findUtf8Length should count ASCII chars as 1 byte each");
    }

    @Test
    @DisplayName("findUtf8Length(char[], offset, length) should handle partial array")
    void findUtf8LengthCharArrayPartial() {
        char[] chars = {'H', 'e', 'l', 'l', 'o'};
        assertEquals(3, AppendableUtil.findUtf8Length(chars, 1, 3),
                "findUtf8Length should count 3 chars starting at offset 1");
    }

    @Test
    @DisplayName("findUtf8Length(char[]) should handle 2-byte chars")
    void findUtf8LengthCharArrayTwoByte() {
        char[] chars = {'\u00E9'}; // 2-byte UTF-8
        assertEquals(2, AppendableUtil.findUtf8Length(chars),
                "findUtf8Length(char[]) should count U+00E9 as 2 bytes");
    }

    @Test
    @DisplayName("findUtf8Length(char[]) should handle 3-byte chars")
    void findUtf8LengthCharArrayThreeByte() {
        char[] chars = {'\u20AC'}; // 3-byte UTF-8
        assertEquals(3, AppendableUtil.findUtf8Length(chars),
                "findUtf8Length(char[]) should count U+20AC as 3 bytes");
    }

    @Test
    @DisplayName("findUtf8Length(char[]) should handle surrogate pairs")
    void findUtf8LengthCharArraySurrogatePair() {
        char[] chars = {'\uD83D', '\uDE00'}; // Surrogate pair for emoji
        assertEquals(4, AppendableUtil.findUtf8Length(chars),
                "findUtf8Length(char[]) should count surrogate pairs as 4 bytes");
    }

    @Test
    @DisplayName("append(CharSequence) to StringBuilder from Bytes")
    void appendCharSequenceToStringBuilderFromBytes() {
        StringBuilder sb = new StringBuilder();
        Bytes<?> source = Bytes.allocateElasticOnHeap(16);
        try {
            source.append("Hello");
            AppendableUtil.append(sb, source, 0, 5);
            assertEquals("Hello", sb.toString(),
                    "append(CharSequence) should append bytes content to StringBuilder");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("append(CharSequence) to StringBuilder from String")
    void appendCharSequenceToStringBuilderFromString() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, "Hello World", 0, 5);
        assertEquals("Hello", sb.toString(),
                "append(CharSequence) should append substring Hello to StringBuilder");
    }

    @Test
    @DisplayName("append(CharSequence) to Bytes should append substring from input")
    void appendCharSequenceToBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            AppendableUtil.append(bytes, "Hello World", 0, 5);
            String result = bytes.toString();
            assertEquals("Hello", result,
                    "append(CharSequence) should append substring Hello to Bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append(CharSequence) to unsupported type should throw")
    void appendCharSequenceToUnsupportedType() {
        // Create a custom CharSequence & Appendable that is neither StringBuilder nor Bytes
        assertThrows(UnsupportedOperationException.class, () -> {
            AppendableUtil.append(new CustomAppendableCharSequence(), "Test", 0, 4);
        }, "append(CharSequence) should reject unsupported Appendable types");
    }

    @Test
    @DisplayName("parse8bit should handle StringBuilder with NativeBytesStore")
    void parse8bitStringBuilderNativeBytes() throws IOException {
        Bytes<?> bytes = Bytes.allocateElasticDirect(32);
        try {
            bytes.write("Hello".getBytes());
            bytes.readPosition(0);
            StringBuilder sb = new StringBuilder();
            AppendableUtil.parse8bit(bytes, sb, 5);
            assertEquals("Hello", sb.toString(),
                    "parse8bit should parse 8-bit chars into StringBuilder");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("parse8bit should handle StringBuilder with HeapBytesStore")
    void parse8bitStringBuilderHeapBytes() throws IOException {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.write("World".getBytes());
            bytes.readPosition(0);
            StringBuilder sb = new StringBuilder();
            AppendableUtil.parse8bit(bytes, sb, 5);
            assertEquals("World", sb.toString(),
                    "parse8bit should parse 8-bit chars from heap bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("parse8bit should handle non-StringBuilder Appendable")
    void parse8bitOtherAppendable() throws IOException {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.write("Test".getBytes());
            bytes.readPosition(0);
            StringWriter writer = new StringWriter();
            AppendableUtil.parse8bit(bytes, writer, 4);
            assertEquals("Test", writer.toString(),
                    "parse8bit should parse into non-StringBuilder Appendable");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("parseUtf8 should decode UTF-8 to StringBuilder")
    void parseUtf8ToStringBuilder() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.write("Hello".getBytes());
            StringBuilder sb = new StringBuilder();
            AppendableUtil.parseUtf8(bytes, sb, true, 5);
            assertEquals("Hello", sb.toString(),
                    "parseUtf8 should decode UTF-8 into StringBuilder");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("parseUtf8 should decode UTF-8 content World from BytesStore into StringBuilder")
    void parseUtf8As8bitToStringBuilder() {
        // Test parseUtf8 with NativeBytesStore - uses position 0 of the store
        Bytes<?> bytes = Bytes.allocateElasticDirect(32);
        try {
            bytes.append("World");
            BytesStore<?, ?> store = bytes.bytesStore();
            StringBuilder sb = new StringBuilder();
            // parseUtf8 in UTF mode (utf=true)
            AppendableUtil.parseUtf8(store, sb, true, 5);
            assertEquals("World", sb.toString(),
                    "parseUtf8 should decode UTF-8 content from the underlying BytesStore");
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * Custom implementation for testing unsupported type handling.
     */
    static class CustomAppendableCharSequence implements Appendable, CharSequence {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public Appendable append(CharSequence csq) {
            sb.append(csq);
            return this;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            sb.append(csq, start, end);
            return this;
        }

        @Override
        public Appendable append(char c) {
            sb.append(c);
            return this;
        }

        @Override
        public int length() {
            return sb.length();
        }

        @Override
        public char charAt(int index) {
            return sb.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return sb.subSequence(start, end);
        }
    }
}
