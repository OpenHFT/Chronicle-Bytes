/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.render.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"squid:S2699", "squid:S5786", "deprecation"})
class DecimaliserDoubleTest extends BytesTestCommon {

    private static final DecimalAppender CHECK_OK = (negative, mantissa, exponent) -> {
        // ok
    };
    private static final DecimalAppender CHECK_NEG314 = (negative, mantissa, exponent) -> {
        assertTrue(negative, "negative flag should be set for -3.14");
        assertEquals(314, mantissa, "mantissa should be 314 for -3.14");
        assertEquals(2, exponent, "exponent should be 2 for -3.14 (314 * 10^-2)");
    };
    private static final DecimalAppender CHECK_123456789_012345 = (negative, mantissa, exponent) -> {
        assertFalse(negative, "negative flag should not be set for positive value");
        assertEquals(123456789012345L, mantissa, "mantissa should be 123456789012345");
        assertEquals(6, exponent, "exponent should be 6 (mantissa * 10^-6 = 123456789.012345)");
        assertEquals(123456789.012345, mantissa / 1e6, 0.0, "reconstructed value should match original");
    };
    private static final DecimalAppender CHECK_NEG_PI = (negative, mantissa, exponent) -> {
        assertTrue(negative, "negative flag should be set for -π");
        assertEquals(3141592653589793L, mantissa, "mantissa should be π with 15 decimal places");
        assertEquals(15, exponent, "exponent should be 15 for full precision π");
        assertEquals(Math.PI, mantissa / 1e15, 0.0, "reconstructed value should match Math.PI");
    };
    private static final DecimalAppender CHECK_ZERO = (negative, mantissa, exponent) -> {
        assertFalse(negative, "negative flag should not be set for +0.0");
        assertEquals(0, mantissa, "mantissa should be 0 for zero value");
        if (exponent != 0)
            assertEquals(1, exponent, "exponent should be 0 or 1 for zero");
    };
    private static final DecimalAppender CHECK_NEG_ZERO = (negative, mantissa, exponent) -> {
        assertTrue(negative, "negative flag should be set for -0.0");
        assertEquals(0, mantissa, "mantissa should be 0 for negative zero");
        if (exponent != 0)
            assertEquals(1, exponent, "exponent should be 0 or 1 for negative zero");
    };
    private static final double HARD_TO_DECIMALISE = 4.8846945805332034E-12;

    @BeforeEach
    void hasDirect() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    void toDoubleTestTest() {
        assertFalse(SimpleDecimaliser.SIMPLE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK), "SIMPLE.toDecimal");
    }

    @Test
    void toDoubleLimitedTestTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "hard-to-decimalise value should be positive");
            assertEquals(48847, mantissa, "mantissa should be rounded to 16 digits precision");
            assertEquals(16, exponent, "exponent should be 16 for limited precision representation");
        };
        assertTrue(new MaximumPrecision(16).toDecimal(HARD_TO_DECIMALISE, check), "MaximumPrecision(16) should successfully convert hard-to-decimalise value");
    }

    @Test
    void toDoubleTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "hard-to-decimalise value should be positive");
            assertEquals(48846945805332034L, mantissa, "mantissa should preserve full precision");
            assertEquals(28, exponent, "exponent should be 28 for full precision representation");
        };
        assertTrue(GeneralDecimaliser.GENERAL.toDecimal(HARD_TO_DECIMALISE, check), "GeneralDecimaliser should successfully convert hard-to-decimalise value");
    }

    @Test
    void toDoubleTest1e_6() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "1e-6 should be positive");
            assertEquals(1, mantissa, "mantissa should be 1 for 1e-6");
            assertEquals(6, exponent, "exponent should be 6 for 1e-6 (1 * 10^-6)");
        };
        assertTrue(GeneralDecimaliser.GENERAL.toDecimal(1e-6, check), "GeneralDecimaliser should convert 1e-6");

        assertTrue(new MaximumPrecision(7).toDecimal(1e-6, check), "MaximumPrecision(7) should preserve 1e-6");
        assertTrue(new MaximumPrecision(6).toDecimal(1e-6, check), "MaximumPrecision(6) should preserve 1e-6");
        DecimalAppender check0 = (negative, mantissa, exponent) -> {
            assertFalse(negative, "1e-6 should be positive");
            assertEquals(0, mantissa, "mantissa should be 0 when precision insufficient");
            assertEquals(0, exponent, "exponent should be 0 when rounded to zero");
        };
        assertTrue(new MaximumPrecision(5).toDecimal(1e-6, check0), "MaximumPrecision(5) should round 1e-6 to zero");
    }

    @Test
    void toDoubleTestRounding() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "rounded values should be positive");
            assertEquals(1, mantissa, "mantissa should round to 1");
            assertEquals(0, exponent, "exponent should be 0 after rounding to integer");
        };
        MaximumPrecision lp7 = new MaximumPrecision(7);
        assertTrue(lp7.toDecimal(1.000000004, check), "MaximumPrecision(7) should round 1.000000004 to 1");
        assertTrue(lp7.toDecimal(0.999999996, check), "MaximumPrecision(7) should round 0.999999996 to 1");
    }

    @Test
    void toDoubleLiteAndBigDecimal() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(0.0, CHECK_OK), "SIMPLE should handle zero as baseline");
        LongStream.range(0, 100_000L)
