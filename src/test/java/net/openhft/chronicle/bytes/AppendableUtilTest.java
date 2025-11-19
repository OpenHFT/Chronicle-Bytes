/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.*;

public class AppendableUtilTest extends BytesTestCommon {

    @Test
    public void setCharAtWithStringBuilder() {
        StringBuilder sb = new StringBuilder("hello");
        AppendableUtil.setCharAt(sb, 1, 'a');
        assertEquals("hallo", sb.toString());
    }

    @Test
    public void testSetCharAtStringBuilder() throws BufferOverflowException {
        StringBuilder sb = new StringBuilder("Hello");
        AppendableUtil.setCharAt(sb, 1, 'a');
        assertEquals("Hallo", sb.toString());
    }

    @Test
    public void testSetLengthStringBuilder() {
        StringBuilder sb = new StringBuilder("Hello");
        AppendableUtil.setLength(sb, 3);
        assertEquals("Hel", sb.toString());
    }

    @Test
    public void testAppendDouble() {
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            AppendableUtil.append(bytes, 2.718);
            bytes.append(',');
            AppendableUtil.append(bytes, 3.14);
            assertEquals("2.718,3.14", bytes.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testAppendLong() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 42L);
        sb.append(',');
        AppendableUtil.append(sb, 128L);
        assertEquals("42,128", sb.toString());

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            AppendableUtil.append(bytes, 42L);
            bytes.append(',');
            AppendableUtil.append(bytes, 128L);
            assertEquals("42,128", bytes.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testSetCharAtWithStringBuilder() throws BufferOverflowException {
        StringBuilder sb = new StringBuilder("Hello World");
        AppendableUtil.setCharAt(sb, 6, 'J');
        Assertions.assertEquals("Hello Jorld", sb.toString());
    }

    @Test
    public void testParseUtf8() throws BufferUnderflowException {
        BytesStore<?, byte[]> bs = BytesStore.from("Hello World");
        StringBuilder sb = new StringBuilder();
        AppendableUtil.parseUtf8(bs, sb, true, 11);
        Assertions.assertEquals("Hello World", sb.toString());
    }

    @Test
    public void read8bitPreservesTextWhenStringBuilderUsesUtf16Storage() {
        requireMaxDirectMemory();
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            bytes.write8bit("field");
            bytes.write8bit("another");
            bytes.write8bit(null);

            StringBuilder sb = utf16StringBuilder();
            assertTrue(bytes.read8bit(sb));

            assertEquals("field", sb.toString());

            assertTrue(bytes.read8bit(sb));

            assertEquals("another", sb.toString());

            assertFalse(bytes.read8bit(sb));

            // end of input == ""
            assertTrue(bytes.read8bit(sb));
            assertEquals("", sb.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void parse8bitPreservesExtendedByteWhenStringBuilderUsesUtf16Storage() throws Exception {
        requireMaxDirectMemory();
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            bytes.writeUnsignedByte(0xA3);
            bytes.writeUnsignedByte(0xFF);

            StringBuilder sb = utf16StringBuilder();

            AppendableUtil.parse8bit(bytes, sb, 1);
            assertEquals("\u00A3", sb.toString());

            AppendableUtil.parse8bit(bytes, sb, 1);
            assertEquals("\u00FF", sb.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void parse8bitFromNativeBytesPreservesExtendedByteWhenStringBuilderUsesUtf16Storage() throws Exception {
        assumeFalse(NativeBytes.areNewGuarded());
        requireMaxDirectMemory();
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        try {
            bytes.writeUnsignedByte(0xA3);
            bytes.writeUnsignedByte(0xFE);

            StringBuilder sb = utf16StringBuilder();

            AppendableUtil.parse8bit(bytes, sb, 1);
            assertEquals("\u00A3", sb.toString());

            AppendableUtil.parse8bit(bytes, sb, 1);
            assertEquals("\u00FE", sb.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testSetLengthWithStringBuilder() {
        StringBuilder sb = new StringBuilder("Hello World");
        AppendableUtil.setLength(sb, 5);
        Assertions.assertEquals("Hello", sb.toString());
    }

    @Test
    public void testAppendDoubleWithStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 2.718);
        sb.append(',');
        AppendableUtil.append(sb, 3.14);
        Assertions.assertEquals("2.718,3.14", sb.toString());
    }

    @Test
    public void testFindUtf8LengthByteArray() {
        byte[] bytes = "Hello World".getBytes(ISO_8859_1);
        long length = AppendableUtil.findUtf8Length(bytes);
        Assertions.assertEquals(22, length);
    }

    @Test
    public void testFindUtf8LengthCharArray() {
        char[] chars = "Hello World".toCharArray();
        long length = AppendableUtil.findUtf8Length(chars);
        Assertions.assertEquals(11, length);
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
    public void appendStringToAppendableAndCharSequence() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, "test");
        AppendableUtil.append(sb, " words");
        assertEquals("test words", sb.toString());

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            AppendableUtil.append(bytes, "test");
            AppendableUtil.append(bytes, " words");
            assertEquals("test words", bytes.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void appendSubsequenceToAppendableAndCharSequence() {
        String text = "prefix text";
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, text, 1, 4);
        AppendableUtil.append(sb, text, 6, 3);
        assertEquals("refi te", sb.toString());

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            AppendableUtil.append(bytes, text, 1, 4);
            AppendableUtil.append(bytes, text, 6, 3);
            assertEquals("refi te", bytes.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void appendSubsequenceFromBytesToStringBuilder() {
        Bytes<?> source = Bytes.from("prefix text");
        try {
            StringBuilder sb = new StringBuilder();
            AppendableUtil.append(sb, source, 1, 4);
            AppendableUtil.append(sb, source, 6, 3);
            assertEquals("refi te", sb.toString());
        } finally {
            source.releaseLast();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void setLength() {
        StringBuilder sb = new StringBuilder("hello world");
        AppendableUtil.setLength(sb, 5);
        assertEquals("hello", sb.toString());

        Bytes<?> b = Bytes.from("Hello World");
        AppendableUtil.setLength(b, 5);
        assertEquals("Hello", b.toString());

        StringBuffer sb2 = new StringBuffer();
        try {
            AppendableUtil.setLength(sb2, 0);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected.
        }
        b.releaseLast();
    }

    @Test
    public void setCharAt() {
        StringBuilder sb = new StringBuilder("hello world");
        Bytes<?> b = Bytes.allocateElasticOnHeap(16).append("Hello World");
        AppendableUtil.setCharAt(sb, 5, 'X');
        AppendableUtil.setCharAt(b, 5, 'X');
        assertEquals("helloXworld", sb.toString());
        assertEquals("HelloXWorld", b.toString());
        b.releaseLast();
    }

    private static StringBuilder utf16StringBuilder() {
        StringBuilder sb = new StringBuilder("\u221A");
        sb.setLength(0);
        return sb;
    }
}
