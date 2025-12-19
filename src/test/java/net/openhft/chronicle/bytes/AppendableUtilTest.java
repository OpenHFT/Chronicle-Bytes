/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class AppendableUtilTest extends BytesTestCommon {

    @Test
    public void setCharAtWithStringBuilder() {
        StringBuilder sb = new StringBuilder("hello");
        AppendableUtil.setCharAt(sb, 1, 'a');
        assertEquals("hallo", sb.toString(), "setCharAt should replace 'e' with 'a' in 'hello' to produce 'hallo'");
    }

    @Test
    public void testSetCharAtStringBuilder() throws BufferOverflowException {
        StringBuilder sb = new StringBuilder("Hello");
        AppendableUtil.setCharAt(sb, 1, 'a');
        assertEquals("Hallo", sb.toString(), "setCharAt should replace 'e' with 'a' in 'Hello' to produce 'Hallo'");
    }

    @Test
    public void testSetLengthStringBuilder() {
        StringBuilder sb = new StringBuilder("Hello");
        AppendableUtil.setLength(sb, 3);
        assertEquals("Hel", sb.toString(), "setLength(3) should truncate 'Hello' to 'Hel'");
    }

    @Test
    public void testAppendDouble() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        assertEquals("3.14", sb.toString(), "append(double) should append '3.14' to empty StringBuilder");
    }

    @Test
    public void testAppendLong() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 42L);
        assertEquals("42", sb.toString(), "append(long) should append '42' to empty StringBuilder");
    }

    @Test
    public void testSetCharAtWithStringBuilder() throws BufferOverflowException {
        StringBuilder sb = new StringBuilder("Hello World");
        AppendableUtil.setCharAt(sb, 6, 'J');
        assertEquals("Hello Jorld", sb.toString(), "setCharAt should replace 'W' with 'J' at index 6 to produce 'Hello Jorld'");
    }

    @Test
    public void testParseUtf8() throws BufferUnderflowException {
        BytesStore<?, byte[]> bs = BytesStore.from("Hello World");
        StringBuilder sb = new StringBuilder();
        AppendableUtil.parseUtf8(bs, sb, true, 11);
        assertEquals("Hello World", sb.toString(), "parseUtf8 should parse 11 bytes into 'Hello World'");
    }

    @Test
    public void testSetLengthWithStringBuilder() {
        StringBuilder sb = new StringBuilder("Hello World");
        AppendableUtil.setLength(sb, 5);
        assertEquals("Hello", sb.toString(), "setLength(5) should truncate 'Hello World' to 'Hello'");
    }

    @Test
    public void testAppendDoubleWithStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        assertEquals("3.14", sb.toString(), "append should convert double 3.14 to string '3.14'");
    }

    @Test
    public void testFindUtf8LengthByteArray() {
        byte[] bytes = "Hello World".getBytes(ISO_8859_1);
        long length = AppendableUtil.findUtf8Length(bytes);
        assertEquals(22, length, "findUtf8Length on byte array should return stop-bit encoded length (11 chars + 11 bytes overhead)");
    }

    @Test
    public void testFindUtf8LengthCharArray() {
        char[] chars = "Hello World".toCharArray();
        long length = AppendableUtil.findUtf8Length(chars);
        assertEquals(11, length, "findUtf8Length on char array should return character count");
    }

    @Test
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

        assertThrows(IllegalArgumentException.class, () -> AppendableUtil.setCharAt(appendable, 1, 'a'));
    }

    @Test
    public void appendDoubleToStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        assertEquals("3.14", sb.toString(), "append(3.14) should produce string '3.14' in StringBuilder");
    }

    @Test
    public void appendStringToAppendableAndCharSequence() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, "test");
        assertEquals("test", sb.toString(), "append(String) should append 'test' to StringBuilder");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void setLength() {
        StringBuilder sb = new StringBuilder("hello world");
        AppendableUtil.setLength(sb, 5);
        assertEquals("hello", sb.toString(), "setLength(5) on StringBuilder should truncate 'hello world' to 'hello'");

        Bytes<?> b = Bytes.from("Hello World");
        AppendableUtil.setLength(b, 5);
        assertEquals("Hello", b.toString(), "setLength(5) on Bytes should truncate 'Hello World' to 'Hello'");

        StringBuffer sb2 = new StringBuffer();
        try {
            assertThrows(IllegalArgumentException.class, () -> AppendableUtil.setLength(sb2, 0));
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void setCharAt() {
        StringBuilder sb = new StringBuilder("hello world");
        Bytes<?> b = Bytes.allocateElasticOnHeap(16).append("Hello World");
        AppendableUtil.setCharAt(sb, 5, 'X');
        AppendableUtil.setCharAt(b, 5, 'X');
        assertEquals("helloXworld", sb.toString(), "setCharAt(5, 'X') on StringBuilder should produce 'helloXworld'");
        assertEquals("HelloXWorld", b.toString(), "setCharAt(5, 'X') on Bytes should produce 'HelloXWorld'");
        b.releaseLast();
    }
}