//                .parallel()
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        double d = (double) x / f;
                        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(d, CHECK_OK), "SIMPLE.toDecimal");

                        // probably requires more precision
                        long l = Double.doubleToLongBits(d);
                        double d2 = -Double.longBitsToDouble(l + x);
                        boolean decimal = UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK);
                        boolean notZero = d2 < 0; // BigDecimal doesn't handle negative 0
                        assertEquals(notZero, decimal, "d: " + d);
                        f *= 10;
                    }
                });
    }

    @Test
    void toDoubleLarge() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertTrue(0 <= exponent, "exponent should be non-negative for large values");
            assertTrue(exponent <= 18, "exponent: " + exponent);
        };
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(1.0, check), "SIMPLE should handle 1.0 as baseline");
        IntStream.range(-325, 309)
                .forEach(x -> {
                    double d = (-18 < x && x < -1) ? 1.0 / Maths.tens(-x) : Math.pow(10, x);
                    double lower = 1e-18;
                    assertEquals(d == 0.0 || (lower <= d && d <= 1e18), SimpleDecimaliser.SIMPLE.toDecimal(d, check), "x: " + x);
                });
    }

    @Test
    void testNegativeValue() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(-3.14, CHECK_NEG314), "testNegativeValue: toDecimal");
    }

    @Test
    void testPositive() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(123456789.012345, CHECK_123456789_012345), "testPositive: toDecimal");
    }

    @Test
    void testPositiveBD() {
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(123456789.012345, CHECK_123456789_012345), "testPositiveBD: toDecimal");
    }

    @Test
    void testNegativePI() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(-Math.PI, CHECK_NEG_PI), "testNegativePI: toDecimal");
    }

    @Test
    void testNegativePIBD() {
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(-Math.PI, CHECK_NEG_PI), "testNegativePIBD: toDecimal");
    }

    @Test
    void testZero() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(0.0, CHECK_ZERO), "testZero: toDecimal");
    }

    @Test
    void testZeroBD() {
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(0.0, CHECK_ZERO), "testZeroBD: toDecimal");
    }

    @Test
    void testNegZero() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(-0.0, CHECK_NEG_ZERO), "testNegZero: toDecimal");
    }

    @Test
    void testNegLongMinValueBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            // -9223372036854775808
            assertTrue(negative, "Long.MIN_VALUE should have negative flag set");
            assertEquals(9223372036854776L, mantissa, "mantissa should be rounded representation of Long.MIN_VALUE");
            assertEquals(-3, exponent, "exponent should be -3 (mantissa * 10^3 = Long.MIN_VALUE)");
        };
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal((double) Long.MIN_VALUE, check), "BigDecimal should handle Long.MIN_VALUE");
    }

    @Test
    void testDouble1() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "value 0.16666... should be positive");
            assertEquals(16666666666666785L, mantissa, "mantissa should preserve all significant digits");
            assertEquals(17, exponent, "exponent should be 17 for 17 decimal places");
        };
        double value = 0.16666666666666785d;
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, check), "BigDecimal should handle repeating decimal");
        assertTrue(GeneralDecimaliser.GENERAL.toDecimal(value, check), "GeneralDecimaliser should handle repeating decimal");
        assertFalse(SimpleDecimaliser.SIMPLE.toDecimal(value, check), "SimpleDecimaliser cannot handle complex repeating decimal");
    }
}
