package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.*;

public class MoreBytesTest {
    private static void testIndexOf(@NotNull String sourceStr, @NotNull String subStr) {
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        Assert.assertEquals(sourceStr.indexOf(subStr), source.indexOf(subBytes));
    }

    @Test
    public void testOneRelease() {
        int count = 0;
        for (@NotNull Bytes b : new Bytes[]{
                Bytes.allocateDirect(10),
                Bytes.allocateDirect(new byte[5]),
                Bytes.allocateElasticDirect(100),
                Bytes.elasticByteBuffer(),
                Bytes.wrapForRead(new byte[1]),
                Bytes.wrapForRead(ByteBuffer.allocateDirect(128)),
                Bytes.wrapForWrite(new byte[1]),
                Bytes.wrapForWrite(ByteBuffer.allocateDirect(128)),
                Bytes.elasticHeapByteBuffer(),
                Bytes.elasticHeapByteBuffer(1),
                Bytes.allocateElasticOnHeap(),
                Bytes.allocateElasticOnHeap(1)
        }) {
            try {
                assertEquals(count + ": " + b.getClass().getSimpleName(), 1, b.refCount());
                assertEquals(count + ": " + b.getClass().getSimpleName(), 1, b.bytesStore().refCount());
            } finally {
                b.releaseLast();
                assertEquals(count + ": " + b.getClass().getSimpleName(), 0, b.refCount());
                assertEquals(count++ + ": " + b.getClass().getSimpleName(), 0, b.bytesStore().refCount());
            }
        }
    }

    @Test
    public void testAppendLongRandomPosition() {
        @NotNull byte[] bytes = "00000".getBytes(ISO_8859_1);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        Bytes to = Bytes.wrapForWrite(bb);
        try {
            to.append(0, 1, 5);
            assertEquals("00001", Bytes.wrapForRead(bb).toString());
        } finally {
            to.releaseLast();
        }
    }

    @Test
    public void testAppendLongRandomPosition2() {
        @NotNull byte[] bytes = "WWWWW00000".getBytes(ISO_8859_1);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        Bytes to = Bytes.wrapForWrite(bb);
        try {
            to.append(5, 10, 5);
            Bytes<ByteBuffer> bbb = Bytes.wrapForRead(bb);
            assertEquals("WWWWW00010", bbb.toString());
            bbb.releaseLast();
        } finally {
            to.releaseLast();
        }
    }

