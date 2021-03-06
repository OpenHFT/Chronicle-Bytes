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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.*;

public class MoreBytesTest {

    private static void testIndexOf(@NotNull final String sourceStr, @NotNull final String subStr) {
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        Assert.assertEquals(sourceStr.indexOf(subStr), source.indexOf(subBytes));
    }

    @Test
    public void testOneRelease() {
        int count = 0;
        for (@NotNull Bytes<?> b : new Bytes[]{
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
        final @NotNull byte[] bytes = "00000".getBytes(ISO_8859_1);
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final Bytes<?> to = Bytes.wrapForWrite(bb);
        try {
            to.append(0, 1, 5);
            assertEquals("00001", Bytes.wrapForRead(bb).toString());
        } finally {
            to.releaseLast();
        }
    }

    @Test
    public void testAppendLongRandomPosition2() {
        final @NotNull byte[] bytes = "WWWWW00000".getBytes(ISO_8859_1);
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final Bytes<?> to = Bytes.wrapForWrite(bb);
        try {
            to.append(5, 10, 5);
            final Bytes<ByteBuffer> bbb = Bytes.wrapForRead(bb);
            assertEquals("WWWWW00010", bbb.toString());
            bbb.releaseLast();
        } finally {
            to.releaseLast();
        }
    }

    @Test
    public void testAppendLongRandomPositionShouldThrowBufferOverflowException() {
        try {
            final @NotNull byte[] bytes = "000".getBytes(ISO_8859_1);
            final ByteBuffer bb = ByteBuffer.wrap(bytes);
            final Bytes<?> to = Bytes.wrapForWrite(bb);
            try {
                to.append(0, 1000, 5);
                fail("Should throw Exception");
            } finally {
                to.releaseLast();
            }
        } catch (Exception ignore) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAppendLongRandomPositionShouldThrowIllegalArgumentException() {
        try {
            final @NotNull byte[] bytes = "000".getBytes(ISO_8859_1);
            final ByteBuffer bb = ByteBuffer.wrap(bytes);
            final Bytes<?> to = Bytes.wrapForWrite(bb);
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
        final @NotNull byte[] bytes = "000000".getBytes(ISO_8859_1);
        final Bytes<?> to = Bytes.wrapForWrite(bytes);
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
            final @NotNull byte[] bytes = "000000".getBytes(ISO_8859_1);
            final Bytes<?> to = Bytes.wrapForWrite(bytes);
            try {
                to.append(0, 3.14, 2, 8);
            } finally {
                to.releaseLast();
            }
            fail("Should throw Exception");
        } catch (BufferOverflowException ignore) {
            // Ignore
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAppendDoubleRandomPositionShouldThrowIllegalArgumentException() {

        final @NotNull byte[] bytes = "000000".getBytes(ISO_8859_1);
        final Bytes<?> to = Bytes.wrapForWrite(bytes);
        try {
            to.append(0, 33333.14, 2, 6);
        } finally {
            to.releaseLast();
        }

    }

    @Test
    public void testInvalidUTF8Scan() {
        int expected = 0;
        for (int i = 0x80; i <= 0xFF; i++)
            for (int j = 0x80; j <= 0xFF; j++) {
                final @NotNull byte[] b = {(byte) i, (byte) j};
                final @NotNull String s = new String(b, StandardCharsets.UTF_8);
                if (s.charAt(0) == 65533) {
                    final Bytes<?> bytes = Bytes.wrapForRead(b);
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
    public void internBytes()
            throws IORuntimeException {
        final Bytes<?> b = Bytes.from("Hello World");
        try {
            b.readSkip(6);
            {
                final @NotNull StringInterner si = new StringInterner(128);
                final @Nullable String s = si.intern(b);
                final @Nullable String s2 = si.intern(b);
                assertEquals("World", s);
                assertSame(s, s2);
            }
            {
                final @NotNull UTF8StringInterner si = new UTF8StringInterner(128);
                final String s = si.intern(b);
                final String s2 = si.intern(b);
                assertEquals("World", s);
                assertSame(s, s2);
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testIndexOfExactMatchAfterReadSkip() {
        final String sourceStr = " some";
        final String subStr = "some";
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        source.readSkip(1);
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        Assert.assertEquals(0, source.indexOf(subBytes));
    }

    @Test
    public void testIndexOfExactMatchAfterReadSkipOnSubStr() {
        final String sourceStr = "some";
        final String subStr = " some";
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        subBytes.readSkip(1);

        Assert.assertEquals(0, source.indexOf(subBytes));
        assertEquals(1, subBytes.readPosition());
        assertEquals(0, source.readPosition());
    }

    @Test
    public void testIndexOfAtEnd() {
        final String sourceStr = "A string of some data";
        final String subStr = "ta";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfEmptySubStr() {
        final String sourceStr = "A string of some data";
        final String subStr = "";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfEmptySubStrAndSource() {
        final String sourceStr = "";
        final String subStr = "";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfEmptySource() {
        final String sourceStr = "";
        final String subStr = "some";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfExactMatch() {
        final String sourceStr = "some";
        final String subStr = "some";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfIncorrectExactMatch() {
        final String sourceStr = "some";
        final String subStr = " some";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfExactMatchAtChar1() {
        final String sourceStr = " some";
        final String subStr = "some";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testIndexOfLastChar() {
        final String sourceStr = " some";
        final String subStr = "e";
        testIndexOf(sourceStr, subStr);
    }

    @Test
    public void testCharAt() {
        final Bytes<?> b = Bytes.from("Hello World");
        try {
            b.readSkip(6);
            assertTrue(StringUtils.isEqual("World", b));
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testReadWithLength()
            throws BufferUnderflowException, IllegalStateException {
        final Bytes<?> b = Bytes.from("Hello World");
        final Bytes<ByteBuffer> bytesOut = Bytes.elasticByteBuffer();
        try {
            // Todo: Why is this cast needed?
            b.readWithLength(2, (Bytes) bytesOut);
            assertEquals("He", bytesOut.toString());
        } finally {
            b.releaseLast();
            bytesOut.releaseLast();
        }
    }

    @Test
    public void testStartsWith() {
        final Bytes<?> aaa = Bytes.from("aaa");
        final Bytes<?> a = Bytes.from("a");
        assertTrue(aaa.startsWith(a));
        final Bytes<?> aa = Bytes.from("aa");
        assertTrue(aaa.startsWith(aa));
        assertTrue(aaa.startsWith(aaa));
        final Bytes<?> aaaa = Bytes.from("aaaa");
        assertFalse(aaa.startsWith(aaaa));
        final Bytes<?> b = Bytes.from("b");
        assertFalse(aaa.startsWith(b));
        a.releaseLast();
        aa.releaseLast();
        aaa.releaseLast();
        aaaa.releaseLast();
        b.releaseLast();
    }

    @Test
    public void testDoesNotRequire3xCapacity() {
        final String SYMBOL_STR = "LCOM1";
        final Bytes SYMBOL = Bytes.allocateDirect(SYMBOL_STR.length());
        SYMBOL.clear();
        SYMBOL.append(SYMBOL_STR);
    }
}
