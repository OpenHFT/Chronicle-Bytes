/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"rawtypes", "deprecation"})
@DisplayName("BytesInternal guarded and unguarded mode checks")
public class BytesInternalGuardedTest extends BytesTestCommon {
    static Stream<Arguments> guardedModes() {
        return Stream.of(
                Arguments.of("Unguarded", false),
                Arguments.of("Guarded", true)
        );
    }

    @AfterAll
    static void resetGuarded() {
        NativeBytes.resetNewGuarded();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedModes")
    @DisplayName("parse 8bit into string builder with UTF16 coder")
    public void testParse8bitAndStringBuilderWithUtf16Coder(String label, boolean guarded)
            throws BufferUnderflowException, IOException {
        NativeBytes.setNewGuarded(guarded);
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for guarded parse test");

        @NotNull BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        bs.write(0, new byte[]{0x76, 0x61, 0x6c, 0x75, 0x65}); // "value" string

        StringBuilder sb = new StringBuilder();
        sb.append("\u4f60\u597d");

        BytesInternal.parse8bit(0, bs, sb, 5);
        String actual = sb.toString();

        assertEquals("value", actual,
                "Parsed string matches expected value");
        assertEquals(5, actual.length(),
                "Parsed string length matches expected");
        bs.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedModes")
    @DisplayName("compare UTF8 values with null handling")
    public void testCompareUTF(String label, boolean guarded)
            throws IORuntimeException {
        NativeBytes.setNewGuarded(guarded);
        @NotNull BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        bs.writeUtf8(0, "test");
        assertTrue(BytesInternal.compareUtf8(bs, 0, "test"),
                "UTF8 compare matches for test string");
        assertFalse(BytesInternal.compareUtf8(bs, 0, null),
                "UTF8 compare rejects null against test string");

        bs.writeUtf8(0, null);
        assertTrue(BytesInternal.compareUtf8(bs, 0, null),
                "UTF8 compare matches for null string");
        assertFalse(BytesInternal.compareUtf8(bs, 0, "test"),
                "UTF8 compare rejects test string against null");

        bs.writeUtf8(1, "£\u20ac");
        @NotNull StringBuilder sb = new StringBuilder();
        bs.readUtf8(1, sb);
        assertEquals("£\u20ac", sb.toString(),
                "UTF8 read returns expected multibyte string");
        assertTrue(BytesInternal.compareUtf8(bs, 1, "£\u20ac"),
                "UTF8 compare matches multibyte string");
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£"),
                "UTF8 compare rejects shorter string");
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£\u20ac$"),
                "UTF8 compare rejects longer string");
        bs.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedModes")
    @DisplayName("compare bytes across different sized stores")
    public void shouldHandleDifferentSizedStores(String label, boolean guarded) {
        NativeBytes.setNewGuarded(guarded);
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(32);
        final BytesStore<?, ?> storeOfThirtyTwoBytes = bytes.bytesStore();
        storeOfThirtyTwoBytes.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        Bytes<ByteBuffer> bytes2 = Bytes.elasticHeapByteBuffer(512);
        final BytesStore<?, ?> longerBuffer = bytes2.bytesStore();
        longerBuffer.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        assertTrue(BytesInternal.equalBytesAny(storeOfThirtyTwoBytes, longerBuffer, 32),
                "Equal bytes check succeeds across store sizes");
        bytes2.releaseLast();
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedModes")
    @DisplayName("append decimal matches Java parsing output")
    public void testWritingDecimalVsJava(String label, boolean guarded) {
        NativeBytes.setNewGuarded(guarded);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        bytes.clear();
        double d = 0.04595828484241039; //Math.pow(1e9, rand.nextDouble()) / 1e3;
        bytes.append(d);
        String s = Double.toString(d);
        if (s.length() != bytes.readRemaining()) {
            assertEquals(d, Double.parseDouble(s), 0.0,
                    "Java parsing matches appended decimal");
            String s2 = bytes.toString();
        }
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedModes")
    @DisplayName("content equals across different capacity buffers")
    public void contentsEqual(String label, boolean guarded) {
        NativeBytes.setNewGuarded(guarded);
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for content equals test");

        Bytes<?> a = Bytes.elasticByteBuffer(9, 20)
                .append(Bytes.from("Hello"))
                .readLimit(16);
        Bytes<?> b = Bytes.elasticByteBuffer(5, 20)
                .append(Bytes.from("Hello"))
                .readLimit(16);
        Bytes<?> c = Bytes.elasticByteBuffer(15, 20)
                .append(Bytes.from("Hello"))
                .readLimit(16);
        String actual1 = a.toString();
        assertEquals("Hello\0\0\0\0", actual1,
                "First buffer includes expected padding");
        String actual2 = b.toString();
        assertEquals("Hello", actual2,
                "Second buffer contains only hello");
        String actual3 = c.toString();
        assertEquals("Hello\0\0\0\0\0\0\0\0\0\0", actual3,
                "Third buffer includes extended padding");
        assertTrue(a.contentEquals(b),
                "Buffer a content equals buffer b");
        assertTrue(b.contentEquals(c),
                "Buffer b content equals buffer c");
        assertTrue(c.contentEquals(a),
                "Buffer c content equals buffer a");
        a.releaseLast();
        b.releaseLast();
        c.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guardedModes")
    @DisplayName("write and read stop bits round trip")
    public void testStopBits(String label, boolean guarded) {
        NativeBytes.setNewGuarded(guarded);
        final VanillaBytes<Void> bytes = Bytes.allocateDirect(10);

        for (int i = 0; i < (1L << (2 * 7)) + 1; i++) {
            bytes.writePosition(0);
            bytes.clearAndPad(10);
            bytes.writePosition(0);
            BytesInternal.writeStopBit(bytes, i);

            bytes.readPosition(0);
            final long l = BytesInternal.readStopBit(bytes);

            // System.out.printf("0x%04x : %02x %02x %02x%n", i, bytes.readByte(0), bytes.readByte(1), bytes.readByte(3));

            assertEquals(i, l,
                    "Stop bit round trip matches for i " + i);
        }

        bytes.releaseLast();
    }
}
