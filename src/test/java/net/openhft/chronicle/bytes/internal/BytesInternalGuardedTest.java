/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@SuppressWarnings("rawtypes")
class BytesInternalGuardedTest extends BytesTestCommon {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("Unguarded", false),
                Arguments.of("Guarded", true)
        );
    }

    @AfterAll
    static void resetGuarded() {
        NativeBytes.resetNewGuarded();
    }

    private void setGuarded(boolean guarded) {
        NativeBytes.setNewGuarded(guarded);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testParse8bitAndStringBuilderWithUtf16Coder(String name, boolean guarded)
            throws BufferUnderflowException, IOException {
        setGuarded(guarded);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        @NotNull BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        bs.write(0, new byte[]{0x76, 0x61, 0x6c, 0x75, 0x65}); // "value" string

        StringBuilder sb = new StringBuilder();
        sb.append("\u4f60\u597d");

        BytesInternal.parse8bit(0, bs, sb, 5);
        String actual = sb.toString();

        assertEquals("value", actual);
        assertEquals(5, actual.length());
        bs.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testCompareUTF(String name, boolean guarded)
            throws IORuntimeException {
        setGuarded(guarded);
        @NotNull BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        bs.writeUtf8(0, "test");
        assertTrue(BytesInternal.compareUtf8(bs, 0, "test"));
        assertFalse(BytesInternal.compareUtf8(bs, 0, null));

        bs.writeUtf8(0, null);
        assertTrue(BytesInternal.compareUtf8(bs, 0, null));
        assertFalse(BytesInternal.compareUtf8(bs, 0, "test"));

        bs.writeUtf8(1, "£\u20ac");
        @NotNull StringBuilder sb = new StringBuilder();
        bs.readUtf8(1, sb);
        assertEquals("£\u20ac", sb.toString());
        assertTrue(BytesInternal.compareUtf8(bs, 1, "£\u20ac"));
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£"));
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£\u20ac$"));
        bs.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void shouldHandleDifferentSizedStores(String name, boolean guarded) {
        setGuarded(guarded);
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(32);
        final BytesStore<?, ?> storeOfThirtyTwoBytes = bytes.bytesStore();
        storeOfThirtyTwoBytes.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        Bytes<ByteBuffer> bytes2 = Bytes.elasticHeapByteBuffer(512);
        final BytesStore<?, ?> longerBuffer = bytes2.bytesStore();
        longerBuffer.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        assertTrue(BytesInternal.equalBytesAny(storeOfThirtyTwoBytes, longerBuffer, 32));
        bytes2.releaseLast();
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testWritingDecimalVsJava(String name, boolean guarded) {
        setGuarded(guarded);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        bytes.clear();
        double d = 0.04595828484241039; //Math.pow(1e9, rand.nextDouble()) / 1e3;
        bytes.append(d);
        String s = Double.toString(d);
        if (s.length() != bytes.readRemaining()) {
            assertEquals(d, Double.parseDouble(s), 0.0);
            String s2 = bytes.toString();
//            System.out.println(s + " != " + s2);
        }
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void contentsEqual(String name, boolean guarded) {
        setGuarded(guarded);
        assumeFalse(Jvm.maxDirectMemory() == 0);

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
        assertEquals("Hello\0\0\0\0", actual1);
        String actual2 = b.toString();
        assertEquals("Hello", actual2);
        String actual3 = c.toString();
        assertEquals("Hello\0\0\0\0\0\0\0\0\0\0", actual3);
        assertTrue(a.contentEquals(b));
        assertTrue(b.contentEquals(c));
        assertTrue(c.contentEquals(a));
        a.releaseLast();
        b.releaseLast();
        c.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testStopBits(String name, boolean guarded) {
        setGuarded(guarded);
        final VanillaBytes<Void> bytes = Bytes.allocateDirect(10);

        for (int i = 0; i < (1L << (2 * 7)) + 1; i++) {
            bytes.writePosition(0);
            bytes.clearAndPad(10);
            bytes.writePosition(0);
            BytesInternal.writeStopBit(bytes, i);

            bytes.readPosition(0);
            final long l = BytesInternal.readStopBit(bytes);

            // System.out.printf("0x%04x : %02x %02x %02x%n", i, bytes.readByte(0), bytes.readByte(1), bytes.readByte(3));

            assertEquals(i, l);
        }

        bytes.releaseLast();
    }
}
