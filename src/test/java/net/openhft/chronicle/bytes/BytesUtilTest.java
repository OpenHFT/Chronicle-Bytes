/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SuppressWarnings("deprecation")
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
    public void testStopBitLength() {
        int length = BytesUtil.stopBitLength(128);
        assertEquals(2, length, "stop-bit encoding of 128 should require 2 bytes");
    }

    @Test
    public void testAsString() {
        Exception exception = new Exception("Test exception");
        String result = BytesUtil.asString("Error occurred", exception);
        assertTrue(result.startsWith("Error occurred\njava.lang.Exception: Test exception"), "asString result should start with error message and exception stack trace");
    }

    @Test
    public void testRoundUpTo64ByteAlign() {
        long result = BytesUtil.roundUpTo64ByteAlign(65);
        assertEquals(128, result, "65 should round up to next 64-byte boundary (128)");
    }

    @Test
    public void testIsControlSpace() {
        assertTrue(BytesUtil.isControlSpace(' '), "space character should be identified as control space");
        assertFalse(BytesUtil.isControlSpace('A'), "letter 'A' should not be identified as control space");
    }

    @Test
    public void fromFileInJar()
            throws IOException {
        Bytes<?> bytes = BytesUtil.readFile("net/openhft/chronicle/core/onoes/Google.properties");
        Bytes<?> apacheLicense = Bytes.from("Apache License");
        long n = bytes.indexOf(apacheLicense);
        assertTrue(n > 0, "file from JAR should contain Apache License text");
        apacheLicense.releaseLast();
    }

    @Test
    public void findFile()
            throws FileNotFoundException {
        String file = BytesUtil.findFile("file-to-find.txt");
        assertTrue(new File(file).exists(), "found file should exist");
        assertTrue(new File(file).canWrite(), "found file should be writable");
    }

    @Test
    public void triviallyCopyable() {
        assumeTrue(Jvm.is64bit());

        int start = BytesUtil.triviallyCopyableStart(Nested.class);
        assertTrue(BytesUtil.isTriviallyCopyable(Nested.class), "Nested class with int field should be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(Nested.class, start, 4), "Nested class 4-byte region should be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(SubNested.class), "SubNested class with primitive fields should be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(SubNested.class, start, 4), "SubNested class 4-byte region should be trivially copyable");
        // TODO allow a portion of B to be trivially copyable
        assertTrue(BytesUtil.isTriviallyCopyable(B.class), "B class with primitive fields should be trivially copyable despite String field");
        assertTrue(BytesUtil.isTriviallyCopyable(B.class, start, 20), "B class 20-byte region should be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(C.class), "C class with transient field should be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(C.class, start, 4), "C class 4-byte region should be trivially copyable");

        assertTrue(BytesUtil.isTriviallyCopyable(A.class), "A class with only primitive fields should be trivially copyable");

        assertEquals(start, BytesUtil.triviallyCopyableStart(A.class), "A class triviallyCopyable region should start at expected offset");
        assertEquals(20, BytesUtil.triviallyCopyableLength(A.class), "A class triviallyCopyable region should span 20 bytes (int + long + double)");

        // Exercise fields so SpotBugs sees them as used
        A a = new A();
        a.i = 1;
        a.l = 2L;
        a.d = 3.0;
        assertEquals(1, a.i, "A.i field assignment should be preserved");
        assertEquals(2L, a.l, "A.l field assignment should be preserved");
        assertEquals(3.0, a.d, 0.0, "A.d field assignment should be preserved");

        B b = new B();
        b.i = 4;
        b.l = 5L;
        b.d = 6.0;
        b.s = "x";
        assertEquals(4, b.i, "B.i field assignment should be preserved");
        assertEquals(5L, b.l, "B.l field assignment should be preserved");
        assertEquals(6.0, b.d, 0.0, "B.d field assignment should be preserved");
        assertEquals("x", b.s, "B.s field assignment should be preserved");

        C c = new C();
        c.i = 7;
        c.l = 8L;
        c.d = 9.0;
        assertEquals(7, c.i, "C.i field assignment should be preserved");
        assertEquals(8L, c.l, "C.l field assignment should be preserved (transient field)");
        assertEquals(9.0, c.d, 0.0, "C.d field assignment should be preserved");

        Nested nested = new Nested();
        nested.i = 10;
        assertEquals(10, nested.i, "Nested.i field assignment should be preserved");

        SubNested subNested = new SubNested();
        subNested.i = 11;
        subNested.j = 12;
        assertEquals(11, subNested.i, "SubNested.i field assignment should be preserved");
        assertEquals(12, subNested.j, "SubNested.j field assignment should be preserved");
    }

    @Test
    public void triviallyCopyableB() {
        assumeTrue(Jvm.is64bit());

        int start = BytesUtil.triviallyCopyableStart(Nested.class);

        assertEquals("[" + start + ", " + (start + 20) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A.class)), "A class triviallyCopyable range should match expected 20-byte span");
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start, 4 + 2 * 8), "A class full 20-byte region should be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start + 4, 8), "A class 8-byte region after int field should be trivially copyable");
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start - 4, 4 + 2 * 8), "A class region starting before valid offset should not be trivially copyable");
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start + 4, 4 + 2 * 8), "A class 20-byte region offset by 4 bytes should not be trivially copyable");

        assertTrue(BytesUtil.isTriviallyCopyable(A2.class), "A2 class extending A with short and char fields should be trivially copyable");
        int size = Jvm.isAzulZing() ? 28 : 24;
        assertEquals("[" + start + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A2.class)), "A2 class triviallyCopyable range should match JVM-specific size");
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start, 4 + 2 * 8 + 2 * 2), "A2 class full 24-byte region should be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start + 4, 8), "A2 class 8-byte region after int field should be trivially copyable");
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start - 4, 4 + 2 * 8), "A2 class region starting before valid offset should not be trivially copyable");
        assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A2.class, start + 8, 4 + 2 * 8), "A2 class offset region triviallyCopyable result should match Azul Zing JVM behavior");
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start + 12, 4 + 2 * 8), "A2 class 20-byte region offset by 12 bytes should not be trivially copyable");

        assertTrue(BytesUtil.isTriviallyCopyable(A3.class), "A3 class with String field should be trivially copyable for primitive region");
        // However, by copying a region that is safe.
        assertEquals("[" + start + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A3.class)), "A3 class triviallyCopyable range should match JVM-specific size despite String field");
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start, 4 + 2 * 8 + 2 * 2), "A3 class full 24-byte primitive region should be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start + 4, 8), "A3 class 8-byte region after int field should be trivially copyable");
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start - 4, 4 + 2 * 8), "A3 class region starting before valid offset should not be trivially copyable");
        assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A3.class, start + 8, 4 + 2 * 8), "A3 class offset region triviallyCopyable result should match Azul Zing JVM behavior");
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start + 12, 4 + 2 * 8), "A3 class 20-byte region offset by 12 bytes should not be trivially copyable");

        // Exercise A2/A3 fields so SpotBugs sees them as read
        A2 a2 = new A2();
        a2.s = 123;
        a2.ch = 'z';
        assertEquals(123, a2.s, "A2.s field assignment should be preserved");
        assertEquals('z', a2.ch, "A2.ch field assignment should be preserved");

        A3 a3 = new A3();
        a3.user = "user3";
        assertEquals("user3", a3.user, "A3.user field assignment should be preserved");
    }

    @Test
    public void triviallyCopyable2() {
        assertFalse(BytesUtil.isTriviallyCopyable(D.class), "D class with only String field should not be trivially copyable");
        assertTrue(BytesUtil.isTriviallyCopyable(E.class), "E class with primitive fields should be trivially copyable despite String in parent");
        int size2 = 20;
        int[] range = BytesUtil.triviallyCopyableRange(E.class);
        assertEquals(size2, range[1] - range[0], "E class triviallyCopyable range should span 20 bytes");

        D d = new D();
        d.user = "user";
        assertEquals("user", d.user, "D.user field assignment should be preserved");

        E e = new E();
        e.user = "user2";
        e.i = 1;
        e.l = 2L;
        e.d = 3.0;
        assertEquals("user2", e.user, "E.user field assignment should be preserved");
        assertEquals(1, e.i, "E.i field assignment should be preserved");
        assertEquals(2L, e.l, "E.l field assignment should be preserved");
        assertEquals(3.0, e.d, 0.0, "E.d field assignment should be preserved");
    }

    @Test
    public void contentsEqualBytesNull() {
        final Bytes<?> bytes = Bytes.from("A");
        try {
            assertFalse(bytes.contentEquals(null), "Bytes content should not equal null");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void contentsEqual() {
        final Bytes<?> a = Bytes.from("A");
        final Bytes<?> b = Bytes.from("A");
        try {
            assertTrue(a.contentEquals(b), "Bytes with identical content 'A' should be equal");
        } finally {
            a.releaseLast();
            b.releaseLast();
        }
    }

    @Test
    public void equals_reference() {
        String a = "a";
        assertTrue(BytesUtil.equals(a, a), "same string reference should equal itself");
    }

    @Test
    public void equals_equivalentCharSequences() {
        Bytes<byte[]> a = Bytes.from("a");
        Bytes<byte[]> aa = Bytes.from("a");
        assertTrue(BytesUtil.equals(a, aa), "Bytes instances with same content 'a' should be equal");
    }

    @SuppressWarnings({"deprecation", "removal"})
    @Test
    public void equals_equivalentObjects() {
        // Intentional boxing to create two equivalent but distinct objects
        assertTrue(BytesUtil.equals(1, 1), "equivalent Integer objects should be equal");
    }

    @Test
    public void toCharArray() {
        Bytes<byte[]> bytes = Bytes.from("test");
        char[] charArray = BytesUtil.toCharArray(bytes);
        for (char c : charArray) {
            assertEquals(bytes.readChar(), c, "readChar should match character from toCharArray");
        }
    }

    @Test
    public void reverse() {
        Bytes<byte[]> test = Bytes.from("test");
        BytesUtil.reverse(test, 0);
        assertEquals(Bytes.from("tset"), test, "reverse should produce 'tset' from 'test'");
    }

    @Test
    public void combineDoubleNewline() {
        final String[][] cases = {
                {"\n", "\n"},
                {"\r\n", "\r\n"},
                {"\n ", "\n "},
                {" ", " "},
                {"  ", "  "},
                {"   ", "   "},
                {"\n", "\n\n"},
                {"\n", " \n"},
                {" \n", "  \n"},
                {"AA", "AA"},
                {"AA ", "AA "},
                {"AA ", "AA  "},
                {"AA   ", "AA   "},
        };
        for (int i = 0; i < cases.length; i++) {
            assertEquals(cases[i][0], combineDoubleNewlineResult(cases[i][1]), "combineDoubleNewline: case " + i);
        }
    }

    @Test
    public void bytesEqualAndCharsEqual() {
        Bytes<?> a = Bytes.from("abcdef");
        Bytes<?> b = Bytes.from("abCdef");
        Bytes<?> c = Bytes.from("abcdef");
        try {
            assertFalse(BytesUtil.bytesEqual(a, 0, b, 0, a.readRemaining()), "Bytes 'abcdef' should not equal 'abCdef' (case-sensitive)");
            assertTrue(BytesUtil.bytesEqual(a, 0, c, 0, a.readRemaining()), "Bytes 'abcdef' should equal identical 'abcdef'");

            assertTrue(BytesUtil.bytesEqual("abc", a, 0, 3), "String 'abc' should equal first 3 bytes of 'abcdef'");
            assertFalse(BytesUtil.bytesEqual("abC", a, 0, 3), "String 'abC' should not equal first 3 bytes of 'abcdef' (case-sensitive)");
            assertFalse(BytesUtil.bytesEqual(null, a, 0, 3), "null string should not equal any Bytes content");
        } finally {
            a.releaseLast();
            b.releaseLast();
            c.releaseLast();
        }
    }

    @Test
    public void asIntStopBitAndPadding() {
        // Validate against native-endian view used by BytesUtil.asInt
        int expected = java.nio.ByteBuffer.wrap("1234".getBytes(ISO_8859_1))
                .order(java.nio.ByteOrder.nativeOrder())
                .getInt();
        assertEquals(expected, BytesUtil.asInt("1234"), "asInt should produce native-endian int from '1234'");
        assertEquals(1, BytesUtil.stopBitLength(0x7F), "stop-bit encoding of 0x7F should require 1 byte");
        assertEquals(2, BytesUtil.stopBitLength(0x80), "stop-bit encoding of 0x80 should require 2 bytes");
        assertEquals(2, BytesUtil.stopBitLength(0x3FFF), "stop-bit encoding of 0x3FFF should require 2 bytes");
        assertTrue(BytesUtil.stopBitLength(0x4000) >= 3, "stop-bit encoding of 0x4000 should require at least 3 bytes");

        assertEquals(64L, BytesUtil.roundUpTo64ByteAlign(1), "1 should round up to 64-byte boundary (64)");
        assertEquals(0L, BytesUtil.roundUpTo64ByteAlign(0), "0 should round up to 64-byte boundary (0)");
        assertEquals(8L, BytesUtil.roundUpTo8ByteAlign(1), "1 should round up to 8-byte boundary (8)");
        assertEquals(0L, BytesUtil.padOffset(0), "padOffset(0) should return 0");
        assertEquals(2L, BytesUtil.padOffset(2), "padOffset(2) should return 2");
    }

    @Test
    public void readWrite8ByteAlignPaddingAndReverseAndCombineNewline() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            bytes.append("hello");
            BytesUtil.read8ByteAlignPadding(bytes);
            assertEquals(0, bytes.readPosition(), "read8ByteAlignPadding should not advance read position");

            bytes.clear();
            bytes.append("abc");
            long wp = bytes.writePosition();
            BytesUtil.write8ByteAlignPadding(bytes);
            long newWp = bytes.writePosition();
            assertTrue(newWp >= wp, "write8ByteAlignPadding should advance writePosition to 8-byte boundary");
            for (long i = wp; i < newWp; i++) {
                assertEquals(0, bytes.peekUnsignedByte(i), "write8ByteAlignPadding should fill padding with zero bytes");
            }

            bytes.clear();
            bytes.append("abcdef");
            BytesUtil.reverse(bytes, 0);
            assertEquals("fedcba", bytes.toString(), "reverse should produce 'fedcba' from 'abcdef'");

            bytes.clear();
            bytes.append("line1\n\n");
            BytesUtil.combineDoubleNewline(bytes);
            assertEquals("line1\n", bytes.toString(), "combineDoubleNewline should reduce double newline to single newline");

            bytes.clear();
            bytes.append("a \n");
            BytesUtil.combineDoubleNewline(bytes);
            assertEquals("a\n", bytes.toString(), "combineDoubleNewline should remove trailing space before newline");

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void findAndReadFileLiteral() throws Exception {
        // exercise literal path in readFile
        Bytes<?> literal = BytesUtil.readFile("=XYZ");
        try {
            assertEquals("XYZ", literal.toString(), "readFile with '=' prefix should return literal content 'XYZ'");
        } finally {
            literal.releaseLast();
        }
    }

    @Test
    public void findFileThrowsWhenMissing() throws Exception {
        assertThrows(FileNotFoundException.class, () ->
                BytesUtil.findFile("this-file-should-not-exist-chronicle-bytes"));
    }

    private static String combineDoubleNewlineResult(String input) {
        final Bytes<byte[]> bytes = Bytes.from(input);
        try {
            BytesUtil.combineDoubleNewline(bytes);
            return bytes.toString();
        } finally {
            bytes.releaseLast();
        }
    }

    static class A {
        int i;
        long l;
        double d;
    }

    static class A2 extends A {
        short s;
        char ch;

        // Accessed reflectively via BytesUtil.triviallyCopyableRange; this assignment keeps static analysis satisfied.
        @SuppressWarnings("unused")
        A2() {
            this.s = 1;
            this.ch = 'x';
        }
    }

    private static class A3 extends A2 {
        String user;

        @SuppressWarnings("unused")
        A3() {
            this.user = "user";
        }
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

    private static class SubNested extends Nested {
        int j;
    }
}
