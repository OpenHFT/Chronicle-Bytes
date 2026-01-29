/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BytesInternal stop-bit encoding and decoding methods because
 * variable-length encoding must handle boundary values to avoid data corruption.
 * Stop-bit encoding uses the high bit of each octet to indicate continuation.
 * These tests verify round-trip correctness in order to ensure encoding integrity.
 * Tests are required by the RFC specification to validate boundary transitions.
 */
@SuppressWarnings({"checkstyle:MMOverusedWord", "checkstyle:MMLacksPurpose"})
@DisplayName("BytesInternal stop-bit variable-length encoding tests")
class BytesInternalStopBitTest extends BytesTestCommon {

    // ========== writeStopBit Long Tests ==========

    @Test
    @DisplayName("writeStopBit encodes single-byte values (0-127)")
    void shouldEncodeStopBitSingleByte() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, 0);
            BytesInternal.writeStopBit(bytes, 1);
            BytesInternal.writeStopBit(bytes, 63);
            BytesInternal.writeStopBit(bytes, 127);

            bytes.readPosition(0);
            assertEquals(0, BytesInternal.readStopBit(bytes), "0 should decode correctly");
            assertEquals(1, BytesInternal.readStopBit(bytes), "1 should decode correctly");
            assertEquals(63, BytesInternal.readStopBit(bytes), "63 should decode correctly");
            assertEquals(127, BytesInternal.readStopBit(bytes), "127 should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeStopBit encodes two-byte values (128-16383)")
    void shouldEncodeStopBitTwoBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, 128);
            BytesInternal.writeStopBit(bytes, 255);
            BytesInternal.writeStopBit(bytes, 1000);
            BytesInternal.writeStopBit(bytes, 16383);

            bytes.readPosition(0);
            assertEquals(128, BytesInternal.readStopBit(bytes), "128 should decode correctly");
            assertEquals(255, BytesInternal.readStopBit(bytes), "255 should decode correctly");
            assertEquals(1000, BytesInternal.readStopBit(bytes), "1000 should decode correctly");
            assertEquals(16383, BytesInternal.readStopBit(bytes), "16383 should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeStopBit encodes multi-byte values (>16383)")
    void shouldEncodeStopBitMultiBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, 16384);
            BytesInternal.writeStopBit(bytes, 100000);
            BytesInternal.writeStopBit(bytes, Integer.MAX_VALUE);
            BytesInternal.writeStopBit(bytes, Long.MAX_VALUE);

            bytes.readPosition(0);
            assertEquals(16384, BytesInternal.readStopBit(bytes), "16384 should decode correctly");
            assertEquals(100000, BytesInternal.readStopBit(bytes), "100000 should decode correctly");
            assertEquals(Integer.MAX_VALUE, BytesInternal.readStopBit(bytes), "MAX_INT should decode correctly");
            assertEquals(Long.MAX_VALUE, BytesInternal.readStopBit(bytes), "MAX_LONG should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeStopBit encodes negative values with sign extension because negatives require special handling")
    void shouldEncodeStopBitNegativeValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, -1);
            BytesInternal.writeStopBit(bytes, -2);
            BytesInternal.writeStopBit(bytes, -128);
            BytesInternal.writeStopBit(bytes, Long.MIN_VALUE);

            bytes.readPosition(0);
            assertEquals(-1, BytesInternal.readStopBit(bytes), "-1 should decode correctly");
            assertEquals(-2, BytesInternal.readStopBit(bytes), "-2 should decode correctly");
            assertEquals(-128, BytesInternal.readStopBit(bytes), "-128 should decode correctly");
            assertEquals(Long.MIN_VALUE, BytesInternal.readStopBit(bytes), "MIN_LONG should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeStopBitNeg1 writes special -1 encoding for efficient sentinel handling")
    void shouldWriteStopBitNeg1() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBitNeg1(bytes);

            bytes.readPosition(0);
            assertEquals(-1, BytesInternal.readStopBit(bytes), "writeStopBitNeg1 encodes -1 sentinel correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== writeStopBit Char Tests ==========

    @Test
    @DisplayName("writeStopBit(char) encodes single-byte character values")
    void shouldEncodeStopBitCharSingleByte() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, 'A');  // 65
            BytesInternal.writeStopBit(bytes, '\0'); // 0
            BytesInternal.writeStopBit(bytes, '\u007F'); // 127

            bytes.readPosition(0);
            assertEquals('A', BytesInternal.readStopBitChar(bytes), "'A' should decode correctly");
            assertEquals('\0', BytesInternal.readStopBitChar(bytes), "NUL should decode correctly");
            assertEquals('\u007F', BytesInternal.readStopBitChar(bytes), "DEL should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeStopBit(char) encodes two-byte character values")
    void shouldEncodeStopBitCharTwoBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, '\u0080'); // 128
            BytesInternal.writeStopBit(bytes, '\u00FF'); // 255
            BytesInternal.writeStopBit(bytes, '\u3FFF'); // 16383

            bytes.readPosition(0);
            assertEquals('\u0080', BytesInternal.readStopBitChar(bytes), "0x80 char should decode correctly");
            assertEquals('\u00FF', BytesInternal.readStopBitChar(bytes), "0xFF char should decode correctly");
            assertEquals('\u3FFF', BytesInternal.readStopBitChar(bytes), "0x3FFF char should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeStopBit(char) encodes multi-byte character values")
    void shouldEncodeStopBitCharMultiBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, '\u4000'); // 16384
            BytesInternal.writeStopBit(bytes, '\uFFFF'); // 65535

            bytes.readPosition(0);
            assertEquals('\u4000', BytesInternal.readStopBitChar(bytes), "0x4000 char should decode correctly");
            assertEquals('\uFFFF', BytesInternal.readStopBitChar(bytes), "0xFFFF char should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== writeStopBit Double Tests ==========

    @Test
    @DisplayName("writeStopBit(double) encodes special double values")
    void shouldEncodeStopBitDouble() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, 0.0);
            BytesInternal.writeStopBit(bytes, 1.0);
            BytesInternal.writeStopBit(bytes, -1.0);
            BytesInternal.writeStopBit(bytes, 123.456);

            bytes.readPosition(0);
            assertEquals(0.0, BytesInternal.readStopBitDouble(bytes), 0.0001, "0.0 should decode correctly");
            assertEquals(1.0, BytesInternal.readStopBitDouble(bytes), 0.0001, "1.0 should decode correctly");
            assertEquals(-1.0, BytesInternal.readStopBitDouble(bytes), 0.0001, "-1.0 should decode correctly");
            assertEquals(123.456, BytesInternal.readStopBitDouble(bytes), 0.0001, "123.456 should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== Stop-Bit Boundary Tests ==========

    @ParameterizedTest(name = "boundary value {0} encodes correctly at byte width transitions")
    @ValueSource(longs = {127, 128, 16383, 16384, 2097151, 2097152, 268435455, 268435456})
    @DisplayName("stop-bit boundary values encode and decode correctly at byte width transitions")
    void shouldHandleStopBitBoundaries(long value) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.writeStopBit(bytes, value - 1);
            BytesInternal.writeStopBit(bytes, value);
            BytesInternal.writeStopBit(bytes, value + 1);

            bytes.readPosition(0);
            assertEquals(value - 1, BytesInternal.readStopBit(bytes), "value-1 should decode correctly");
            assertEquals(value, BytesInternal.readStopBit(bytes), "value should decode correctly");
            assertEquals(value + 1, BytesInternal.readStopBit(bytes), "value+1 should decode correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== stopBitLength Tests ==========

    @Test
    @DisplayName("stopBitLength0 calculates correct lengths for various values")
    void shouldCalculateStopBitLength() {
        // Single-byte values (0-127)
        assertEquals(1, BytesInternal.stopBitLength0(0), "0 needs 1 byte");
        assertEquals(1, BytesInternal.stopBitLength0(1), "1 needs 1 byte");
        assertEquals(1, BytesInternal.stopBitLength0(127), "127 needs 1 byte");

        // Two-byte values (128-16383)
        assertEquals(2, BytesInternal.stopBitLength0(128), "128 needs 2 bytes");
        assertEquals(2, BytesInternal.stopBitLength0(16383), "16383 needs 2 bytes");

        // Three-byte values (16384-2097151)
        assertEquals(3, BytesInternal.stopBitLength0(16384), "16384 needs 3 bytes");
        assertEquals(3, BytesInternal.stopBitLength0(2097151), "2097151 needs 3 bytes");

        // Negative values
        assertEquals(2, BytesInternal.stopBitLength0(-1), "-1 needs 2 bytes");
        assertEquals(2, BytesInternal.stopBitLength0(-128), "-128 needs 2 bytes");
    }

    // ========== Round-trip Tests ==========

    @Test
    @DisplayName("all positive long values round-trip correctly")
    void shouldRoundTripPositiveLongs() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            long[] testValues = {
                    0, 1, 63, 64, 126, 127, 128, 255, 256, 1000,
                    16382, 16383, 16384, 16385,
                    Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
                    Long.MAX_VALUE / 2, Long.MAX_VALUE - 1, Long.MAX_VALUE
            };

            for (long value : testValues) {
                bytes.clear();
                BytesInternal.writeStopBit(bytes, value);
                bytes.readPosition(0);
                assertEquals(value, BytesInternal.readStopBit(bytes),
                        "Positive value " + value + " encodes and decodes correctly");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("all negative long values round-trip correctly")
    void shouldRoundTripNegativeLongs() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            long[] testValues = {
                    -1, -2, -63, -64, -127, -128, -129, -255, -256,
                    -16383, -16384, -16385,
                    Integer.MIN_VALUE + 1, Integer.MIN_VALUE,
                    Long.MIN_VALUE / 2, Long.MIN_VALUE + 1, Long.MIN_VALUE
            };

            for (long value : testValues) {
                bytes.clear();
                BytesInternal.writeStopBit(bytes, value);
                bytes.readPosition(0);
                assertEquals(value, BytesInternal.readStopBit(bytes),
                        "Negative value " + value + " encodes and decodes correctly");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("all char values round-trip correctly")
    void shouldRoundTripChars() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            char[] testValues = {
                    '\0', 'A', 'Z', '\u007F', '\u0080', '\u00FF',
                    '\u0100', '\u3FFF', '\u4000', '\uFFFF'
            };

            for (char value : testValues) {
                bytes.clear();
                BytesInternal.writeStopBit(bytes, value);
                bytes.readPosition(0);
                assertEquals(value, BytesInternal.readStopBitChar(bytes),
                        "Char " + (int) value + " should round-trip correctly");
            }
        } finally {
            bytes.releaseLast();
        }
    }
}
