/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash;
import net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Allocator.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

@SuppressWarnings({"rawtypes"})
@RunWith(Parameterized.class)
public class BytesTest extends BytesTestCommon {

    private final Allocator alloc1;

    public BytesTest(String ignored, Allocator alloc1) {
        this.alloc1 = alloc1;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Native Unchecked", NATIVE_UNCHECKED},
                {"Native Wrapped", NATIVE},
                {"Native Address", NATIVE_ADDRESS},
                {"Heap", HEAP},
                {"Heap ByteBuffer", BYTE_BUFFER},
                {"Heap Unchecked", HEAP_UNCHECKED},
                {"Heap Embedded", HEAP_EMBEDDED},
                {"Hex Dump", HEX_DUMP}
        });
    }

    @Test
    public void readWriteLimit() {
        final Bytes<?> data = alloc1.elasticBytes(120);
        data.write8bit("Test me again");
        data.writeLimit(data.readLimit()); // this breaks the check
        assertEquals(data.read8bit(), "Test me again");
    }

    @Test
    public void emptyHash() {
        Bytes<?> bytes = alloc1.elasticBytes(2);
        try {
            final long actual1 = OptimisedBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertEquals(0, actual1);
            final long actual2 = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertEquals(0, actual2);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testElastic2() {
        assumeFalse(alloc1 == HEX_DUMP);
        Bytes<?> bytes = alloc1.elasticBytes(2);
        assumeTrue(bytes.isElastic());

        assertFalse(bytes.realCapacity() >= 1000);
        try {
            bytes.writePosition(1000);
            assertTrue(bytes.realCapacity() >= 1000);
            assertEquals(0L, bytes.readLong());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void throwExceptionIfReleased() {
        assumeFalse(alloc1 == HEX_DUMP);
        Bytes<?> bytes = alloc1.elasticBytes(16);
        ((AbstractReferenceCounted) bytes).throwExceptionIfReleased();
        postTest(bytes);
        try {
            ((AbstractReferenceCounted) bytes).throwExceptionIfReleased();
            fail();
        } catch (IllegalStateException ise) {
            // expected.
        }
    }

    @Test
    public void writeAdv() {
        Bytes<?> bytes = alloc1.fixedBytes(32);
        for (int i = 0; i < 4; i++)
            bytes.writeIntAdv('1', 1);
        assertEquals("1111", bytes.toString());
        postTest(bytes);
    }

    @Test
    public void writeLongAdv() {
        Bytes<?> bytes = alloc1.fixedBytes(32);
        for (int i = 0; i < 4; i++)
            bytes.writeLongAdv('1', 1);
        assertEquals("1111", bytes.toString());
        postTest(bytes);
    }

    @Test
    public void testName()
            throws IORuntimeException {
        Bytes<?> bytes = alloc1.fixedBytes(30);
        try {
            long expected = 12345L;
            int offset = 5;

            bytes.writeLong(offset, expected);
            bytes.writePosition(offset + 8);
            assertEquals(expected, bytes.readLong(offset));
        } finally {
            postTest(bytes);
        }
    }

    @Test
    public void readUnsignedByte() {
        Bytes<?> bytes = alloc1.fixedBytes(30);
        try {
            bytes.writeInt(0x11111111);
            bytes.readLimit(1);

            assertEquals(0x11, bytes.readUnsignedByte(0));
            assertEquals(-1, bytes.peekUnsignedByte(-1));
            assertEquals(-1, bytes.peekUnsignedByte(1));

            // as the offset is given it only needs to be under the writeLimit.
            assertEquals(0x11, bytes.readUnsignedByte(1));

        } finally {
            postTest(bytes);
        }
    }

    @Test
    public void writeHistogram() {
        assumeFalse(alloc1 == HEAP_EMBEDDED);

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(0xFFFFF);
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
        postTest(bytes);
    }

    @Test
    public void testCopy() {
        assumeFalse(alloc1 == HEAP_EMBEDDED);

        Bytes<ByteBuffer> bbb = (Bytes) alloc1.fixedBytes(1024);
        try {
            for (int i = 'a'; i <= 'z'; i++)
                bbb.writeUnsignedByte(i);
            bbb.readPositionRemaining(4, 12);
            BytesStore<Bytes<ByteBuffer>, ByteBuffer> copy = bbb.copy();
            bbb.writeUnsignedByte(10, '0');
            assertEquals("[pos: 0, rlim: 12, wlim: 12, cap: 12 ] efghijklmnop", copy.toDebugString());
            copy.releaseLast();
        } finally {
            postTest(bbb);
        }
    }

    @Test
    public void toHexString() {
        assumeFalse(alloc1 == HEAP_EMBEDDED);
        assumeFalse(alloc1 == HEX_DUMP);

        Bytes<?> bytes = alloc1.elasticBytes(1020);
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
            postTest(bytes);
        }
    }

    @Test
    public void fromHexString() {
        assumeFalse(NativeBytes.areNewGuarded());
        assumeFalse(alloc1 == HEAP_EMBEDDED);
        assumeFalse(alloc1 == HEX_DUMP);

        Bytes<?> bytes = alloc1.elasticBytes(260);
        try {
            for (int i = 0; i < 259; i++)
                bytes.writeByte((byte) i);
            @NotNull String s = bytes.toHexString();
            Bytes<?> bytes2 = Bytes.fromHexString(s);
            assertEquals(s, bytes2.toHexString());
            postTest(bytes2);
        } finally {
            postTest(bytes);
        }
    }

    @Test
    public void internRegressionTest()
            throws IORuntimeException {
        UTF8StringInterner utf8StringInterner = new UTF8StringInterner(4096);

        Bytes<?> bytes1 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N1");
        Bytes<?> bytes2 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N15");
        utf8StringInterner.intern(bytes1);
        String intern = utf8StringInterner.intern(bytes2);
        assertEquals(bytes2.toString(), intern);
        String intern2 = utf8StringInterner.intern(bytes1);
        assertEquals(bytes1.toString(), intern2);
        postTest(bytes1);
        postTest(bytes2);
    }

    @Test
    public void testEqualBytesWithSecondStoreBeingLonger()
            throws IORuntimeException {

        BytesStore store1 = null, store2 = null;
        try {
            store1 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N1");
            store2 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N15");
            assertFalse(store1.equalBytes(store2, store2.length()));
        } finally {
            store1.releaseLast();
            store2.releaseLast();
        }
    }

    @Test
    public void testStopBitDouble()
            throws IORuntimeException {
        assumeFalse(alloc1 == HEX_DUMP);
        Bytes<?> b = alloc1.elasticBytes(1);
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
            postTest(b);
        }
    }

    private void testSBD(@NotNull Bytes<?> b, double v, String s)
            throws IORuntimeException {
        b.clear();
        b.writeStopBit(v);
        assertEquals(s, b.toHexString().toUpperCase());
    }

    @Test
    public void testParseUtf8() {
        Bytes<?> bytes = alloc1.elasticBytes(1);
        try {
            assertEquals(1, bytes.refCount());
            bytes.appendUtf8("starting Hello World");
            @NotNull String s0 = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
            assertEquals("starting", s0);
            @NotNull String s = bytes.parseUtf8(StopCharTesters.ALL);
            assertEquals("Hello World", s);
            assertEquals(1, bytes.refCount());
        } finally {
            postTest(bytes);
            assertEquals(0, bytes.refCount());
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testPartialWriteArray() {
        assumeFalse(alloc1 == HEX_DUMP);
        @NotNull byte[] array = "Hello World".getBytes(ISO_8859_1);
        Bytes<?> to = alloc1.fixedBytes(6);
        try {
            to.write(array);
        } finally {
            postTest(to);
        }
    }

    @Test
    public void testPartialWriteBB() {
        assumeFalse(alloc1 == HEX_DUMP);
        ByteBuffer bb = ByteBuffer.wrap("Hello World".getBytes(ISO_8859_1));
        Bytes<?> to = alloc1.fixedBytes(6);

        to.writeSome(bb);
        assertEquals("World", Bytes.wrapForRead(bb).toString());
        postTest(to);
    }

    @Test
    public void testCompact() {
        assumeFalse(alloc1 == HEX_DUMP);
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes<?> from = alloc1.elasticBytes(1);
        try {
            from.write("Hello World");
            from.readLong();
            from.compact();
            assertEquals("rld", from.toString());
            assertEquals(0, from.readPosition());
        } finally {
            postTest(from);
        }
    }

    @Test
    public void testReadIncompleteLong()
            throws IllegalStateException, BufferOverflowException, BufferUnderflowException {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes<?> bytes = alloc1.elasticBytes(16);
        bytes.writeLong(0x0706050403020100L);
        bytes.writeLong(0x0F0E0D0C0B0A0908L);
        try {
            assertEquals(0x0706050403020100L, bytes.readIncompleteLong());
            assertEquals(0x0F0E0D0C0B0A0908L, bytes.readIncompleteLong());
            for (int i = 0; i <= 7; i++) {
                assertEquals("i: " + i, Long.toHexString(0x0B0A090807060504L >>> (i * 8)),
                        Long.toHexString(bytes.readPositionRemaining(4 + i, 8 - i)
                                .readIncompleteLong()));
            }
            assertEquals(0, bytes.readPositionRemaining(4, 0).readIncompleteLong());

        } finally {
            postTest(bytes);
        }
    }

    @Test
    public void testUnwrite()
            throws IllegalArgumentException, BufferOverflowException, IllegalStateException, BufferUnderflowException {
        assumeFalse(alloc1 == HEX_DUMP);
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes<?> bytes = alloc1.elasticBytes(1);
        try {
            for (int i = 0; i < 26; i++) {
                bytes.writeUnsignedByte('A' + i);
            }
            assertEquals(26, (int) bytes.writePosition());
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", bytes.toString());
            bytes.unwrite(1, 1);
            assertEquals(25, (int) bytes.writePosition());
            assertEquals("ACDEFGHIJKLMNOPQRSTUVWXYZ", bytes.toString());
        } finally {
            postTest(bytes);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExpectNegativeOffsetAbsoluteWriteOnElasticBytesThrowsIllegalArgumentException()
            throws BufferOverflowException, IllegalStateException {
        assumeFalse(alloc1 == HEX_DUMP);
        Bytes<?> bytes = alloc1.elasticBytes(4);
        assumeFalse(bytes.unchecked());

        try {
            bytes.writeInt(-1, 1);
        } finally {
            postTest(bytes);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExpectNegativeOffsetAbsoluteWriteOnElasticBytesOfInsufficientCapacityThrowsIllegalArgumentException()
            throws IllegalStateException, BufferOverflowException {
        assumeFalse(alloc1 == HEX_DUMP);
        Bytes<?> bytes = alloc1.elasticBytes(1);
        assumeFalse(bytes.unchecked());

        try {
            bytes.writeInt(-1, 1);
        } finally {
            postTest(bytes);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExpectNegativeOffsetAbsoluteWriteOnFixedBytesThrowsIllegalArgumentException() {
        assumeFalse(alloc1 == HEX_DUMP);
        Bytes<ByteBuffer> bytes = (Bytes) alloc1.fixedBytes(4);
        try {
            bytes.writeInt(-1, 1);
        } finally {
            postTest(bytes);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExpectNegativeOffsetAbsoluteWriteOnFixedBytesOfInsufficientCapacityThrowsIllegalArgumentException() {
        assumeFalse(alloc1 == HEX_DUMP);
        Bytes<ByteBuffer> bytes = (Bytes) alloc1.fixedBytes(1);
        try {
            bytes.writeInt(-1, 1);
        } finally {
            postTest(bytes);
        }
    }

    @Test
    public void testWriter()
            throws IllegalStateException {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes<?> bytes = alloc1.elasticBytes(1);
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
            postTest(bytes);
        }
    }

    @Test
    public void testParseUtf8High()
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {

        @NotNull Bytes<?> b = alloc1.elasticBytes(4);
        for (int i = ' '; i <= Character.MAX_VALUE; i++) {
            if (!Character.isValidCodePoint(i))
                continue;

            b.clear();
            b.appendUtf8(i);
            b.appendUtf8("\r\n");
            @NotNull StringBuilder sb = new StringBuilder();
            b.parseUtf8(sb, StopCharTesters.CONTROL_STOP);
            assertEquals(Character.toString((char) i), sb.toString());
            sb.setLength(0);
            b.readPosition(0);
            b.parseUtf8(sb, (ch, nextCh) -> ch < ' ' && nextCh < ' ');
            assertEquals(Character.toString((char) i), sb.toString());
        }
        postTest(b);
    }

    @Test
    public void testBigDecimalBinary()
            throws BufferUnderflowException, ArithmeticException {
        for (double d : new double[]{1.0, 1000.0, 0.1}) {
            @NotNull Bytes<?> b = alloc1.elasticBytes(16);
            b.writeBigDecimal(new BigDecimal(d));

            @NotNull BigDecimal bd = b.readBigDecimal();
            assertEquals(new BigDecimal(d), bd);
            postTest(b);
        }
    }

    @Test
    public void testBigDecimalText() {
        assumeFalse(alloc1 == HEAP_EMBEDDED);
        for (double d : new double[]{1.0, 1000.0, 0.1}) {
            @NotNull Bytes<?> b = alloc1.elasticBytes(0xFFFF);
            b.append(new BigDecimal(d));

            @NotNull BigDecimal bd = b.parseBigDecimal();
            assertEquals(new BigDecimal(d), bd);
            postTest(b);
        }
    }

    @Test
    public void testWithLength() {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes<?> hello = Bytes.from("hello");
        Bytes<?> world = Bytes.from("world");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        b.writeWithLength(hello);
        b.writeWithLength(world);
        assertEquals("hello", hello.toString());

        @NotNull Bytes<?> b2 = alloc1.elasticBytes(16);
        b.readWithLength(b2);
        assertEquals("hello", b2.toString());
        b.readWithLength(b2);
        assertEquals("world", b2.toString());

        postTest(b);
        postTest(b2);
        postTest(hello);
        postTest(world);
    }

    @Test
    public void testAppendBase() {
        assumeFalse(alloc1 == HEX_DUMP);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        for (long value : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            for (int base : new int[]{10, 16}) {
                String s = Long.toString(value, base);
                b.clear().appendBase(value, base);
                assertEquals(s, b.toString());
            }
        }
        postTest(b);
    }

    @Test
    public void testAppendBase16() {
        assumeFalse(alloc1 == HEX_DUMP);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        for (long value : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            String s = Long.toHexString(value).toLowerCase();
            b.clear().appendBase16(value);
            assertEquals(s, b.toString());
        }
        postTest(b);
    }

    @Test
    public void testMove() {
        assumeFalse(alloc1 == HEX_DUMP);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.append("Hello World");
            b.move(3, 1, 3);
            assertEquals("Hlo o World", b.toString());
            b.move(3, 5, 3);
            assertEquals("Hlo o o rld", b.toString());
        } finally {
            postTest(b);
        }
    }

    @Test
    public void testMove2() {
        assumeFalse(alloc1 == HEX_DUMP);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);

        b.append("0123456789");
        b.move(3, 1, 3);
        assertEquals("0345456789", b.toString());
        postTest(b);
        assertThrows(IllegalStateException.class, () ->
                b.move(3, 5, 3)
        );
    }

    @Test
    public void testMoveForward() {
        assumeFalse(alloc1 == HEX_DUMP);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);

        b.append("0123456789abcdefg");
        b.move(1, 3, 10);
        assertEquals("012123456789adefg", b.toString());
        postTest(b);
    }

    @Test
    public void testMoveBackward() {
        assumeFalse(alloc1 == HEX_DUMP);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);

        b.append("0123456789abcdefg");
        b.move(3, 1, 10);
        assertEquals("03456789abcbcdefg", b.toString());
        postTest(b);
    }

    @Test
    public void testMove2B() {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);

        b.append("Hello World");
        b.bytesStore().move(3, 1, 3);
        assertEquals("Hlo o World", b.toString());
        postTest(b);
        BackgroundResourceReleaser.releasePendingResources();
        final BytesStore<?, ?> bs = b.bytesStore();
        assertNotNull(bs);
        assertThrows(IllegalStateException.class, () ->
                bs.move(3, 5, 3)
        );
    }

    @Test
    public void testReadPosition() {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.readPosition(17);
            assertTrue(b.unchecked());
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked());
        } finally {
            postTest(b);
        }
    }

    @Test
    public void testReadPositionTooSmall() {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.readPosition(-1);
            assertTrue(b.unchecked());
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked());
        } finally {
            postTest(b);
        }
    }

    @Test
    public void testReadLimit() {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.readPosition(b.writeLimit() + 1);
            assertTrue(b.unchecked());
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked());
        } finally {
            postTest(b);
        }
    }

    @Test
    public void testReadLimitTooSmall() {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.readPosition(b.start() - 1);
            assertTrue(b.unchecked());
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked());
        } finally {
            postTest(b);
        }
    }

    @Test
    public void uncheckedSkip() {
        assumeFalse(NativeBytes.areNewGuarded());

        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
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
            postTest(b);
        }
    }

    @Test
    public void readVolatile() {
        assumeFalse(alloc1 == HEX_DUMP);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
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
            postTest(b);
        }
    }

    @Test
    public void testHashCode() {
        assumeFalse(NativeBytes.areNewGuarded());

        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
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
            postTest(b);
        }
    }

    @Test
    public void testEnum() {

        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.writeEnum(HEAP);
            b.writeEnum(NATIVE);
            assertEquals(HEAP, b.readEnum(Allocator.class));
            assertEquals(NATIVE, b.readEnum(Allocator.class));

        } finally {
            postTest(b);
        }
    }

    @Test
    public void testTimeMillis() {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.appendTimeMillis(12345678L);
            assertEquals("03:25:45.678", b.toString());

        } finally {
            postTest(b);
        }
    }

    @Test
    public void testDateTimeMillis() {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.appendDateMillis(12345 * 86400_000L);
            assertEquals("20031020", b.toString());

        } finally {
            postTest(b);
        }
    }

    @Test
    public void testWriteOffset() {
        int length = 127;
        Bytes<?> from = NativeBytes.nativeBytes(length).unchecked(true);
        Bytes<?> to = alloc1.elasticBytes(length);

        Bytes<?> a = Bytes.from("a");
        for (int i = 0; i < length; i++) {
            from.write(i, a, 0L, 1);
        }
        postTest(a);

        try {
            to.write(from, 0L, length);
            assertEquals(from.readLong(0), to.readLong(0));
        } finally {
            postTest(from);
            postTest(to);
        }
    }

    @Test
    public void testToStringDoesNotChange() {
        @NotNull Bytes<?> a = alloc1.elasticBytes(16);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            String hello = "hello";
            a.append(hello);
            b.append(hello);

            assertTrue(a.contentEquals(b));
            assertEquals(a.bytesStore(), b.bytesStore());

            assertEquals(hello, b.toString());

            assertTrue(a.contentEquals(b));
            assertEquals(a.bytesStore(), b.bytesStore());
        } finally {
            postTest(a);
            postTest(b);
        }
    }

    @Test
    public void to8BitString() {
        @NotNull Bytes<?> a = alloc1.elasticBytes(16);
        try {
            assertEquals(a.toString(), a.to8bitString());
            String hello = "hello";
            a.append(hello);
            assertEquals(a.toString(), a.to8bitString());
        } finally {
            postTest(a);
        }
    }

    @Test
    public void testParseDoubleReadLimit() {
        Bytes<ByteBuffer> bytes = (Bytes) alloc1.fixedBytes(32);
        try {
            final String spaces = "   ";
            bytes.append(spaces).append(1.23);
            bytes.readLimit(spaces.length());
            // only fails when assertions are off
            assertEquals(0, BytesInternal.parseDouble(bytes), 0);
        } finally {
            postTest(bytes);
        }
    }

    @Test
    public void write8BitString() {
        assumeFalse(alloc1 == HEAP_EMBEDDED);

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(703);
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i <= 36; i++) {
                final String s = sb.toString();
                bytes.write8bit(s);
                String s2 = bytes.read8bit();
                assertEquals(s, s2);
                sb.append(Integer.toString(i, 36));
            }
        } finally {
            postTest(bytes);
        }
    }

    @Test
    public void write8BitNativeBytes() {
        assumeFalse(alloc1 == HEAP_EMBEDDED);

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(703);
        Bytes<?> nbytes = Bytes.allocateDirect(36);
        Bytes<?> nbytes2 = Bytes.allocateDirect(36);
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i <= 36; i++) {
                nbytes.clear().append(sb);
                if (nbytes == null) {
                    bytes.writeStopBit(-1);
                } else {
                    long offset = nbytes.readPosition();
                    long readRemaining = Math.min(bytes.writeRemaining(), nbytes.readLimit() - offset);
                    bytes.writeStopBit(readRemaining);
                    try {
                        bytes.write(nbytes, offset, readRemaining);
                    } catch (BufferUnderflowException | IllegalArgumentException e) {
                        throw new AssertionError(e);
                    }
                }
                bytes.read8bit(nbytes2.clear());

                final String s = sb.toString();
                assertEquals(s, nbytes2.toString());
                sb.append(Integer.toString(i, 36));
            }
        } finally {
            postTest(bytes);
            postTest(nbytes);
            postTest(nbytes2);
        }
    }

    @Test
    public void write8BitHeapBytes() {
        assumeFalse(alloc1 == HEAP_EMBEDDED);

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(703);
        Bytes<?> nbytes = Bytes.allocateElasticOnHeap(36);
        Bytes<?> nbytes2 = Bytes.allocateElasticOnHeap(36);
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i <= 36; i++) {
                nbytes.clear().append(sb);
                if (nbytes == null) {
                    bytes.writeStopBit(-1);
                } else {
                    long offset = nbytes.readPosition();
                    long readRemaining = Math.min(bytes.writeRemaining(), nbytes.readLimit() - offset);
                    bytes.writeStopBit(readRemaining);
                    try {
                        bytes.write(nbytes, offset, readRemaining);
                    } catch (BufferUnderflowException | IllegalArgumentException e) {
                        throw new AssertionError(e);
                    }
                }
                bytes.read8bit(nbytes2.clear());

                assertEquals(sb.toString(), nbytes2.toString());
                sb.append(Integer.toString(i, 36));
            }
        } finally {
            postTest(bytes);
            postTest(nbytes);
            postTest(nbytes2);
        }
    }

    @Test
    public void write8BitCharSequence() {
        assumeFalse(alloc1 == HEAP_EMBEDDED);

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(703);
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        try {
            for (int i = 0; i <= 36; i++) {
                bytes.write8bit(sb);
                bytes.read8bit(sb2);

                assertEquals(sb.toString(), sb2.toString());
                sb.append(Integer.toString(i, 36));
            }
        } finally {
            postTest(bytes);
        }
    }

    @Test
    public void stopBitChar() {
        final Bytes<?> bytes = alloc1.fixedBytes(64);
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            bytes.clear();
            char ch = (char) i;
            bytes.writeStopBit(ch);
            bytes.writeUnsignedByte(0x80);
            char c2 = bytes.readStopBitChar();
            assertEquals(ch, c2);
            assertEquals(0x80, bytes.readUnsignedByte());
        }
        postTest(bytes);
    }

    @Test
    public void stopBitLong() {
        final Bytes<?> bytes = alloc1.fixedBytes(64);
        for (int i = 0; i <= 63; i++) {
            long l = 1L << i;
            stopBitLong0(bytes, l);
            stopBitLong0(bytes, l - 1);
            stopBitLong0(bytes, -l);
            stopBitLong0(bytes, ~l);
        }
        postTest(bytes);
    }

    @Test
    public void stopBitNeg1() {
        final Bytes<?> bytes = alloc1.fixedBytes(64);
        BytesInternal.writeStopBitNeg1(bytes);
        BytesInternal.writeStopBitNeg1(bytes);
        assertEquals(-1, bytes.readStopBit());
        assertEquals(0xFFFF, bytes.readStopBitChar());
        postTest(bytes);
    }

    private void postTest(Bytes<?> bytes) {
        bytes.clear();
        assertTrue(bytes.isClear());
        assertEquals(0, bytes.readRemaining());
        bytes.releaseLast();
    }

    private void stopBitLong0(Bytes<?> bytes, long l) {
        bytes.clear();
        bytes.writeStopBit(l);
        bytes.writeUnsignedByte(0x80);
        long l2 = bytes.readStopBit();
        assertEquals(l, l2);
        assertEquals(0x80, bytes.readUnsignedByte());
    }

    @Test
    public void capacityVsWriteLimitInvariant() {
        final Bytes<?> bytes = alloc1.elasticBytes(20);
        assumeTrue(bytes.isElastic());
        assertEquals(bytes.capacity(), bytes.writeLimit());
    }

    @Test
    public void isClear() {
        final Bytes<?> bytes = alloc1.elasticBytes(20);
        assertTrue(bytes.isClear());
        bytes.releaseLast();
    }

    @Test
    public void testAppendReallySmallDouble() {
        assumeFalse(alloc1 == NATIVE || alloc1 == NATIVE_ADDRESS);
        Bytes<?> bytes = alloc1.elasticBytes(32);

        for (double d = 1; d >= 1e-19; d *= 0.99) {
            bytes.clear();
            bytes.append(d);
            double err = d > 4.3e-10 ? 0
                    : d > 2.14e-13 ? Math.ulp(d)
                    : 2 * Math.ulp(d);
            assertEquals(d, bytes.parseDouble(), err);
        }
    }

    @Test
    public void testReadWithOffset() {
        Bytes<?> bytes = alloc1.elasticBytes(32);
        bytes.append("Hello");
        int offset = 2;
        int offsetInRDI = 1;
        byte[] ba = new byte[bytes.length() + offset - offsetInRDI];
        ba[0] = '0';
        ba[1] = '1';
        bytes.read(offsetInRDI, ba, offset, bytes.length() - offsetInRDI);
        assertEquals("01ello", new String(ba));
    }

    @Test
    public void writeSkipNegative() {
        @NotNull Bytes<?> a = alloc1.elasticBytes(16);
        try {
            String hello = "hello";
            a.append(hello);
            assertEquals(hello, a.toString());
            a.writeSkip(-hello.length());
            assertEquals("", a.toString());
            if (!a.unchecked())
                assertThrows(BufferOverflowException.class, () -> a.writeSkip(-1));
        } finally {
            postTest(a);
        }
    }

    @Test
    public void testCopyToStream() throws IOException {
        @NotNull Bytes<?> a = alloc1.elasticBytes(16);
        String text = "Hello World";

        try {
            a.append(text);
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                a.copyTo(os);

                byte[] array = os.toByteArray();
                assertEquals(text.length(), array.length);
                assertArrayEquals(text.getBytes("UTF8"), array);
            }
        } finally {
            postTest(a);
        }
    }
}
