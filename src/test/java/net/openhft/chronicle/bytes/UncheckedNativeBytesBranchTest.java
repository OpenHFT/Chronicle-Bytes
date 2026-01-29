/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import net.openhft.chronicle.bytes.internal.UncheckedRandomDataInput;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UncheckedNativeBytes covering branch coverage for unchecked
 * native memory operations.
 */
@SuppressWarnings("deprecation")
@DisplayName("UncheckedNativeBytes branch coverage for unchecked native memory operations")
class UncheckedNativeBytesBranchTest extends BytesTestCommon {

    private Bytes<?> underlying;
    private UncheckedNativeBytes<?> uncheckedBytes;

    @BeforeEach
    void setUp() {
        underlying = Bytes.allocateElasticDirect(256);
        uncheckedBytes = new UncheckedNativeBytes<>(underlying);
    }

    @AfterEach
    void tearDown() {
        if (uncheckedBytes != null) {
            uncheckedBytes.releaseLast();
        }
        if (underlying != null) {
            underlying.releaseLast();
        }
    }

    // ========== Basic Properties ==========

    @Test
    @DisplayName("unchecked reports true for unchecked bytes view state")
    void shouldReturnTrueForUnchecked() {
        assertTrue(uncheckedBytes.unchecked(),
                "unchecked returns true for unchecked bytes view");
    }

    @Test
    @DisplayName("isDirectMemory reports direct memory backing for unchecked bytes")
    void shouldReturnTrueForDirectMemory() {
        assertTrue(uncheckedBytes.isDirectMemory(),
                "isDirectMemory returns true for direct memory");
    }

    @Test
    @DisplayName("isElastic reports non-elastic bytes view")
    void shouldReturnFalseForIsElastic() {
        assertFalse(uncheckedBytes.isElastic(),
                "isElastic returns false for unchecked bytes");
    }

    @Test
    @DisplayName("lenient reports strict unchecked bytes mode")
    void shouldReturnFalseForLenient() {
        assertFalse(uncheckedBytes.lenient(),
                "lenient returns false for unchecked bytes");
    }

    @Test
    @DisplayName("lenient(true) rejects lenient mode change")
    void shouldThrowWhenSettingLenient() {
        assertThrows(UnsupportedOperationException.class,
                () -> uncheckedBytes.lenient(true),
                "lenient(true) throws UnsupportedOperationException");
    }

    @Test
    @DisplayName("unchecked(boolean) returns same instance for unchecked bytes")
    void shouldReturnSelfForUncheckedSetter() {
        Bytes<?> result = uncheckedBytes.unchecked(false);
        assertSame(uncheckedBytes, result,
                "unchecked(false) returns same instance");
    }

    // ========== Read/Write Position and Limit ==========

    @Test
    @DisplayName("readPosition stores and returns position for unchecked bytes")
    void shouldSetAndGetReadPosition() {
        uncheckedBytes.writeLong(0x123456789ABCDEF0L);
        uncheckedBytes.readPosition(4);

        assertEquals(4, uncheckedBytes.readPosition(),
                "readPosition returns 4 after set");
    }

    @Test
    @DisplayName("writePosition stores and returns position for unchecked bytes")
    void shouldSetAndGetWritePosition() {
        uncheckedBytes.writePosition(10);

        assertEquals(10, uncheckedBytes.writePosition(),
                "writePosition returns 10 after set");
    }

    @Test
    @DisplayName("readLimit equals writePosition after set for unchecked bytes")
    void shouldReturnWritePositionAsReadLimit() {
        uncheckedBytes.writePosition(20);

        assertEquals(20, uncheckedBytes.readLimit(),
                "readLimit equals writePosition value 20");
    }

    @Test
    @DisplayName("writeLimit stores and returns limit for unchecked bytes")
    void shouldSetAndGetWriteLimit() {
        uncheckedBytes.writeLimit(100);

        assertEquals(100, uncheckedBytes.writeLimit(),
                "writeLimit returns 100 after set");
    }

    @Test
    @DisplayName("readSkip advances read position by length")
    void shouldSkipReadPosition() {
        uncheckedBytes.write(new byte[20]);
        uncheckedBytes.readPosition(0);
        uncheckedBytes.readSkip(5);

        assertEquals(5, uncheckedBytes.readPosition(),
                "readPosition advances to 5 after readSkip");
    }

    @Test
    @DisplayName("writeSkip advances write position by length")
    void shouldSkipWritePosition() {
        uncheckedBytes.writeSkip(10);

        assertEquals(10, uncheckedBytes.writePosition(),
                "writePosition advances to 10 after writeSkip");
    }

    // ========== Byte Operations ==========

