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
 * Tests for BytesInternal number parsing methods covering various input formats
 * and edge cases.
 */
@DisplayName("BytesInternal number parsing coverage for formats and edge cases")
class BytesInternalNumberParsingTest extends BytesTestCommon {

    // ========== parseLong Tests ==========

    @Test
    @DisplayName("parseLong parses positive integer string values")
    void shouldParsePositiveIntegers() {
        assertEquals(0, Bytes.from("0").parseLong(), "parseLong returns 0 for input '0'");
        assertEquals(1, Bytes.from("1").parseLong(), "parseLong returns 1 for input '1'");
        assertEquals(123, Bytes.from("123").parseLong(), "parseLong returns 123 for input '123'");
        assertEquals(Long.MAX_VALUE, Bytes.from(String.valueOf(Long.MAX_VALUE)).parseLong(),
                "parseLong returns Long.MAX_VALUE for MAX_VALUE input");
    }

    @Test
    @DisplayName("parseLong parses negative integer string values")
    void shouldParseNegativeIntegers() {
        assertEquals(-1, Bytes.from("-1").parseLong(), "parseLong returns -1 for input '-1'");
        assertEquals(-123, Bytes.from("-123").parseLong(), "parseLong returns -123 for input '-123'");
        assertEquals(Long.MIN_VALUE, Bytes.from(String.valueOf(Long.MIN_VALUE)).parseLong(),
                "parseLong returns Long.MIN_VALUE for MIN_VALUE input");
    }

    @Test
    @DisplayName("parseLong skips leading whitespace characters in input")
    void shouldHandleLeadingWhitespace() {
        assertEquals(123, Bytes.from("  123").parseLong(),
                "parseLong skips leading [space][space] before digits 123");
        assertEquals(123, Bytes.from("\t123").parseLong(),
                "parseLong skips leading [tab] before digits 123");
    }

    @Test
    @DisplayName("parseLong returns zero for empty or non-numeric input")
    void shouldReturnZeroForEmptyInput() {
        assertEquals(0, Bytes.from("").parseLong(), "parseLong returns 0 for empty input");
        assertEquals(0, Bytes.from("   ").parseLong(), "parseLong returns 0 for whitespace input");
        assertEquals(0, Bytes.from("abc").parseLong(), "parseLong returns 0 for non-numeric input");
    }

    @Test
    @DisplayName("parseLong handles plus sign prefix in input")
    void shouldHandlePlusSignPrefix() {
        assertEquals(123, Bytes.from("+123").parseLong(), "parseLong returns 123 for input '+123'");
    }

    // ========== parseInt Tests ==========

    @Test
    @DisplayName("parseInt parses positive integer string values")
    void shouldParseIntPositive() {
        assertEquals(0, Bytes.from("0").parseInt(), "parseInt returns 0 for input '0'");
        assertEquals(1, Bytes.from("1").parseInt(), "parseInt returns 1 for input '1'");
        assertEquals(12345, Bytes.from("12345").parseInt(), "parseInt returns 12345 for input '12345'");
        assertEquals(Integer.MAX_VALUE, Bytes.from(String.valueOf(Integer.MAX_VALUE)).parseInt(),
                "parseInt returns Integer.MAX_VALUE for MAX_VALUE input");
    }

    @Test
    @DisplayName("parseInt parses negative integer string values")
    void shouldParseIntNegative() {
        assertEquals(-1, Bytes.from("-1").parseInt(), "parseInt returns -1 for input '-1'");
        assertEquals(-12345, Bytes.from("-12345").parseInt(), "parseInt returns -12345 for input '-12345'");
        assertEquals(Integer.MIN_VALUE, Bytes.from(String.valueOf(Integer.MIN_VALUE)).parseInt(),
                "parseInt returns Integer.MIN_VALUE for MIN_VALUE input");
    }

    // ========== parseDouble Tests ==========

    @Test
    @DisplayName("parseDouble parses positive decimal string values")
    void shouldParsePositiveDoubles() {
        assertEquals(0.0, Bytes.from("0.0").parseDouble(), 0.0001, "parseDouble returns 0.0 for input '0.0'");
        assertEquals(1.5, Bytes.from("1.5").parseDouble(), 0.0001, "parseDouble returns 1.5 for input '1.5'");
        assertEquals(123.456, Bytes.from("123.456").parseDouble(), 0.0001, "parseDouble returns 123.456 for input '123.456'");
    }

    @Test
    @DisplayName("parseDouble parses negative decimal string values")
    void shouldParseNegativeDoubles() {
        assertEquals(-1.5, Bytes.from("-1.5").parseDouble(), 0.0001, "parseDouble returns -1.5 for input '-1.5'");
        assertEquals(-123.456, Bytes.from("-123.456").parseDouble(), 0.0001, "parseDouble returns -123.456 for input '-123.456'");
    }

