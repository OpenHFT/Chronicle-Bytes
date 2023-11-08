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

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

@SuppressWarnings("rawtypes")
public class BytesUtilTest extends BytesTestCommon {
    @Test
    public void fromFileInJar()
            throws IOException {
        Bytes<?> bytes = BytesUtil.readFile("/net/openhft/chronicle/core/onoes/Google.properties");
        Bytes<?> apache_license = Bytes.from("Apache License");
        long n = bytes.indexOf(apache_license);
        assertTrue(n > 0);
        apache_license.releaseLast();
    }

    @Test
    public void findFile()
            throws FileNotFoundException {
        String file = BytesUtil.findFile("file-to-find.txt");
        assertTrue(new File(file).exists());
        assertTrue(new File(file).canWrite());
    }

    @Test
    public void triviallyCopyable() {
        assumeTrue(Jvm.is64bit());

        int start = Jvm.objectHeaderSize();
        assertTrue(BytesUtil.isTriviallyCopyable(Nested.class));
        assertTrue(BytesUtil.isTriviallyCopyable(Nested.class, start, 4));
        assertTrue(BytesUtil.isTriviallyCopyable(SubNested.class));
        assertTrue(BytesUtil.isTriviallyCopyable(SubNested.class, start, 4));
        // TODO allow a portion of B to be trivially copyable
        assertTrue(BytesUtil.isTriviallyCopyable(B.class));
        assertTrue(BytesUtil.isTriviallyCopyable(B.class, start, 20));
        assertTrue(BytesUtil.isTriviallyCopyable(C.class));
        assertTrue(BytesUtil.isTriviallyCopyable(C.class, start, 4));

        assertTrue(BytesUtil.isTriviallyCopyable(A.class));

        assertEquals(start, BytesUtil.triviallyCopyableStart(A.class));
        assertEquals(20, BytesUtil.triviallyCopyableLength(A.class));
    }

    @Test
    public void triviallyCopyableB() {
        assumeTrue(Jvm.is64bit());

        int start = Jvm.objectHeaderSize();

        assertEquals("[" + start + ", " + (start + 20) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start, 4 + 2 * 8));
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start - 4, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start + 4, 4 + 2 * 8));

        assertTrue(BytesUtil.isTriviallyCopyable(A2.class));
        int size = Jvm.isAzulZing() ? 28 : 24;
        assertEquals("[" + start + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A2.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start, 4 + 2 * 8 + 2 * 2));
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start - 4, 4 + 2 * 8));
        assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A2.class, start + 8, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start + 12, 4 + 2 * 8));

        assertTrue(BytesUtil.isTriviallyCopyable(A3.class));
        // However, by copying a region that is safe.
        assertEquals("[" + start + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A3.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start, 4 + 2 * 8 + 2 * 2));
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start - 4, 4 + 2 * 8));
        assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A3.class, start + 8, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start + 12, 4 + 2 * 8));
    }

    @Test
    public void triviallyCopyable2() {
        assertFalse(BytesUtil.isTriviallyCopyable(D.class));
        assertTrue(BytesUtil.isTriviallyCopyable(E.class));
        int size2 = 20;
        int[] range = BytesUtil.triviallyCopyableRange(E.class);
        assertEquals(size2, range[1] - range[0]);
    }

    @Test
    public void contentsEqualBytesNull() {
        final Bytes<?> bytes = Bytes.from("A");
        try {
            assertFalse(bytes.contentEquals(null));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void contentsEqual() {
        final Bytes<?> a = Bytes.from("A");
        final Bytes<?> b = Bytes.from("A");
        try {
            assertTrue(a.contentEquals(b));
        } finally {
            a.releaseLast();
            b.releaseLast();
        }
    }

    @Test
    public void equals_reference() {
        String a = "a";
        assertTrue(BytesUtil.equals(a, a));
    }
    @Test
    public void equals_equivalentCharSequences() {
        Bytes<byte[]> a = Bytes.from("a");
        Bytes<byte[]> aa = Bytes.from("a");
        assertTrue(BytesUtil.equals(a, aa));
    }

    @Test
    public void equals_equivalentObjects() {
        // Intentional boxing to create two equivalent but distinct objects
        assertTrue(BytesUtil.equals(new Integer(1), new Integer(1)));
    }

    @Test
    public void toCharArray() {
        Bytes<byte[]> bytes = Bytes.from("test");
        char[] charArray = BytesUtil.toCharArray(bytes);
        for (char c : charArray) {
            assertEquals(bytes.readChar(), c);
        }
    }

    @Test
    public void reverse() {
        Bytes<byte[]> test = Bytes.from("test");
        BytesUtil.reverse(test, 0);
        assertEquals(Bytes.from("tset"), test);
    }

    @Test
    public void combineDoubleNewline() {
        doTestCombineDoubleNewline("\n", "\n");
        doTestCombineDoubleNewline("\r\n", "\r\n");
        doTestCombineDoubleNewline("\n ", "\n ");
        doTestCombineDoubleNewline(" ", " ");
        doTestCombineDoubleNewline("  ", "  ");
        doTestCombineDoubleNewline("   ", "   ");
        doTestCombineDoubleNewline("\n", "\n\n");
        doTestCombineDoubleNewline("\n", " \n");
        doTestCombineDoubleNewline(" \n", "  \n");
        doTestCombineDoubleNewline("AA", "AA");
        doTestCombineDoubleNewline("AA ", "AA ");
        doTestCombineDoubleNewline("AA ", "AA  ");
        doTestCombineDoubleNewline("AA   ", "AA   ");
    }

    private void doTestCombineDoubleNewline(String a, String b) {
        final Bytes<byte[]> b2 = Bytes.from(b);
        BytesUtil.combineDoubleNewline(b2);
        assertEquals(a, b2.toString());
    }

    static class A {
        int i;
        long l;
        double d;
    }

    static class A2 extends A {
        short s;
        char ch;
    }

    static class A3 extends A2 {
        String user;
    }

    static class B {
        int i;
        long l;
        double d;
        String s;
    }

    static class C {
        int i;
        transient long l;
        double d;
    }

    static class D {
        String user;
    }

    static class E extends D {
        int i;
        long l;
        double d;
    }

    static class Nested {
        // implicit this$0
        int i;
    }

    class SubNested extends Nested {
        int j;
    }
}