    @Test
    @DisplayName("writeByte and readByte round trip value")
    void shouldWriteAndReadByte() {
        uncheckedBytes.writeByte((byte) 42);
        uncheckedBytes.readPosition(0);

        assertEquals(42, uncheckedBytes.readByte(),
                "readByte returns 42 after write");
    }

    @Test
    @DisplayName("rawWriteByte writes byte to backing store")
    void shouldRawWriteByte() {
        uncheckedBytes.rawWriteByte((byte) 99);

        uncheckedBytes.readPosition(0);
        assertEquals(99, uncheckedBytes.readByte(),
                "rawWriteByte stores value 99");
    }

    @Test
    @DisplayName("readUnsignedByte returns unsigned value from unchecked bytes")
    void shouldReadUnsignedByte() {
        uncheckedBytes.writeByte((byte) -1); // 255 unsigned
        uncheckedBytes.readPosition(0);

        assertEquals(255, uncheckedBytes.readUnsignedByte(),
                "readUnsignedByte returns 255 for 0xFF");
    }

    @Test
    @DisplayName("uncheckedReadUnsignedByte returns value without checks from unchecked bytes")
    void shouldUncheckedReadUnsignedByte() {
        uncheckedBytes.writeByte((byte) 128);
        uncheckedBytes.readPosition(0);

        assertEquals(128, uncheckedBytes.uncheckedReadUnsignedByte(),
                "uncheckedReadUnsignedByte returns 128 after write");
    }

    @Test
    @DisplayName("prewriteByte writes before current read position")
    void shouldPrewriteByte() {
        uncheckedBytes.writePosition(10);
        uncheckedBytes.readPosition(10);
        uncheckedBytes.prewriteByte((byte) 55);

        assertEquals(9, uncheckedBytes.readPosition(),
                "readPosition moves back to 9 after prewriteByte");
        assertEquals(55, uncheckedBytes.readByte(9),
                "prewriteByte stores value 55 at position 9");
    }

    // ========== Short Operations ==========

    @Test
    @DisplayName("writeShort and readShort round trip value")
    void shouldWriteAndReadShort() {
        uncheckedBytes.writeShort((short) 12345);
        uncheckedBytes.readPosition(0);

        assertEquals(12345, uncheckedBytes.readShort(),
                "readShort returns 12345 after write");
    }

    @Test
    @DisplayName("prewriteShort writes before current read position")
    void shouldPrewriteShort() {
        uncheckedBytes.writePosition(10);
        uncheckedBytes.readPosition(10);
        uncheckedBytes.prewriteShort((short) 1234);

        assertEquals(8, uncheckedBytes.readPosition(),
                "readPosition moves back to 8 after prewriteShort");
    }

    // ========== Int Operations ==========

    @Test
    @DisplayName("writeInt and readInt round trip value")
    void shouldWriteAndReadInt() {
        uncheckedBytes.writeInt(0x12345678);
        uncheckedBytes.readPosition(0);

        assertEquals(0x12345678, uncheckedBytes.readInt(),
                "readInt returns 0x12345678 after write");
    }

    @Test
    @DisplayName("writeIntAdv writes int and advances write position")
    void shouldWriteIntAdv() {
        uncheckedBytes.writeIntAdv(0xABCDEF01, 8);

        assertEquals(8, uncheckedBytes.writePosition(),
                "writePosition advances to 8 after writeIntAdv");
        uncheckedBytes.readPosition(0);
        assertEquals(0xABCDEF01, uncheckedBytes.readInt(),
                "writeIntAdv stores 0xABCDEF01 at position 0");
    }

    @Test
    @DisplayName("prewriteInt writes before current read position")
    void shouldPrewriteInt() {
        uncheckedBytes.writePosition(20);
        uncheckedBytes.readPosition(20);
        uncheckedBytes.prewriteInt(0x11223344);

        assertEquals(16, uncheckedBytes.readPosition(),
                "readPosition moves back to 16 after prewriteInt");
    }

    @Test
    @DisplayName("writeOrderedInt stores value with ordering in unchecked bytes")
    void shouldWriteOrderedInt() {
        uncheckedBytes.writeOrderedInt(0x55667788);
        uncheckedBytes.readPosition(0);

        assertEquals(0x55667788, uncheckedBytes.readVolatileInt(),
                "readVolatileInt returns ordered value 0x55667788");
    }

    // ========== Long Operations ==========

    @Test
    @DisplayName("writeLong and readLong round trip value")
    void shouldWriteAndReadLong() {
        uncheckedBytes.writeLong(0x123456789ABCDEF0L);
        uncheckedBytes.readPosition(0);

        assertEquals(0x123456789ABCDEF0L, uncheckedBytes.readLong(),
                "readLong returns 0x123456789ABCDEF0 after write");
    }

