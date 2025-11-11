/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

@SuppressWarnings("rawtypes")
@RunWith(Parameterized.class)
public class BytesInternalGuardedTest extends BytesTestCommon {

    private final boolean guarded;

    public BytesInternalGuardedTest(String name, boolean guarded) {
        this.guarded = guarded;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Unguarded", false},
                {"Guarded", true}
        });
    }

    @AfterClass
    public static void resetGuarded() {
        NativeBytes.resetNewGuarded();
    }

    @Before
    public void setGuarded() {
        NativeBytes.setNewGuarded(guarded);
    }

    @Test
    public void testParse8bitAndStringBuilderWithUtf16Coder()
            throws BufferUnderflowException, IOException {
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

    @Test
    public void testCompareUTF()
            throws IORuntimeException {
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

    @Test
    public void shouldHandleDifferentSizedStores() {
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

    @Test
    public void testWritingDecimalVsJava() {
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

    @Test
    public void contentsEqual() {
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

    @Test
    public void testStopBits() {
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
