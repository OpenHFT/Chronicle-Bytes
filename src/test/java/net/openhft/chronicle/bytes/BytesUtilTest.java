/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests BytesUtil utility methods for stop bit encoding, alignment, and trivially copyable
 * detection because correct utility behaviour is essential for low-level byte operations.
 */
@DisplayName("BytesUtil utility methods should handle stop bit encoding, alignment, and copyable detection")
public class BytesUtilTest extends BytesTestCommon {

    File testFile;

    @BeforeEach
    public void setUp() {
        testFile = new File(OS.getTarget(), "testFile-" + System.nanoTime() + ".bin");
    }

    @AfterEach
    public void tearDown() throws IOException {
        BackgroundResourceReleaser.releasePendingResources();
        Files.deleteIfExists(testFile.toPath());
    }

    @Test
    @DisplayName("stop bit length returns expected size")
    public void testStopBitLength() {
        int length = BytesUtil.stopBitLength(128);
        assertEquals(2, length,
                "Stop bit length for 128 uses two bytes");
    }

    @Test
    @DisplayName("asString formats message and exception details for output")
    public void testAsString() {
        Exception exception = new Exception("Test exception");
        String result = BytesUtil.asString("Error occurred", exception);
        assertTrue(result.startsWith("Error occurred\njava.lang.Exception: Test exception"),
                "asString output " + result + " prefixes message and exception details");
    }

    @Test
    @DisplayName("roundUpTo64ByteAlign rounds values up to boundary")
    public void testRoundUpTo64ByteAlign() {
        long result = BytesUtil.roundUpTo64ByteAlign(65);
        assertEquals(128, result,
                "64 byte alignment rounds 65 up to 128");
    }

    @Test
    @DisplayName("isControlSpace flags spaces and letters correctly")
    public void testIsControlSpace() {
        assertTrue(BytesUtil.isControlSpace(' '),
                "Space is treated as control space");
        assertFalse(BytesUtil.isControlSpace('A'),
                "Letter A is not treated as control space");
    }

    @Test
    @DisplayName("readFile loads file content from jar resource")
    public void fromFileInJar()
            throws IOException {
        Bytes<?> bytes = BytesUtil.readFile("net/openhft/chronicle/core/onoes/Google.properties");
        Bytes<?> apacheLicense = Bytes.from("Apache License");
        long n = bytes.indexOf(apacheLicense);
        assertTrue(n > 0,
                "Jar file content contains Apache License marker at index " + n);
        apacheLicense.releaseLast();
    }

    @Test
    @DisplayName("locating a file path returns an existing writable file")
    public void findFile()
            throws FileNotFoundException {
        String file = BytesUtil.findFile("file-to-find.txt");
        assertTrue(new File(file).exists(),
                "located path should exist on disk");
        assertTrue(new File(file).canWrite(),
                "located path should be writable");
    }

    @Test
    @DisplayName("trivially copyable detection for nested classes")
    public void triviallyCopyable() {
        assumeTrue(Jvm.is64bit(),
                "Trivially copyable checks require a 64-bit JVM");

        int start = BytesUtil.triviallyCopyableStart(Nested.class);
        assertTrue(BytesUtil.isTriviallyCopyable(Nested.class),
                "Nested is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(Nested.class, start, 4),
                "Nested range is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(SubNested.class),
                "SubNested is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(SubNested.class, start, 4),
                "SubNested range is trivially copyable");
        // TODO allow a portion of B to be trivially copyable
        assertTrue(BytesUtil.isTriviallyCopyable(B.class),
                "Class B is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(B.class, start, 20),
                "Class B range is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(C.class),
                "Class C is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(C.class, start, 4),
                "Class C range is trivially copyable");

        assertTrue(BytesUtil.isTriviallyCopyable(A.class),
                "Class A is trivially copyable");

        assertEquals(start, BytesUtil.triviallyCopyableStart(A.class),
                "Class A trivially copyable start matches");
        assertEquals(20, BytesUtil.triviallyCopyableLength(A.class),
                "Class A trivially copyable length matches");
        assertEquals(0L, sumDefaultFieldValues(),
                "Default field values remain zero for trivially copyable fixtures");
    }

