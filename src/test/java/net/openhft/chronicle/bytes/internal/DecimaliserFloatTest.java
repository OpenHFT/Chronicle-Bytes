/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.render.*;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"squid:S2699", "squid:S5786", "deprecation"})
class DecimaliserFloatTest extends BytesTestCommon {

    private static final DecimalAppender CHECK_OK = (negative, mantissa, exponent) -> {
        // ok
    };
    private static final DecimalAppender CHECK_NEG314 = (negative, mantissa, exponent) -> {
        assertTrue(negative, "negative flag should be set for -3.14f");
        assertEquals(314, mantissa, "mantissa should be 314 for -3.14f");
        assertEquals(2, exponent, "exponent should be 2 for -3.14f (314 * 10^-2)");
    };
    private static final DecimalAppender CHECK_123456_789 = (negative, mantissa, exponent) -> {
        assertFalse(negative, "negative flag should not be set for positive value");
        assertEquals(12345679L, mantissa, "mantissa should be 12345679 (float precision)");
        assertEquals(2, exponent, "exponent should be 2 (mantissa * 10^-2 = 123456.79f)");
        assertEquals(123456.79f, mantissa / 1e2f, 0.0f, "reconstructed value should match original float");
    };
    private static final DecimalAppender CHECK_NEG_PI = (negative, mantissa, exponent) -> {
        assertTrue(negative, "negative flag should be set for -π");
        assertEquals(31415927, mantissa, "mantissa should be π with float precision");
        assertEquals(7, exponent, "exponent should be 7 for float precision π");
        assertEquals((float) Math.PI, mantissa / 1e7f, 0.0f, "reconstructed value should match Math.PI as float");
    };
    private static final DecimalAppender CHECK_ZERO = (negative, mantissa, exponent) -> {
        assertFalse(negative, "negative flag should not be set for +0.0f");
        assertEquals(0, mantissa, "mantissa should be 0 for zero value");
        if (exponent != 0)
            assertEquals(1, exponent, "exponent should be 0 or 1 for zero");
    };
    private static final DecimalAppender CHECK_NEG_ZERO = (negative, mantissa, exponent) -> {
        assertTrue(negative, "negative flag should be set for -0.0f");
        assertEquals(0, mantissa, "mantissa should be 0 for negative zero");
        if (exponent != 0)
            assertEquals(1, exponent, "exponent should be 0 or 1 for negative zero");
    };
    private static final float HARD_TO_DECIMALISE = 4.8846945805332034E-12f;

