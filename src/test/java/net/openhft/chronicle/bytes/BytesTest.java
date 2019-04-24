/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.threads.ThreadDump;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Allocator.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

@SuppressWarnings({"rawtypes", "unchecked"})
@RunWith(Parameterized.class)
public class BytesTest {

    private Allocator alloc1;
    private ThreadDump threadDump;

    public BytesTest(String name, Allocator alloc1) {
        this.alloc1 = alloc1;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Native", NATIVE},
                {"Heap", HEAP},
                {"Native Unchecked", NATIVE_UNCHECKED},
                {"Heap Unchecked", HEAP_UNCHECKED}
        });
    }

    private static void testIndexOf(@NotNull String sourceStr, @NotNull String subStr) {
        final Bytes<?> source = Bytes.wrapForRead(sourceStr.getBytes(StandardCharsets.ISO_8859_1));
        final Bytes<?> subBytes = Bytes.wrapForRead(subStr.getBytes(StandardCharsets.ISO_8859_1));
        Assert.assertEquals(sourceStr.indexOf(subStr), source.indexOf(subBytes));
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @Test
    public void checkRefCount() {
        Bytes bytes = alloc1.elasticBytes(16);
        bytes.checkRefCount();
        bytes.release();
        try {
            bytes.checkRefCount();
            fail();
        } catch (IllegalStateException ise) {
            // expected.
        }
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
        Assert.assertTrue(subBytes.readPosition() == 1);
        Assert.assertTrue(source.readPosition() == 0);
    }

    @Test
    public void writeAdv() {
        Bytes<?> bytes = alloc1.fixedBytes(32);
        for (int i = 0; i < 4; i++)
            bytes.writeIntAdv('1', 1);
        assertEquals("1111", bytes.toString());
        bytes.release();
    }

    @Test
    public void writeLongAdv() {
        Bytes<?> bytes = alloc1.fixedBytes(32);
        for (int i = 0; i < 4; i++)
            bytes.writeLongAdv('1', 1);
        assertEquals("1111", bytes.toString());
        bytes.release();
    }

    @Test
    public void testName() throws IORuntimeException {
        Bytes<?> bytes = alloc1.fixedBytes(30);
        try {
            long expected = 12345L;
            int offset = 5;

            bytes.writeLong(offset, expected);
            bytes.writePosition(offset + 8);
            assertEquals(expected, bytes.readLong(offset));
        } finally {
            bytes.release();
        }
    }

    @Test
    public void readUnsignedByte() {
        Bytes<?> bytes = alloc1.fixedBytes(30);
        try {
            bytes.writeInt(0x11111111);
            bytes.readLimit(1);

            assertEquals(0x11, bytes.readUnsignedByte(0));
            assertEquals(-1, bytes.peekUnsignedByte(1));

            // as the offset is given it only needs to be under the writeLimit.
            assertEquals(0x11, bytes.readUnsignedByte(1));

        } finally {
            bytes.release();
        }

    }

    @Test
    public void writeHistogram() {
        @NotNull Bytes bytes = alloc1.elasticBytes(0xFFFFF);
        @NotNull Histogram hist = new Histogram();
        hist.sample(10);
        @NotNull Histogram hist2 = new Histogram();
        for (int i = 0; i < 10000; i++)
            hist2.sample(i);

        bytes.writeHistogram(hist);
        bytes.writeHistogram(hist2);

        @NotNull Histogram histB = new Histogram();
        @NotNull Histogram histC = new Histogram();
        bytes.readHistogram(histB);
        bytes.readHistogram(histC);

        assertEquals(hist, histB);
        assertEquals(hist2, histC);
        bytes.release();
    }

    @Test
    public void testCopy() {
        Bytes<ByteBuffer> bbb = alloc1.fixedBytes(1024);
        try {
            for (int i = 'a'; i <= 'z'; i++)
                bbb.writeUnsignedByte(i);
            bbb.readPositionRemaining(4, 12);
            BytesStore<Bytes<ByteBuffer>, ByteBuffer> copy = bbb.copy();
            bbb.writeUnsignedByte(10, '0');
            assertEquals("[pos: 0, rlim: 12, wlim: 12, cap: 12 ] efghijklmnop", copy.toDebugString());
        } finally {
            bbb.release();
        }

    }

    @Test
    public void toHexString() {
        Bytes bytes = alloc1.elasticBytes(1020);
        try {
            bytes.append("Hello World");
            assertEquals("00000000 48 65 6c 6c 6f 20 57 6f  72 6c 64                Hello Wo rld     \n", bytes.toHexString());
            bytes.readLimit(bytes.realCapacity());
            assertEquals("00000000 48 65 6c 6c 6f 20 57 6f  72 6c 64 00 00 00 00 00 Hello Wo rld·····\n" +
                    "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "000003f0 00 00 00 00 00 00 00 00  00 00 00 00             ········ ····    \n", bytes.toHexString());

            assertEquals("00000000 48 65 6c 6c 6f 20 57 6f  72 6c 64 00 00 00 00 00 Hello Wo rld·····\n" +
                    "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "000000f0 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "... truncated", bytes.toHexString(256));
        } finally {
            bytes.release();
        }
    }

    @Test
    public void fromHexString() {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes bytes = alloc1.elasticBytes(260);
        try {
            for (int i = 0; i < 259; i++)
                bytes.writeByte((byte) i);
            @NotNull String s = bytes.toHexString();
            Bytes bytes2 = Bytes.fromHexString(s);
            assertEquals(s, bytes2.toHexString());
            bytes2.release();
        } finally {
            bytes.release();
        }
    }

    @Test
    public void testReadWithLength() throws Exception {
        Bytes b = Bytes.from("Hello World");
        final Bytes<ByteBuffer> bytesOut = Bytes.elasticByteBuffer();
        try {
            b.readWithLength(2, bytesOut);
            assertThat(bytesOut.toString(), is("He"));
        } finally {
            b.release();
            bytesOut.release();
        }
    }

    @Test
    public void testCharAt() {
        Bytes b = Bytes.from("Hello World");
        try {
            b.readSkip(6);
            assertTrue(StringUtils.isEqual("World", b));
        } finally {
            b.release();
        }
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
            b.release();
        }
    }

    @Test
    public void internRegressionTest() throws IORuntimeException {
        UTF8StringInterner utf8StringInterner = new UTF8StringInterner(4096);

        Bytes bytes1 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N1");
        Bytes bytes2 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N15");
        utf8StringInterner.intern(bytes1);
        String intern = utf8StringInterner.intern(bytes2);
        assertThat(intern, is(bytes2.toString()));
        String intern2 = utf8StringInterner.intern(bytes1);
        assertThat(intern2, is(bytes1.toString()));
        bytes1.release();
        bytes2.release();
    }

    @Test
    public void testEqualBytesWithSecondStoreBeingLonger() throws IORuntimeException {
        BytesStore store1 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N1");
        BytesStore store2 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N15");
        assertThat(store1.equalBytes(store2, store2.length()), is(false));
        store1.release();
        store2.release();
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
        a.release();
        aa.release();
        aaa.release();
        aaaa.release();
        b.release();
    }

    @Test
    public void testStopBitDouble() throws IORuntimeException {
        Bytes b = alloc1.elasticBytes(1);
        try {
            testSBD(b, -0.0, "00000000 40                                               @         " +
                    "       \n");
            testSBD(b, -1.0, "00000000 DF 7C                                            ·|               \n");
            testSBD(b, -12345678, "00000000 E0 D9 F1 C2 4E                                   ····N            \n");
            testSBD(b, 0.0, "00000000 00                                               ·                \n");
            testSBD(b, 1.0, "00000000 9F 7C                                            ·|               \n");
            testSBD(b, 1024, "00000000 A0 24                                            ·$               \n");
            testSBD(b, 1000000, "00000000 A0 CB D0 48                                      ···H             \n");
            testSBD(b, 0.1, "00000000 9F EE B3 99 CC E6 B3 99  4D                      ········ M       \n");
            testSBD(b, Double.NaN, "00000000 BF 7E                                            ·~               \n");
        } finally {
            b.release();
        }
    }

    private void testSBD(@NotNull Bytes b, double v, String s) throws IORuntimeException {
        b.clear();
        b.writeStopBit(v);
        assertEquals(s, b.toHexString().toUpperCase());
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
                Bytes.elasticHeapByteBuffer(1)
        }) {
            try {
                assertEquals(count + ": " + b.getClass().getSimpleName(), 1, b.refCount());
                assertEquals(count + ": " + b.getClass().getSimpleName(), 1, b.bytesStore().refCount());
            } finally {
                b.release();
                assertEquals(count + ": " + b.getClass().getSimpleName(), 0, b.refCount());
                assertEquals(count++ + ": " + b.getClass().getSimpleName(), 0, b.bytesStore().refCount());
            }
        }
    }

    @Test
    public void testParseUtf8() {
        Bytes bytes = alloc1.elasticBytes(1);
        try {
            assertEquals(1, bytes.refCount());
            bytes.appendUtf8("starting Hello World");
            @NotNull String s0 = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
            assertEquals("starting", s0);
            @NotNull String s = bytes.parseUtf8(StopCharTesters.ALL);
            assertEquals("Hello World", s);
            assertEquals(1, bytes.refCount());
        } finally {
            bytes.release();
            assertEquals(0, bytes.refCount());
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testPartialWriteArray() {
        @NotNull byte[] array = "Hello World".getBytes(ISO_8859_1);
        Bytes to = alloc1.fixedBytes(6);
        try {
            to.write(array);
        } finally {
            to.release();
        }
    }

    @Test
    public void testPartialWriteBB() {
        ByteBuffer bb = ByteBuffer.wrap("Hello World".getBytes(ISO_8859_1));
        Bytes to = alloc1.fixedBytes(6);

        to.writeSome(bb);
        assertEquals("World", Bytes.wrapForRead(bb).toString());
        to.release();
    }

    @Test
    public void testCompact() {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes from = alloc1.elasticBytes(1);
        try {
            from.write("Hello World");
            from.readLong();
            from.compact();
            assertEquals("rld", from.toString());
            assertEquals(0, from.readPosition());
        } finally {
            from.release();
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
            to.release();
        }
    }

    @Test
    public void testAppendLongRandomPosition2() {
        @NotNull byte[] bytes = "WWWWW00000".getBytes(ISO_8859_1);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        Bytes to = Bytes.wrapForWrite(bb);
        try {
            to.append(5, 10, 5);
            assertEquals("WWWWW00010", Bytes.wrapForRead(bb).toString());
        } finally {
            to.release();
        }
    }

    public void testAppendLongRandomPositionShouldThrowBufferOverflowException() {
        try {
            @NotNull byte[] bytes = "000".getBytes(ISO_8859_1);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            Bytes to = Bytes.wrapForWrite(bb);
            try {
                to.append(0, 1000, 5);
                fail("Should throw Exception");
            } finally {
                to.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testAppendLongRandomPositionShouldThrowIllegalArgumentException() {
        try {
            @NotNull byte[] bytes = "000".getBytes(ISO_8859_1);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            Bytes to = Bytes.wrapForWrite(bb);
            try {
                to.append(0, 1000, 3);
            } finally {
                to.release();
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
            to.release();
        }
        assertEquals("003.14", Bytes.wrapForRead(bytes).toString());
    }

    public void testAppendDoubleRandomPositionShouldThrowBufferOverflowException() {
        try {
            @NotNull byte[] bytes = "000000".getBytes(ISO_8859_1);
            Bytes to = Bytes.wrapForWrite(bytes);
            try {
                to.append(0, 3.14, 2, 8);
            } finally {
                to.release();
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
                to.release();
            }
            fail("Should throw Exception");
        } catch (BufferOverflowException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testUnwrite() {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes bytes = alloc1.elasticBytes(1);
        try {
            for (int i = 0; i < 26; i++) {
                bytes.writeUnsignedByte('A' + i);
            }
            assertEquals(26, bytes.writePosition());
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", bytes.toString());
            bytes.unwrite(1, 1);
            assertEquals(25, bytes.writePosition());
            assertEquals("ACDEFGHIJKLMNOPQRSTUVWXYZ", bytes.toString());
        } finally {
            bytes.release();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testExpectNegativeOffsetAbsoluteWriteOnElasticBytesThrowsBufferOverflowException() {
        Bytes<ByteBuffer> bytes = alloc1.elasticBytes(4);
        try {
            if (bytes.unchecked())
                throw new BufferOverflowException();
            bytes.writeInt(-1, 1);
        } finally {
            bytes.release();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testExpectNegativeOffsetAbsoluteWriteOnElasticBytesOfInsufficientCapacityThrowsBufferOverflowException() {
        Bytes<ByteBuffer> bytes = alloc1.elasticBytes(1);

        try {
            if (bytes.unchecked())
                throw new BufferOverflowException();
            bytes.writeInt(-1, 1);
        } finally {
            bytes.release();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testExpectNegativeOffsetAbsoluteWriteOnFixedBytesThrowsBufferOverflowException() {
        Bytes<ByteBuffer> bytes = alloc1.fixedBytes(4);
        try {
            bytes.writeInt(-1, 1);
        } finally {
            bytes.release();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testExpectNegativeOffsetAbsoluteWriteOnFixedBytesOfInsufficientCapacityThrowsBufferOverflowException() {
        Bytes<ByteBuffer> bytes = alloc1.fixedBytes(1);
        try {
            bytes.writeInt(-1, 1);
        } finally {
            bytes.release();
        }
    }

    @Test
    public void testWriter() {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes bytes = alloc1.elasticBytes(1);
        @NotNull PrintWriter writer = new PrintWriter(bytes.writer());
        writer.println(1);
        writer.println("Hello");
        writer.println(12.34);
        writer.append('a').append('\n');
        writer.append("bye\n");
        writer.append("for now\nxxxx", 0, 8);
        assertEquals("1\n" +
                "Hello\n" +
                "12.34\n" +
                "a\n" +
                "bye\n" +
                "for now\n", bytes.toString().replaceAll("\r\n", "\n"));
        try (@NotNull Scanner scan = new Scanner(bytes.reader())) {
            scan.useLocale(Locale.ENGLISH);
            assertEquals(1, scan.nextInt());
            assertEquals("", scan.nextLine());
            assertEquals("Hello", scan.nextLine());
            assertEquals(12.34, scan.nextDouble(), 0.0);
            assertEquals("", scan.nextLine());
            assertEquals("a", scan.nextLine());
            assertEquals("bye", scan.nextLine());
            assertEquals("for now", scan.nextLine());
            assertFalse(scan.hasNext());
            bytes.release();
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
    public void testParseUtf8High() {
        assumeFalse(NativeBytes.areNewGuarded());
        @NotNull Bytes b = alloc1.elasticBytes(0xFFFFF);
        for (int i = ' '; i < Character.MAX_VALUE; i++)
            if (Character.isValidCodePoint(i))
                b.appendUtf8(i);
        b.appendUtf8(0);
        @NotNull StringBuilder sb = new StringBuilder();
        b.parseUtf8(sb, StopCharTesters.CONTROL_STOP);
        sb.setLength(0);
        b.readPosition(0);
        b.parseUtf8(sb, (c1, c2) -> c2 <= 0);
        b.release();
    }

    @Test
    public void testBigDecimalBinary() {
        for (double d : new double[]{1.0, 1000.0, 0.1}) {
            @NotNull Bytes b = alloc1.elasticBytes(16);
            b.writeBigDecimal(new BigDecimal(d));

            @NotNull BigDecimal bd = b.readBigDecimal();
            assertEquals(new BigDecimal(d), bd);
            b.release();
        }
    }

    @Test
    public void testBigDecimalText() {
        for (double d : new double[]{1.0, 1000.0, 0.1}) {
            @NotNull Bytes b = alloc1.elasticBytes(0xFFFF);
            b.append(new BigDecimal(d));

            @NotNull BigDecimal bd = b.parseBigDecimal();
            assertEquals(new BigDecimal(d), bd);
            b.release();
        }
    }

    @Test
    public void testWithLength() {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes hello = Bytes.from("hello");
        Bytes world = Bytes.from("world");
        @NotNull Bytes b = alloc1.elasticBytes(16);
        b.writeWithLength(hello);
        b.writeWithLength(world);
        assertEquals("hello", hello.toString());

        @NotNull Bytes b2 = alloc1.elasticBytes(16);
        b.readWithLength(b2);
        assertEquals("hello", b2.toString());
        b.readWithLength(b2);
        assertEquals("world", b2.toString());

        b.release();
        b2.release();
        hello.release();
        world.release();
    }

    @Test
    public void testAppendBase() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        for (long value : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            for (int base : new int[]{10, 16}) {
                String s = Long.toString(value, base);
                b.clear().appendBase(value, base);
                assertEquals(s, b.toString());
            }
        }
        b.release();
    }

    @Test
    public void testAppendBase16() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        for (long value : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            String s = Long.toHexString(value).toLowerCase();
            b.clear().appendBase16(value);
            assertEquals(s, b.toString());
        }
        b.release();
    }

    @Test
    public void testMove() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.append("Hello World");
            b.move(3, 1, 3);
            assertEquals("Hlo o World", b.toString());
            b.move(3, 5, 3);
            assertEquals("Hlo o o rld", b.toString());
        } finally {
            b.release();
        }
    }

    @Test
    public void testReadPosition() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.readPosition(17);
            assertTrue(b.unchecked());
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked());
        } finally {
            b.release();
        }
    }

    @Test
    public void testReadPositionTooSmall() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.readPosition(-1);
            assertTrue(b.unchecked());
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked());
        } finally {
            b.release();
        }
    }

    @Test
    public void testReadLimit() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.readPosition(b.writeLimit() + 1);
            assertTrue(b.unchecked());
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked());
        } finally {
            b.release();
        }
    }

    @Test
    public void testReadLimitTooSmall() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.readPosition(b.start() - 1);
            assertTrue(b.unchecked());
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked());
        } finally {
            b.release();
        }
    }

    @Test
    public void uncheckedSkip() {
        assumeFalse(NativeBytes.areNewGuarded());

        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.uncheckedReadSkipOne();
            assertEquals(1, b.readPosition());
            b.uncheckedReadSkipBackOne();
            assertEquals(0, b.readPosition());
            b.writeUnsignedByte('H');
            b.writeUnsignedByte(0xFF);
            assertEquals('H', b.uncheckedReadUnsignedByte());
            assertEquals(0xFF, b.uncheckedReadUnsignedByte());
        } finally {
            b.release();
        }
    }

    @Test
    public void readVolatile() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.writeVolatileByte(0, (byte) 1);
            b.writeVolatileShort(1, (short) 2);
            b.writeVolatileInt(3, 3);
            b.writeVolatileLong(7, 4);
            assertEquals(1, b.readVolatileByte(0));
            assertEquals(2, b.readVolatileShort(1));
            assertEquals(3, b.readVolatileInt(3));
            assertEquals(4, b.readVolatileLong(7));

        } finally {
            b.release();
        }
    }

    @Test
    public void testHashCode() {
        assumeFalse(NativeBytes.areNewGuarded());

        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.writeLong(0);
            assertEquals(0, b.hashCode());
            b.clear();
            b.writeLong(1);
            assertEquals(0x152ad77e, b.hashCode());
            b.clear();
            b.writeLong(2);
            assertEquals(0x2a55aefc, b.hashCode());
            b.clear();
            b.writeLong(3);
            assertEquals(0x7f448df2, b.hashCode());
            b.clear();
            b.writeLong(4);
            assertEquals(0x54ab5df8, b.hashCode());

        } finally {
            b.release();
        }
    }

    @Test
    public void testEnum() {

        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.writeEnum(HEAP);
            b.writeEnum(NATIVE);
            assertEquals(HEAP, b.readEnum(Allocator.class));
            assertEquals(NATIVE, b.readEnum(Allocator.class));

        } finally {
            b.release();
        }
    }

    @Test
    public void testTimeMillis() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.appendTimeMillis(12345678L);
            assertEquals("03:25:45.678", b.toString());

        } finally {
            b.release();
        }
    }

    @Test
    public void testDateTimeMillis() {
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            b.appendDateMillis(12345 * 86400_000L);
            assertEquals("20031020", b.toString());

        } finally {
            b.release();
        }
    }

    @Test
    public void testWriteOffset() {
        int length = 128;
        Bytes from = NativeBytes.nativeBytes(length).unchecked(true);
        Bytes to = alloc1.elasticBytes(length);

        Bytes<?> a = Bytes.from("a");
        for (int i = 0; i < length; i++) {
            from.write(i, a, 0L, 1);
        }
        a.release();

        try {
            to.write(from, 0L, length);
            assertEquals(from.readLong(0), to.readLong(0));
        } finally {
            from.release();
            to.release();
        }
    }

    @Test
    public void testToStringDoesNotChange() {
        @NotNull Bytes a = alloc1.elasticBytes(16);
        @NotNull Bytes b = alloc1.elasticBytes(16);
        try {
            String hello = "hello";
            a.append(hello);
            b.append(hello);

            assertTrue(a.equals(b));
            assertTrue(a.bytesStore().equals(b.bytesStore()));

            assertEquals(hello, b.toString());

            assertTrue(a.equals(b));
            assertTrue(a.bytesStore().equals(b.bytesStore()));
        } finally {
            a.release();
            b.release();
        }
    }
}
