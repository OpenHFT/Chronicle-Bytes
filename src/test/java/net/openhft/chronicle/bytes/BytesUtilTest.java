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
        assumeFalse(Jvm.isAzulZing());

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
        assumeFalse(Jvm.isAzulZing());

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
        assumeFalse(Jvm.isAzulZing());
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

    class Nested {
        // implicit this$0
        int i;
    }

    class SubNested extends Nested {
        int j;
    }
}