    @BeforeEach
    void hasDirect() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    void toFloatTestTest() {
        assertFalse(SimpleDecimaliser.SIMPLE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK), "SIMPLE.toDecimal");
    }

    @Test
    void toFloatLimitedTestTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "hard-to-decimalise float should be positive");
            assertEquals(48847, mantissa, "mantissa should be rounded to 16 digits precision");
            assertEquals(16, exponent, "exponent should be 16 for limited precision representation");
        };
        assertTrue(new MaximumPrecision(16).toDecimal(HARD_TO_DECIMALISE, check), "MaximumPrecision(16) should successfully convert hard-to-decimalise float");
    }

    @Test
    void toFloatTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "hard-to-decimalise float should be positive");
            assertEquals(48846946, mantissa, "mantissa should preserve float precision");
            assertEquals(19, exponent, "exponent should be 19 for float precision representation");
        };
        assertTrue(GeneralDecimaliser.GENERAL.toDecimal(HARD_TO_DECIMALISE, check), "GENERAL.toDecimal");
    }

    @Test
    void toFloatTest1e_6() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "1e-6f should be positive");
            assertEquals(1, mantissa, "mantissa should be 1 for 1e-6f");
            assertEquals(6, exponent, "exponent should be 6 for 1e-6f (1 * 10^-6)");
        };
        assertTrue(GeneralDecimaliser.GENERAL.toDecimal(1e-6f, check), "GeneralDecimaliser should convert 1e-6f");

        assertTrue(new MaximumPrecision(7).toDecimal(1e-6f, check), "MaximumPrecision(7) should preserve 1e-6f");
        assertTrue(new MaximumPrecision(6).toDecimal(1e-6f, check), "MaximumPrecision(6) should preserve 1e-6f");
        DecimalAppender check0 = (negative, mantissa, exponent) -> {
            assertFalse(negative, "1e-6f should be positive");
            assertEquals(0, mantissa, "mantissa should be 0 when precision insufficient");
            assertEquals(0, exponent, "exponent should be 0 when rounded to zero");
        };
        assertTrue(new MaximumPrecision(5).toDecimal(1e-6f, check0), "MaximumPrecision(5) should round 1e-6f to zero");
    }

    @Test
    void toFloatTestRounding() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative, "rounded float values should be positive");
            assertEquals(1, mantissa, "mantissa should round to 1");
            assertEquals(0, exponent, "exponent should be 0 after rounding to integer");
        };
        MaximumPrecision lp5 = new MaximumPrecision(5);
        assertTrue(lp5.toDecimal(1.0000004, check), "MaximumPrecision(5) should round 1.0000004 to 1");
        assertTrue(lp5.toDecimal(0.9999996, check), "MaximumPrecision(5) should round 0.9999996 to 1");
    }

    @Test
    void toFloatLiteAndBigDecimal() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(0.0f, CHECK_OK), "SIMPLE should handle zero float as baseline");
        IntStream.range(0, 100_000)
                .parallel()
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        float d = (float) x / f;
                        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(d, CHECK_OK), "SIMPLE.toDecimal");

                        // probably requires more precision
                        int l = Float.floatToRawIntBits(d);
                        float d2 = Float.intBitsToFloat(l + x);
                        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK), "USES_BIG_DECIMAL.toDecimal");
                        f *= 10;
                    }
                });
    }

    @Test
    void toFloatLarge() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertTrue(0 <= exponent, "exponent should be non-negative for large float values");
            assertTrue(exponent <= 18, "exponent: " + exponent);
        };
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(1.0f, check), "SIMPLE should handle 1.0f as baseline");
        LongStream.range(-46, 39)
                .forEach(x -> {
                    float f = (float) Math.pow(10, x);
                    float lower = 1e-18f;
                    assertEquals(f == 0 || (lower <= f && f < 1e18), SimpleDecimaliser.SIMPLE.toDecimal(f, check), "x: " + x);
                });
    }

    @Test
    void testNegativeValue() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(-3.14f, CHECK_NEG314), "testNegativeValue: toDecimal");
    }

    @Test
    void testPositive() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(123456.789f, CHECK_123456_789), "testPositive: toDecimal");
    }

    @Test
    void testPositiveBD() {
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(123456.789f, CHECK_123456_789), "testPositiveBD: toDecimal");
    }

    @Test
    void testNegativePI() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal((float) -Math.PI, CHECK_NEG_PI), "testNegativePI: toDecimal");
    }

    @Test
    void testNegativePIBD() {
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal((float) -Math.PI, CHECK_NEG_PI), "testNegativePIBD: toDecimal");
    }

    @Test
    void testZero() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(0.0f, CHECK_ZERO), "testZero: toDecimal");
    }

    @Test
    void testZeroBD() {
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(0.0f, CHECK_ZERO), "testZeroBD: toDecimal");
    }

    @Test
    void testNegZero() {
        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(-0.0f, CHECK_NEG_ZERO), "testNegZero: toDecimal");
    }

    @Test
    void testNegLongMinValueBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            // -9223372036854775808
            assertTrue(negative, "Long.MIN_VALUE as float should have negative flag set");
            assertEquals(9223372L, mantissa, "mantissa should be truncated representation for float precision");
            assertEquals(-12, exponent, "exponent should be -12 (mantissa * 10^12 approximates Long.MIN_VALUE)");
        };
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal((float) Long.MIN_VALUE, check), "BigDecimal should handle Long.MIN_VALUE as float");
    }
}
