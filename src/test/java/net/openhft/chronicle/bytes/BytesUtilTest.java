/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class BytesUtilTest extends BytesTestCommon {

    File testFile;

    @Before
    public void setUp() {
        testFile = new File(OS.getTarget(), "testFile-" + System.nanoTime() + ".bin");
    }

    @After
    public void tearDown() throws IOException {
        BackgroundResourceReleaser.releasePendingResources();
        Files.deleteIfExists(testFile.toPath());
    }

    @Test
    public void testStopBitLength() {
        int length = BytesUtil.stopBitLength(128);
        assertEquals(2, length);
    }

    @Test
    public void testAsString() {
        Exception exception = new Exception("Test exception");
        String result = BytesUtil.asString("Error occurred", exception);
        assertTrue(result.startsWith("Error occurred\njava.lang.Exception: Test exception"));
    }

    @Test
    public void testRoundUpTo64ByteAlign() {
        long result = BytesUtil.roundUpTo64ByteAlign(65);
        assertEquals(128, result);
    }

    @Test
    public void testIsControlSpace() {
        assertTrue(BytesUtil.isControlSpace(' '));
        assertFalse(BytesUtil.isControlSpace('A'));
    }

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

        int start = BytesUtil.triviallyCopyableStart(Nested.class);
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

        Assert.assertEquals(start, BytesUtil.triviallyCopyableStart(A.class));
        Assert.assertEquals(20, BytesUtil.triviallyCopyableLength(A.class));
    }

    @Test
    public void triviallyCopyableB() {
        assumeTrue(Jvm.is64bit());

        int start = BytesUtil.triviallyCopyableStart(Nested.class);

        Assert.assertEquals("[" + start + ", " + (start + 20) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start, 4 + 2 * 8));
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start - 4, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start + 4, 4 + 2 * 8));

        assertTrue(BytesUtil.isTriviallyCopyable(A2.class));
        int size = Jvm.isAzulZing() ? 28 : 24;
        Assert.assertEquals("[" + start + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A2.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start, 4 + 2 * 8 + 2 * 2));
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start - 4, 4 + 2 * 8));
        Assert.assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A2.class, start + 8, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start + 12, 4 + 2 * 8));

        assertTrue(BytesUtil.isTriviallyCopyable(A3.class));
        // However, by copying a region that is safe.
        Assert.assertEquals("[" + start + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A3.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start, 4 + 2 * 8 + 2 * 2));
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start - 4, 4 + 2 * 8));
        Assert.assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A3.class, start + 8, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start + 12, 4 + 2 * 8));
    }

    @Test
    public void triviallyCopyable2() {
        assertFalse(BytesUtil.isTriviallyCopyable(D.class));
        assertTrue(BytesUtil.isTriviallyCopyable(E.class));
        int size2 = 20;
        int[] range = BytesUtil.triviallyCopyableRange(E.class);
        Assert.assertEquals(size2, range[1] - range[0]);
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

    @SuppressWarnings({"deprecation", "removal"})
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
            Assert.assertEquals(bytes.readChar(), c);
        }
    }

    @Test
    public void reverse() {
        Bytes<byte[]> test = Bytes.from("test");
        BytesUtil.reverse(test, 0);
        Assert.assertEquals(Bytes.from("tset"), test);
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


    @Test
    public void bytesEqualAndCharsEqual() {
        Bytes<?> a = Bytes.from("abcdef");
        Bytes<?> b = Bytes.from("abCdef");
        Bytes<?> c = Bytes.from("abcdef");
        try {
            assertFalse(BytesUtil.bytesEqual(a, 0, b, 0, a.readRemaining()));
            assertTrue(BytesUtil.bytesEqual(a, 0, c, 0, a.readRemaining()));

            assertTrue(BytesUtil.bytesEqual("abc", a, 0, 3));
            assertFalse(BytesUtil.bytesEqual("abC", a, 0, 3));
            assertFalse(BytesUtil.bytesEqual(null, a, 0, 3));
        } finally {
            a.releaseLast();
            b.releaseLast();
            c.releaseLast();
        }
    }

    @Test
    public void asIntStopBitAndPadding() {
        // Validate against native-endian view used by BytesUtil.asInt
        int expected = java.nio.ByteBuffer.wrap("1234".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))
                .order(java.nio.ByteOrder.nativeOrder())
                .getInt();
        Assert.assertEquals(expected, BytesUtil.asInt("1234"));
        Assert.assertEquals(1, BytesUtil.stopBitLength(0x7F));
        Assert.assertEquals(2, BytesUtil.stopBitLength(0x80));
        Assert.assertEquals(2, BytesUtil.stopBitLength(0x3FFF));
        assertTrue(BytesUtil.stopBitLength(0x4000) >= 3);

        Assert.assertEquals(64L, BytesUtil.roundUpTo64ByteAlign(1));
        Assert.assertEquals(0L, BytesUtil.roundUpTo64ByteAlign(0));
        Assert.assertEquals(8L, BytesUtil.roundUpTo8ByteAlign(1));
        Assert.assertEquals(0L, BytesUtil.padOffset(0));
        Assert.assertEquals(2L, BytesUtil.padOffset(2));
    }

    @Test
    public void readWrite8ByteAlignPaddingAndReverseAndCombineNewline() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            bytes.append("hello");
            BytesUtil.read8ByteAlignPadding(bytes);
            Assert.assertEquals(0, bytes.readPosition());

            bytes.clear();
            bytes.append("abc");
            long wp = bytes.writePosition();
            BytesUtil.write8ByteAlignPadding(bytes);
            long newWp = bytes.writePosition();
            assertTrue(newWp >= wp);
            for (long i = wp; i < newWp; i++) {
                Assert.assertEquals(0, bytes.peekUnsignedByte(i));
            }

            bytes.clear();
            bytes.append("abcdef");
            BytesUtil.reverse(bytes, 0);
            Assert.assertEquals("fedcba", bytes.toString());

            bytes.clear();
            bytes.append("line1\n\n");
            BytesUtil.combineDoubleNewline(bytes);
            Assert.assertEquals("line1\n", bytes.toString());

            bytes.clear();
            bytes.append("a \n");
            BytesUtil.combineDoubleNewline(bytes);
            Assert.assertEquals("a\n", bytes.toString());

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void findAndReadFileLiteral() throws Exception {
        // exercise literal path in readFile
        Bytes<?> literal = BytesUtil.readFile("=XYZ");
        try {
            Assert.assertEquals("XYZ", literal.toString());
        } finally {
            literal.releaseLast();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void findFileThrowsWhenMissing() throws Exception {
        BytesUtil.findFile("this-file-should-not-exist-chronicle-bytes");
    }

    private void doTestCombineDoubleNewline(String a, String b) {
        final Bytes<byte[]> b2 = Bytes.from(b);
        BytesUtil.combineDoubleNewline(b2);
        Assert.assertEquals(a, b2.toString());
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