    @Test
    @DisplayName("writeLongAdv writes long and advances write position")
    void shouldWriteLongAdv() {
        uncheckedBytes.writeLongAdv(0xFEDCBA9876543210L, 16);

        assertEquals(16, uncheckedBytes.writePosition(),
                "writePosition advances to 16 after writeLongAdv");
        uncheckedBytes.readPosition(0);
        assertEquals(0xFEDCBA9876543210L, uncheckedBytes.readLong(),
                "writeLongAdv stores 0xFEDCBA9876543210 at position 0");
    }

    @Test
    @DisplayName("prewriteLong writes before current read position")
    void shouldPrewriteLong() {
        uncheckedBytes.writePosition(24);
        uncheckedBytes.readPosition(24);
        uncheckedBytes.prewriteLong(0xAABBCCDDEEFF0011L);

        assertEquals(16, uncheckedBytes.readPosition(),
                "readPosition moves back to 16 after prewriteLong");
    }

    @Test
    @DisplayName("writeOrderedLong stores value with ordering in unchecked bytes")
    void shouldWriteOrderedLong() {
        uncheckedBytes.writeOrderedLong(0x1122334455667788L);
        uncheckedBytes.readPosition(0);

        assertEquals(0x1122334455667788L, uncheckedBytes.readVolatileLong(),
                "readVolatileLong returns ordered value 0x1122334455667788");
    }

    // ========== Float and Double Operations ==========

