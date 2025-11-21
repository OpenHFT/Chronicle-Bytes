/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.render.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SuppressWarnings({"squid:S2699", "squid:S5786"})
class DecimaliserDoubleTest extends BytesTestCommon {

    private static final DecimalAppender CHECK_OK = (negative, mantissa, exponent) -> {
        // ok
    };
    private static final DecimalAppender CHECK_NEG314 = (negative, mantissa, exponent) -> {
        Assertions.assertTrue(negative);
        Assertions.assertEquals(314, mantissa);
        Assertions.assertEquals(2, exponent);
    };
    private static final DecimalAppender CHECK_123456789_012345 = (negative, mantissa, exponent) -> {
        Assertions.assertFalse(negative);
        Assertions.assertEquals(123456789012345L, mantissa);
        Assertions.assertEquals(6, exponent);
        Assertions.assertEquals(123456789.012345, mantissa / 1e6, 0.0);
    };
    private static final DecimalAppender CHECK_NEG_PI = (negative, mantissa, exponent) -> {
        Assertions.assertTrue(negative);
        Assertions.assertEquals(3141592653589793L, mantissa);
        Assertions.assertEquals(15, exponent);
        Assertions.assertEquals(Math.PI, mantissa / 1e15, 0.0);
    };
    private static final DecimalAppender CHECK_ZERO = (negative, mantissa, exponent) -> {
        Assertions.assertFalse(negative);
        Assertions.assertEquals(0, mantissa);
        if (exponent != 0)
            Assertions.assertEquals(1, exponent);
    };
    private static final DecimalAppender CHECK_NEG_ZERO = (negative, mantissa, exponent) -> {
        Assertions.assertTrue(negative);
        Assertions.assertEquals(0, mantissa);
        if (exponent != 0)
            Assertions.assertEquals(1, exponent);
    };
    private static final double HARD_TO_DECIMALISE = 4.8846945805332034E-12;

    @BeforeEach
    void hasDirect() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    void toDoubleTestTest() {
        Assertions.assertFalse(SimpleDecimaliser.SIMPLE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK));
    }

    @Test
    void toDoubleLimitedTestTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(48847, mantissa);
            Assertions.assertEquals(16, exponent);
        };
        Assertions.assertTrue(new MaximumPrecision(16).toDecimal(HARD_TO_DECIMALISE, check));
    }

    @Test
    void toDoubleTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(48846945805332034L, mantissa);
            Assertions.assertEquals(28, exponent);
        };
        Assertions.assertTrue(GeneralDecimaliser.GENERAL.toDecimal(HARD_TO_DECIMALISE, check));
    }

    @Test
    void toDoubleTest1e_6() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(1, mantissa);
            Assertions.assertEquals(6, exponent);
        };
        Assertions.assertTrue(GeneralDecimaliser.GENERAL.toDecimal(1e-6, check));

        Assertions.assertTrue(new MaximumPrecision(7).toDecimal(1e-6, check));
        Assertions.assertTrue(new MaximumPrecision(6).toDecimal(1e-6, check));
        DecimalAppender check0 = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(0, mantissa);
            Assertions.assertEquals(0, exponent);
        };
        Assertions.assertTrue(new MaximumPrecision(5).toDecimal(1e-6, check0));
    }

    @Test
    void toDoubleTestRounding() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(1, mantissa);
            Assertions.assertEquals(0, exponent);
        };
        MaximumPrecision lp7 = new MaximumPrecision(7);
        Assertions.assertTrue(lp7.toDecimal(1.000000004, check));
        Assertions.assertTrue(lp7.toDecimal(0.999999996, check));
    }

    @Test
    void toDoubleLiteAndBigDecimal() {
        LongStream.range(0, 100_000L)
//                .parallel()
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        double d = (double) x / f;
                        Assertions.assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(d, CHECK_OK));

                        // probably requires more precision
                        long l = Double.doubleToLongBits(d);
                        double d2 = -Double.longBitsToDouble(l + x);
                        boolean decimal = UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK);
                        boolean notZero = d2 < 0; // BigDecimal doesn't handle negative 0
                        Assertions.assertEquals(notZero, decimal, "d: " + d);
                        f *= 10;
                    }
                });
    }

    @Test
    void toDoubleLarge() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertTrue(0 <= exponent);
            Assertions.assertTrue(exponent <= 18, "exponent: " + exponent);
        };
        IntStream.range(-325, 309)
                .forEach(x -> {
                    double d = (-18 < x && x < -1) ? 1.0 / Maths.tens(-x) : Math.pow(10, x);
                    double lower = 1e-18;
                    Assertions.assertEquals(d == 0.0 || (lower <= d && d <= 1e18), SimpleDecimaliser.SIMPLE.toDecimal(d, check), "x: " + x);
                });
    }

    @Test
    void testNegativeValue() {
        SimpleDecimaliser.SIMPLE.toDecimal(-3.14, CHECK_NEG314);
    }

    @Test
    void testPositive() {
        SimpleDecimaliser.SIMPLE.toDecimal(123456789.012345, CHECK_123456789_012345);
    }

    @Test
    void testPositiveBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(123456789.012345, CHECK_123456789_012345);
    }

    @Test
    void testNegativePI() {
        SimpleDecimaliser.SIMPLE.toDecimal(-Math.PI, CHECK_NEG_PI);
    }

    @Test
    void testNegativePIBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(-Math.PI, CHECK_NEG_PI);
    }

    @Test
    void testZero() {
        SimpleDecimaliser.SIMPLE.toDecimal(0.0, CHECK_ZERO);
    }

    @Test
    void testZeroBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(0.0, CHECK_ZERO);
    }

    @Test
    void testNegZero() {
        SimpleDecimaliser.SIMPLE.toDecimal(-0.0, CHECK_NEG_ZERO);
    }

    @Test
    void testNegLongMinValueBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            // -9223372036854775808
            Assertions.assertTrue(negative);
            Assertions.assertEquals(9223372036854776L, mantissa);
            Assertions.assertEquals(-3, exponent);
        };
        Assertions.assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal((double) Long.MIN_VALUE, check));
    }

    @Test
    void testDouble1() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(16666666666666785L, mantissa);
            Assertions.assertEquals(17, exponent);
        };
        double value = 0.16666666666666785d;
        Assertions.assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, check));
        Assertions.assertTrue(GeneralDecimaliser.GENERAL.toDecimal(value, check));
        Assertions.assertFalse(SimpleDecimaliser.SIMPLE.toDecimal(value, check));
    }
}
