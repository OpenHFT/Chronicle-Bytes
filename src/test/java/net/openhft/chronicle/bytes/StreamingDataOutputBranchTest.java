/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch coverage for StreamingDataOutput write and append operations,
 * including null handling, range writes, numeric encodings, and round-trip checks.
 */
@DisplayName("StreamingDataOutput branch coverage for write and append methods")
class StreamingDataOutputBranchTest extends BytesTestCommon {

    private Bytes<?> bytes;

    @BeforeEach
    void setUp() {
        bytes = Bytes.allocateElasticOnHeap(256);
    }

    @AfterEach
    void tearDown() {
        if (bytes != null) {
            bytes.releaseLast();
        }
    }

    // ========== writeBoolean Tests ==========

    @Test
    @DisplayName("writeBoolean writes 'Y' marker for true input value")
    void shouldWriteYForTrue() {
        bytes.writeBoolean(true);

        bytes.readPosition(0);
        assertEquals('Y', bytes.readByte(), "writeBoolean(true) writes 'Y' marker");
    }

    @Test
    @DisplayName("writeBoolean writes 'N' marker for false input value")
    void shouldWriteNForFalse() {
        bytes.writeBoolean(false);

        bytes.readPosition(0);
        assertEquals('N', bytes.readByte(), "writeBoolean(false) writes 'N' marker");
    }

    // ========== writeStopBit Tests ==========

    @ParameterizedTest(name = "writeStopBit long round-trip value {0}")
    @ValueSource(longs = {0, 1, 127, 128, 16383, 16384, Long.MAX_VALUE, -1, Long.MIN_VALUE})
    @DisplayName("writeStopBit round-trips signed long values with stop-bit encoding")
    void shouldWriteStopBitLong(long value) {
        bytes.writeStopBit(value);

        bytes.readPosition(0);
        assertEquals(value, bytes.readStopBit(),
                "stop bit round-trip returns value " + value);
    }

    @ParameterizedTest(name = "writeStopBit char round-trip value {0}")
    @ValueSource(chars = {0, 1, 127, 128, 255, 16383, 16384, Character.MAX_VALUE})
    @DisplayName("writeStopBit round-trips char values with stop-bit encoding")
    void shouldWriteStopBitChar(char value) {
        bytes.writeStopBit(value);

        bytes.readPosition(0);
        assertEquals(value, bytes.readStopBitChar(),
                "stop bit round-trip returns char " + (int) value);
    }

    @ParameterizedTest(name = "writeStopBit double round-trip value {0}")
    @ValueSource(doubles = {0.0, 1.0, -1.0, 123.456, -123.456, Double.MAX_VALUE, Double.MIN_VALUE})
    @DisplayName("writeStopBit round-trips double values with stop-bit encoding")
    void shouldWriteStopBitDouble(double value) {
        bytes.writeStopBit(value);

        bytes.readPosition(0);
        assertEquals(value, bytes.readStopBitDouble(), 0.0001,
                "stop bit round-trip returns double " + value);
    }

    // ========== writeStopBitDecimal Tests ==========

    @Test
    @DisplayName("writeStopBitDecimal handles whole number values")
    void shouldWriteStopBitDecimalWholeNumbers() {
        bytes.writeStopBitDecimal(123.0);

        bytes.readPosition(0);
        assertEquals(123.0, bytes.readStopBitDecimal(), 0.0001,
                "stop bit decimal round-trip returns 123.0");
    }

    @Test
    @DisplayName("writeStopBitDecimal handles negative decimal values")
    void shouldWriteStopBitDecimalNegative() {
        bytes.writeStopBitDecimal(-45.67);

        bytes.readPosition(0);
        assertEquals(-45.67, bytes.readStopBitDecimal(), 0.0001,
                "stop bit decimal round-trip returns -45.67");
    }

    @Test
    @DisplayName("writeStopBitDecimal handles values with trailing zeros")
    void shouldWriteStopBitDecimalTrailingZeros() {
        bytes.writeStopBitDecimal(12.50);

        bytes.readPosition(0);
        assertEquals(12.5, bytes.readStopBitDecimal(), 0.0001,
                "stop bit decimal normalises trailing zeros");
    }

