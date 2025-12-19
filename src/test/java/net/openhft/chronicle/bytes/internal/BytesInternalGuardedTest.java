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
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"rawtypes", "deprecation"})
public class BytesInternalGuardedTest extends BytesTestCommon {

    private boolean guarded;

    public void initBytesInternalGuardedTest(String name, boolean guarded) {
        this.guarded = guarded;
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Unguarded", false},
                {"Guarded", true}
        });
    }

    @AfterAll
    public static void resetGuarded() {
        NativeBytes.resetNewGuarded();
    }

    @BeforeEach
    public void setGuarded() {
        NativeBytes.setNewGuarded(guarded);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testParse8bitAndStringBuilderWithUtf16Coder(String name, boolean guarded)
            throws BufferUnderflowException, IOException {
        initBytesInternalGuardedTest(name, guarded);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        @NotNull BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        bs.write(0, new byte[]{0x76, 0x61, 0x6c, 0x75, 0x65}); // "value" string

        StringBuilder sb = new StringBuilder();
        sb.append("\u4f60\u597d");

        BytesInternal.parse8bit(0, bs, sb, 5);
        String actual = sb.toString();

        assertEquals("value", actual, "parse8bit should correctly append to StringBuilder with UTF-16 coder");
        assertEquals(5, actual.length(), "parsed string length should be 5 characters after appending 'value' to UTF-16 StringBuilder");
        bs.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testCompareUTF(String name, boolean guarded)
            throws IORuntimeException {
        initBytesInternalGuardedTest(name, guarded);
        @NotNull BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        bs.writeUtf8(0, "test");
        assertTrue(BytesInternal.compareUtf8(bs, 0, "test"), "compareUtf8 should return true when stored and expected strings match");
        assertFalse(BytesInternal.compareUtf8(bs, 0, null), "compareUtf8 should return false when comparing non-null stored value to null");

        bs.writeUtf8(0, null);
        assertTrue(BytesInternal.compareUtf8(bs, 0, null), "compareUtf8 should return true when both stored and expected are null");
        assertFalse(BytesInternal.compareUtf8(bs, 0, "test"), "compareUtf8 should return false when comparing null stored value to non-null");

        bs.writeUtf8(1, "£\u20ac");
        @NotNull StringBuilder sb = new StringBuilder();
        bs.readUtf8(1, sb);
        assertEquals("£\u20ac", sb.toString(), "read UTF-8 content should match written unicode string with pound and euro symbols");
        assertTrue(BytesInternal.compareUtf8(bs, 1, "£\u20ac"), "compareUtf8 should return true for matching unicode strings");
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£"), "compareUtf8 should return false when expected string is prefix of stored");
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£\u20ac$"), "compareUtf8 should return false when stored string is prefix of expected");
        bs.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void shouldHandleDifferentSizedStores(String name, boolean guarded) {
        initBytesInternalGuardedTest(name, guarded);
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(32);
        final BytesStore<?, ?> storeOfThirtyTwoBytes = bytes.bytesStore();
        storeOfThirtyTwoBytes.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        Bytes<ByteBuffer> bytes2 = Bytes.elasticHeapByteBuffer(512);
        final BytesStore<?, ?> longerBuffer = bytes2.bytesStore();
        longerBuffer.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        assertTrue(BytesInternal.equalBytesAny(storeOfThirtyTwoBytes, longerBuffer, 32), "BytesInternal.equalBytesAny");
        bytes2.releaseLast();
        bytes.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testWritingDecimalVsJava(String name, boolean guarded) {
        initBytesInternalGuardedTest(name, guarded);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        bytes.clear();
        double d = 0.04595828484241039; //Math.pow(1e9, rand.nextDouble()) / 1e3;
        bytes.append(d);
        String s = Double.toString(d);
        if (s.length() != bytes.readRemaining()) {
            assertEquals(d, Double.parseDouble(s), 0.0, "Double.parseDouble");
            String s2 = bytes.toString();
//            System.out.println(s + " != " + s2);
        }
        bytes.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void contentsEqual(String name, boolean guarded) {
        initBytesInternalGuardedTest(name, guarded);
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
        assertEquals("Hello\0\0\0\0", actual1, "9-byte buffer should show Hello with 4 trailing nulls");
        String actual2 = b.toString();
        assertEquals("Hello", actual2, "5-byte buffer should show Hello with no trailing nulls");
        String actual3 = c.toString();
        assertEquals("Hello\0\0\0\0\0\0\0\0\0\0", actual3, "15-byte buffer should show Hello with 10 trailing nulls");
        assertTrue(a.contentEquals(b), "a.contentEquals");
        assertTrue(b.contentEquals(c), "b.contentEquals");
        assertTrue(c.contentEquals(a), "c.contentEquals");
        a.releaseLast();
        b.releaseLast();
        c.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testStopBits(String name, boolean guarded) {
        initBytesInternalGuardedTest(name, guarded);
        final VanillaBytes<Void> bytes = Bytes.allocateDirect(10);

        for (int i = 0; i < (1L << (2 * 7)) + 1; i++) {
            bytes.writePosition(0);
            bytes.clearAndPad(10);
            bytes.writePosition(0);
            BytesInternal.writeStopBit(bytes, i);

            bytes.readPosition(0);
            final long l = BytesInternal.readStopBit(bytes);

            // System.out.printf("0x%04x : %02x %02x %02x%n", i, bytes.readByte(0), bytes.readByte(1), bytes.readByte(3));

            assertEquals(i, l, "stop-bit encoding round-trip should preserve value");
        }

        bytes.releaseLast();
    }
}
