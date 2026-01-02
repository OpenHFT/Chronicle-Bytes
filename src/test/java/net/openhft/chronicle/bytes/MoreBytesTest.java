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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("More bytes utilities and index operations")
public class MoreBytesTest extends BytesTestCommon {

    private static void testIndexOf(@NotNull final String sourceStr, @NotNull final String subStr) {
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(sourceStr.indexOf(subStr), source.indexOf(subBytes),
                "IndexOf matches String result for source '" + sourceStr + "' and sub '" + subStr + "'");
    }

    @SuppressWarnings("rawtypes")
    @Test
    @DisplayName("release reduces reference counts for all bytes")
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
                final String type = b.getClass().getSimpleName();
                assertEquals(1, b.refCount(),
                        "Ref count starts at one for " + type + " at index " + count + " for bytes " + b);
                assertEquals(1, b.bytesStore().refCount(),
                        "BytesStore ref count starts at one for " + type + " at index " + count + " for bytes " + b);
            } finally {
                b.releaseLast();
                final String type = b.getClass().getSimpleName();
                assertEquals(0, b.refCount(),
                        "Ref count returns to zero for " + type + " at index " + count + " for bytes " + b);
                assertEquals(0, b.bytesStore().refCount(),
                        "BytesStore ref count returns to zero for " + type + " at index " + count + " for bytes " + b);
                count++;
            }
        }
    }

    @Test
    @DisplayName("append long writes into random position")
    public void testAppendLongRandomPosition() {
        final byte[] bytes = "00000".getBytes(ISO_8859_1);
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final Bytes<?> to = Bytes.wrapForWrite(bb);
        try {
            to.append(0, 1, 5);
            assertEquals("00001", Bytes.wrapForRead(bb).toString(),
                    "Append writes final digit at expected position");
        } finally {
            to.releaseLast();
        }
    }

    @Test
    @DisplayName("append long writes into later position")
    public void testAppendLongRandomPosition2() {
        final byte[] bytes = "WWWWW00000".getBytes(ISO_8859_1);
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final Bytes<?> to = Bytes.wrapForWrite(bb);
        try {
            to.append(5, 10, 5);
            final Bytes<ByteBuffer> bbb = Bytes.wrapForRead(bb);
            assertEquals("WWWWW00010", bbb.toString(),
                    "Append writes digits at later position");
            bbb.releaseLast();
        } finally {
            to.releaseLast();
        }
    }

    @Test
    @DisplayName("append long overflow throws BufferOverflowException error")
    public void testAppendLongRandomPositionShouldThrowBufferOverflowException() {
        final byte[] bytes = "000".getBytes(ISO_8859_1);
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final Bytes<?> to = Bytes.wrapForWrite(bb);
        try {
            assertThrows(BufferOverflowException.class,
                    () -> to.append(0, 1000, 5),
                    "Append beyond buffer should overflow for long value");
        } finally {
            to.releaseLast();
        }
    }

    @Test
    @DisplayName("append long width throws IllegalArgumentException error")
    public void testAppendLongRandomPositionShouldThrowIllegalArgumentException() {
        final byte[] bytes = "000".getBytes(ISO_8859_1);
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final Bytes<?> to = Bytes.wrapForWrite(bb);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> to.append(0, 1000, 3),
                    "Append with short width should reject long value");
        } finally {
            to.releaseLast();
        }
    }

    @Test
    @DisplayName("append double writes into random position")
    public void testAppendDoubleRandomPosition() {
        final byte[] bytes = "000000".getBytes(ISO_8859_1);
        final Bytes<?> to = Bytes.wrapForWrite(bytes);
        try {
            to.append(0, 3.14, 2, 6);
        } finally {
            to.releaseLast();
        }
        assertEquals("003.14", Bytes.wrapForRead(bytes).toString(),
                "Append writes double digits at expected position");
    }

    @Test
    @DisplayName("append double overflow throws BufferOverflowException error")
    public void testAppendDoubleRandomPositionShouldThrowBufferOverflowException() {
        final byte[] bytes = "000000".getBytes(ISO_8859_1);
        final Bytes<?> to = Bytes.wrapForWrite(bytes);
        try {
            assertThrows(BufferOverflowException.class,
                    () -> to.append(0, 3.14, 2, 8),
                    "Append beyond buffer should overflow for double value");
        } finally {
            to.releaseLast();
        }
    }

    @Test
    @DisplayName("append double width throws IllegalArgumentException error")
    public void testAppendDoubleRandomPositionShouldThrowIllegalArgumentException() {

        final byte[] bytes = "000000".getBytes(ISO_8859_1);
        final Bytes<?> to = Bytes.wrapForWrite(bytes);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> to.append(0, 33333.14, 2, 6),
                    "Append with short width should reject double value");
        } finally {
            to.releaseLast();
        }
    }

    @Test
    @DisplayName("invalid UTF8 scan counts expected failures")
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
                        fail("Invalid UTF8 bytes should fail parse at i " + i + " j " + j + " bytes " + Arrays.toString(b));
                }
            }
        assertEquals(14464, expected,
                "Invalid UTF8 count matches expected failure total");
    }

    @Test
    @DisplayName("interning returns identical string instances for repeats")
    public void internBytes()
            throws IORuntimeException {
        final Bytes<?> b = Bytes.from("Hello World");
        try {
            b.readSkip(6);
            {
                final @NotNull StringInterner si = new StringInterner(128);
                final @Nullable String s = si.intern(b);
                final @Nullable String s2 = si.intern(b);
                assertEquals("World", s,
                        "String interner returns expected suffix text");
                assertSame(s, s2,
                        "String interner returns pooled instance for repeat");
            }
            {
                final @NotNull UTF8StringInterner si = new UTF8StringInterner(128);
                final String s = si.intern(b);
                final String s2 = si.intern(b);
                assertEquals("World", s,
                        "UTF8 interner returns expected suffix text");
                assertSame(s, s2,
                        "UTF8 interner returns pooled instance for repeat");
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("indexOf matches after skip in source")
    public void testIndexOfExactMatchAfterReadSkip() {
        final String sourceStr = " some";
        final String subStr = "some";
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        source.readSkip(1);
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(0, source.indexOf(subBytes),
                "IndexOf matches substring after skipping source");
    }

    @Test
    @DisplayName("indexOf matches after skip in substring")
    public void testIndexOfExactMatchAfterReadSkipOnSubStr() {
        final String sourceStr = "some";
        final String subStr = " some";
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        subBytes.readSkip(1);

        assertEquals(0, source.indexOf(subBytes),
                "IndexOf matches substring after skipping sub bytes");
        assertEquals(1, subBytes.readPosition(),
                "Sub bytes read position advances by one");
        assertEquals(0, source.readPosition(),
                "Source bytes read position remains unchanged");
    }

    @Test
    @DisplayName("indexOf matches at end of source")
    public void testIndexOfAtEnd() {
        testIndexOf("A string of some data", "ta");
    }

    @Test
    @DisplayName("indexOf handles empty substring on non empty source")
    public void testIndexOfEmptySubStr() {
        testIndexOf("A string of some data", "");
    }

    @Test
    @DisplayName("indexOf handles empty source and substring")
    public void testIndexOfEmptySubStrAndSource() {
        testIndexOf("", "");
    }

    @Test
    @DisplayName("indexOf handles empty source with non empty substring")
    public void testIndexOfEmptySource() {
        testIndexOf("", "some");
    }

    @Test
    @DisplayName("indexOf matches exact substring in full source")
    public void testIndexOfExactMatch() {
        testIndexOf("some", "some");
    }

    @Test
    @DisplayName("indexOf rejects incorrect exact match case")
    public void testIndexOfIncorrectExactMatch() {
        testIndexOf("some", " some");
    }

    @Test
    @DisplayName("indexOf matches exact substring at index one")
    public void testIndexOfExactMatchAtChar1() {
        testIndexOf(" some", "some");
    }

    @Test
    @DisplayName("indexOf matches last character in source")
    public void testIndexOfLastChar() {
        testIndexOf(" some", "e");
    }

    @Test
    @DisplayName("charAt checks remaining text after skip")
    public void testCharAt() {
        final Bytes<?> b = Bytes.from("Hello World");
        try {
            b.readSkip(6);
            assertTrue(StringUtils.isEqual("World", b),
                    "StringUtils matches remaining text after skip");
        } finally {
            b.releaseLast();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    @DisplayName("readWithLength transfers bytes into output buffer")
    public void testReadWithLength()
            throws BufferUnderflowException, IllegalStateException {
        final Bytes<?> b = Bytes.from("Hello World");
        final Bytes<ByteBuffer> bytesOut = Bytes.elasticHeapByteBuffer();
        try {
            b.readWithLength(2, bytesOut);
            assertEquals("He", bytesOut.toString(),
                    "readWithLength writes expected prefix to output");
        } finally {
            b.releaseLast();
            bytesOut.releaseLast();
        }
    }

    @Test
    @DisplayName("startsWith matches various prefixes in bytes")
    public void testStartsWith() {
        final Bytes<?> aaa = Bytes.from("aaa");
        final Bytes<?> a = Bytes.from("a");
        assertTrue(aaa.startsWith(a),
                aaa + " should start with " + a);
        final Bytes<?> aa = Bytes.from("aa");
        assertTrue(aaa.startsWith(aa),
                aaa + " should start with " + aa);
        assertTrue(aaa.startsWith(aaa),
                aaa + " should start with " + aaa);
        final Bytes<?> aaaa = Bytes.from("aaaa");
        assertFalse(aaa.startsWith(aaaa),
                aaa + " should not start with " + aaaa);
        final Bytes<?> b = Bytes.from("b");
        assertFalse(aaa.startsWith(b),
                aaa + " should not start with " + b);
        a.releaseLast();
        aa.releaseLast();
        aaa.releaseLast();
        aaaa.releaseLast();
        b.releaseLast();
    }

    @Test
    @DisplayName("symbol capacity stays below triple length")
    public void testDoesNotRequire3xCapacity() {
        final String symbolStr = "LCOM1";
        final Bytes<?> symbol = Bytes.allocateDirect(symbolStr.length());
        symbol.clear();
        symbol.append(symbolStr);
        assertTrue(symbol.realCapacity() < 3L * symbolStr.length(),
                "Symbol capacity remains below triple string length");
        symbol.releaseLast();
    }
}