    @Test
    @DisplayName("writeStopBitDecimal handles very small decimal values")
    void shouldWriteStopBitDecimalSmallValues() {
        bytes.writeStopBitDecimal(0.000001);

        bytes.readPosition(0);
        double result = bytes.readStopBitDecimal();
        assertEquals(0.000001, result, 0.0000001,
                "stop bit decimal round-trip returns 0.000001");
    }

    // ========== writeUtf8 Tests ==========

    @Test
    @DisplayName("writeUtf8 returns null for null string input")
    void shouldWriteUtf8Null() {
        bytes.writeUtf8((String) null);

        bytes.readPosition(0);
        assertNull(bytes.readUtf8(), "writeUtf8 returns null for null input");
    }

    @Test
    @DisplayName("writeUtf8 returns empty string for empty input")
    void shouldWriteUtf8Empty() {
        bytes.writeUtf8("");

        bytes.readPosition(0);
        assertEquals("", bytes.readUtf8(), "writeUtf8 returns empty string for empty input");
    }

    @Test
    @DisplayName("writeUtf8 returns ASCII string for round-trip input")
    void shouldWriteUtf8Ascii() {
        bytes.writeUtf8("Hello, World!");

        bytes.readPosition(0);
        assertEquals("Hello, World!", bytes.readUtf8(),
                "writeUtf8 returns ASCII string value");
    }

    @Test
    @DisplayName("writeUtf8 returns Latin-1 string for accented input")
    void shouldWriteUtf8Unicode() {
        String unicode = "Hello \u00e9\u00e8\u00ea";
        bytes.writeUtf8(unicode);

        bytes.readPosition(0);
        assertEquals(unicode, bytes.readUtf8(),
                "writeUtf8 returns Unicode string value");
    }

    @Test
    @DisplayName("writeUtf8 returns CharSequence content for round-trip input")
    void shouldWriteUtf8CharSequence() {
        CharSequence cs = new StringBuilder("Test CharSequence");
        bytes.writeUtf8(cs);

        bytes.readPosition(0);
        assertEquals("Test CharSequence", bytes.readUtf8(),
                "writeUtf8 returns CharSequence string value");
    }

    // ========== write8bit Tests ==========

    @Test
    @DisplayName("write8bit returns null for null CharSequence input")
    void shouldWrite8bitNull() {
        bytes.write8bit((CharSequence) null);

        bytes.readPosition(0);
        assertNull(bytes.read8bit(), "write8bit returns null for null input");
    }

    @Test
    @DisplayName("write8bit returns String for round-trip 8-bit input")
    void shouldWrite8bitString() {
        bytes.write8bit("Hello");

        bytes.readPosition(0);
        assertEquals("Hello", bytes.read8bit(),
                "write8bit returns String value via 8-bit encoding");
    }

