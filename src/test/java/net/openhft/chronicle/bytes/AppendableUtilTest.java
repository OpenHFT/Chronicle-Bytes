/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        assertEquals("3.14", sb.toString());
    }

    @Test
    public void testAppendLong() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 42L);
        assertEquals("42", sb.toString());
    }

    @Test
    public void testSetCharAtWithStringBuilder() throws BufferOverflowException {
        StringBuilder sb = new StringBuilder("Hello World");
        AppendableUtil.setCharAt(sb, 6, 'J');
        Assertions.assertEquals("Hello Jorld", sb.toString());
    }

    @Test
    public void testParseUtf8() throws BufferUnderflowException {
        BytesStore<Bytes<byte[]>, byte[]> bs = BytesStore.from("Hello World");
        StringBuilder sb = new StringBuilder();
        AppendableUtil.parseUtf8(bs, sb, true, 11);
        Assertions.assertEquals("Hello World", sb.toString());
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
        AppendableUtil.append(sb, 3.14);
        Assertions.assertEquals("3.14", sb.toString());
    }

    @Test
    public void testFindUtf8LengthByteArray() {
        byte[] bytes = "Hello World".getBytes();
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
            public Appendable append(CharSequence csq) { return this; }
            @Override
            public Appendable append(CharSequence csq, int start, int end) { return this; }
            @Override
            public Appendable append(char c) { return this; }
        };

        assertThrows(IllegalArgumentException.class, () -> AppendableUtil.setCharAt(appendable, 1, 'a'));
    }

    @Test
    public void appendDoubleToStringBuilder() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, 3.14);
        assertEquals("3.14", sb.toString());
    }

    @Test
    public void appendStringToAppendableAndCharSequence() {
        StringBuilder sb = new StringBuilder();
        AppendableUtil.append(sb, "test");
        assertEquals("test", sb.toString());
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
}