    @ParameterizedTest(name = "writeFloat round-trip value {0}")
    @ValueSource(floats = {0.0f, -0.0f, 1.0f, -1.0f, Float.MAX_VALUE, Float.MIN_VALUE,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    @DisplayName("writeFloat and readFloat round trip special values")
    void shouldWriteAndReadFloat(float value) {
        uncheckedBytes.writeFloat(value);
        uncheckedBytes.readPosition(0);

        assertEquals(value, uncheckedBytes.readFloat(), 0.0f,
                "readFloat returns value " + value);
    }

    @Test
    @DisplayName("writeFloat preserves NaN value in unchecked bytes")
    void shouldWriteFloatNaN() {
        uncheckedBytes.writeFloat(Float.NaN);
        uncheckedBytes.readPosition(0);

        assertTrue(Float.isNaN(uncheckedBytes.readFloat()),
                "readFloat returns NaN after write");
    }

    @ParameterizedTest(name = "writeDouble round-trip value {0}")
    @ValueSource(doubles = {0.0, -0.0, 1.0, -1.0, Double.MAX_VALUE, Double.MIN_VALUE,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("writeDouble and readDouble round trip special values")
    void shouldWriteAndReadDouble(double value) {
        uncheckedBytes.writeDouble(value);
        uncheckedBytes.readPosition(0);

        assertEquals(value, uncheckedBytes.readDouble(), 0.0,
                "readDouble returns value " + value);
    }

    @Test
    @DisplayName("writeDoubleAndInt stores double and int values")
    void shouldWriteDoubleAndInt() {
        uncheckedBytes.writeDoubleAndInt(3.14159, 42);
        uncheckedBytes.readPosition(0);

        assertEquals(3.14159, uncheckedBytes.readDouble(), 0.00001,
                "writeDoubleAndInt stores double 3.14159");
        assertEquals(42, uncheckedBytes.readInt(),
                "writeDoubleAndInt stores int 42");
    }

    // ========== Volatile Operations ==========

    @Test
    @DisplayName("readVolatileByte reads latest byte value in unchecked bytes")
    void shouldReadVolatileByte() {
        uncheckedBytes.writeByte(0, (byte) 77);

        assertEquals(77, uncheckedBytes.readVolatileByte(0),
                "readVolatileByte returns 77 after write");
    }

    @Test
    @DisplayName("readVolatileShort reads latest short value in unchecked bytes")
    void shouldReadVolatileShort() {
        uncheckedBytes.writeShort(0, (short) 1234);

        assertEquals(1234, uncheckedBytes.readVolatileShort(0),
                "readVolatileShort returns 1234 after write");
    }

    @Test
    @DisplayName("readVolatileInt reads latest int value in unchecked bytes")
    void shouldReadVolatileInt() {
        uncheckedBytes.writeInt(0, 0x12345678);

        assertEquals(0x12345678, uncheckedBytes.readVolatileInt(0),
                "readVolatileInt returns 0x12345678 after write");
    }

    @Test
    @DisplayName("readVolatileLong reads latest long value in unchecked bytes")
    void shouldReadVolatileLong() {
        uncheckedBytes.writeLong(0, 0x123456789ABCDEF0L);

        assertEquals(0x123456789ABCDEF0L, uncheckedBytes.readVolatileLong(0),
                "readVolatileLong returns 0x123456789ABCDEF0 after write");
    }

    @Test
    @DisplayName("writeVolatileByte stores value with ordering in unchecked bytes")
    void shouldWriteVolatileByte() {
        uncheckedBytes.writeVolatileByte(0, (byte) 88);

        assertEquals(88, uncheckedBytes.readByte(0),
                "readByte returns 88 after writeVolatileByte");
    }

    @Test
    @DisplayName("writeVolatileShort stores value with ordering in unchecked bytes")
    void shouldWriteVolatileShort() {
        uncheckedBytes.writeVolatileShort(0, (short) 5678);

        assertEquals(5678, uncheckedBytes.readShort(0),
                "readShort returns 5678 after writeVolatileShort");
    }

    @Test
    @DisplayName("writeVolatileInt stores value with ordering in unchecked bytes")
    void shouldWriteVolatileInt() {
        uncheckedBytes.writeVolatileInt(0, 0xAABBCCDD);

        assertEquals(0xAABBCCDD, uncheckedBytes.readInt(0),
                "readInt returns 0xAABBCCDD after writeVolatileInt");
    }

    @Test
    @DisplayName("writeVolatileLong stores value with ordering in unchecked bytes")
    void shouldWriteVolatileLong() {
        uncheckedBytes.writeVolatileLong(0, 0xAABBCCDDEEFF0011L);

        assertEquals(0xAABBCCDDEEFF0011L, uncheckedBytes.readLong(0),
                "readLong returns 0xAABBCCDDEEFF0011 after writeVolatileLong");
    }

    // ========== Offset-based Operations ==========

    @Test
    @DisplayName("writeByte writes value at offset in unchecked bytes")
    void shouldWriteByteAtOffset() {
        uncheckedBytes.writeByte(10, (byte) 42);

        assertEquals(42, uncheckedBytes.readByte(10),
                "readByte returns 42 at offset 10");
    }

    @Test
    @DisplayName("writeShort writes value at offset in unchecked bytes")
    void shouldWriteShortAtOffset() {
        uncheckedBytes.writeShort(10, (short) 1234);

        assertEquals(1234, uncheckedBytes.readShort(10),
                "readShort returns 1234 at offset 10");
    }

    @Test
    @DisplayName("writeInt writes value at offset in unchecked bytes")
    void shouldWriteIntAtOffset() {
        uncheckedBytes.writeInt(10, 0x12345678);

        assertEquals(0x12345678, uncheckedBytes.readInt(10),
                "readInt returns 0x12345678 at offset 10");
    }

    @Test
    @DisplayName("writeLong writes value at offset in unchecked bytes")
    void shouldWriteLongAtOffset() {
        uncheckedBytes.writeLong(10, 0x123456789ABCDEF0L);

        assertEquals(0x123456789ABCDEF0L, uncheckedBytes.readLong(10),
                "readLong returns 0x123456789ABCDEF0 at offset 10");
    }

    @Test
    @DisplayName("writeFloat writes value at offset in unchecked bytes")
    void shouldWriteFloatAtOffset() {
        uncheckedBytes.writeFloat(10, 3.14f);

        assertEquals(3.14f, uncheckedBytes.readFloat(10), 0.001f,
                "readFloat returns 3.14 at offset 10");
    }

    @Test
    @DisplayName("writeDouble writes value at offset in unchecked bytes")
    void shouldWriteDoubleAtOffset() {
        uncheckedBytes.writeDouble(10, 3.14159);

        assertEquals(3.14159, uncheckedBytes.readDouble(10), 0.00001,
                "readDouble returns 3.14159 at offset 10");
    }

    @Test
    @DisplayName("readUnsignedByte returns unsigned at offset in unchecked bytes")
    void shouldReadUnsignedByteAtOffset() {
        uncheckedBytes.writeByte(5, (byte) -1);

        assertEquals(255, uncheckedBytes.readUnsignedByte(5),
                "readUnsignedByte returns 255 at offset 5");
    }

    // ========== peekUnsignedByte Tests ==========

    @Test
    @DisplayName("peekUnsignedByte returns minus one for empty buffer state")
    void shouldReturnMinusOneWhenPeekingEmpty() {
        assertEquals(-1, uncheckedBytes.peekUnsignedByte(),
                "peekUnsignedByte returns -1 for empty bytes");
    }

    @Test
    @DisplayName("peekUnsignedByte returns value without advancing read position in buffer")
    void shouldPeekWithoutAdvancing() {
        uncheckedBytes.writeByte((byte) 42);
        uncheckedBytes.readPosition(0);

        int peeked = uncheckedBytes.peekUnsignedByte();
        assertEquals(42, peeked, "peekUnsignedByte returns 42 without advance");
        assertEquals(0, uncheckedBytes.readPosition(),
                "readPosition remains at 0 after peek");
    }

    @Test
    @DisplayName("peekUnsignedByte at offset rejects invalid offset")
    void shouldReturnMinusOneForInvalidOffset() {
        uncheckedBytes.writeByte((byte) 42);

        assertEquals(-1, uncheckedBytes.peekUnsignedByte(100),
                "peekUnsignedByte returns -1 for invalid offset");
    }

    // ========== Byte Array Operations ==========

    @Test
    @DisplayName("write(byte[]) copies entire array into bytes")
    void shouldWriteByteArray() {
        byte[] data = {1, 2, 3, 4, 5};
        uncheckedBytes.write(data, 0, data.length);

        uncheckedBytes.readPosition(0);
        for (byte b : data) {
            assertEquals(b, uncheckedBytes.readByte(),
                    "readByte returns array value " + b);
        }
    }

    @Test
    @DisplayName("write(byte[], offset, length) rejects invalid range")
    void shouldThrowForInvalidByteArrayRange() {
        byte[] data = {1, 2, 3};

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> uncheckedBytes.write(data, 0, 10),
                "write rejects invalid byte array range");
    }

    @Test
    @DisplayName("write at offset copies byte array")
    void shouldWriteByteArrayAtOffset() {
        byte[] data = {10, 20, 30, 40, 50};
        uncheckedBytes.write(5, data, 0, data.length);

        assertEquals(10, uncheckedBytes.readByte(5),
                "readByte returns 10 at offset 5");
    }

    @Test
    @DisplayName("prewrite(byte[]) writes before current read position")
    void shouldPrewriteByteArray() {
        uncheckedBytes.writePosition(20);
        uncheckedBytes.readPosition(20);

        byte[] data = {1, 2, 3, 4, 5};
        uncheckedBytes.prewrite(data);

        assertEquals(15, uncheckedBytes.readPosition(),
                "readPosition moves back by array length");
    }

    // ========== ByteBuffer Operations ==========

    @Test
    @DisplayName("writeSome(ByteBuffer) copies buffer content into bytes")
    void shouldWriteSomeFromByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(new byte[]{10, 20, 30, 40, 50});
        buffer.flip();

        uncheckedBytes.writeSome(buffer);

        uncheckedBytes.readPosition(0);
        assertEquals(10, uncheckedBytes.readByte(),
                "readByte returns 10 after writeSome");
    }

    @Test
    @DisplayName("write at offset copies ByteBuffer into unchecked bytes")
    void shouldWriteByteBufferAtOffset() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(java.nio.ByteOrder.nativeOrder());
        buffer.putLong(0x123456789ABCDEF0L);
        buffer.flip();

        uncheckedBytes.write(10, buffer, 0, 8);

        assertEquals(0x123456789ABCDEF0L, uncheckedBytes.readLong(10),
                "readLong returns buffer value at offset 10");
    }

    // ========== BytesStore Operations ==========

    @Test
    @DisplayName("prewrite(BytesStore) writes before current read position")
    void shouldPrewriteBytesStore() {
        Bytes<?> source = Bytes.allocateElasticDirect(32);
        try {
            source.write(new byte[]{1, 2, 3, 4, 5});

            uncheckedBytes.writePosition(20);
            uncheckedBytes.readPosition(20);
            uncheckedBytes.prewrite(source);

            assertEquals(15, uncheckedBytes.readPosition(),
                    "readPosition moves back after prewrite");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("write at offset copies RandomDataInput into unchecked bytes")
    void shouldWriteRandomDataInputAtOffset() {
        Bytes<?> source = Bytes.allocateElasticDirect(32);
        try {
            source.writeLong(0xDEADBEEFCAFEBABEL);

            uncheckedBytes.write(10, source, 0, 8);

            assertEquals(0xDEADBEEFCAFEBABEL, uncheckedBytes.readLong(10),
                    "readLong returns source value at offset 10");
        } finally {
            source.releaseLast();
        }
    }

    // ========== Compare and Swap Operations ==========

    @Test
    @DisplayName("compareAndSwapInt updates value on match in unchecked bytes")
    void shouldCompareAndSwapIntOnMatch() {
        uncheckedBytes.writeInt(0, 100);

        boolean result = uncheckedBytes.compareAndSwapInt(0, 100, 200);

        assertTrue(result, "compareAndSwapInt should return true when expected value 100 matches, enabling atomic update");
        assertEquals(200, uncheckedBytes.readInt(0),
                "compareAndSwapInt updates value to 200");
    }

    @Test
    @DisplayName("compareAndSwapInt leaves value on mismatch in unchecked bytes")
    void shouldFailCompareAndSwapIntOnMismatch() {
        uncheckedBytes.writeInt(0, 100);

        boolean result = uncheckedBytes.compareAndSwapInt(0, 50, 200);

        assertFalse(result, "compareAndSwapInt returns false on mismatch");
        assertEquals(100, uncheckedBytes.readInt(0),
                "compareAndSwapInt keeps value at 100");
    }

    @Test
    @DisplayName("compareAndSwapLong updates value on match in unchecked bytes")
    void shouldCompareAndSwapLongOnMatch() {
        uncheckedBytes.writeLong(0, 100L);

        boolean result = uncheckedBytes.compareAndSwapLong(0, 100L, 200L);

        assertTrue(result, "compareAndSwapLong should return true when expected value 100 matches, enabling atomic update");
        assertEquals(200L, uncheckedBytes.readLong(0),
                "compareAndSwapLong updates value to 200");
    }

    // ========== Clear and Compact Operations ==========

    @Test
    @DisplayName("clear resets read and write positions")
    void shouldClearPositions() {
        uncheckedBytes.writeLong(0x123456789ABCDEF0L);
        uncheckedBytes.readPosition(4);

        uncheckedBytes.clear();

        assertEquals(0, uncheckedBytes.readPosition(),
                "readPosition resets to 0 after clear");
        assertEquals(0, uncheckedBytes.writePosition(),
                "writePosition resets to 0 after clear");
    }

    @Test
    @DisplayName("clearAndPad sets positions to padding in unchecked bytes")
    void shouldClearAndPad() {
        uncheckedBytes.clearAndPad(10);

        assertEquals(10, uncheckedBytes.readPosition(),
                "readPosition equals padding 10");
        assertEquals(10, uncheckedBytes.writePosition(),
                "writePosition equals padding 10");
    }

    @Test
    @DisplayName("clearAndPad rejects excessive padding for unchecked bytes")
    void shouldThrowForExcessivePadding() {
        assertThrows(BufferOverflowException.class,
                () -> uncheckedBytes.clearAndPad(1000000),
                "clearAndPad throws BufferOverflowException for excessive padding");
    }

    @Test
    @DisplayName("compact moves remaining bytes to start")
    void shouldCompactData() {
        uncheckedBytes.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        uncheckedBytes.readPosition(8); // Read 8 bytes, leaving 2

        uncheckedBytes.compact();

        assertEquals(0, uncheckedBytes.readPosition(),
                "readPosition resets to 0 after compact");
        assertTrue(uncheckedBytes.writePosition() <= 2,
                "writePosition is at end of remaining data");
    }

    @Test
    @DisplayName("compact may skip when freeing little data")
    void shouldSkipCompactWhenNotBeneficial() {
        uncheckedBytes.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        uncheckedBytes.readPosition(1); // Only 1 byte read, 9 remaining

        long originalReadPos = uncheckedBytes.readPosition();
        uncheckedBytes.compact();

        // Compact may skip if freeing less than 1/4 of remaining
        // Either position stays or moves to 0
        assertTrue(uncheckedBytes.readPosition() <= originalReadPos,
                "readPosition is at or before original");
    }

    // ========== String Operations ==========

    @Test
    @DisplayName("append8bit writes CharSequence as 8-bit data")
    void shouldAppend8bitCharSequence() {
        uncheckedBytes.append8bit("Hello");

        uncheckedBytes.readPosition(0);
        byte[] result = new byte[5];
        for (int i = 0; i < 5; i++) {
            result[i] = uncheckedBytes.readByte();
        }
        assertEquals("Hello", new String(result),
                "append8bit writes 8-bit characters");
    }

    @Test
    @DisplayName("append8bit writes BytesStore content into unchecked bytes")
    void shouldAppend8bitBytesStore() {
        Bytes<?> source = Bytes.from("Test");
        try {
            uncheckedBytes.append8bit(source);

            uncheckedBytes.readPosition(0);
            assertEquals('T', (char) uncheckedBytes.readByte(),
                    "first character equals 'T'");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("write8bit(String) writes length prefixed string")
    void shouldWrite8bitString() {
        uncheckedBytes.write8bit("Test", 0, 4);

        uncheckedBytes.readPosition(0);
        String result = uncheckedBytes.read8bit();
        assertEquals("Test", result, "read8bit returns original string");
    }

    @Test
    @DisplayName("write8bit(BytesStore) handles null value for unchecked bytes")
    void shouldWrite8bitNullBytesStore() {
        uncheckedBytes.write8bit((BytesStore<?, ?>) null);

        uncheckedBytes.readPosition(0);
        assertNull(uncheckedBytes.read8bit(),
                "read8bit returns null when input store is null");
    }

    // ========== Append Number Operations ==========

    @Test
    @DisplayName("append(int) writes decimal characters into unchecked bytes")
    void shouldAppendInt() {
        uncheckedBytes.append(12345);

        uncheckedBytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (uncheckedBytes.readRemaining() > 0) {
            sb.append((char) uncheckedBytes.readByte());
        }
        assertEquals("12345", sb.toString(),
                "appended text equals 12345");
    }

    @Test
    @DisplayName("append(long) writes decimal characters into unchecked bytes")
    void shouldAppendLong() {
        uncheckedBytes.append(123456789012L);

        uncheckedBytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (uncheckedBytes.readRemaining() > 0) {
            sb.append((char) uncheckedBytes.readByte());
        }
        assertEquals("123456789012", sb.toString(),
                "appended text equals 123456789012");
    }

    @Test
    @DisplayName("append(long) handles Long.MIN_VALUE value")
    void shouldAppendLongMinValue() {
        uncheckedBytes.append(Long.MIN_VALUE);

        uncheckedBytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (uncheckedBytes.readRemaining() > 0) {
            sb.append((char) uncheckedBytes.readByte());
        }
        assertEquals(String.valueOf(Long.MIN_VALUE), sb.toString(),
                "appended text equals Long.MIN_VALUE");
    }

    @Test
    @DisplayName("append(double) writes decimal characters into unchecked bytes")
    void shouldAppendDouble() {
        uncheckedBytes.append(3.14);

        uncheckedBytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (uncheckedBytes.readRemaining() > 0) {
            sb.append((char) uncheckedBytes.readByte());
        }
        assertTrue(sb.toString().startsWith("3.14"),
                "appended text starts with 3.14");
    }

    @Test
    @DisplayName("append(float) writes decimal characters into unchecked bytes")
    void shouldAppendFloat() {
        uncheckedBytes.append(2.5f);

        uncheckedBytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (uncheckedBytes.readRemaining() > 0) {
            sb.append((char) uncheckedBytes.readByte());
        }
        assertTrue(sb.toString().startsWith("2.5"),
                "appended text starts with 2.5");
    }

    // ========== Capacity and Address Operations ==========

    @Test
    @DisplayName("capacity returns positive buffer capacity in unchecked bytes")
    void shouldReturnCapacity() {
        assertTrue(uncheckedBytes.capacity() > 0,
                "capacity returns positive value");
    }

    @Test
    @DisplayName("realCapacity returns positive allocation size in unchecked bytes")
    void shouldReturnRealCapacity() {
        assertTrue(uncheckedBytes.realCapacity() > 0,
                "realCapacity returns positive value");
    }

    @Test
    @DisplayName("realWriteRemaining matches writeRemaining in unchecked bytes")
    void shouldReturnRealWriteRemaining() {
        long remaining = uncheckedBytes.realWriteRemaining();
        assertEquals(uncheckedBytes.writeRemaining(), remaining,
                "realWriteRemaining value " + remaining + " equals writeRemaining");
    }

    @Test
    @DisplayName("addressForRead returns non-zero address in unchecked bytes")
    void shouldReturnAddressForRead() {
        long address = uncheckedBytes.addressForRead(0);
        assertTrue(address != 0, "addressForRead returns address " + address + " != 0");
    }

    @Test
    @DisplayName("addressForWrite returns non-zero address in unchecked bytes")
    void shouldReturnAddressForWrite() {
        long address = uncheckedBytes.addressForWrite(0);
        assertTrue(address != 0, "addressForWrite returns address " + address + " != 0");
    }

    @Test
    @DisplayName("addressForWritePosition returns non-zero address in unchecked bytes")
    void shouldReturnAddressForWritePosition() {
        long address = uncheckedBytes.addressForWritePosition();
        assertTrue(address != 0, "addressForWritePosition returns address " + address + " != 0");
    }

    // ========== Decimaliser Operations ==========

    @Test
    @DisplayName("decimaliser returns default implementation for unchecked bytes")
    void shouldGetAndSetDecimaliser() {
        assertNotNull(uncheckedBytes.decimaliser(),
                "default decimaliser implementation instance is present");
    }

    @Test
    @DisplayName("lastDecimalPlaces returns set decimal count for unchecked bytes")
    void shouldGetAndSetLastDecimalPlaces() {
        uncheckedBytes.lastDecimalPlaces(5);
        assertEquals(5, uncheckedBytes.lastDecimalPlaces(),
                "lastDecimalPlaces returns 5 after set");
    }

    @Test
    @DisplayName("lastDecimalPlaces clamps negative input for unchecked bytes")
    void shouldClampNegativeDecimalPlaces() {
        uncheckedBytes.lastDecimalPlaces(-5);
        assertEquals(0, uncheckedBytes.lastDecimalPlaces(),
                "lastDecimalPlaces returns 0 after negative input");
    }

    @Test
    @DisplayName("lastNumberHadDigits returns set digit flag for unchecked bytes")
    void shouldGetAndSetLastNumberHadDigits() {
        uncheckedBytes.lastNumberHadDigits(true);
        assertTrue(uncheckedBytes.lastNumberHadDigits(),
                "lastNumberHadDigits flag is true after set");
    }

    // ========== Miscellaneous Operations ==========

    @Test
    @DisplayName("move copies data within buffer in unchecked bytes")
    void shouldMoveData() {
        uncheckedBytes.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        uncheckedBytes.move(0, 10, 5);

        assertEquals(1, uncheckedBytes.readByte(10),
                "move copies value 1 to offset 10");
    }

    @Test
    @DisplayName("ensureCapacity expands allocation when required in unchecked bytes")
    void shouldEnsureCapacity() {
        long originalCapacity = uncheckedBytes.realCapacity();
        uncheckedBytes.ensureCapacity(originalCapacity + 100);

        assertTrue(uncheckedBytes.realCapacity() >= originalCapacity + 100,
                "realCapacity expands beyond original");
    }

    @Test
    @DisplayName("bytesStore returns underlying store for unchecked bytes")
    void shouldReturnBytesStore() {
        assertNotNull(uncheckedBytes.bytesStore(),
                "bytesStore returns non-null store");
    }

    @Test
    @DisplayName("underlyingObject returns underlying object for unchecked bytes")
    void shouldReturnUnderlyingObject() {
        // For direct memory, this may be null
        // Just ensure it doesn't throw
        uncheckedBytes.underlyingObject();
    }

    @Test
    @DisplayName("toString returns string representation for unchecked bytes")
    void shouldReturnStringRepresentation() {
        uncheckedBytes.append8bit("Test");
        String str = uncheckedBytes.toString();
        assertNotNull(str, "toString returns non-null string representation");
    }

    @Test
    @DisplayName("hashCode remains consistent across calls for unchecked bytes")
    void shouldReturnConsistentHashCode() {
        uncheckedBytes.writeLong(0x123456789ABCDEF0L);

        int hash1 = uncheckedBytes.hashCode();
        int hash2 = uncheckedBytes.hashCode();

        assertEquals(hash1, hash2, "hashCode returns same value across calls");
    }

    @Test
    @DisplayName("equals compares byte content for unchecked bytes")
    void shouldCompareContent() {
        uncheckedBytes.writeLong(0x123456789ABCDEF0L);

        Bytes<?> other = Bytes.allocateElasticDirect(32);
        UncheckedNativeBytes<?> otherUnchecked = new UncheckedNativeBytes<>(other);
        try {
            otherUnchecked.writeLong(0x123456789ABCDEF0L);

            assertEquals(uncheckedBytes, otherUnchecked,
                    "equals returns true for equal content");
        } finally {
            otherUnchecked.releaseLast();
            other.releaseLast();
        }
    }

    @Test
    @DisplayName("acquireUncheckedInput returns unchecked input view for unchecked bytes")
    void shouldAcquireUncheckedInput() {
        uncheckedBytes.writeLong(0x123456789ABCDEF0L);

        UncheckedRandomDataInput input = uncheckedBytes.acquireUncheckedInput();

        assertNotNull(input, "acquireUncheckedInput returns non-null view");
        assertEquals((byte) 0xF0, input.readByte(0),
                "unchecked input readByte returns 0xF0");
    }

    @Test
    @DisplayName("uncheckedReadSkipOne advances read position for unchecked bytes")
    void shouldUncheckedReadSkipOne() {
        uncheckedBytes.write(new byte[]{1, 2, 3});
        uncheckedBytes.readPosition(0);

        uncheckedBytes.uncheckedReadSkipOne();

        assertEquals(1, uncheckedBytes.readPosition(),
                "readPosition advances to 1 after uncheckedReadSkipOne");
    }

    @Test
    @DisplayName("uncheckedReadSkipBackOne moves read position back for unchecked bytes")
    void shouldUncheckedReadSkipBackOne() {
        uncheckedBytes.write(new byte[]{1, 2, 3});
        uncheckedBytes.readPosition(2);

        uncheckedBytes.uncheckedReadSkipBackOne();

        assertEquals(1, uncheckedBytes.readPosition(),
                "readPosition moves back to 1 after uncheckedReadSkipBackOne");
    }
}
