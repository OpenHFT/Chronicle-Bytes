/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash;
import net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.render.DecimalAppender;
import net.openhft.chronicle.bytes.render.GeneralDecimaliser;
import net.openhft.chronicle.bytes.render.MaximumPrecision;
import net.openhft.chronicle.bytes.render.StandardDecimaliser;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import java.util.concurrent.Callable;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Allocator.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests Bytes behaviour across allocator variants and scenarios because comprehensive coverage
 * is essential for ensuring consistent behaviour across heap, direct, and mapped implementations.
 */
@SuppressWarnings({"rawtypes", "deprecation", "PMD.JUnit5TestShouldBePackagePrivate", "PMD.JUnitUseExpected"})
@DisplayName("Bytes behaviour across allocator variants and scenarios")
public class BytesTest extends BytesTestCommon {
    private boolean parseDouble;

    static Stream<Allocator> data() {
        Stream<Allocator> base = Stream.of(
                HEAP,
                BYTE_BUFFER,
                HEAP_UNCHECKED,
                HEAP_EMBEDDED,
                HEX_DUMP);
        if (Jvm.maxDirectMemory() > 0) {
            return Stream.concat(base, Stream.of(NATIVE_UNCHECKED, NATIVE, NATIVE_ADDRESS));
        }
        return base;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Read/write limit preserves content and read limit")
    public void readWriteLimit(Allocator alloc1) {
        final Bytes<?> data = alloc1.elasticBytes(120);
        data.write8bit("Test me again");
        data.writeLimit(data.readLimit()); // this breaks the check
        assertEquals("Test me again", data.read8bit(),
                "Read/write limit should preserve the written string");
        data.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Empty bytes hash produces zero result")
    public void emptyHash(Allocator alloc1) {
        Bytes<?> bytes = alloc1.elasticBytes(2);
        try {
            final long actual1 = OptimisedBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertEquals(0, actual1, "Optimised hash should be zero for empty bytes");
            final long actual2 = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertEquals(0, actual2, "Vanilla hash should be zero for empty bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Elastic bytes grow and read back after expanding write position")
    public void testElastic2(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support elastic expansion");
        Bytes<?> bytes = alloc1.elasticBytes(2);
        try {
            assumeTrue(bytes.isElastic(), "Elastic bytes required for capacity growth checks");

            assertFalse(bytes.realCapacity() >= 1000,
                    "Initial real capacity should be below the expansion target");

            bytes.writePosition(1000);
            assertTrue(bytes.realCapacity() >= 1000, "Real capacity should grow after advancing write position");
            assertEquals(0L, bytes.readLong(), "Default long at position 0 should be zero");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Released bytes throw exceptions when reused")
    public void throwExceptionIfReleased(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support release checks");
        Bytes<?> bytes = alloc1.elasticBytes(16);
        ((AbstractReferenceCounted) bytes).throwExceptionIfReleased();
        postTest(bytes);
        try {
            ((AbstractReferenceCounted) bytes).throwExceptionIfReleased();
            fail("IllegalStateException should be thrown after releasing bytes");
        } catch (IllegalStateException ise) {
            // expected - released bytes should reject use
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("writeIntAdv appends repeated digit sequence correctly")
    public void writeAdv(Allocator alloc1) {
        Bytes<?> bytes = alloc1.fixedBytes(32);
        for (int i = 0; i < 4; i++)
            bytes.writeIntAdv('1', 1);
        assertEquals("1111", Bytes.toString(bytes), "writeIntAdv should append repeated digits");
        assertEquals("1111", Bytes.toString(bytes), "Repeated toString should remain stable");
        postTest(bytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("writeLongAdv appends repeated digit sequence correctly")
    public void writeLongAdv(Allocator alloc1) {
        Bytes<?> bytes = alloc1.fixedBytes(32);
        for (int i = 0; i < 4; i++)
            bytes.writeLongAdv('1', 1);
        assertEquals("1111", bytes.toString(), "writeLongAdv should append repeated digits");
        postTest(bytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Offset writes can be read from the same position")
    public void testName(Allocator alloc1)
            throws IORuntimeException {
        Bytes<?> bytes = alloc1.fixedBytes(30);
        try {
            long expected = 12345L;
            int offset = 5;

            bytes.writeLong(offset, expected);
            bytes.writePosition(offset + 8);
            assertEquals(expected, bytes.readLong(offset),
                    "Reading at the write offset should return the expected long");
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Unsigned byte reads respect limits and bounds")
    public void readUnsignedByte(Allocator alloc1) {
        Bytes<?> bytes = alloc1.fixedBytes(30);
        try {
            bytes.writeInt(0x11111111);
            bytes.readLimit(1);

            assertEquals(0x11, bytes.readUnsignedByte(0),
                    "Unsigned byte read at offset 0 should match the written value");
            assertEquals(-1, bytes.peekUnsignedByte(-1),
                    "Peek before start should return -1");
            assertEquals(-1, bytes.peekUnsignedByte(1),
                    "Peek past read limit should return -1");

            // as the offset is given it only needs to be under the writeLimit.
            assertEquals(0x11, bytes.readUnsignedByte(1),
                    "Unsigned byte read at offset 1 should still succeed under write limit");

        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Histogram values round-trip through bytes")
    public void writeHistogram(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support histogram writes");

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

        assertEquals(hist, histB, "First histogram should round-trip through bytes");
        assertEquals(hist2, histC, "Second histogram should round-trip through bytes");
        postTest(bytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Copies preserve debug string snapshot content")
    public void testCopy(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support copy scenario");

        Bytes<?> bbb = alloc1.fixedBytes(1024);
        try {
            for (int i = 'a'; i <= 'z'; i++)
                bbb.writeUnsignedByte(i);
            bbb.readPositionRemaining(4, 12);
            BytesStore<? extends Bytes<?>, ?> copy = bbb.copy();
            bbb.writeUnsignedByte(10, '0');
            assertEquals("[pos: 0, rlim: 12, wlim: 12, cap: 12 ] efghijklmnop", copy.toDebugString(),
                    "Copy debug view should match the captured slice");
            copy.releaseLast();
        } finally {
            postTest(bbb);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Hex dump renders expected content output")
    public void toHexString(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support hex dump tests");
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support hex rendering tests");

        Bytes<?> bytes = alloc1.elasticBytes(1020);
        try {
            bytes.append("Hello World");
            assertEquals("00000000 48 65 6c 6c 6f 20 57 6f  72 6c 64                Hello Wo rld     \n",
                    bytes.toHexString(), "Hex dump should match for short content");
            bytes.readLimit(bytes.realCapacity());
            assertEquals("00000000 48 65 6c 6c 6f 20 57 6f  72 6c 64 00 00 00 00 00 Hello Wo rld·····\n" +
                    "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "000003f0 00 00 00 00 00 00 00 00  00 00 00 00             ········ ····    \n",
                    bytes.toHexString(), "Full hex dump should include trailing padding lines");

            assertEquals("00000000 48 65 6c 6c 6f 20 57 6f  72 6c 64 00 00 00 00 00 Hello Wo rld·····\n" +
                    "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "000000f0 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "... truncated", bytes.toHexString(256), "Truncated hex dump should end with truncation marker");
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Hex string round-trips to bytes")
    public void fromHexString(Allocator alloc1) {
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes alter hex dump formatting");
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support hex round-trip tests");
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support hex round-trip tests");

        Bytes<?> bytes = alloc1.elasticBytes(260);
        try {
            for (int i = 0; i < 259; i++)
                bytes.writeByte((byte) i);
            @NotNull String s = bytes.toHexString();
            Bytes<?> bytes2 = Bytes.fromHexString(s);
            assertEquals(s, bytes2.toHexString(), "Hex dump from parsed bytes should match original dump");
            postTest(bytes2);
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("UTF-8 interner reuses canonical strings")
    public void internRegressionTest(Allocator alloc1)
            throws IORuntimeException {
        UTF8StringInterner utf8StringInterner = new UTF8StringInterner(4096);

        Bytes<?> bytes1 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N1");
        Bytes<?> bytes2 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N15");
        utf8StringInterner.intern(bytes1);
        String intern = utf8StringInterner.intern(bytes2);
        assertEquals(bytes2.toString(), intern, "Second interning should return bytes2 string");
        String intern2 = utf8StringInterner.intern(bytes1);
        assertEquals(bytes1.toString(), intern2, "First interning should return bytes1 string");
        postTest(bytes1);
        postTest(bytes2);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("equalBytes fails when second store is longer")
    public void testEqualBytesWithSecondStoreBeingLonger(Allocator alloc1)
            throws IORuntimeException {

        BytesStore<?, ?> store1 = null;
        BytesStore<?, ?> store2 = null;
        try {
            store1 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N1");
            store2 = alloc1.elasticBytes(64).append("TW-TRSY-20181217-NY572677_3256N15");
            assertFalse(store1.equalBytes(store2, store2.length()),
                    "equalBytes should fail when length exceeds first store");
        } finally {
            if (store1 != null) {
                store1.releaseLast();
            }
            if (store2 != null) {
                store2.releaseLast();
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Stop-bit double encodes expected hex")
    public void testStopBitDouble(Allocator alloc1)
            throws IORuntimeException {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support stop-bit encoding tests");
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
        assertEquals(s, b.toHexString().toUpperCase(),
                "Stop-bit hex encoding should match expected output");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("UTF-8 parsing returns tokens and preserves ref counts")
    public void testParseUtf8(Allocator alloc1) {
        Bytes<?> bytes = alloc1.elasticBytes(1);
        try {
            assertEquals(1, bytes.refCount(), "Fresh bytes should start with one reference");
            bytes.appendUtf8("starting Hello World");
            @NotNull String s0 = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
            assertEquals("starting", s0, "First token should stop at space");
            @NotNull String s = bytes.parseUtf8(StopCharTesters.ALL);
            assertEquals("Hello World", s, "Remaining token should include spaces");
            assertEquals(1, bytes.refCount(), "Parsing should not change reference count");
        } finally {
            postTest(bytes);
            assertEquals(0, bytes.refCount(), "Bytes should be released after postTest");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Oversized array write throws BufferOverflowException for fixed bytes")
    public void testPartialWriteArray(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support partial array writes");
        @NotNull byte[] array = "Hello World".getBytes(ISO_8859_1);
        Bytes<?> to = alloc1.fixedBytes(6);
        try {
            assertThrows(BufferOverflowException.class, () -> to.write(array),
                    "Fixed bytes should throw when writing oversized arrays");
        } finally {
            postTest(to);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Partial ByteBuffer writes leave remaining bytes")
    public void testPartialWriteBB(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support partial ByteBuffer writes");
        ByteBuffer bb = ByteBuffer.wrap("Hello World".getBytes(ISO_8859_1));
        Bytes<?> to = alloc1.fixedBytes(6);

        to.writeSome(bb);
        assertEquals("World", Bytes.wrapForRead(bb).toString(),
                "Remaining ByteBuffer content should be the unwritten suffix");
        postTest(to);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Compact trims unread bytes and resets positions")
    public void testCompact(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support compact tests");
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes do not support compact expectations");
        Bytes<?> from = alloc1.elasticBytes(1);
        try {
            from.write("Hello World");
            from.readLong();
            from.compact();
            assertEquals("rld", from.toString(), "Compact should preserve unread suffix");
            assertEquals(0, from.readPosition(), "Compact should reset read position");

            from.readSkip(from.readRemaining());
            assertEquals(3, from.readPosition(), "Read position should reflect skipped bytes");
            from.compact();
            assertEquals(0, from.readPosition(), "Compact should reset read position again");
        } finally {
            postTest(from);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("readIncompleteLong reconstructs masked values correctly for data")
    public void testReadIncompleteLong(Allocator alloc1)
            throws IllegalStateException, BufferOverflowException, BufferUnderflowException {
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes alter incomplete long behaviour");
        Bytes<?> bytes = alloc1.elasticBytes(16);
        bytes.writeLong(0x0706050403020100L);
        bytes.writeLong(0x0F0E0D0C0B0A0908L);
        try {
            assertEquals(0x0706050403020100L, bytes.readIncompleteLong(),
                    "First incomplete long should match written value");
            assertEquals(0x0F0E0D0C0B0A0908L, bytes.readIncompleteLong(),
                    "Second incomplete long should match written value");
            for (int i = 0; i <= 7; i++) {
                assertEquals(Long.toHexString(0x0B0A090807060504L >>> (i * 8)),
                        Long.toHexString(bytes.readPositionRemaining(4 + i, 8 - i)
                                .readIncompleteLong()),
                        "Masked read index " + i);
            }
            assertEquals(0, bytes.readPositionRemaining(4, 0).readIncompleteLong(),
                    "Zero-length incomplete read should return zero");

        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Unwrite removes bytes and preserves remaining data")
    public void testUnwrite(Allocator alloc1)
            throws IllegalArgumentException, BufferOverflowException, IllegalStateException, BufferUnderflowException {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support unwrite tests");
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes alter unwrite expectations");
        Bytes<?> bytes = alloc1.elasticBytes(1);
        try {
            for (int i = 0; i < 26; i++) {
                bytes.writeUnsignedByte('A' + i);
            }
            assertEquals(26, (int) bytes.writePosition(), "Write position should track 26 bytes");
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", bytes.toString(),
                    "Written bytes should span the full alphabet");
            bytes.unwrite(1, 1);
            assertEquals(25, (int) bytes.writePosition(), "Write position should shrink after unwrite");
            assertEquals("ACDEFGHIJKLMNOPQRSTUVWXYZ", bytes.toString(),
                    "Unwrite should remove the expected character");
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Elastic bytes reject negative absolute write offsets")
    public void testExpectNegativeOffsetAbsoluteWriteOnElasticBytesThrowsIllegalArgumentException(Allocator alloc1)
            throws BufferOverflowException, IllegalStateException {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support negative offset tests for elastic bytes");
        Bytes<?> bytes = alloc1.elasticBytes(4);

        try {
            assumeFalse(bytes.unchecked(), "Unchecked bytes do not validate negative offsets for elastic bytes");
            assertThrows(IllegalArgumentException.class, () -> bytes.writeInt(-1, 1),
                    "Negative absolute offset should throw for elastic bytes");
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Elastic bytes reject negative offset with small capacity")
    public void testExpectNegativeOffsetAbsoluteWriteOnElasticBytesOfInsufficientCapacityThrowsIllegalArgumentException(Allocator alloc1)
            throws IllegalStateException, BufferOverflowException {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support negative offset tests on tiny elastic bytes");
        Bytes<?> bytes = alloc1.elasticBytes(1);

        try {
            assumeFalse(bytes.unchecked(), "Unchecked bytes do not validate negative offsets on tiny elastic bytes");
            assertThrows(IllegalArgumentException.class, () -> bytes.writeInt(-1, 1),
                    "Negative absolute offset should throw for tiny elastic bytes");
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Fixed bytes reject negative absolute write offsets")
    public void testExpectNegativeOffsetAbsoluteWriteOnFixedBytesThrowsIllegalArgumentException(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support negative offset tests for fixed bytes");
        Bytes<?> bytes = alloc1.fixedBytes(4);
        try {
            assertThrows(IllegalArgumentException.class, () -> bytes.writeInt(-1, 1),
                    "Negative absolute offset should throw for fixed bytes");
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Fixed bytes reject negative offset with small capacity")
    public void testExpectNegativeOffsetAbsoluteWriteOnFixedBytesOfInsufficientCapacityThrowsIllegalArgumentException(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support negative offset tests on tiny fixed bytes");
        Bytes<?> bytes = alloc1.fixedBytes(1);
        try {
            assertThrows(IllegalArgumentException.class, () -> bytes.writeInt(-1, 1),
                    "Negative absolute offset should throw for tiny fixed bytes");
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Writer and reader round-trip typed values")
    public void testWriter(Allocator alloc1)
            throws IllegalStateException {
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes alter writer/reader expectations");
        Bytes<?> bytes = alloc1.elasticBytes(1);
        try (PrintWriter writer = new PrintWriter(bytes.writer())) {
            writer.println(1);
            writer.println("Hello");
            writer.println(12.34);
            writer.append('a').append('\n');
            writer.append("bye\n");
            writer.append("for now\nxxxx", 0, 8);
        }
        assertEquals("1\n" +
                "Hello\n" +
                "12.34\n" +
                "a\n" +
                "bye\n" +
                "for now\n", bytes.toString().replaceAll("\r\n", "\n"),
                "Writer output should match expected lines");
        try (@NotNull Scanner scan = new Scanner(bytes.reader())) {
            scan.useLocale(Locale.ENGLISH);
            assertEquals(1, scan.nextInt(), "First scanned int should match");
            assertEquals("", scan.nextLine(), "Next line should be empty after int");
            assertEquals("Hello", scan.nextLine(), "Next line should contain Hello");
            assertEquals(12.34, scan.nextDouble(), 0.0, "Scanned double should match");
            assertEquals("", scan.nextLine(), "Next line should be empty after double");
            assertEquals("a", scan.nextLine(), "Next line should contain single character");
            assertEquals("bye", scan.nextLine(), "Next line should contain bye");
            assertEquals("for now", scan.nextLine(), "Next line should contain for now");
            assertFalse(scan.hasNext(), "Scanner should be exhausted after reading all lines");
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("UTF-8 parsing handles high code points")
    public void testParseUtf8High(Allocator alloc1)
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
            assertEquals(Character.toString((char) i), sb.toString(),
                    "CONTROL_STOP should parse code point " + i);
            sb.setLength(0);
            b.readPosition(0);
            b.parseUtf8(sb, (ch, nextCh) -> ch < ' ' && nextCh < ' ');
            assertEquals(Character.toString((char) i), sb.toString(),
                    "Custom stop tester should parse code point " + i);
        }
        postTest(b);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Binary BigDecimal round-trips through bytes")
    @SuppressWarnings("PMD.AvoidDecimalLiteralsInBigDecimalConstructor")
    public void testBigDecimalBinary(Allocator alloc1)
            throws BufferUnderflowException, ArithmeticException {
        // Intentionally use BigDecimal(double) (not valueOf) to exercise constructor semantics.
        for (double d : new double[]{1.0, 1000.0, 0.1}) {
            @NotNull Bytes<?> b = alloc1.elasticBytes(16);
            b.writeBigDecimal(new BigDecimal(d));

            @NotNull BigDecimal bd = b.readBigDecimal();
            assertEquals(new BigDecimal(d), bd, "Binary BigDecimal should round-trip for " + d);
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Text BigDecimal parsing round-trips values")
    @SuppressWarnings("PMD.AvoidDecimalLiteralsInBigDecimalConstructor")
    public void testBigDecimalText(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support BigDecimal text parsing");
        // Intentionally use BigDecimal(double) (not valueOf) to exercise constructor semantics.
        for (double d : new double[]{1.0, 1000.0, 0.1}) {
            @NotNull Bytes<?> b = alloc1.elasticBytes(0xFFFF);
            b.append(new BigDecimal(d));

            @NotNull BigDecimal bd = b.parseBigDecimal();
            assertEquals(new BigDecimal(d), bd, "Text BigDecimal should round-trip for " + d);
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Length-prefixed bytes round-trip values")
    public void testWithLength(Allocator alloc1) {
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes alter length-prefixed behaviour");
        Bytes<?> hello = Bytes.from("hello");
        Bytes<?> world = Bytes.from("world");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        b.writeWithLength(hello);
        b.writeWithLength(world);
        assertEquals("hello", hello.toString(), "Original hello bytes should remain unchanged");

        @NotNull Bytes<?> b2 = alloc1.elasticBytes(16);
        b.readWithLength(b2);
        assertEquals("hello", b2.toString(), "First length-prefixed value should be hello");
        b.readWithLength(b2);
        assertEquals("world", b2.toString(), "Second length-prefixed value should be world");

        postTest(b);
        postTest(b2);
        postTest(hello);
        postTest(world);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("appendBase renders decimal and hex values")
    public void testAppendBase(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support base append tests");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        for (long value : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE, ~4, ~2, ~1, ~0, 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            for (int base : new int[]{10, 16}) {
                String s = Long.toString(value, base);
                b.clear().appendBase(value, base);
                assertEquals(s, b.toString(),
                        "appendBase should format " + value + " in base " + base);
            }
        }
        postTest(b);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("appendBase16 renders lowercase hex values correctly")
    public void testAppendBase16(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support base16 append tests");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        for (long value : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, Long.MAX_VALUE}) {
            String s = Long.toHexString(value).toLowerCase();
            b.clear().appendBase16(value);
            assertEquals(s, b.toString(), "appendBase16 should format " + value + " in hex");
        }
        postTest(b);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("move rearranges bytes within a buffer")
    public void testMove(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support move reordering tests");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.append("Hello World");
            b.move(3, 1, 3);
            assertEquals("Hlo o World", b.toString(), "First move should reposition bytes correctly");
            b.move(3, 5, 3);
            assertEquals("Hlo o o rld", b.toString(), "Second move should update bytes as expected");
        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("move throws when invoked after release")
    public void testMove2(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support move-after-release tests");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);

        b.append("0123456789");
        b.move(3, 1, 3);
        assertEquals("0345456789", b.toString(), "Initial move should shift digits correctly");
        postTest(b);
        assertThrows(IllegalStateException.class, () -> b.move(3, 5, 3),
                "Moving after release should throw IllegalStateException");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Forward move shifts bytes to a later position")
    public void testMoveForward(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support forward move tests");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);

        b.append("0123456789abcdefg");
        b.move(1, 3, 10);
        assertEquals("012123456789adefg", b.toString(), "Forward move should duplicate the shifted range");
        postTest(b);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Backward move shifts bytes to an earlier position")
    public void testMoveBackward(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support backward move tests");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);

        b.append("0123456789abcdefg");
        b.move(3, 1, 10);
        assertEquals("03456789abcbcdefg", b.toString(), "Backward move should duplicate the shifted range");
        postTest(b);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("ByteStore move throws after release state change")
    public void testMove2B(Allocator alloc1) {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);

        b.append("Hello World");
        b.bytesStore().move(3, 1, 3);
        assertEquals("Hlo o World", b.toString(), "ByteStore move should adjust bytes as expected");
        postTest(b);
        BackgroundResourceReleaser.releasePendingResources();
        final BytesStore<?, ?> bs = b.bytesStore();
        assertNotNull(bs, "BytesStore reference should remain available");
        assertThrows(IllegalStateException.class, () -> bs.move(3, 5, 3),
                "Move after release should throw IllegalStateException");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("readPosition beyond limit toggles unchecked flag")
    public void testReadPosition(Allocator alloc1) {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.readPosition(17);
            assertTrue(b.unchecked(), "Unchecked flag should be set after out-of-bounds readPosition");
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked(), "Unchecked flag should be cleared after readPosition underflow");
        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("readPosition below zero toggles unchecked flag")
    public void testReadPositionTooSmall(Allocator alloc1) {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.readPosition(-1);
            assertTrue(b.unchecked(), "Unchecked flag should be set after negative readPosition");
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked(), "Unchecked flag should be cleared after negative readPosition");
        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("readPosition past writeLimit toggles unchecked flag")
    public void testReadLimit(Allocator alloc1) {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.readPosition(b.writeLimit() + 1);
            assertTrue(b.unchecked(), "Unchecked flag should be set after passing writeLimit");
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked(), "Unchecked flag should be cleared after writeLimit underflow");
        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("readPosition before start toggles unchecked flag")
    public void testReadLimitTooSmall(Allocator alloc1) {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.readPosition(b.start() - 1);
            assertTrue(b.unchecked(), "Unchecked flag should be set after moving before start");
        } catch (DecoratedBufferUnderflowException ex) {
            assertFalse(b.unchecked(), "Unchecked flag should be cleared after start underflow");
        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Unchecked skip moves read position as expected")
    public void uncheckedSkip(Allocator alloc1) {
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes alter unchecked skip behaviour");

        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.uncheckedReadSkipOne();
            assertEquals(1, b.readPosition(), "Skip one should advance read position");
            b.uncheckedReadSkipBackOne();
            assertEquals(0, b.readPosition(), "Skip back should return to start");
            b.writeUnsignedByte('H');
            b.writeUnsignedByte(0xFF);
            assertEquals('H', b.uncheckedReadUnsignedByte(),
                    "First unchecked unsigned byte should match written value");
            assertEquals(0xFF, b.uncheckedReadUnsignedByte(),
                    "Second unchecked unsigned byte should match written value");
        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Volatile reads return the values written")
    public void readVolatile(Allocator alloc1) {
        assumeFalse(alloc1 == HEX_DUMP, "Hex dump allocator does not support volatile reads");
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.writeVolatileByte(0, (byte) 1);
            b.writeVolatileShort(1, (short) 2);
            b.writeVolatileInt(3, 3);
            b.writeVolatileLong(7, 4);
            assertEquals(1, b.readVolatileByte(0), "Volatile byte should match at offset 0");
            assertEquals(2, b.readVolatileShort(1), "Volatile short should match at offset 1");
            assertEquals(3, b.readVolatileInt(3), "Volatile int should match at offset 3");
            assertEquals(4, b.readVolatileLong(7), "Volatile long should match at offset 7");

        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Hash code reflects written long values")
    public void testHashCode(Allocator alloc1) {
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded native bytes change hash code expectations");

        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.writeLong(0);
            assertEquals(0, b.hashCode(), "Hash code should be zero for value 0");
            b.clear();
            b.writeLong(1);
            assertEquals(0x152ad77e, b.hashCode(), "Hash code should match for value 1");
            b.clear();
            b.writeLong(2);
            assertEquals(0x2a55aefc, b.hashCode(), "Hash code should match for value 2");
            b.clear();
            b.writeLong(3);
            assertEquals(0x7f448df2, b.hashCode(), "Hash code should match for value 3");
            b.clear();
            b.writeLong(4);
            assertEquals(0x54ab5df8, b.hashCode(), "Hash code should match for value 4");

        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Enum values round-trip through bytes")
    public void testEnum(Allocator alloc1) {

        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.writeEnum(HEAP);
            b.writeEnum(NATIVE);
            assertEquals(HEAP, b.readEnum(Allocator.class), "Enum read should return HEAP");
            assertEquals(NATIVE, b.readEnum(Allocator.class), "Enum read should return NATIVE");

        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Time millis formatting converts 12345678ms to HH:MM:SS.mmm string")
    public void testTimeMillis(Allocator alloc1) {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.appendTimeMillis(12345678L);
            assertEquals("03:25:45.678", b.toString(), "Time millis formatting should match expected output");

        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Date millis formatting matches expected output")
    public void testDateTimeMillis(Allocator alloc1) {
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            b.appendDateMillis(12345 * 86400_000L);
            assertEquals("20031020", b.toString(), "Date millis formatting should match expected output");

        } finally {
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Offset writes copy data correctly between buffers")
    public void testWriteOffset(Allocator alloc1) {
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
            assertEquals(from.readLong(0), to.readLong(0), "Copied long should match at offset 0");
        } finally {
            postTest(from);
            postTest(to);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("toString returns appended string without mutating read or write positions")
    public void testToStringDoesNotChange(Allocator alloc1) {
        @NotNull Bytes<?> a = alloc1.elasticBytes(16);
        @NotNull Bytes<?> b = alloc1.elasticBytes(16);
        try {
            String hello = "hello";
            a.append(hello);
            b.append(hello);

            assertTrue(a.contentEquals(b), "Bytes a and b contentEquals should return true before calling toString on b");
            assertEquals(a.bytesStore(), b.bytesStore(), "Bytes a and b bytesStore equality should hold before calling toString on b");

            assertEquals(hello, b.toString(), "toString should return the appended content");

            assertTrue(a.contentEquals(b), "Bytes a and b contentEquals should remain true after calling toString on b");
            assertEquals(a.bytesStore(), b.bytesStore(), "Bytes a and b bytesStore equality should remain after calling toString on b");
        } finally {
            postTest(a);
            postTest(b);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("to8bitString matches toString output exactly for bytes")
    public void to8BitString(Allocator alloc1) {
        @NotNull Bytes<?> a = alloc1.elasticBytes(16);
        try {
            assertEquals(a.toString(), a.to8bitString(), "Empty bytes should render identically in 8-bit string");
            String hello = "hello";
            a.append(hello);
            assertEquals(a.toString(), a.to8bitString(), "8-bit string should match toString output");
        } finally {
            postTest(a);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("parseDouble returns zero when readLimit excludes digits")
    public void testParseDoubleReadLimit(Allocator alloc1) {
        Bytes<?> bytes = alloc1.fixedBytes(52);
        try {
            final String spaces = "   ";
            bytes.append(spaces).append(1.23);
            bytes.readLimit(spaces.length());
            // only fails when assertions are off
            assertEquals(0, BytesInternal.parseDouble(bytes), 0,
                    "parseDouble should return zero when readLimit excludes digits");
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write8bit round-trips string values correctly for bytes")
    public void write8BitString(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support write8bit tests");

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(703);
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i <= 36; i++) {
                final String s = sb.toString();
                bytes.write8bit(s);
                String s2 = bytes.read8bit();
                assertEquals(s, s2, "8-bit string round-trip should match at iteration " + i);
                sb.append(Integer.toString(i, 36));
            }
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write8bit round-trips native bytes correctly for buffers")
    public void write8BitNativeBytes(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support native bytes tests");

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(703);
        Bytes<?> nbytes = Bytes.allocateDirect(36);
        Bytes<?> nbytes2 = Bytes.allocateDirect(36);
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i <= 36; i++) {
                nbytes.clear().append(sb);
                long offset = nbytes.readPosition();
                long readRemaining = Math.min(bytes.writeRemaining(), nbytes.readLimit() - offset);
                bytes.writeStopBit(readRemaining);
                try {
                    bytes.write(nbytes, offset, readRemaining);
                } catch (BufferUnderflowException | IllegalArgumentException e) {
                    throw new AssertionError("Failed to write native bytes at iteration " + i, e);
                }
                bytes.read8bit(nbytes2.clear());

                final String s = sb.toString();
                assertEquals(s, nbytes2.toString(), "Native bytes round-trip should match at iteration " + i);
                sb.append(Integer.toString(i, 36));
            }
        } finally {
            postTest(bytes);
            postTest(nbytes);
            postTest(nbytes2);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write8bit round-trips heap bytes correctly for buffers")
    public void write8BitHeapBytes(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support heap bytes tests");

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(703);
        Bytes<?> nbytes = Bytes.allocateElasticOnHeap(36);
        Bytes<?> nbytes2 = Bytes.allocateElasticOnHeap(36);
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i <= 36; i++) {
                nbytes.clear().append(sb);
                long offset = nbytes.readPosition();
                long readRemaining = Math.min(bytes.writeRemaining(), nbytes.readLimit() - offset);
                bytes.writeStopBit(readRemaining);
                try {
                    bytes.write(nbytes, offset, readRemaining);
                } catch (BufferUnderflowException | IllegalArgumentException e) {
                    throw new AssertionError("Failed to write heap bytes at iteration " + i, e);
                }
                bytes.read8bit(nbytes2.clear());

                assertEquals(sb.toString(), nbytes2.toString(),
                        "Heap bytes round-trip should match at iteration " + i);
                sb.append(Integer.toString(i, 36));
            }
        } finally {
            postTest(bytes);
            postTest(nbytes);
            postTest(nbytes2);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("write8bit round-trips CharSequence values correctly for bytes")
    public void write8BitCharSequence(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_EMBEDDED, "Heap embedded allocator does not support char sequence tests");

        @NotNull Bytes<?> bytes = alloc1.elasticBytes(703);
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        try {
            for (int i = 0; i <= 36; i++) {
                bytes.write8bit(sb);
                bytes.read8bit(sb2);

                assertEquals(sb.toString(), sb2.toString(),
                        "CharSequence round-trip should match at iteration " + i);
                sb.append(Integer.toString(i, 36));
            }
        } finally {
            postTest(bytes);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Stop-bit char values round-trip with sentinel")
    public void stopBitChar(Allocator alloc1) {
        final Bytes<?> bytes = alloc1.fixedBytes(64);
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            bytes.clear();
            char ch = (char) i;
            bytes.writeStopBit(ch);
            bytes.writeUnsignedByte(0x80);
            char c2 = bytes.readStopBitChar();
            assertEquals(ch, c2, "Stop-bit char should round-trip for code " + i);
            assertEquals(0x80, bytes.readUnsignedByte(),
                    "Sentinel byte should follow stop-bit char for code " + i);
        }
        postTest(bytes);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Stop-bit long values round-trip correctly")
    public void stopBitLong(Allocator alloc1) {
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Stop-bit -1 values round-trip for long and char")
    public void stopBitNeg1(Allocator alloc1) {
        final Bytes<?> bytes = alloc1.fixedBytes(64);
        BytesInternal.writeStopBitNeg1(bytes);
        BytesInternal.writeStopBitNeg1(bytes);
        assertEquals(-1, bytes.readStopBit(), "Stop-bit -1 should round-trip for long");
        assertEquals(0xFFFF, bytes.readStopBitChar(), "Stop-bit -1 should round-trip for char");
        postTest(bytes);
    }

    private void postTest(Bytes<?> bytes) {
        bytes.clear();
        assertTrue(bytes.isClear(), "Bytes should be clear after reset");
        assertEquals(0, bytes.readRemaining(), "Bytes should have no readable data after reset");
        bytes.releaseLast();
    }

    private void stopBitLong0(Bytes<?> bytes, long l) {
        bytes.clear();
        bytes.writeStopBit(l);
        bytes.writeUnsignedByte(0x80);
        long l2 = bytes.readStopBit();
        assertEquals(l, l2, "Stop-bit value should round-trip for input " + l);
        assertEquals(0x80, bytes.readUnsignedByte(), "Sentinel byte should follow stop-bit value");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Capacity equals writeLimit for fresh elastic bytes")
    public void capacityVsWriteLimitInvariant(Allocator alloc1) {
        final Bytes<?> bytes = alloc1.elasticBytes(20);
        try {
            assumeTrue(bytes.isElastic(), "Elastic bytes required for capacity/writeLimit invariant");
            assertEquals(bytes.capacity(), bytes.writeLimit(),
                    "Write limit should match capacity for fresh elastic bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Fresh bytes report clear state initially")
    public void isClear(Allocator alloc1) {
        final Bytes<?> bytes = alloc1.elasticBytes(20);
        assertTrue(bytes.isClear(), "Newly allocated bytes should report clear state");
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append double without parseDouble validation enabled")
    public void testAppendDoubleWithoutParseDouble(Allocator alloc1) {

        parseDouble = false;
        // TODO FIX parseDouble()
        testAppendDoubleOnce(alloc1, 1e-11 + Math.ulp(1e-11), "0.00000000001", "0.00000000001", "0.000000000010000000000000001", "0");
        testAppendDoubleOnce(alloc1, 1.0626477603237785E-10, "0.000000000106264776", "0.000000000106264775", "0.00000000010626477603237785", "0");
        testAppendDoubleOnce(alloc1, 1e-18 - Math.ulp(1e-18), "0.000000000000000001", "0.000000000000000001", "0.0000000000000000009999999999999999", "0");
        testAppendDoubleOnce(alloc1, 1e45, "1000000000000000000000000000000000000000000000", "Infinity", "1.0E45", "");
        testAppendDoubleOnce(alloc1, 1e45 + Math.ulp(1e45), "1000000000000000100000000000000000000000000000", "Infinity", "1.0000000000000001E45", "");
        testAppendDoubleOnce(alloc1, -Float.MAX_VALUE, "-340282346638528860000000000000000000000", "-340282350000000000000000000000000000000", "-340282346638528860000000000000000000000", "");
        testAppendDoubleOnce(alloc1, -Double.MIN_NORMAL, "-0", "-0", "-2.2250738585072014E-308", "-0");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append double handles random input values")
    public void testAppendDoubleRandom(Allocator alloc1) {

        parseDouble = true;

        // ok
        testAppendDoubleOnce(alloc1, -145344868913.80002, "-145344868913.80002", "-145344872448", "-145344868913.80002", "-145344868913.80002");

        testAppendDoubleOnce(alloc1, -1.4778838950354771E-9, "-0.000000001477883895", "-0.0000000014778839", "-0.0000000014778838950354771", "-0.000000001");
        testAppendDoubleOnce(alloc1, 1.4753448053710411E-8, "0.000000014753448054", "0.000000014753448", "0.000000014753448053710411", "0.000000015");
        testAppendDoubleOnce(alloc1, 4.731428525883379E-10, "0.000000000473142853", "0.00000000047314286", "0.0000000004731428525883379", "0");
        testAppendDoubleOnce(alloc1, 1.0E-5, "0.00001", "0.00001", "0.00001", "0.00001");
        testAppendDoubleOnce(alloc1, 5.7270847085938394E-9, "0.000000005727084709", "0.0000000057270846", "0.0000000057270847085938394", "0.000000006");
        testAppendDoubleOnce(alloc1, -3.5627763205104632E-9, "-0.000000003562776321", "-0.0000000035627763", "-0.0000000035627763205104632", "-0.000000004");
        testAppendDoubleOnce(alloc1, 3.4363211797092447E-10, "0.000000000343632118", "0.00000000034363212", "0.00000000034363211797092447", "0");
        testAppendDoubleOnce(alloc1, 0.7205789375929972, "0.7205789375929972", "0.7205789", "0.7205789375929972", "0.720578938");
        testAppendDoubleOnce(alloc1, 1.7205789375929972E-8, "0.000000017205789376", "0.000000017205789", "0.000000017205789375929972", "0.000000017");
        testAppendDoubleOnce(alloc1, 1.000000459754255, "1.000000459754255", "1.0000005", "1.000000459754255", "1.00000046");
        testAppendDoubleOnce(alloc1, 1.0000004597542551, "1.0000004597542552", "1.0000005", "1.0000004597542551", "1.00000046");
        testAppendDoubleOnce(alloc1, -0.0042633243189823394, "-0.00426332431898234", "-0.004263324", "-0.0042633243189823394", "-0.004263324");
        testAppendDoubleOnce(alloc1, 4.3634067645459027E-4, "0.00043634067645459", "0.00043634066", "0.00043634067645459027", "0.000436341");
        testAppendDoubleOnce(alloc1, -4.8378951079402273E-4, "-0.000483789510794023", "-0.0004837895", "-0.00048378951079402273", "-0.00048379");
        testAppendDoubleOnce(alloc1, 3.8098893793449994E-4, "0.0003809889379345", "0.00038098893", "0.00038098893793449994", "0.000380989");
        testAppendDoubleOnce(alloc1, -0.0036980489197619678, "-0.003698048919761968", "-0.0036980489", "-0.0036980489197619678", "-0.003698049");
        testAppendDoubleOnce(alloc1, 1.1777536373898703E-7, "0.000000117775363739", "0.000000117775365", "0.00000011777536373898703", "0.000000118");
        testAppendDoubleOnce(alloc1, 8.577881719106565E-8, "0.000000085778817191", "0.000000085778815", "0.00000008577881719106565", "0.000000086");
        testAppendDoubleOnce(alloc1, 1.1709707236415293E-7, "0.000000117097072364", "0.00000011709707", "0.00000011709707236415293", "0.000000117");
        testAppendDoubleOnce(alloc1, 1.0272238286878982E-7, "0.000000102722382869", "0.00000010272238", "0.00000010272238286878982", "0.000000103");
        testAppendDoubleOnce(alloc1, 9.077547054210796E-8, "0.000000090775470542", "0.00000009077547", "0.00000009077547054210796", "0.000000091");
        testAppendDoubleOnce(alloc1, -1.1914407211387385E-7, "-0.000000119144072114", "-0.00000011914407", "-0.00000011914407211387385", "-0.000000119");
        testAppendDoubleOnce(alloc1, 8.871684275243539E-4, "0.000887168427524354", "0.00088716845", "0.0008871684275243539", "0.000887168");
        testAppendDoubleOnce(alloc1, 8.807878708605213E-4, "0.000880787870860521", "0.00088078785", "0.0008807878708605213", "0.000880788");
        testAppendDoubleOnce(alloc1, 8.417670165790972E-4, "0.000841767016579097", "0.000841767", "0.0008417670165790972", "0.000841767");
        testAppendDoubleOnce(alloc1, 0.0013292726996348332, "0.001329272699634833", "0.0013292728", "0.0013292726996348332", "0.001329273");
        testAppendDoubleOnce(alloc1, 2.4192540417349368E-4, "0.000241925404173494", "0.0002419254", "0.00024192540417349368", "0.000241925");
        testAppendDoubleOnce(alloc1, 1.9283711356548258E-4, "0.000192837113565483", "0.00019283712", "0.00019283711356548258", "0.000192837");
        testAppendDoubleOnce(alloc1, -8.299137873077923E-5, "-0.000082991378730779", "-0.00008299138", "-0.00008299137873077923", "-0.000082991");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append double formats powers of ten")
    public void testAppendDoublePowersOfTen(Allocator alloc1) {
        parseDouble = true;
        // OK
        testAppendDoubleOnce(alloc1, 0.0, "0", "0", "0", "0");
        testAppendDoubleOnce(alloc1, 0.001, "0.001", "0.001", "0.001", "0.001");
        testAppendDoubleOnce(alloc1, 1.0E-4, "0.0001", "0.0001", "0.0001", "0.0001");
        testAppendDoubleOnce(alloc1, 1.0E-6, "0.000001", "0.000001", "0.000001", "0.000001");
        testAppendDoubleOnce(alloc1, 1.0E-7, "0.0000001", "0.0000001", "0.0000001", "0.0000001");
        testAppendDoubleOnce(alloc1, 1.0E-8, "0.00000001", "0.00000001", "0.00000001", "0.00000001");
        testAppendDoubleOnce(alloc1, 1.0E-9, "0.000000001", "0.000000001", "0.000000001", "0.000000001");
        testAppendDoubleOnce(alloc1, 0.009, "0.009", "0.009", "0.009", "0.009");
        testAppendDoubleOnce(alloc1, 9.0E-4, "0.0009", "0.0009", "0.0009", "0.0009");
        testAppendDoubleOnce(alloc1, 9.0E-5, "0.00009", "0.00009", "0.00009", "0.00009");
        testAppendDoubleOnce(alloc1, 9.0E-6, "0.000009", "0.000009", "0.000009", "0.000009");
        testAppendDoubleOnce(alloc1, 9.0E-7, "0.0000009", "0.0000009", "0.0000009", "0.0000009");
        testAppendDoubleOnce(alloc1, 9.0E-8, "0.00000009", "0.00000009", "0.00000009", "0.00000009");
        testAppendDoubleOnce(alloc1, 9.0E-9, "0.000000009", "0.000000009", "0.000000009", "0.000000009");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append double formats edge cases correctly")
    public void testAppendDoubleEdgeCases(Allocator alloc1) {
        parseDouble = true;
        testAppendDoubleOnce(alloc1, Double.NaN, "NaN", "NaN", "NaN", "");
        testAppendDoubleOnce(alloc1, Double.POSITIVE_INFINITY, "Infinity", "Infinity", "Infinity", "");
        testAppendDoubleOnce(alloc1, Double.NEGATIVE_INFINITY, "-Infinity", "-Infinity", "-Infinity", "");
        testAppendDoubleOnce(alloc1, 0.1, "0.1", "0.1", "0.1", "0.1");
        testAppendDoubleOnce(alloc1, 12.0, "12", "12", "12", "12");
        testAppendDoubleOnce(alloc1, 12.1, "12.1", "12.1", "12.1", "12.1");
        testAppendDoubleOnce(alloc1, 12.00000001, "12.00000001", "12", "12.00000001", "12.00000001");

        testAppendDoubleOnce(alloc1, 1e-6 + Math.ulp(1e-6), "0.000001", "0.000001", "0.0000010000000000000002", "0.000001");
        testAppendDoubleOnce(alloc1, 1e-7 + Math.ulp(1e-7), "0.0000001", "0.0000001", "0.00000010000000000000001", "0.0000001");
        testAppendDoubleOnce(alloc1, 1e-8 + Math.ulp(1e-8), "0.00000001", "0.00000001", "0.000000010000000000000002", "0.00000001");
        testAppendDoubleOnce(alloc1, 1e-9 + Math.ulp(1e-9), "0.000000001", "0.000000001", "0.0000000010000000000000003", "0.000000001");
        testAppendDoubleOnce(alloc1, 1e-10 + Math.ulp(1e-10), "0.0000000001", "0.0000000001", "0.00000000010000000000000002", "0");
        testAppendDoubleOnce(alloc1, 1e-12 + Math.ulp(1e-12), "0.000000000001", "0.000000000001", "0.0000000000010000000000000002", "0");
        testAppendDoubleOnce(alloc1, 1.0626477603237786E-11, "0.000000000010626478", "0.000000000010626478", "0.000000000010626477603237786", "0");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append double formats limit values correctly")
    public void testAppendDoubleLimits(Allocator alloc1) {
        // limits
        testAppendDoubleOnce(alloc1, 1.0E-18, "0.000000000000000001", "0.000000000000000001", "0.000000000000000001", "0");
        testAppendDoubleOnce(alloc1, 1.0E-29, "0", "0", "0.000000000000000000000000000010", "0");
        testAppendDoubleOnce(alloc1, 1e-29 - Math.ulp(1e-29), "0", "0", "9.999999999999998E-30", "0");
        testAppendDoubleOnce(alloc1, 1e45 - Math.ulp(1e45), "999999999999999800000000000000000000000000000", "Infinity", "999999999999999800000000000000000000000000000", "");
        testAppendDoubleOnce(alloc1, -Double.MIN_VALUE, "-0", "-0", "-4.9E-324", "-0");
        testAppendDoubleOnce(alloc1, -Float.MIN_VALUE, "-0", "-0", "-1.401298464324817E-45", "-0");

        testAppendDoubleOnce(alloc1, 0.0, "0.0", "0.0", "0.0", "0.0", true);
        testAppendDoubleOnce(alloc1, -Double.MIN_VALUE, "-0.0", "-0.0", "-4.9E-324", "-0.0", true);
        testAppendDoubleOnce(alloc1, -Float.MIN_VALUE, "-0.0", "-0.0", "-1.401298464324817E-45", "-0.0", true);
        testAppendDoubleOnce(alloc1, 12.0, "12.0", "12.0", "12.0", "12.0", true);
        testAppendDoubleOnce(alloc1, Long.MIN_VALUE, "-9223372036854776000.0", "-9223372000000000000.0", "-9223372036854775807.0", "", true);
        testAppendDoubleOnce(alloc1, Long.MAX_VALUE, "9223372036854776000.0", "9223372000000000000.0", "9223372036854775807.0", "", true);

        assumeFalse(alloc1 == HEAP_EMBEDDED || alloc1 == HEAP_UNCHECKED,
                "Embedded or unchecked heap allocators do not support this double append case");
        testAppendDoubleOnce(alloc1, -Double.MAX_VALUE, "-179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000", "-Infinity", "-1.7976931348623157E308", "");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append parses very small double values")
    public void testAppendReallySmallDouble(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_UNCHECKED, "Unchecked heap allocator cannot validate small doubles");
        int size = 48;
        Bytes<?> bytes = alloc1.elasticBytes(size + 8);
        bytes.decimaliser(GeneralDecimaliser.GENERAL);

        for (double d = 1; d >= Double.MIN_NORMAL; d *= 0.99) {
            bytes.writeLong(size, 0);
            bytes.clear();
            bytes.append(d);
            assertEquals(0, bytes.readLong(size), "Scratch long should remain zero for small double " + d);
            // Determine expected precision error based on magnitude of value
            // ok for not easily decimalised
            double err = d > 2.3e-10 ? 0
                    : d > 2.0e-13 && !Jvm.isArm() ? Math.ulp(d)
                    : 2 * Math.ulp(d);
            assertEquals(d, bytes.parseDouble(), err, "Parsed double should match appended small double " + d);
        }
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append parses very large double values")
    public void testAppendReallyBigDouble(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_UNCHECKED, "Unchecked heap allocator cannot validate big doubles");
        int size = 48;
        Bytes<?> bytes = alloc1.elasticBytes(size + 8);
        bytes.decimaliser(GeneralDecimaliser.GENERAL);

        for (double d = -1; d > Double.NEGATIVE_INFINITY; d *= 1.01) {
            bytes.writeLong(size, 0);
            bytes.clear();
            bytes.append(d);
            assertEquals(0, bytes.readLong(size), "Scratch long should remain zero for large double " + d);
            // Determine expected precision error based on magnitude of value
            // ok for not easily decimalised
            double err = d > -1.3e12 ? 0
                    : d > -1e39 ? Math.ulp(d)
                    : 2 * Math.ulp(d);
            double actual = bytes.parseDouble();
            assertEquals(d, actual, err, "Parsed double should match appended large double " + d);
        }
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append parses very small float values")
    public void testAppendReallySmallFloat(Allocator alloc1) {
        assumeFalse(alloc1 == HEAP_UNCHECKED, "Unchecked heap allocator cannot validate small floats");
        int size = 48;
        Bytes<?> bytes = alloc1.elasticBytes(size + 8);
        bytes.decimaliser(GeneralDecimaliser.GENERAL);

        for (float f = 1; f > Float.MIN_NORMAL; f *= 0.99f) {
            bytes.writeLong(size, 0);
            bytes.clear();
            bytes.append(f);
            assertEquals(0, bytes.readLong(size), "Scratch long should remain zero for small float " + f);
            // Determine expected precision error based on magnitude of value
            // ok for not easily decimalised
            float err = f > 1.2e-4 ? 0 : Math.ulp(f);
            assertEquals(f, bytes.parseFloat(), err, "Parsed float should match appended small float " + f);
        }
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Append parses very large float values")
    public void testAppendReallyBigFloat(Allocator alloc1) {
        int size = 48;
        Bytes<?> bytes = alloc1.elasticBytes(size + 8);

        for (float f = 1; f < Float.POSITIVE_INFINITY; f *= 1.01f) {
            bytes.writeLong(size, 0);
            bytes.clear();
            bytes.append(f);
            assertEquals(0, bytes.readLong(size), "Scratch long should remain zero for large float " + f);
            assertEquals(f, bytes.parseFloat(), 0.0f, "Parsed float should match appended large float " + f);
        }
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Read with offset preserves prefix bytes")
    public void testReadWithOffset(Allocator alloc1) {
        Bytes<?> bytes = alloc1.elasticBytes(32);
        bytes.append("Hello");
        int offset = 2;
        int offsetInRDI = 1;
        byte[] ba = new byte[bytes.length() + offset - offsetInRDI];
        ba[0] = '0';
        ba[1] = '1';
        bytes.read(offsetInRDI, ba, offset, bytes.length() - offsetInRDI);
        assertEquals("01ello", new String(ba, ISO_8859_1),
                "Offset read should preserve prefix and shifted content");
        bytes.releaseLast();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("writeSkip handles negative offsets safely for bytes")
    public void writeSkipNegative(Allocator alloc1) {
        @NotNull Bytes<?> a = alloc1.elasticBytes(16);
        try {
            String hello = "hello";
            a.append(hello);
            assertEquals(hello, a.toString(), "Initial append should store the full string");
            a.writeSkip(-hello.length());
            assertEquals("", a.toString(), "Negative skip should rewind to empty string");
            if (!a.unchecked())
                assertThrows(BufferOverflowException.class, () -> a.writeSkip(-1),
                        "Checked bytes should reject negative skip beyond bounds");
        } finally {
            postTest(a);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("copyTo writes bytes to output stream")
    public void testCopyToStream(Allocator alloc1) throws IOException {
        @NotNull Bytes<?> a = alloc1.elasticBytes(16);
        String text = "Hello World";

        try {
            a.append(text);
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                a.copyTo(os);

                byte[] array = os.toByteArray();
                assertEquals(text.length(), array.length, "Copied stream length should match source length");
                assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), array,
                        "Copied stream bytes should match UTF-8 source");
            }
        } finally {
            postTest(a);
        }
    }

    private void testAppendDoubleOnce(Allocator alloc1,
                                      double value,
                                      String standard,
                                      String standardFloat,
                                      String general,
                                      String expectedDecimal9) {
        testAppendDoubleOnce(alloc1, value, standard, standardFloat, general, expectedDecimal9, false);
    }

    @SuppressWarnings("deprecation")
    private void testAppendDoubleOnce(Allocator alloc1,
                                      double value,
                                      String standard,
                                      String standardFloat,
                                      String general,
                                      String expectedDecimal9,
                                      boolean append0) {
        @NotNull Bytes<?> a = alloc1.elasticBytes(255)
                .fpAppend0(append0)
                .decimaliser(StandardDecimaliser.STANDARD);
        try {
            a.append(value);
            String actual = a.toString();
            assertEquals(standard, actual, "Standard decimal output should match for value " + value);

            a.clear();
            a.append((float) value);
            String actual2 = a.toString();
            assertEquals(standardFloat, actual2, "Standard float output should match for value " + value);

            a.decimaliser(GeneralDecimaliser.GENERAL);
            a.clear();
            a.append(value);
            String actualg = a.toString();
            double actualParsed = a.parseDouble();
            if (parseDouble)
                assertEquals(value, actualParsed, 0.0, "Parsed double should match general output for value " + value);
            assertEquals(general, actualg, "General decimal output should match for value " + value);

            a.clear();
            // if empty don't expect it to be translated
            boolean decimal = new MaximumPrecision(9).toDecimal(value, (DecimalAppender) a);
            assertEquals(!expectedDecimal9.isEmpty(), decimal,
                    "Decimal conversion flag should match expected output for value " + value);
            String actual3 = a.toString();
            assertEquals(expectedDecimal9, actual3,
                    "Decimal precision output should match for value " + value);

        } finally {
            a.releaseLast();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Write into heap ByteBuffer matches expected layout")
    public void testWriteOnHeap(Allocator alloc1) throws Exception {
        doTestWrite(alloc1, () -> ByteBuffer.allocate(128));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Write into direct ByteBuffer matches expected layout")
    public void testWriteDirect(Allocator alloc1) throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for direct write test");

        doTestWrite(alloc1, () -> ByteBuffer.allocateDirect(128));
    }

    private void doTestWrite(Allocator alloc1, Callable<ByteBuffer> generator) throws Exception {
        final Bytes<?> data = alloc1.elasticBytes(128);
        try {
            ByteBuffer buffer = generator.call();

            for (byte c = ' '; c < '`'; c++) {
                data.writeChar((char) c);
                buffer.put(c);
            }

            BytesStore<?, ?> heapBytesStore = data.bytesStore();
            heapBytesStore.write(16, buffer, 32, 8);
            for (int i = 0; i < 16; i++)
                assertEquals(i + ' ', heapBytesStore.readByte(i), "Lower region should match at index " + i);

            for (int i = 16; i < 24; i++)
                assertEquals(i + '0', heapBytesStore.readByte(i), "Middle region should match at index " + i);

            for (int i = 24; i < 32; i++)
                assertEquals(i + ' ', heapBytesStore.readByte(i), "Upper region should match at index " + i);
        } finally {
            data.releaseLast();
        }
    }
}
