/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BytesInternal number appending methods covering various input formats
 * and edge cases.
 */
@DisplayName("BytesInternal number appending behaviour coverage tests")
class BytesInternalAppendTest extends BytesTestCommon {

    // ========== appendBase10 Long Tests ==========

    @Test
    @DisplayName("appendBase10 appends zero value in base 10")
    void shouldAppendZero() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, 0L);
            assertEquals("0", bytes.toString(), "appendBase10 renders zero value as '0'");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendBase10 appends positive single digit value")
    void shouldAppendPositiveSingleDigit() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, 5L);
            assertEquals("5", bytes.toString(), "appendBase10 renders single digit value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendBase10 appends positive two digit value")
    void shouldAppendPositiveTwoDigits() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, 42L);
            assertEquals("42", bytes.toString(), "appendBase10 renders two digit value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendBase10 appends multi-digit positive value")
    void shouldAppendMultiDigitPositive() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, 12345L);
            assertEquals("12345", bytes.toString(), "appendBase10 renders multi-digit positive value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendBase10 appends negative number value with minus sign")
    void shouldAppendNegative() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, -123L);
            assertEquals("-123", bytes.toString(), "appendBase10 renders negative value with minus sign");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendBase10 appends Long.MAX_VALUE value")
    void shouldAppendLongMaxValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, Long.MAX_VALUE);
            assertEquals(String.valueOf(Long.MAX_VALUE), bytes.toString(),
                    "appendBase10 renders Long.MAX_VALUE value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendBase10 appends Long.MIN_VALUE value")
    void shouldAppendLongMinValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, Long.MIN_VALUE);
            assertEquals(String.valueOf(Long.MIN_VALUE), bytes.toString(),
                    "appendBase10 renders Long.MIN_VALUE value");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== appendBase10 Int Tests ==========

    @Test
    @DisplayName("appendBase10 int delegates to long method")
    void shouldAppendIntAsLong() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, 99);
            assertEquals("99", bytes.toString(), "appendBase10 renders int value via long path");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== appendBase16 Tests ==========

    @Test
    @DisplayName("appendBase16 appends hex digits with minimum width")
    void shouldAppendHexWithMinWidth() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase16(bytes, 255L, 2);
            assertEquals("ff", bytes.toString(), "appendBase16 renders 0xFF as 'ff'");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendBase16 pads value to minimum digits")
    void shouldPadHexToMinDigits() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase16(bytes, 15L, 4);
            assertEquals("000f", bytes.toString(), "appendBase16 pads to 4 digits");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendBase16 handles larger hex values correctly")
    void shouldAppendLargeHex() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase16(bytes, 0xDEADBEEFL, 1);
            assertEquals("deadbeef", bytes.toString(), "appendBase16 renders full hex value");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== appendDecimal Tests ==========

    @Test
    @DisplayName("appendDecimal with 0 decimal places delegates to appendBase10")
    void shouldAppendDecimalZeroPlaces() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendDecimal(bytes, 12345L, 0);
            assertEquals("12345", bytes.toString(), "appendDecimal renders integer with zero decimal places");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendDecimal inserts decimal point at correct position")
    void shouldAppendDecimalWithPoint() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendDecimal(bytes, 12345L, 2);
            assertEquals("123.45", bytes.toString(), "appendDecimal renders two decimal places");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendDecimal handles negative numeric values with sign")
    void shouldAppendNegativeDecimal() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendDecimal(bytes, -12345L, 2);
            assertEquals("-123.45", bytes.toString(), "appendDecimal renders minus sign for negative value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendDecimal handles Long.MIN_VALUE value")
    void shouldAppendDecimalMinValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendDecimal(bytes, Long.MIN_VALUE, 0);
            assertEquals(String.valueOf(Long.MIN_VALUE), bytes.toString(),
                    "appendDecimal renders Long.MIN_VALUE value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendDecimal with more decimal places than digits adds leading zeros")
    void shouldAppendDecimalWithLeadingZeros() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendDecimal(bytes, 5L, 3);
            assertEquals("0.005", bytes.toString(), "appendDecimal adds leading zeros before fraction");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== append Double Tests ==========

    @Test
    @DisplayName("append double zero value to text")
    void shouldAppendDoubleZero() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, 0.0);
            assertEquals("0.0", bytes.toString(), "append renders 0.0 for zero value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append negative zero value with sign")
    void shouldAppendNegativeZero() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, -0.0);
            assertEquals("-0.0", bytes.toString(), "append renders minus sign for negative zero");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append positive infinity value as text")
    void shouldAppendPositiveInfinity() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, Double.POSITIVE_INFINITY);
            assertEquals("Infinity", bytes.toString(), "append renders positive Infinity text");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append negative infinity value as text")
    void shouldAppendNegativeInfinity() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, Double.NEGATIVE_INFINITY);
            assertEquals("-Infinity", bytes.toString(), "append renders negative Infinity text");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append NaN value as text for double")
    void shouldAppendNaN() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, Double.NaN);
            assertEquals("NaN", bytes.toString(), "append renders NaN text");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append positive double with integer and fraction digits")
    void shouldAppendPositiveDoubleWithFraction() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, 123.456);
            String result = bytes.toString();
            assertTrue(result.startsWith("123.45"), "appended text starts with 123.45: " + result);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append negative double value as text")
    void shouldAppendNegativeDouble() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, -42.5);
            assertEquals("-42.5", bytes.toString(), "append renders -42.5 text");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append very small double fraction value")
    void shouldAppendVerySmallDouble() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, 0.00123);
            String result = bytes.toString();
            assertTrue(result.startsWith("0.00123"), "appended text represents small fraction: " + result);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append very large double value as text")
    void shouldAppendVeryLargeDouble() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, 1e15);
            String result = bytes.toString();
            assertNotNull(result, "append renders large double text");
            assertTrue(result.length() > 0, "appended text is not empty");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append double value exactly representable as integer")
    void shouldAppendDoubleAsInteger() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, 42.0);
            assertEquals("42.0", bytes.toString(), "append renders integer double with .0");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== prepend Tests ==========

    @Test
    @DisplayName("prepend positive number value to bytes")
    void shouldPrependPositive() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            bytes.writePosition(10); // Leave room at start
            bytes.readPosition(10);
            BytesInternal.prepend(bytes, 123L);
            bytes.readPosition(bytes.start());
            String result = bytes.toString();
            assertTrue(result.contains("123"), "prepend inserts 123 at start: " + result);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("prepend negative number value to bytes")
    void shouldPrependNegative() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            bytes.writePosition(10);
            bytes.readPosition(10);
            BytesInternal.prepend(bytes, -42L);
            bytes.readPosition(bytes.start());
            String result = bytes.toString();
            assertTrue(result.contains("-42"), "prepend inserts -42 at start: " + result);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("prepend Long.MIN_VALUE value to bytes")
    void shouldPrependMinValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            bytes.writePosition(25); // Leave room for MIN_VALUE
            bytes.readPosition(25);
            BytesInternal.prepend(bytes, Long.MIN_VALUE);
            bytes.readPosition(bytes.start());
            String result = bytes.toString();
            assertTrue(result.contains(String.valueOf(Long.MIN_VALUE)),
                    "prepend inserts Long.MIN_VALUE: " + result);
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== append with fixed width Tests ==========

    @Test
    @DisplayName("append long value with fixed width")
    void shouldAppendLongWithFixedWidth() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(10);
        try {
            bytes.zeroOut(0, 10);
            BytesInternal.append(bytes, 0, 42L, 5);
            bytes.readPosition(0);
            bytes.readLimit(5);
            assertEquals("00042", bytes.toString(), "append pads to 5 digits");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append negative long value with fixed width")
    void shouldAppendNegativeLongWithFixedWidth() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(10);
        try {
            bytes.zeroOut(0, 10);
            BytesInternal.append(bytes, 0, -42L, 5);
            bytes.readPosition(0);
            bytes.readLimit(5);
            assertEquals("-0042", bytes.toString(), "append renders minus sign with padding");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append throws when number exceeds fixed width")
    void shouldThrowWhenNumberTooLarge() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(10);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> BytesInternal.append(bytes, 0, 12345L, 3),
                    "append throws when number exceeds width");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== appendDecimal with offset and width Tests ==========

    @Test
    @DisplayName("appendDecimal with offset and width padding")
    void shouldAppendDecimalWithOffsetAndWidth() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(20);
        try {
            bytes.zeroOut(0, 20);
            BytesInternal.appendDecimal(bytes, 12345L, 0, 2, 8);
            bytes.readPosition(0);
            bytes.readLimit(8);
            assertEquals("00123.45", bytes.toString(), "appendDecimal adds padding and decimal");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendDecimal with offset handles leading zeros in fraction")
    void shouldAppendDecimalWithLeadingZerosFraction() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(20);
        try {
            bytes.zeroOut(0, 20);
            BytesInternal.appendDecimal(bytes, 5L, 0, 3, 6);
            bytes.readPosition(0);
            bytes.readLimit(6);
            assertEquals("0.005\0", bytes.toString(), "appendDecimal adds leading zeros in fraction");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== appendTimeMillis Tests ==========

    @Test
    @DisplayName("appendTimeMillis formats time with hours minutes seconds")
    void shouldAppendTimeMillis() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            // 1 hour, 23 minutes, 45 seconds, 678 millis
            long timeInMS = (1 * 60 * 60 * 1000) + (23 * 60 * 1000) + (45 * 1000) + 678;
            BytesInternal.appendTimeMillis(bytes, timeInMS);
            assertEquals("01:23:45.678", bytes.toString(), "appendTimeMillis renders HH:mm:ss.SSS");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendTimeMillis handles zero time value input")
    void shouldAppendTimeMillisZero() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendTimeMillis(bytes, 0L);
            assertEquals("00:00:00.000", bytes.toString(), "appendTimeMillis renders 00:00:00.000 for zero");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendTimeMillis handles hours greater than 99")
    void shouldAppendTimeMillisLargeHours() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            // 100 hours
            long timeInMS = 100L * 60 * 60 * 1000;
            BytesInternal.appendTimeMillis(bytes, timeInMS);
            String result = bytes.toString();
            assertTrue(result.startsWith("100:"), "appendTimeMillis renders hours greater than 99: " + result);
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== appendDateMillis Tests ==========

    @Test
    @DisplayName("appendDateMillis formats date with year component")
    void shouldAppendDateMillis() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            // Use a known epoch millis for 2020-01-15
            long timeInMS = 1579046400000L; // 2020-01-15 00:00:00 UTC
            BytesInternal.appendDateMillis(bytes, timeInMS);
            String result = bytes.toString();
            assertTrue(result.contains("2020"), "appendDateMillis output contains year 2020: " + result);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendDateMillis caches output for same day")
    void shouldCacheSameDayDate() {
        Bytes<?> bytes1 = Bytes.allocateElasticOnHeap();
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        try {
            long timeInMS = 1579046400000L; // 2020-01-15 00:00:00 UTC
            BytesInternal.appendDateMillis(bytes1, timeInMS);
            BytesInternal.appendDateMillis(bytes2, timeInMS + 1000); // Same day, 1 second later

            assertEquals(bytes1.toString(), bytes2.toString(),
                    "appendDateMillis returns cached date for same day");
        } finally {
            bytes1.releaseLast();
            bytes2.releaseLast();
        }
    }

    @Test
    @DisplayName("appendDateMillis updates cache for different day")
    void shouldUpdateCacheForDifferentDay() {
        Bytes<?> bytes1 = Bytes.allocateElasticOnHeap();
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        try {
            long timeInMS1 = 1579046400000L; // 2020-01-15 00:00:00 UTC
            long timeInMS2 = timeInMS1 + 86400000L; // 2020-01-16 00:00:00 UTC (next day)

            BytesInternal.appendDateMillis(bytes1, timeInMS1);
            BytesInternal.appendDateMillis(bytes2, timeInMS2);

            assertNotEquals(bytes1.toString(), bytes2.toString(),
                    "appendDateMillis returns different date for different day");
        } finally {
            bytes1.releaseLast();
            bytes2.releaseLast();
        }
    }

    // ========== Round-trip Tests ==========

    @ParameterizedTest(name = "appendBase10 round-trip value {0}")
    @ValueSource(longs = {0, 1, 10, 99, 100, 999, 1000, 12345, -1, -99, -12345,
            Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE})
    @DisplayName("appendBase10 values can be parsed back")
    void shouldRoundTripLong(long value) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.appendBase10(bytes, value);
            bytes.readPosition(0);
            long parsed = bytes.parseLong();
            assertEquals(value, parsed, "parsed value matches original " + value);
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "append double round-trip value {0}")
    @CsvSource({
            "0.0, 0.0",
            "1.0, 1.0",
            "-1.0, -1.0",
            "0.5, 0.5",
            "0.25, 0.25",
            "42.0, 42.0"
    })
    @DisplayName("append double values can be parsed back")
    void shouldRoundTripDouble(double value, double expected) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            BytesInternal.append(bytes, value);
            bytes.readPosition(0);
            double parsed = bytes.parseDouble();
            assertEquals(expected, parsed, 0.0001, "parsed value matches expected " + expected);
        } finally {
            bytes.releaseLast();
        }
    }
}