    @Test
    @DisplayName("write8bit returns BytesStore content for round-trip 8-bit input")
    void shouldWrite8bitBytesStore() {
        Bytes<?> source = Bytes.from("Source data");
        try {
            bytes.write8bit(source);

            bytes.readPosition(0);
            assertEquals("Source data", bytes.read8bit(),
                    "write8bit returns BytesStore data via 8-bit encoding");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("write8bit returns CharSequence range for start and length")
    void shouldWrite8bitCharSequenceRange() {
        bytes.write8bit("Hello, World!", 0, 5);

        bytes.readPosition(0);
        assertEquals("Hello", bytes.read8bit(),
                "write8bit returns expected substring value");
    }

    // ========== write(CharSequence) Tests ==========

    @Test
    @DisplayName("write(CharSequence) handles BytesStore input value")
    void shouldWriteCharSequenceBytesStore() {
        Bytes<?> source = Bytes.from("BytesStore content");
        try {
            bytes.write((CharSequence) source);

            bytes.readPosition(0);
            byte[] result = new byte[(int) bytes.readRemaining()];
            bytes.read(result);
            assertEquals("BytesStore content", new String(result),
                    "write(CharSequence) copies BytesStore content");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("write(CharSequence, start, length) writes substring range")
    void shouldWriteCharSequenceRange() {
        bytes.write("Hello, World!", 7, 5);

        bytes.readPosition(0);
        byte[] result = new byte[5];
        bytes.read(result);
        assertEquals("World", new String(result),
                "write(CharSequence) range copies substring value");
    }

    // ========== Primitive Write Tests ==========

    @Test
    @DisplayName("writeUnsignedByte round-trips max unsigned byte value")
    void shouldWriteUnsignedByte() {
        bytes.writeUnsignedByte(255);

        bytes.readPosition(0);
        assertEquals(255, bytes.readUnsignedByte(),
                "writeUnsignedByte round-trips value 255");
    }

    @Test
    @DisplayName("writeUnsignedByte rejects value above 255")
    void shouldThrowForInvalidUnsignedByte() {
        assertThrows(ArithmeticException.class, () -> bytes.writeUnsignedByte(256),
                "writeUnsignedByte throws for value 256");
    }

    @Test
    @DisplayName("writeChar stores char using stop-bit encoding")
    void shouldWriteChar() {
        bytes.writeChar('A');

        bytes.readPosition(0);
        assertEquals('A', bytes.readStopBitChar(),
                "writeChar stores stop bit value 'A'");
    }

    @Test
    @DisplayName("writeUnsignedShort round-trips max unsigned short value")
    void shouldWriteUnsignedShort() {
        bytes.writeUnsignedShort(65535);

        bytes.readPosition(0);
        assertEquals(65535, bytes.readUnsignedShort(),
                "writeUnsignedShort round-trips value 65535");
    }

    @Test
    @DisplayName("writeInt24 round-trips minimum signed 24-bit value")
    void shouldWriteInt24() {
        bytes.writeInt24(-8388608); // Min 24-bit signed

        bytes.readPosition(0);
        assertEquals(-8388608, bytes.readInt24(),
                "writeInt24 round-trips value -8388608");
    }

    @Test
    @DisplayName("writeUnsignedInt24 round-trips maximum unsigned 24-bit value")
    void shouldWriteUnsignedInt24() {
        bytes.writeUnsignedInt24(16777215); // Max 24-bit unsigned

        bytes.readPosition(0);
        assertEquals(16777215, bytes.readUnsignedInt24(),
                "writeUnsignedInt24 round-trips value 16777215");
    }

    @Test
    @DisplayName("writeUnsignedInt round-trips max unsigned int value")
    void shouldWriteUnsignedInt() {
        bytes.writeUnsignedInt(4294967295L);

        bytes.readPosition(0);
        assertEquals(4294967295L, bytes.readUnsignedInt(),
                "writeUnsignedInt round-trips value 4294967295");
    }

    // ========== write(BytesStore) Tests ==========

    @Test
    @DisplayName("write(BytesStore) copies all readable data")
    void shouldWriteBytesStore() {
        Bytes<?> source = Bytes.allocateElasticOnHeap(64);
        try {
            source.writeUtf8("Test data");
            source.readPosition(0);

            bytes.write(source);

            bytes.readPosition(0);
            assertEquals("Test data", bytes.readUtf8(),
                    "write(BytesStore) copies UTF-8 content");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("write(BytesStore, offset, length) copies data range")
    void shouldWriteBytesStoreRange() {
        Bytes<?> source = Bytes.allocateElasticOnHeap(64);
        try {
            source.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

            bytes.write((BytesStore<?, ?>) source, 2L, 5L);

            bytes.readPosition(0);
            assertEquals(5, bytes.readRemaining(), "write(BytesStore) writes 5 bytes from offset 2");
            assertEquals(3, bytes.readByte(), "BytesStore range first byte equals 3 at offset 2");
        } finally {
            source.releaseLast();
        }
    }

    // ========== write(byte[]) Tests ==========

    @Test
    @DisplayName("write(byte[]) writes entire array contents")
    void shouldWriteByteArray() {
        byte[] data = {1, 2, 3, 4, 5};
        bytes.write(data);

        bytes.readPosition(0);
        assertEquals(5, bytes.readRemaining(), "write(byte[]) writes 5 bytes");
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], bytes.readByte(),
                    "byte at index " + i + " matches value " + data[i]);
        }
    }

    @Test
    @DisplayName("write(byte[], offset, length) writes array range")
    void shouldWriteByteArrayRange() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        bytes.write(data, 2, 4);

        bytes.readPosition(0);
        assertEquals(4, bytes.readRemaining(), "write(byte[]) range writes 4 bytes");
        assertEquals(3, bytes.readByte(), "byte array range first byte equals 3 at offset 2");
    }

    // ========== writeSome(ByteBuffer) Tests ==========

    @Test
    @DisplayName("writeSome copies remaining bytes from heap ByteBuffer")
    void shouldWriteSomeFromByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(new byte[]{10, 20, 30, 40, 50});
        buffer.flip();

        bytes.writeSome(buffer);

        bytes.readPosition(0);
        assertEquals(5, bytes.readRemaining(), "writeSome writes 5 bytes from buffer");
        assertEquals(10, bytes.readByte(), "first buffer byte equals 10");
    }

    @Test
    @DisplayName("writeSome copies remaining bytes from direct ByteBuffer")
    void shouldWriteSomeFromDirectByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(16);
        buffer.put(new byte[]{11, 22, 33});
        buffer.flip();

        bytes.writeSome(buffer);

        bytes.readPosition(0);
        assertEquals(3, bytes.readRemaining(), "writeSome writes 3 bytes from direct buffer");
    }

