package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@SuppressWarnings("rawtypes")
public class BytesUtilTest extends BytesTestCommon {
    @Test
    public void fromFileInJar()
            throws IOException {
        Bytes bytes = BytesUtil.readFile("/net/openhft/chronicle/core/onoes/Google.properties");
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
        assertFalse(BytesUtil.isTriviallyCopyable(Nested.class));
        assertTrue(BytesUtil.isTriviallyCopyable(Nested.class, start, 4));
        assertFalse(BytesUtil.isTriviallyCopyable(SubNested.class));
        assertTrue(BytesUtil.isTriviallyCopyable(SubNested.class, start, 4));
        // TODO allow a portion of B to be trivially copyable
        assertFalse(BytesUtil.isTriviallyCopyable(B.class));
        assertTrue(BytesUtil.isTriviallyCopyable(B.class, start, 20));
        assertFalse(BytesUtil.isTriviallyCopyable(C.class));
        assertTrue(BytesUtil.isTriviallyCopyable(C.class, start, 4));

        assertTrue(BytesUtil.isTriviallyCopyable(A.class));
        assertEquals("[" + start + ", " + (start + 20) + ", " + (start + 20) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start, 4 + 2 * 8));
        assertTrue(BytesUtil.isTriviallyCopyable(A.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start - 4, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A.class, start + 4, 4 + 2 * 8));

        assertTrue(BytesUtil.isTriviallyCopyable(A2.class));
        int size = Jvm.isAzulZing() ? 28 : 24;
        assertEquals("[" + start + ", " + (start + size) + ", " + (start + size) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A2.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start, 4 + 2 * 8 + 2 * 2));
        assertTrue(BytesUtil.isTriviallyCopyable(A2.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start - 4, 4 + 2 * 8));
        assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A2.class, start + 8, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A2.class, start + 12, 4 + 2 * 8));

        assertFalse(BytesUtil.isTriviallyCopyable(A3.class));
        // however by copying a region that is safe.
        assertEquals("[" + start + ", " + (start + size) + ", " + (start + size + 4) + "]", Arrays.toString(BytesUtil.triviallyCopyableRange(A3.class)));
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start, 4 + 2 * 8 + 2 * 2));
        assertTrue(BytesUtil.isTriviallyCopyable(A3.class, start + 4, 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start - 4, 4 + 2 * 8));
        assertEquals(Jvm.isAzulZing(), BytesUtil.isTriviallyCopyable(A3.class, start + 8, 4 + 2 * 8));
        assertFalse(BytesUtil.isTriviallyCopyable(A3.class, start + 12, 4 + 2 * 8));
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

    class Nested {
        // implicit this$0
        int i;
    }

    class SubNested extends Nested {
        int j;
    }
}
