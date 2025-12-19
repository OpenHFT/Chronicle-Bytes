/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class MoreBytesTest extends BytesTestCommon {

    private static long indexOfBytes(@NotNull final String sourceStr, @NotNull final String subStr) {
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(ISO_8859_1));
        try {
            return source.indexOf(subBytes);
        } finally {
            source.releaseLast();
            subBytes.releaseLast();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testOneRelease() {
        int count = 0;
        List<Bytes> bytesArray = new ArrayList<>(Arrays.asList(
                Bytes.allocateDirect(10),
                Bytes.allocateDirect(new byte[5]),
                Bytes.allocateElasticDirect(100),
                Bytes.wrapForRead(new byte[1]),
                Bytes.wrapForWrite(new byte[1]),
                Bytes.elasticHeapByteBuffer(),
                Bytes.elasticHeapByteBuffer(1),
                Bytes.allocateElasticOnHeap(),
                Bytes.allocateElasticOnHeap(1)
        ));
        if (Jvm.maxDirectMemory() > 0) {
            bytesArray.add(Bytes.elasticByteBuffer());
            bytesArray.add(Bytes.wrapForRead(ByteBuffer.allocateDirect(128)));
            bytesArray.add(Bytes.wrapForWrite(ByteBuffer.allocateDirect(128)));
        }
        for (Bytes<?> b : bytesArray) {
            try {
                assertEquals(1, b.refCount(), count + ": " + b.getClass().getSimpleName());
                assertEquals(1, b.bytesStore().refCount(), count + ": " + b.getClass().getSimpleName());
            } finally {
                b.releaseLast();
                assertEquals(0, b.refCount(), count + ": " + b.getClass().getSimpleName());
                assertEquals(0, b.bytesStore().refCount(), count++ + ": " + b.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void testAppendLongRandomPosition() {
        final byte[] bytes = "00000".getBytes(ISO_8859_1);
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final Bytes<?> to = Bytes.wrapForWrite(bb);
        try {
            to.append(0, 1, 5);
            assertEquals("00001", Bytes.wrapForRead(bb).toString(), "append at random position should write long value '1' at specified offset");
        } finally {
            to.releaseLast();
        }
    }

    @Test
    public void testAppendLongRandomPosition2() {
        final byte[] bytes = "WWWWW00000".getBytes(ISO_8859_1);
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final Bytes<?> to = Bytes.wrapForWrite(bb);
        try {
            to.append(5, 10, 5);
            final Bytes<ByteBuffer> bbb = Bytes.wrapForRead(bb);
            assertEquals("WWWWW00010", bbb.toString(), "append at offset 5 should write long value '10' preserving prefix 'WWWWW'");
            bbb.releaseLast();
        } finally {
            to.releaseLast();
        }
    }

    @Test
    public void testAppendLongRandomPositionShouldThrowBufferOverflowException() {
        try {
            final byte[] bytes = "000".getBytes(ISO_8859_1);
            final ByteBuffer bb = ByteBuffer.wrap(bytes);
            final Bytes<?> to = Bytes.wrapForWrite(bb);
            try {
                to.append(0, 1000, 5);
                fail("Should throw Exception");
            } finally {
                to.releaseLast();
            }
        } catch (Exception ex) {
            assertInstanceOf(BufferOverflowException.class, ex, "appending long at invalid position should throw BufferOverflowException");
        }
    }

    @Test
    public void testAppendLongRandomPositionShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            final byte[] bytes = "000".getBytes(ISO_8859_1);
            final ByteBuffer bb = ByteBuffer.wrap(bytes);
            final Bytes<?> to = Bytes.wrapForWrite(bb);
            try {
                to.append(0, 1000, 3);
            } catch (BufferOverflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                to.releaseLast();
            }
        });
    }

    @Test
    public void testAppendDoubleRandomPosition() {
        final byte[] bytes = "000000".getBytes(ISO_8859_1);
        final Bytes<?> to = Bytes.wrapForWrite(bytes);
        try {
            to.append(0, 3.14, 2, 6);
        } finally {
            to.releaseLast();
        }
        assertEquals("003.14", Bytes.wrapForRead(bytes).toString(), "append double at random position should write '3.14' with 2 decimals at offset 0");
    }

    @Test
    public void testAppendDoubleRandomPositionShouldThrowBufferOverflowException() {
        final byte[] bytes = "000000".getBytes(ISO_8859_1);
        final Bytes<?> to = Bytes.wrapForWrite(bytes);
        try {
            assertThrows(BufferOverflowException.class, () -> to.append(0, 3.14, 2, 8));
        } finally {
            to.releaseLast();
        }
    }

    @Test
    public void testAppendDoubleRandomPositionShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {

            final byte[] bytes = "000000".getBytes(ISO_8859_1);
            final Bytes<?> to = Bytes.wrapForWrite(bytes);
            try {
                to.append(0, 33333.14, 2, 6);
            } finally {
                to.releaseLast();
            }
        });
    }

    @Test
    public void testInvalidUTF8Scan() {
        int expected = 0;
        for (int i = 0x80; i <= 0xFF; i++)
            for (int j = 0x80; j <= 0xFF; j++) {
                final byte[] b = {(byte) i, (byte) j};
                final String s = new String(b, StandardCharsets.UTF_8);
                if (s.charAt(0) == 65533) {
                    final Bytes<?> bytes = Bytes.wrapForRead(b);
                    // Use a flag so that there are just one method that can throw in the try-block
                    boolean fail = false;
                    try {
                        bytes.parseUtf8(StopCharTesters.ALL);
                        fail = true;
                    } catch (UTFDataFormatRuntimeException e) {
                        expected++;
                    }
                    if (fail)
                        fail(Arrays.toString(b));
                }
            }
        assertEquals(14464, expected, "parseUtf8 should detect exactly 14464 invalid UTF-8 sequences in 0x80-0xFF byte pair test data");
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
                assertEquals("World", s, "StringInterner should intern bytes as expected string");
                assertSame(s, s2, "StringInterner should return same instance for identical bytes");
            }
            {
                final @NotNull UTF8StringInterner si = new UTF8StringInterner(128);
                final String s = si.intern(b);
                final String s2 = si.intern(b);
                assertEquals("World", s, "UTF8StringInterner should intern bytes as expected string");
                assertSame(s, s2, "UTF8StringInterner should return same instance for identical bytes");
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testIndexOfExactMatchAfterReadSkip() {
        final String sourceStr = " some";
        final String subStr = "some";
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(ISO_8859_1));
        source.readSkip(1);
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(ISO_8859_1));
        assertEquals(0, source.indexOf(subBytes), "source.indexOf");
    }

    @Test
    public void testIndexOfExactMatchAfterReadSkipOnSubStr() {
        final String sourceStr = "some";
        final String subStr = " some";
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(ISO_8859_1));
        subBytes.readSkip(1);

        assertEquals(0, source.indexOf(subBytes), "source.indexOf");
        assertEquals(1, subBytes.readPosition(), "subBytes read position should remain at 1 after indexOf call");
        assertEquals(0, source.readPosition(), "source read position should remain at 0 after indexOf call");
    }

    @Test
    public void testIndexOfAtEnd() {
        assertEquals("A string of some data".indexOf("ta"), indexOfBytes("A string of some data", "ta"), "testIndexOfAtEnd: bytes index");
    }

    @Test
    public void testIndexOfEmptySubStr() {
        assertEquals("A string of some data".indexOf(""), indexOfBytes("A string of some data", ""), "testIndexOfEmptySubStr: bytes index");
    }

    @Test
    public void testIndexOfEmptySubStrAndSource() {
        assertEquals("".indexOf(""), indexOfBytes("", ""), "testIndexOfEmptySubStrAndSource: bytes index");
    }

    @Test
    public void testIndexOfEmptySource() {
        assertEquals("".indexOf("some"), indexOfBytes("", "some"), "testIndexOfEmptySource: bytes index");
    }

    @Test
    public void testIndexOfExactMatch() {
        assertEquals("some".indexOf("some"), indexOfBytes("some", "some"), "testIndexOfExactMatch: bytes index");
    }

    @Test
    public void testIndexOfIncorrectExactMatch() {
        assertEquals("some".indexOf(" some"), indexOfBytes("some", " some"), "testIndexOfIncorrectExactMatch: bytes index");
    }

    @Test
    public void testIndexOfExactMatchAtChar1() {
        assertEquals(" some".indexOf("some"), indexOfBytes(" some", "some"), "testIndexOfExactMatchAtChar1: bytes index");
    }

    @Test
    public void testIndexOfLastChar() {
        assertEquals(" some".indexOf("e"), indexOfBytes(" some", "e"), "testIndexOfLastChar: bytes index");
    }

    @Test
    public void testCharAt() {
        final Bytes<?> b = Bytes.from("Hello World");
        try {
            b.readSkip(6);
            assertTrue(StringUtils.isEqual("World", b), "StringUtils.isEqual");
        } finally {
            b.releaseLast();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testReadWithLength()
            throws BufferUnderflowException, IllegalStateException {
        final Bytes<?> b = Bytes.from("Hello World");
        final Bytes<ByteBuffer> bytesOut = Bytes.elasticHeapByteBuffer();
        try {
            b.readWithLength(2, bytesOut);
            assertEquals("He", bytesOut.toString(), "readWithLength should read exactly 2 bytes from 'Hello World'");
        } finally {
            b.releaseLast();
            bytesOut.releaseLast();
        }
    }

    @Test
    public void testStartsWith() {
        final Bytes<?> aaa = Bytes.from("aaa");
        final Bytes<?> a = Bytes.from("a");
        assertTrue(aaa.startsWith(a), "aaa.startsWith");
        final Bytes<?> aa = Bytes.from("aa");
        assertTrue(aaa.startsWith(aa), "aaa.startsWith");
        assertTrue(aaa.startsWith(aaa), "aaa.startsWith");
        final Bytes<?> aaaa = Bytes.from("aaaa");
        assertFalse(aaa.startsWith(aaaa), "aaa.startsWith");
        final Bytes<?> b = Bytes.from("b");
        assertFalse(aaa.startsWith(b), "aaa.startsWith");
        a.releaseLast();
        aa.releaseLast();
        aaa.releaseLast();
        aaaa.releaseLast();
        b.releaseLast();
    }

    @Test
    public void testDoesNotRequire3xCapacity() {
        final String symbolStr = "LCOM1";
        final Bytes<?> symbol = Bytes.allocateDirect(symbolStr.length());
        symbol.clear();
        symbol.append(symbolStr);
        assertTrue(symbol.realCapacity() < 3L * symbolStr.length(), "real capacity should be true");
        symbol.releaseLast();
    }
}