    // ========== writePositionForHeader Tests ==========

    @Test
    @DisplayName("writePositionForHeader without padding returns current position")
    void shouldReturnPositionWithoutPadding() {
        bytes.writeLong(0x1234567890ABCDEFL);

        long pos = bytes.writePositionForHeader(false);
        assertEquals(8, pos, "writePositionForHeader returns 8 without padding");
    }

    @Test
    @DisplayName("writePositionForHeader with padding aligns to 4 bytes")
    void shouldAlignPositionWithPadding() {
        bytes.writeByte((byte) 1); // Position is now 1

        long pos = bytes.writePositionForHeader(true);
        assertEquals(0, pos % 4, "writePositionForHeader aligns position to 4 bytes");
    }

    // ========== writeEnum Tests ==========

    @Test
    @DisplayName("writeEnum writes enum name in 8-bit format")
    void shouldWriteEnum() {
        bytes.writeEnum(Thread.State.RUNNABLE);

        bytes.readPosition(0);
        assertEquals("RUNNABLE", bytes.read8bit(),
                "writeEnum stores enum name as 8-bit string");
    }

    // ========== appendUtf8 Tests ==========

    @Test
    @DisplayName("appendUtf8 writes ASCII CharSequence with UTF-8 encoding")
    void shouldAppendUtf8Ascii() {
        bytes.appendUtf8("ASCII text");

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        bytes.parseUtf8(sb, (int) bytes.readRemaining());
        assertEquals("ASCII text", sb.toString(),
                "appendUtf8 writes ASCII text value");
    }

    @Test
    @DisplayName("appendUtf8 writes UTF-8 bytes for code point 0x1F600")
    void shouldAppendUtf8Codepoint() {
        bytes.appendUtf8(0x1F600); // Emoji: grinning face

        bytes.readPosition(0);
        assertTrue(bytes.readRemaining() >= 4,
                "appendUtf8 writes 4-byte UTF-8 sequence");
    }

    @Test
    @DisplayName("appendUtf8 writes Latin-1 char array with accented byte")
    void shouldAppendUtf8CharArrayNonAscii() {
        char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', '\u00e9'};
        bytes.appendUtf8(chars, 0, chars.length);

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        bytes.parseUtf8(sb, (int) bytes.readRemaining());
        assertEquals("Hello \u00e9", sb.toString(),
                "appendUtf8 writes non-ASCII char array value");
    }

    @Test
    @DisplayName("appendUtf8 writes CharSequence range slice")
    void shouldAppendUtf8CharSequenceRange() {
        bytes.appendUtf8("Hello, World!", 7, 5);

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        bytes.parseUtf8(sb, (int) bytes.readRemaining());
        assertEquals("World", sb.toString(),
                "appendUtf8 range writes substring value");
    }

