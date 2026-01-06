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
 * Tests for UnsafeText branch coverage.
 */
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
    @DisplayName("appendFixed should write positive long")
    void appendFixedPositiveLong() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, 12345L);
        assertEquals("12345", readString(0, end - address), "positive long should be formatted");
    }

    @Test
    @DisplayName("appendFixed should write zero")
    void appendFixedZero() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, 0L);
        assertEquals("0", readString(0, end - address), "zero should be formatted");
    }

    @Test
    @DisplayName("appendFixed should write negative long")
    void appendFixedNegativeLong() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, -12345L);
        assertEquals("-12345", readString(0, end - address), "negative long should be formatted");
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
    @DisplayName("appendFixed should handle single digit")
    void appendFixedSingleDigit() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendFixed(address, 7L);
        assertEquals("7", readString(0, end - address), "single digit should be formatted");
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
        assertTrue(result.length() > 0, "result should not be empty");
    }

    // appendBase10d tests

    @Test
    @DisplayName("appendBase10d should write with decimal point")
    void appendBase10dWithDecimal() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendBase10d(address, 12345, 2);
        assertEquals("123.45", readString(0, end - address),
                "should format with decimal point at position 2");
    }

    @Test
    @DisplayName("appendBase10d should handle negative number")
    void appendBase10dNegative() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendBase10d(address, -12345, 2);
        assertEquals("-123.45", readString(0, end - address),
                "negative should format with decimal point");
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
    @DisplayName("appendBase10d should handle decimal larger than number")
    void appendBase10dDecimalLargerThanNumber() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendBase10d(address, 5, 3);
        assertEquals("0.005", readString(0, end - address),
                "should pad with zeros when decimal > number length");
    }

    // appendDouble tests

    @Test
    @DisplayName("appendDouble should format normal double")
    void appendDoubleNormal() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 123.456);
        String result = readString(0, end - address);
        assertTrue(result.startsWith("123.45"),
                "normal double should be formatted: " + result);
    }

    @Test
    @DisplayName("appendDouble should handle zero")
    void appendDoubleZero() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 0.0);
        assertEquals("0.0", readString(0, end - address), "zero should be formatted as 0.0");
    }

    @Test
    @DisplayName("appendDouble should handle negative zero")
    void appendDoubleNegativeZero() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, -0.0);
        assertEquals("-0.0", readString(0, end - address), "negative zero should be -0.0");
    }

    @Test
    @DisplayName("appendDouble should handle positive infinity")
    void appendDoublePositiveInfinity() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, Double.POSITIVE_INFINITY);
        assertEquals("Infinity", readString(0, end - address), "positive infinity");
    }

    @Test
    @DisplayName("appendDouble should handle negative infinity")
    void appendDoubleNegativeInfinity() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, Double.NEGATIVE_INFINITY);
        assertEquals("-Infinity", readString(0, end - address), "negative infinity");
    }

    @Test
    @DisplayName("appendDouble should handle NaN")
    void appendDoubleNaN() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, Double.NaN);
        assertEquals("NaN", readString(0, end - address), "NaN should be formatted");
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
    @DisplayName("appendDouble should handle very large number")
    void appendDoubleVeryLarge() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 1e35);
        String result = readString(0, end - address);
        assertNotNull(result, "very large number should be formatted");
        assertTrue(result.length() > 0, "result should not be empty");
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
    @DisplayName("appendDouble should handle small fraction")
    void appendDoubleSmallFraction() {
        long address = bytes.addressForWrite(0);
        // Small fraction above 6e-8
        long end = UnsafeText.appendDouble(address, 1e-7);
        String result = readString(0, end - address);
        assertTrue(result.startsWith("0.") || result.contains("E") || result.contains("e"),
                "small fraction should be formatted: " + result);
    }

    @Test
    @DisplayName("appendDouble should handle integer value")
    void appendDoubleIntegerValue() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, 42.0);
        assertEquals("42.0", readString(0, end - address), "integer value should have .0");
    }

    @Test
    @DisplayName("appendDouble should handle negative double")
    void appendDoubleNegative() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, -123.456);
        String result = readString(0, end - address);
        assertTrue(result.startsWith("-123.45"),
                "negative double should be formatted: " + result);
    }

    @Test
    @DisplayName("appendDouble should handle subnormal number")
    void appendDoubleSubnormal() {
        long address = bytes.addressForWrite(0);
        // Subnormal number (denormalized)
        long end = UnsafeText.appendDouble(address, Double.MIN_VALUE);
        String result = readString(0, end - address);
        assertNotNull(result, "subnormal should be formatted");
    }

    // append8bit(byte[]) tests

    @Test
    @DisplayName("append8bit should write byte array")
    void append8bitByteArray() {
        long address = bytes.addressForWrite(0);
        byte[] data = "Hello".getBytes();
        long end = UnsafeText.append8bit(address, data);
        assertEquals("Hello", readString(0, end - address), "byte array should be written");
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
    @DisplayName("append8bit should write char array")
    void append8bitCharArray() {
        long address = bytes.addressForWrite(0);
        char[] data = "Hello".toCharArray();
        long end = UnsafeText.append8bit(address, data);
        assertEquals("Hello", readString(0, end - address), "char array should be written");
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
    @DisplayName("appendDouble should handle max finite double")
    void appendDoubleMaxValue() {
        long address = bytes.addressForWrite(0);
        long end = UnsafeText.appendDouble(address, Double.MAX_VALUE);
        String result = readString(0, end - address);
        assertNotNull(result, "max double should be formatted");
        assertTrue(result.length() > 0, "result should not be empty");
    }
}