    @Test
    @DisplayName("parseDouble parses scientific notation string values")
    void shouldParseScientificNotation() {
        assertEquals(1e10, Bytes.from("1e10").parseDouble(), 1e5,
                "parseDouble returns 1e10 for lower-case exponent input '1e10'");
        assertEquals(1E10, Bytes.from("1E10").parseDouble(), 1e5,
                "parseDouble returns 1e10 for upper-case exponent input '1E10'");
        assertEquals(1.5e3, Bytes.from("1.5e3").parseDouble(), 0.0001, "parseDouble returns 1500 for input '1.5e3'");
        assertEquals(1.5e-3, Bytes.from("1.5e-3").parseDouble(), 0.0000001, "parseDouble returns 0.0015 for input '1.5e-3'");
    }

    @Test
    @DisplayName("parseDouble parses special NaN and Infinity values")
    void shouldParseSpecialDoubleValues() {
        assertTrue(Double.isNaN(Bytes.from("NaN").parseDouble()), "parseDouble returns NaN for input 'NaN'");
        assertTrue(Double.isInfinite(Bytes.from("Infinity").parseDouble()), "parseDouble returns Infinity for input 'Infinity'");
        assertTrue(Double.isInfinite(Bytes.from("-Infinity").parseDouble()), "parseDouble returns Infinity for input '-Infinity'");
    }

    @Test
    @DisplayName("parseDouble parses negative zero value input")
    void shouldParseNegativeZero() {
        double value = Bytes.from("-0.0").parseDouble();
        assertEquals(0.0, value, 0.0, "parseDouble returns zero for input '-0.0'");
        // Note: IEEE 754 negative zero comparison
    }

    // ========== parseLongDecimal Tests ==========

    @ParameterizedTest(name = "parseLongDecimal input {0} expected {1}")
    @CsvSource({
            "123, 123",
            "123.0, 1230",  // Fixed-point: includes decimal digits
            "123.5, 1235",  // Fixed-point: includes decimal digits
            "-123, -123",
            "-123.5, -1235" // Fixed-point: includes decimal digits
    })
    @DisplayName("parseLongDecimal returns fixed point representation value")
    void shouldParseLongDecimal(String input, long expected) {
        // parseLongDecimal parses a long including decimal digits (fixed-point)
        // The decimal places count is stored in lastDecimalPlaces()
        Bytes<?> bytes = Bytes.from(input);
        try {
            long result = bytes.parseLongDecimal();
            assertEquals(expected, result,
                    "parseLongDecimal returns " + expected + " for input '" + input + "'");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== Number Termination Tests ==========

    @Test
    @DisplayName("parseLong stops at first non-digit character")
    void shouldStopAtNonDigit() {
        assertEquals(123, Bytes.from("123abc").parseLong(), "parseLong stops before non-digit 'a'");
        assertEquals(123, Bytes.from("123 456").parseLong(), "parseLong stops before space in input");
        assertEquals(123, Bytes.from("123.456").parseLong(), "parseLong stops before decimal point");
    }

    // ========== Precision Tests ==========

    @Test
    @DisplayName("parseDouble maintains precision for common inputs")
    void shouldMaintainDoublePrecision() {
        // Test values that are exactly representable in IEEE 754
        assertEquals(0.5, Bytes.from("0.5").parseDouble(), 0, "parseDouble preserves exact value 0.5");
        assertEquals(0.25, Bytes.from("0.25").parseDouble(), 0, "parseDouble preserves exact value 0.25");
        assertEquals(0.125, Bytes.from("0.125").parseDouble(), 0, "parseDouble preserves exact value 0.125");
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("parseLong handles maximum length numeric strings")
    void shouldHandleMaxLengthNumbers() {
        // 19 digits for Long.MAX_VALUE
        String maxValue = "9223372036854775807";
        assertEquals(Long.MAX_VALUE, Bytes.from(maxValue).parseLong(),
                "parseLong returns Long.MAX_VALUE for max length input");

        // 20 characters for Long.MIN_VALUE (including minus sign)
        String minValue = "-9223372036854775808";
        assertEquals(Long.MIN_VALUE, Bytes.from(minValue).parseLong(),
                "parseLong returns Long.MIN_VALUE for min length input");
    }

    @Test
    @DisplayName("parseInt parses Integer.MIN_VALUE and Integer.MAX_VALUE inputs")
    void shouldHandleIntBoundaries() {
        assertEquals(Integer.MAX_VALUE, Bytes.from("2147483647").parseInt(),
                "parseInt returns Integer.MAX_VALUE for max input");
        assertEquals(Integer.MIN_VALUE, Bytes.from("-2147483648").parseInt(),
                "parseInt returns Integer.MIN_VALUE for min input");
    }

    @ParameterizedTest(name = "whitespace input case {index} parses to 42")
    @ValueSource(strings = {" 42", "  42", "\t42", " \t42", "42 ", "42  "})
    @DisplayName("parseLong handles various whitespace formats in input")
    void shouldHandleVariousWhitespace(String input) {
        String visible = input.replace("\t", "[tab]").replace(" ", "[space]");
        assertEquals(42, Bytes.from(input).parseLong(),
                "parseLong returns 42 for whitespace input " + visible);
    }
}