    // ========== writePositionRemaining Tests ==========

    @Test
    @DisplayName("writePositionRemaining sets position and limit values")
    void shouldSetPositionAndRemaining() {
        bytes.writePositionRemaining(10, 20);

        assertEquals(10, bytes.writePosition(), "writePositionRemaining sets position to 10");
        assertEquals(30, bytes.writeLimit(), "writePositionRemaining sets limit to 30");
    }

    // ========== Float and Double Special Values ==========

    @ParameterizedTest(name = "writeFloat round-trip value {0}")
    @ValueSource(floats = {0.0f, -0.0f, 1.0f, -1.0f, Float.MAX_VALUE, Float.MIN_VALUE,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    @DisplayName("writeFloat round-trips special float values")
    void shouldWriteSpecialFloats(float value) {
        bytes.writeFloat(value);

        bytes.readPosition(0);
        assertEquals(value, bytes.readFloat(), 0.0f,
                "writeFloat round-trip returns value " + value);
    }

    @Test
    @DisplayName("writeFloat handles NaN value input")
    void shouldWriteFloatNaN() {
        bytes.writeFloat(Float.NaN);

        bytes.readPosition(0);
        assertTrue(Float.isNaN(bytes.readFloat()), "writeFloat preserves NaN value");
    }

    @ParameterizedTest(name = "writeDouble round-trip value {0}")
    @ValueSource(doubles = {0.0, -0.0, 1.0, -1.0, Double.MAX_VALUE, Double.MIN_VALUE,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("writeDouble round-trips special double values")
    void shouldWriteSpecialDoubles(double value) {
        bytes.writeDouble(value);

        bytes.readPosition(0);
        assertEquals(value, bytes.readDouble(), 0.0,
                "writeDouble round-trip returns value " + value);
    }

    @Test
    @DisplayName("writeDouble handles NaN value input")
    void shouldWriteDoubleNaN() {
        bytes.writeDouble(Double.NaN);

        bytes.readPosition(0);
        assertTrue(Double.isNaN(bytes.readDouble()), "writeDouble preserves NaN value");
    }

    // ========== rawWrite Tests ==========

    @Test
    @DisplayName("rawWriteByte writes same value as writeByte")
    void shouldRawWriteByte() {
        bytes.rawWriteByte((byte) 42);

        bytes.readPosition(0);
        assertEquals(42, bytes.readByte(), "rawWriteByte stores value 42");
    }

    @Test
    @DisplayName("rawWriteInt writes same value as writeInt")
    void shouldRawWriteInt() {
        bytes.rawWriteInt(0x12345678);

        bytes.readPosition(0);
        assertEquals(0x12345678, bytes.readInt(), "rawWriteInt stores value 0x12345678");
    }

    @Test
    @DisplayName("rawWriteLong writes same value as writeLong")
    void shouldRawWriteLong() {
        bytes.rawWriteLong(0x123456789ABCDEF0L);

        bytes.readPosition(0);
        assertEquals(0x123456789ABCDEF0L, bytes.readLong(),
                "rawWriteLong stores value 0x123456789ABCDEF0");
    }

    // ========== writeDoubleAndInt Tests ==========

    @Test
    @DisplayName("writeDoubleAndInt writes both double and int values")
    void shouldWriteDoubleAndInt() {
        bytes.writeDoubleAndInt(3.14159, 42);

        bytes.readPosition(0);
        assertEquals(3.14159, bytes.readDouble(), 0.00001, "writeDoubleAndInt stores double 3.14159");
        assertEquals(42, bytes.readInt(), "writeDoubleAndInt stores int 42");
    }

    // ========== canWriteDirect Tests ==========

    @Test
    @DisplayName("canWriteDirect returns false for heap bytes default path")
    void shouldReturnFalseForCanWriteDirect() {
        assertFalse(bytes.canWriteDirect(10),
                "canWriteDirect returns false for heap bytes default path");
    }
}
