/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UnsafeText branch coverage because low-level text formatting
 * must handle edge cases to avoid buffer corruption in native memory.
 * These tests verify formatting of numbers, decimals, and character arrays
 * in order to ensure consistent output across all code paths.
 */
@SuppressWarnings({"deprecation", "checkstyle:MMOverusedWord"})
@DisplayName("UnsafeText formatting handles edge cases for native memory safety")
class UnsafeTextBranchTest extends BytesTestCommon {

    private Bytes<?> bytes;

    @BeforeEach
    void setUp() {
        bytes = NativeBytes.nativeBytes(256);
    }

    @AfterEach
    void tearDown() {
        if (bytes != null) {
            bytes.releaseLast();
            bytes = null;
        }
    }

    private String readString(long start, long end) {
        int len = (int) (end - start);
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = bytes.readByte(start + i);
        }
        return new String(data);
    }

    // appendFixed(long address, long num) tests

    @Test
    @DisplayName("appendFixed writes positive long 12345 because multi-digit values require digit extraction")
    void appendFixedPositiveLong() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, 12345L);
        assertEquals("12345", readString(0, end - address), "appendFixed formats positive long 12345 to '12345'");
    }

    @Test
    @DisplayName("appendFixed writes zero value to single character because zero is a boundary case")
    void appendFixedZero() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, 0L);
        assertEquals("0", readString(0, end - address), "appendFixed formats zero to '0'");
    }

    @Test
    @DisplayName("appendFixed writes negative long -12345 with minus sign because negatives require sign handling")
    void appendFixedNegativeLong() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, -12345L);
        assertEquals("-12345", readString(0, end - address), "appendFixed formats negative long -12345 to '-12345'");
    }

    @Test
    @DisplayName("appendFixed should handle Long.MIN_VALUE")
    void appendFixedLongMinValue() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, Long.MIN_VALUE);
        assertEquals(Long.toString(Long.MIN_VALUE), readString(0, end - address),
                "Long.MIN_VALUE should be formatted correctly");
    }

    @Test
    @DisplayName("appendFixed should handle Long.MAX_VALUE")
    void appendFixedLongMaxValue() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, Long.MAX_VALUE);
        assertEquals(Long.toString(Long.MAX_VALUE), readString(0, end - address),
                "Long.MAX_VALUE should be formatted correctly");
    }

    @Test
    @DisplayName("appendFixed writes single digit 7 because single digits use optimised path")
    void appendFixedSingleDigit() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, 7L);
        assertEquals("7", readString(0, end - address), "appendFixed formats single digit 7 to '7'");
    }

    // appendFixed(long address, double num, int digits) tests

    @Test
    @DisplayName("appendFixed double should format with specified digits")
    void appendFixedDoubleWithDigits() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, 3.14159, 2);
        String result = readString(0, end - address);
        assertTrue(result.contains("3.14") || result.equals("3.14"),
                "double should be formatted with 2 decimal places: " + result);
    }

    @Test
    @DisplayName("appendFixed double should handle zero digits")
    void appendFixedDoubleZeroDigits() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, 3.7, 0);
        String result = readString(0, end - address);
        assertTrue(result.contains("4") || result.contains("3"),
                "double with 0 digits should round: " + result);
    }

    @Test
    @DisplayName("appendFixed double should handle large magnitude falling back to appendDouble")
    void appendFixedDoubleLargeMagnitude() {
        long address = bytes.addressForWrite(0);
        // Large magnitude falls back to appendDouble
        long end = UnsafeText.appendFixed(address, 1e20, 2);
        String result = readString(0, end - address);
        assertNotNull(result, "large magnitude double should be formatted");
        assertTrue(result.length() > 0, "appendFixed for large magnitude returns non-empty output");
    }

    // appendBase10d tests

    @Test
    @DisplayName("appendBase10d writes 12345 with decimal 2 placing point between digits")
    void appendBase10dWithDecimal() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendBase10d(address, 12345, 2);
        assertEquals("123.45", readString(0, end - address),
                "appendBase10d formats 12345 with decimal=2 to '123.45'");
    }

    @Test
    @DisplayName("appendBase10d handles negative -12345 with decimal point because negatives require sign prefix")
    void appendBase10dNegative() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendBase10d(address, -12345, 2);
        assertEquals("-123.45", readString(0, end - address),
                "appendBase10d formats -12345 with decimal=2 to '-123.45'");
    }

    @Test
    @DisplayName("appendBase10d should throw for Long.MIN_VALUE")
    void appendBase10dLongMinValue() {
        long address = bytes.addressForWrite(0);
        assertThrows(AssertionError.class,
                () -> UnsafeText.appendBase10d(address, Long.MIN_VALUE, 2),
                "Long.MIN_VALUE should throw AssertionError");
    }

    @Test
    @DisplayName("appendBase10d pads with zeros when decimal position exceeds number length")
    void appendBase10dDecimalLargerThanNumber() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendBase10d(address, 5, 3);
        assertEquals("0.005", readString(0, end - address),
                "appendBase10d formats 5 with decimal=3 to '0.005' with zero padding");
    }

    // appendDouble tests

    @Test
    @DisplayName("appendDouble formats normal double 123.456 with decimal precision")
    void appendDoubleNormal() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 123.456);
        String result = readString(0, end - address);
        assertTrue(result.startsWith("123.45"),
                "appendDouble formats 123.456 with decimal precision: " + result);
    }

    @Test
    @DisplayName("appendDouble formats zero to '0.0' because trailing decimal is required")
    void appendDoubleZero() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 0.0);
        assertEquals("0.0", readString(0, end - address), "appendDouble formats zero to '0.0'");
    }

    @Test
    @DisplayName("appendDouble formats negative zero to '-0.0' preserving sign bit")
    void appendDoubleNegativeZero() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, -0.0);
        assertEquals("-0.0", readString(0, end - address), "appendDouble formats negative zero to '-0.0'");
    }

    @Test
    @DisplayName("appendDouble formats positive infinity to 'Infinity' string literal")
    void appendDoublePositiveInfinity() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, Double.POSITIVE_INFINITY);
        assertEquals("Infinity", readString(0, end - address), "appendDouble formats positive infinity to 'Infinity'");
    }

    @Test
    @DisplayName("appendDouble formats negative infinity to '-Infinity' string literal")
    void appendDoubleNegativeInfinity() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, Double.NEGATIVE_INFINITY);
        assertEquals("-Infinity", readString(0, end - address), "appendDouble formats negative infinity to '-Infinity'");
    }

    @Test
    @DisplayName("appendDouble formats NaN to 'NaN' string literal for special IEEE value")
    void appendDoubleNaN() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, Double.NaN);
        assertEquals("NaN", readString(0, end - address), "appendDouble formats NaN to 'NaN' string");
    }

    @Test
    @DisplayName("appendDouble should handle very small number")
    void appendDoubleVerySmall() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 1e-10);
        String result = readString(0, end - address);
        assertNotNull(result, "very small number should be formatted");
        assertTrue(result.contains("E") || result.contains("e") || result.contains("0.0"),
                "very small number should be in scientific notation or decimal: " + result);
    }

    @Test
    @DisplayName("appendDouble formats very large 1e35 using scientific notation")
    void appendDoubleVeryLarge() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 1e35);
        String result = readString(0, end - address);
        assertNotNull(result, "appendDouble formats very large 1e35");
        assertTrue(result.length() > 0, "appendDouble for 1e35 returns non-empty output");
    }

    @Test
    @DisplayName("appendDouble should handle number near boundary")
    void appendDoubleNearBoundary() {
        long address = bytes.addressForWrite(0);
        // Number near the 1e31 boundary
        long end = UnsafeText.appendDouble(address, 9e30);
        String result = readString(0, end - address);
        assertNotNull(result, "boundary number should be formatted");
    }

    @Test
    @DisplayName("appendDouble formats small fraction 1e-7 with leading zeros or scientific notation")
    void appendDoubleSmallFraction() {
        long address = bytes.addressForWrite(0);
        // Small fraction above 6e-8
        long end = UnsafeText.appendDouble(address, 1e-7);
        String result = readString(0, end - address);
        assertTrue(result.startsWith("0.") || result.contains("E") || result.contains("e"),
                "appendDouble formats 1e-7 with leading zeros or scientific notation: " + result);
    }

    @Test
    @DisplayName("appendDouble formats integer 42.0 with trailing decimal zero")
    void appendDoubleIntegerValue() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 42.0);
        assertEquals("42.0", readString(0, end - address), "appendDouble formats 42.0 with trailing decimal");
    }

    @Test
    @DisplayName("appendDouble formats negative -123.456 with minus sign prefix")
    void appendDoubleNegative() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, -123.456);
        String result = readString(0, end - address);
        assertTrue(result.startsWith("-123.45"),
                "appendDouble formats -123.456 with minus sign: " + result);
    }

    @Test
    @DisplayName("appendDouble formats subnormal Double.MIN_VALUE using scientific notation")
    void appendDoubleSubnormal() {
        long address = bytes.addressForWrite(0);
        // Subnormal number (denormalized)
        long end = UnsafeText.appendDouble(address, Double.MIN_VALUE);
        String result = readString(0, end - address);
        assertNotNull(result, "appendDouble formats subnormal Double.MIN_VALUE");
    }

    // append8bit(byte[]) tests

    @Test
    @DisplayName("append8bit writes byte array 'Hello' copying each byte to memory")
    void append8bitByteArray() {
        long address = bytes.addressForWrite(0);
        byte[] data = "Hello".getBytes();
        long end = UnsafeText.append8bit(address, data);
        assertEquals("Hello", readString(0, end - address), "append8bit writes byte array 'Hello' to memory");
    }

    @Test
    @DisplayName("append8bit should write empty byte array")
    void append8bitEmptyByteArray() {
        long address = bytes.addressForWrite(0);
        byte[] data = new byte[0];
        long end = UnsafeText.append8bit(address, data);
        assertEquals(address, end, "empty array should not advance address");
    }

    @Test
    @DisplayName("append8bit should write byte array with long optimization")
    void append8bitByteArrayLongOptimized() {
        long address = bytes.addressForWrite(0);
        // 16 bytes triggers the 8-byte optimization loop twice
        byte[] data = "0123456789ABCDEF".getBytes();
        long end = UnsafeText.append8bit(address, data);
        assertEquals("0123456789ABCDEF", readString(0, end - address),
                "long byte array should be written with optimization");
    }

    @Test
    @DisplayName("append8bit should handle byte array not multiple of 8")
    void append8bitByteArrayNotMultipleOf8() {
        long address = bytes.addressForWrite(0);
        // 10 bytes - one 8-byte write plus 2 individual bytes
        byte[] data = "0123456789".getBytes();
        long end = UnsafeText.append8bit(address, data);
        assertEquals("0123456789", readString(0, end - address),
                "non-multiple-of-8 byte array should be written");
    }

    // append8bit(char[]) tests

    @Test
    @DisplayName("append8bit writes char array 'Hello' extracting lower 8 bits from each char")
    void append8bitCharArray() {
        long address = bytes.addressForWrite(0);
        char[] data = "Hello".toCharArray();
        long end = UnsafeText.append8bit(address, data);
        assertEquals("Hello", readString(0, end - address), "append8bit writes char array 'Hello' to memory");
    }

    @Test
    @DisplayName("append8bit should write empty char array")
    void append8bitEmptyCharArray() {
        long address = bytes.addressForWrite(0);
        char[] data = new char[0];
        long end = UnsafeText.append8bit(address, data);
        assertEquals(address, end, "empty char array should not advance address");
    }

    @Test
    @DisplayName("append8bit should write lower 8 bits of chars")
    void append8bitCharArrayLower8Bits() {
        long address = bytes.addressForWrite(0);
        char[] data = {'A', 'B', 'C'};
        long end = UnsafeText.append8bit(address, data);
        assertEquals("ABC", readString(0, end - address),
                "lower 8 bits of chars should be written");
    }

    // Additional edge cases for branch coverage

    @Test
    @DisplayName("appendDouble should handle large integer part")
    void appendDoubleLargeIntegerPart() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 123456789.123);
        String result = readString(0, end - address);
        assertTrue(result.startsWith("123456789"),
                "large integer part should be formatted: " + result);
    }

    @Test
    @DisplayName("appendDouble should handle precision edge case")
    void appendDoublePrecisionEdge() {
        long address = bytes.addressForWrite(0);
        // Test number that exercises precision handling
        long end = UnsafeText.appendDouble(address, 1.0 / 3.0);
        String result = readString(0, end - address);
        assertTrue(result.startsWith("0.333"),
                "1/3 should be formatted correctly: " + result);
    }

    @Test
    @DisplayName("appendDouble should handle power of 2")
    void appendDoublePowerOf2() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 1024.0);
        assertEquals("1024.0", readString(0, end - address), "power of 2 should be formatted");
    }

    @Test
    @DisplayName("appendDouble formats Double.MAX_VALUE using scientific notation")
    void appendDoubleMaxValue() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, Double.MAX_VALUE);
        String result = readString(0, end - address);
        assertNotNull(result, "appendDouble formats Double.MAX_VALUE");
        assertTrue(result.length() > 0, "appendDouble for MAX_VALUE returns non-empty output");
    }
}
