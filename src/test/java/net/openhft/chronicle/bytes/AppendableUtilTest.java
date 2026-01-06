/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
public class AppendableUtilTest extends BytesTestCommon {

    @Test
    @DisplayName("setCharAt updates StringBuilder content at index")
    public void setCharAtWithStringBuilder() {
        StringBuilder sb = new StringBuilder("hello");
        AppendableUtil.setCharAt(sb, 1, 'a');
        assertEquals("hallo", sb.toString(),
                "setCharAt updates character in StringBuilder");
    }

    @Test
    @DisplayName("setCharAt updates StringBuilder with overflow checks")
    public void testSetCharAtStringBuilder() throws BufferOverflowException {
        StringBuilder sb = new StringBuilder("Hello");
        AppendableUtil.setCharAt(sb, 1, 'a');
        assertEquals("Hallo", sb.toString(),
                "setCharAt updates StringBuilder at index");
    }

    @Test
    @DisplayName("setLength truncates StringBuilder content to length")
    public void testSetLengthStringBuilder() {
        StringBuilder sb = new StringBuilder("Hello");
        AppendableUtil.setLength(sb, 3);
        assertEquals("Hel", sb.toString(),
                "setLength truncates StringBuilder to length 3");
    }

    @Test
    @DisplayName("append formats double into StringBuilder text output")
    public void testAppendDouble() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        assertEquals("3.14", sb.toString(),
                "append formats double value into StringBuilder");
    }

    @Test
    @DisplayName("append formats long into StringBuilder text output")
    public void testAppendLong() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 42L);
        assertEquals("42", sb.toString(),
                "append formats long value into StringBuilder");
    }

    @Test
    @DisplayName("setCharAt updates StringBuilder content with offset")
    public void testSetCharAtWithStringBuilder() throws BufferOverflowException {
        StringBuilder sb = new StringBuilder("Hello World");
        AppendableUtil.setCharAt(sb, 6, 'J');
        assertEquals("Hello Jorld", sb.toString(),
                "setCharAt updates StringBuilder at offset");
    }

    @Test
    @DisplayName("parseUtf8 writes expected characters to builder")
    public void testParseUtf8() throws BufferUnderflowException {
        BytesStore<?, byte[]> bs = BytesStore.from("Hello World");
        StringBuilder sb = new StringBuilder();
        AppendableUtil.parseUtf8(bs, sb, true, 11);
        assertEquals("Hello World", sb.toString(),
                "parseUtf8 writes expected StringBuilder content");
    }

    @Test
    @DisplayName("setLength truncates StringBuilder with longer input")
    public void testSetLengthWithStringBuilder() {
        StringBuilder sb = new StringBuilder("Hello World");
        AppendableUtil.setLength(sb, 5);
        assertEquals("Hello", sb.toString(),
                "setLength truncates StringBuilder to length 5 for longer input");
    }

    @Test
    @DisplayName("append formats double using StringBuilder append util")
    public void testAppendDoubleWithStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        assertEquals("3.14", sb.toString(),
                "append formats double into StringBuilder output via helper");
    }

    @Test
    @DisplayName("findUtf8Length counts bytes for byte array")
    public void testFindUtf8LengthByteArray() {
        byte[] bytes = "Hello World".getBytes(StandardCharsets.ISO_8859_1);
        long length = AppendableUtil.findUtf8Length(bytes);
        assertEquals(22, length,
                "findUtf8Length reports expected byte length");
    }

    @Test
    @DisplayName("findUtf8Length counts characters for char array")
    public void testFindUtf8LengthCharArray() {
        char[] chars = "Hello World".toCharArray();
        long length = AppendableUtil.findUtf8Length(chars);
        assertEquals(11, length,
                "findUtf8Length reports expected char length");
    }

    @Test
    @DisplayName("setCharAt rejects unsupported Appendable implementations safely")
    public void setCharAtWithUnsupportedAppendable() {
        Appendable appendable = new Appendable() {
            @Override
            public Appendable append(CharSequence csq) {
                return this;
            }
            @Override
            public Appendable append(CharSequence csq, int start, int end) {
                return this;
            }
            @Override
            public Appendable append(char c) {
                return this;
            }
        };

        assertThrows(IllegalArgumentException.class,
                () -> AppendableUtil.setCharAt(appendable, 1, 'a'),
                "setCharAt rejects unsupported Appendable implementation");
    }

    @Test
    @DisplayName("append formats double into StringBuilder output instance")
    public void appendDoubleToStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        assertEquals("3.14", sb.toString(),
                "append formats double into StringBuilder output for AppendableUtil");
    }

    @Test
    @DisplayName("append writes string into Appendable target")
    public void appendStringToAppendableAndCharSequence() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, "test");
        assertEquals("test", sb.toString(),
                "append writes string into Appendable destination");
    }

    @SuppressWarnings("rawtypes")
    @Test
    @DisplayName("setLength truncates StringBuilder and Bytes targets")
    public void setLength() {
        StringBuilder sb = new StringBuilder("hello world");
        AppendableUtil.setLength(sb, 5);
        assertEquals("hello", sb.toString(),
                "setLength truncates StringBuilder to length 5 for Bytes test");

        Bytes<?> b = Bytes.from("Hello World");
        AppendableUtil.setLength(b, 5);
        assertEquals("Hello", b.toString(),
                "setLength truncates Bytes to length 5");

        StringBuffer sb2 = new StringBuffer();
        assertThrows(IllegalArgumentException.class,
                () -> AppendableUtil.setLength(sb2, 0),
                "setLength rejects unsupported StringBuffer");
        b.releaseLast();
    }

    @Test
    @DisplayName("setCharAt updates StringBuilder and Bytes targets")
    public void setCharAt() {
        StringBuilder sb = new StringBuilder("hello world");
        Bytes<?> b = Bytes.allocateElasticOnHeap(16).append("Hello World");
        AppendableUtil.setCharAt(sb, 5, 'X');
        AppendableUtil.setCharAt(b, 5, 'X');
        assertEquals("helloXworld", sb.toString(),
                "setCharAt updates StringBuilder content for text");
        assertEquals("HelloXWorld", b.toString(),
                "setCharAt updates Bytes content");
        b.releaseLast();
    }

    @Test
    @DisplayName("AppendableUtil append(double) writes formatted value to Bytes")
    public void appendDoubleToBytes() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            AppendableUtil.append(b, 3.14159);
            assertTrue(b.toString().startsWith("3.14"),
                    "append formats double into Bytes output");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("AppendableUtil append(long) writes decimal value to Bytes")
    public void appendLongToBytes() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            AppendableUtil.append(b, 123456789L);
            assertEquals("123456789", b.toString(),
                    "append formats long into Bytes output");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("AppendableUtil append(long) rejects unsupported Appendable implementations")
    public void appendLongToUnsupportedAppendable() {
        Appendable unsupported = new Appendable() {
            @Override
            public Appendable append(CharSequence csq) {
                return this;
            }
            @Override
            public Appendable append(CharSequence csq, int start, int end) {
                return this;
            }
            @Override
            public Appendable append(char c) {
                return this;
            }
        };
        assertThrows(IllegalArgumentException.class,
                () -> AppendableUtil.append(unsupported, 42L),
                "append(long) rejects unsupported Appendable");
    }

    @Test
    @DisplayName("AppendableUtil append(double) rejects unsupported Appendable implementations")
    public void appendDoubleToUnsupportedAppendable() {
        Appendable unsupported = new Appendable() {
            @Override
            public Appendable append(CharSequence csq) {
                return this;
            }
            @Override
            public Appendable append(CharSequence csq, int start, int end) {
                return this;
            }
            @Override
            public Appendable append(char c) {
                return this;
            }
        };
        assertThrows(IllegalArgumentException.class,
                () -> AppendableUtil.append(unsupported, 3.14),
                "append(double) rejects unsupported Appendable");
    }

    @ParameterizedTest
    @DisplayName("findUtf8Length reports UTF-8 length for CharSequence input")
    @CsvSource({
            "hello, 5",
            "a, 1",
            "'', 0"
    })
    void findUtf8LengthCharSequence(String input, long expected) {
        assertEquals(expected, AppendableUtil.findUtf8Length(input),
                "findUtf8Length should return " + expected + " for input '" + input + "'");
    }

    @Test
    @DisplayName("findUtf8Length counts multi-byte UTF-8 characters correctly")
    void findUtf8LengthMultibyteChars() {
        // Euro sign (U+20AC) = 3 bytes in UTF-8
        String euro = "\u20AC";
        assertEquals(3, AppendableUtil.findUtf8Length(euro),
                "Euro sign requires 3 bytes in UTF-8");

        // 2-byte character (e.g., Latin Extended character)
        String twoByteChar = "\u00E9"; // e with acute
        assertEquals(2, AppendableUtil.findUtf8Length(twoByteChar),
                "Latin extended char requires 2 bytes");
    }

    @Test
    @DisplayName("findUtf8Length with byte array coder 0")
    void findUtf8LengthByteArrayCoderZero() {
        byte[] bytes = "hello".getBytes(StandardCharsets.ISO_8859_1);
        long length = AppendableUtil.findUtf8Length(bytes, (byte) 0);
        assertEquals(5, length,
                "findUtf8Length with coder 0 for ASCII");
    }

    @Test
    @DisplayName("findUtf8Length with byte array coder 0 and high bytes")
    void findUtf8LengthByteArrayCoderZeroHighBytes() {
        // Bytes > 0x7F require 2 bytes in UTF-8
        byte[] bytes = new byte[]{(byte) 0x80, (byte) 0xFF};
        long length = AppendableUtil.findUtf8Length(bytes, (byte) 0);
        assertEquals(4, length,
                "findUtf8Length counts extra bytes for high values");
    }

    @Test
    @DisplayName("findUtf8Length with byte array coder 1 (UTF-16)")
    void findUtf8LengthByteArrayCoderOne() {
        // UTF-16 encoding of 'AB' = 0x0041, 0x0042
        byte[] bytes = new byte[]{0x41, 0x00, 0x42, 0x00};
        long length = AppendableUtil.findUtf8Length(bytes, (byte) 1);
        assertEquals(2, length,
                "findUtf8Length with coder 1 for ASCII chars");
    }

    @Test
    @DisplayName("findUtf8Length with char array offset and length")
    void findUtf8LengthCharArrayWithOffset() {
        char[] chars = "hello world".toCharArray();
        long length = AppendableUtil.findUtf8Length(chars, 6, 5);
        assertEquals(5, length,
                "findUtf8Length with offset returns correct length");
    }

    @Test
    @DisplayName("AppendableUtil appends CharSequence subsequence to StringBuilder")
    void appendSubsequenceToStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, "hello world", 0, 5);
        assertEquals("hello", sb.toString(),
                "append should copy CharSequence subsequence into StringBuilder");
    }

    @Test
    @DisplayName("AppendableUtil appends Bytes subsequence to StringBuilder")
    void appendBytesSubsequenceToStringBuilder() {
        Bytes<?> source = Bytes.from("hello world");
        StringBuilder sb = new StringBuilder();
        try {
            AppendableUtil.append(sb, source, 0, 5);
            assertEquals("hello", sb.toString(),
                    "append should copy Bytes subsequence into StringBuilder");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("AppendableUtil appends CharSequence subsequence to Bytes target")
    void appendSubsequenceToBytes() {
        Bytes<?> target = Bytes.allocateElasticOnHeap(32);
        try {
            AppendableUtil.append(target, "hello world", 0, 5);
            assertEquals("hello", target.toString(),
                    "append should copy CharSequence subsequence into Bytes target");
        } finally {
            target.releaseLast();
        }
    }
}