    @Test
    @DisplayName("trivially copyable ranges computed for subclasses")
    public void triviallyCopyableB() {
        assumeTrue(Jvm.is64bit(),
                "Trivially copyable range checks require a 64-bit JVM");

        int start = BytesUtil.triviallyCopyableStart(Nested.class);

        assertEquals("[" + start + ", " + (start + 20) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A.class)),
                "Class A trivially copyable range matches");
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start, 4 + 2 * 8),
                "Class A base range is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start + 4, 8),
                "Class A inner range is trivially copyable");
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start - 4, 4 + 2 * 8),
                "Class A invalid range is not trivially copyable");
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start + 4, 4 + 2 * 8),
                "Class A overlapping range is not trivially copyable");

        assertTrue(BytesUtil.isTriviallyCopyable(A2.class),
                "Class A2 is trivially copyable");
        int size = Jvm.isAzulZing() ? 28 : 24;
        assertEquals("[" + start + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A2.class)),
                "Class A2 trivially copyable range matches");
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start, 4 + 2 * 8 + 2 * 2),
                "Class A2 base range is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start + 4, 8),
                "Class A2 inner range is trivially copyable");
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start - 4, 4 + 2 * 8),
                "Class A2 invalid range is not trivially copyable");
        assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A2.class, start + 8, 4 + 2 * 8),
                "Class A2 long range is trivially copyable only for Azul Zing");
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start + 12, 4 + 2 * 8),
                "Class A2 shifted range is not trivially copyable");

        assertTrue(BytesUtil.isTriviallyCopyable(A3.class),
                "Class A3 is trivially copyable");
        // However, by copying a region that is safe.
        assertEquals("[" + start + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A3.class)),
                "Class A3 trivially copyable range matches");
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start, 4 + 2 * 8 + 2 * 2),
                "Class A3 base range is trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start + 4, 8),
                "Class A3 inner range is trivially copyable");
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start - 4, 4 + 2 * 8),
                "Class A3 invalid range is not trivially copyable");
        assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A3.class, start + 8, 4 + 2 * 8),
                "Class A3 long range is trivially copyable only for Azul Zing");
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start + 12, 4 + 2 * 8),
                "Class A3 shifted range is not trivially copyable");
    }

    @Test
    @DisplayName("trivially copyable range size for class E")
    public void triviallyCopyable2() {
        assertFalse(BytesUtil.isTriviallyCopyable(D.class),
                "Class D is not trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(E.class),
                "Class E is trivially copyable");
        int size2 = 20;
        int[] range = BytesUtil.triviallyCopyableRange(E.class);
        assertEquals(size2, range[1] - range[0],
                "Class E trivially copyable range length matches");
    }

    @Test
    @DisplayName("contentEquals comparison rejects null target reference")
    public void contentsEqualBytesNull() {
        final Bytes<?> bytes = Bytes.from("A");
        try {
            assertFalse(bytes.contentEquals(null),
                    "contentEquals rejects null target value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("contentEquals returns true for equal byte contents")
    public void contentsEqual() {
        final Bytes<?> a = Bytes.from("A");
        final Bytes<?> b = Bytes.from("A");
        try {
            assertTrue(a.contentEquals(b),
                    "contentEquals matches equal byte contents");
        } finally {
            a.releaseLast();
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("equals comparison matches identical object reference instance")
    public void equals_reference() {
        String a = "a";
        assertTrue(BytesUtil.equals(a, a),
                "equals matches identical object reference");
    }

    @Test
    @DisplayName("equals comparison matches equivalent character sequence values")
    public void equals_equivalentCharSequences() {
        Bytes<byte[]> a = Bytes.from("a");
        Bytes<byte[]> aa = Bytes.from("a");
        assertTrue(BytesUtil.equals(a, aa),
                "equals matches equivalent Bytes instances");
    }

    @Test
    @SuppressWarnings({"deprecation", "removal"})
    @DisplayName("equals returns true for equivalent boxed integer objects")
    public void equals_equivalentObjects() {
        // Intentional boxing to create two equivalent but distinct objects
        assertTrue(BytesUtil.equals(new Integer(1), new Integer(1)),
                "equals matches equivalent boxed integers");
    }

    @Test
    @DisplayName("toCharArray reads back stored characters in order")
    public void toCharArray() {
        Bytes<byte[]> bytes = Bytes.from("test");
        char[] charArray = BytesUtil.toCharArray(bytes);
        int index = 0;
        for (char c : charArray) {
            assertEquals(bytes.readChar(), c,
                    "Char array entry " + c + " matches bytes at index " + index);
            index++;
        }
    }

    @Test
    @DisplayName("reverse writes bytes in reverse order")
    public void reverse() {
        Bytes<byte[]> test = Bytes.from("test");
        BytesUtil.reverse(test, 0);
        assertEquals(Bytes.from("tset"), test,
                "reverse produces expected reversed string value");
    }

    @Test
    @DisplayName("combineDoubleNewline collapses extra newline sequences in place")
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
    @DisplayName("bytesEqual and charsEqual compare content values")
    public void bytesEqualAndCharsEqual() {
        Bytes<?> a = Bytes.from("abcdef");
        Bytes<?> b = Bytes.from("abCdef");
        Bytes<?> c = Bytes.from("abcdef");
        try {
            assertFalse(BytesUtil.bytesEqual(a, 0, b, 0, a.readRemaining()),
                    "bytesEqual detects mismatch between a and b");
            assertTrue(BytesUtil.bytesEqual(a, 0, c, 0, a.readRemaining()),
                    "bytesEqual confirms match between a and c");

            assertTrue(BytesUtil.bytesEqual("abc", a, 0, 3),
                    "bytesEqual matches string abc to bytes prefix");
            assertFalse(BytesUtil.bytesEqual("abC", a, 0, 3),
                    "bytesEqual detects case mismatch in prefix");
            assertFalse(BytesUtil.bytesEqual(null, a, 0, 3),
                    "bytesEqual returns false for null string input");
        } finally {
            a.releaseLast();
            b.releaseLast();
            c.releaseLast();
        }
    }

    @Test
    @DisplayName("asInt and stop bit length calculations")
    public void asIntStopBitAndPadding() {
        // Validate against native-endian view used by BytesUtil.asInt
        int expected = java.nio.ByteBuffer.wrap("1234".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))
                .order(java.nio.ByteOrder.nativeOrder())
                .getInt();
        assertEquals(expected, BytesUtil.asInt("1234"),
                "asInt reads native-endian value for 1234");
        assertEquals(1, BytesUtil.stopBitLength(0x7F),
                "stop bit length for 0x7F is one byte");
        assertEquals(2, BytesUtil.stopBitLength(0x80),
                "stop bit length for 0x80 is two bytes");
        assertEquals(2, BytesUtil.stopBitLength(0x3FFF),
                "stop bit length for 0x3FFF is two bytes");
        assertTrue(BytesUtil.stopBitLength(0x4000) >= 3,
                "stop bit length for 0x4000 is at least three bytes");

        assertEquals(64L, BytesUtil.roundUpTo64ByteAlign(1),
                "64 byte alignment rounds 1 up to 64");
        assertEquals(0L, BytesUtil.roundUpTo64ByteAlign(0),
                "64 byte alignment keeps zero aligned");
        assertEquals(8L, BytesUtil.roundUpTo8ByteAlign(1),
                "8 byte alignment rounds 1 up to 8");
        assertEquals(0L, BytesUtil.padOffset(0),
                "padOffset returns zero for zero position");
        assertEquals(2L, BytesUtil.padOffset(2),
                "padOffset returns two for position two");
    }

    @Test
    @DisplayName("padding, reverse, and newline combination operations")
    public void readWrite8ByteAlignPaddingAndReverseAndCombineNewline() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            bytes.append("hello");
            BytesUtil.read8ByteAlignPadding(bytes);
            assertEquals(0, bytes.readPosition(),
                    "read8ByteAlignPadding resets read position");

            bytes.clear();
            bytes.append("abc");
            long wp = bytes.writePosition();
            BytesUtil.write8ByteAlignPadding(bytes);
            long newWp = bytes.writePosition();
            assertTrue(newWp >= wp,
                    "write8ByteAlignPadding advances write position: newWp=" + newWp + " >= wp=" + wp);
            for (long i = wp; i < newWp; i++) {
                assertEquals(0, bytes.peekUnsignedByte(i),
                        "Padding byte is zero at offset " + i);
            }

            bytes.clear();
            bytes.append("abcdef");
            BytesUtil.reverse(bytes, 0);
            assertEquals("fedcba", bytes.toString(),
                    "reverse produces expected reversed string output");

            bytes.clear();
            bytes.append("line1\n\n");
            BytesUtil.combineDoubleNewline(bytes);
            assertEquals("line1\n", bytes.toString(),
                    "combineDoubleNewline trims double newline");

            bytes.clear();
            bytes.append("a \n");
            BytesUtil.combineDoubleNewline(bytes);
            assertEquals("a\n", bytes.toString(),
                    "combineDoubleNewline trims whitespace newline");

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readFile handles literal content prefix correctly")
    public void findAndReadFileLiteral() throws Exception {
        // exercise literal path in readFile
        Bytes<?> literal = BytesUtil.readFile("=XYZ");
        try {
            assertEquals("XYZ", literal.toString(),
                    "readFile literal returns exact content");
        } finally {
            literal.releaseLast();
        }
    }

    @Test
    @DisplayName("findFile throws when requested file is missing")
    public void findFileThrowsWhenMissing() throws Exception {
        assertThrows(FileNotFoundException.class,
                () -> BytesUtil.findFile("this-file-should-not-exist-chronicle-bytes"),
                "findFile rejects missing file path");
    }

    private void doTestCombineDoubleNewline(String a, String b) {
        final Bytes<byte[]> b2 = Bytes.from(b);
        BytesUtil.combineDoubleNewline(b2);
        assertEquals(a, b2.toString(),
                "combineDoubleNewline transforms [" + b + "] to [" + a + "]");
    }

    private static long sumDefaultFieldValues() {
        long sum = 0L;
        A a = new A();
        sum += a.i;
        sum += a.l;
        sum += Double.doubleToLongBits(a.d);
        A2 a2 = new A2();
        sum += a2.s;
        sum += a2.ch;
        A3 a3 = new A3();
        sum += a3.user == null ? 0 : a3.user.length();
        a3.user = "";
        sum += a3.user.length();
        B b = new B();
        sum += b.i;
        sum += b.l;
        sum += Double.doubleToLongBits(b.d);
        sum += b.s == null ? 0 : b.s.length();
        C c = new C();
        sum += c.i;
        sum += c.l;
        sum += Double.doubleToLongBits(c.d);
        D d = new D();
        sum += d.user == null ? 0 : d.user.length();
        d.user = "";
        sum += d.user.length();
        E e = new E();
        sum += e.i;
        sum += e.l;
        sum += Double.doubleToLongBits(e.d);
        Nested nested = new Nested();
        sum += nested.i;
        SubNested subNested = new BytesUtilTest().new SubNested();
        sum += subNested.j;
        return sum;
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

    private static class A3 extends A2 {
        String user;
    }

    private static class B {
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

    private class SubNested extends Nested {
        int j;
    }
}