    @Test
    public void testAppendLongRandomPositionShouldThrowBufferOverflowException() {
        try {
            @NotNull byte[] bytes = "000".getBytes(ISO_8859_1);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            Bytes to = Bytes.wrapForWrite(bb);
            try {
                to.append(0, 1000, 5);
                fail("Should throw Exception");
            } finally {
                to.releaseLast();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAppendLongRandomPositionShouldThrowIllegalArgumentException() {
        try {
            @NotNull byte[] bytes = "000".getBytes(ISO_8859_1);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            Bytes to = Bytes.wrapForWrite(bb);
            try {
                to.append(0, 1000, 3);
            } finally {
                to.releaseLast();
            }
            fail("Should throw Exception");
        } catch (BufferOverflowException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testAppendDoubleRandomPosition() {
        @NotNull byte[] bytes = "000000".getBytes(ISO_8859_1);
        Bytes to = Bytes.wrapForWrite(bytes);
        try {
            to.append(0, 3.14, 2, 6);
        } finally {
            to.releaseLast();
        }
        assertEquals("003.14", Bytes.wrapForRead(bytes).toString());
    }

    @Test
    public void testAppendDoubleRandomPositionShouldThrowBufferOverflowException() {
        try {
            @NotNull byte[] bytes = "000000".getBytes(ISO_8859_1);
            Bytes to = Bytes.wrapForWrite(bytes);
            try {
                to.append(0, 3.14, 2, 8);
            } finally {
                to.releaseLast();
            }
            fail("Should throw Exception");
        } catch (BufferOverflowException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAppendDoubleRandomPositionShouldThrowIllegalArgumentException() {
        try {
            @NotNull byte[] bytes = "000000".getBytes(ISO_8859_1);
            Bytes to = Bytes.wrapForWrite(bytes);
            try {
                to.append(0, 33333.14, 2, 6);
            } finally {
                to.releaseLast();
            }
            fail("Should throw Exception");
        } catch (BufferOverflowException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidUTF8Scan() {
        int expected = 0;
        for (int i = 0x80; i <= 0xFF; i++)
            for (int j = 0x80; j <= 0xFF; j++) {
                @NotNull byte[] b = {(byte) i, (byte) j};
                @NotNull String s = new String(b, StandardCharsets.UTF_8);
                if (s.charAt(0) == 65533) {
                    Bytes bytes = Bytes.wrapForRead(b);
                    try {
                        bytes.parseUtf8(StopCharTesters.ALL);
                        fail(Arrays.toString(b));
                    } catch (UTFDataFormatRuntimeException e) {
                        expected++;
                    }
                }
            }
        assertEquals(14464, expected);
    }

    @Test
    public void internBytes() throws IORuntimeException {
        Bytes b = Bytes.from("Hello World");
        try {
            b.readSkip(6);
            {
                @NotNull StringInterner si = new StringInterner(128);
                @Nullable String s = si.intern(b);
                @Nullable String s2 = si.intern(b);
                assertEquals("World", s);
                assertSame(s, s2);
            }
            {
                @NotNull UTF8StringInterner si = new UTF8StringInterner(128);
                String s = si.intern(b);
                String s2 = si.intern(b);
                assertEquals("World", s);
                assertSame(s, s2);
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testIndexOfExactMatchAfterReadSkip() {
        String sourceStr = " some";
        String subStr = "some";
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        source.readSkip(1);
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        Assert.assertEquals(0, source.indexOf(subBytes));
    }

    @Test
    public void testIndexOfExactMatchAfterReadSkipOnSubStr() {
        String sourceStr = "some";
        String subStr = " some";
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        subBytes.readSkip(1);

        Assert.assertEquals(0, source.indexOf(subBytes));
        assertEquals(1, subBytes.readPosition());
        assertEquals(0, source.readPosition());
    }

    @Test
    public void testIndexOfAtEnd() {
        String sourceStr = "A string of some data";
        String subStr = "ta";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfEmptySubStr() {
        String sourceStr = "A string of some data";
        String subStr = "";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfEmptySubStrAndSource() {
        String sourceStr = "";
        String subStr = "";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfEmptySource() {
        String sourceStr = "";
        String subStr = "some";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfExactMatch() {
        String sourceStr = "some";
        String subStr = "some";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfIncorrectExactMatch() {
        String sourceStr = "some";
        String subStr = " some";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfExactMatchAtChar1() {
        String sourceStr = " some";
        String subStr = "some";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfLastChar() {
        String sourceStr = " some";
        String subStr = "e";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testCharAt() {
        Bytes b = Bytes.from("Hello World");
        try {
            b.readSkip(6);
            assertTrue(StringUtils.isEqual("World", b));
        } finally {
            b.releaseLast();
        }
    }


    @Test
    public void testReadWithLength() {
        Bytes b = Bytes.from("Hello World");
        final Bytes<ByteBuffer> bytesOut = Bytes.elasticByteBuffer();
        try {
            b.readWithLength(2, bytesOut);
            assertEquals("He", bytesOut.toString());
        } finally {
            b.releaseLast();
            bytesOut.releaseLast();
        }
    }

    @Test
    public void testStartsWith() {
        Bytes<?> aaa = Bytes.from("aaa");
        Bytes<?> a = Bytes.from("a");
        assertTrue(aaa.startsWith(a));
        Bytes<?> aa = Bytes.from("aa");
        assertTrue(aaa.startsWith(aa));
        assertTrue(aaa.startsWith(aaa));
        Bytes<?> aaaa = Bytes.from("aaaa");
        assertFalse(aaa.startsWith(aaaa));
        Bytes<?> b = Bytes.from("b");
        assertFalse(aaa.startsWith(b));
        a.releaseLast();
        aa.releaseLast();
        aaa.releaseLast();
        aaaa.releaseLast();
        b.releaseLast();
    }
}
