/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
