/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.render.*;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SuppressWarnings({"squid:S2699", "squid:S5786", "deprecation"})
class DecimaliserFloatTest extends BytesTestCommon {

    private static final DecimalAppender CHECK_OK = (negative, mantissa, exponent) -> {
        // ok
    };
    private static final DecimalAppender CHECK_NEG314 = (negative, mantissa, exponent) -> {
        Assertions.assertTrue(negative);
        Assertions.assertEquals(314, mantissa);
        Assertions.assertEquals(2, exponent);
    };
    private static final DecimalAppender CHECK_123456_789 = (negative, mantissa, exponent) -> {
        Assertions.assertFalse(negative);
        Assertions.assertEquals(12345679L, mantissa);
        Assertions.assertEquals(2, exponent);
        Assertions.assertEquals(123456.79f, mantissa / 1e2f, 0.0f);
    };
    private static final DecimalAppender CHECK_NEG_PI = (negative, mantissa, exponent) -> {
        Assertions.assertTrue(negative);
        Assertions.assertEquals(31415927, mantissa);
        Assertions.assertEquals(7, exponent);
        Assertions.assertEquals((float) Math.PI, mantissa / 1e7f, 0.0f);
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
    private static final float HARD_TO_DECIMALISE = 4.8846945805332034E-12f;

    @BeforeEach
    void hasDirect() {
        Assumptions.assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    void toFloatTestTest() {
        Assertions.assertFalse(SimpleDecimaliser.SIMPLE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK));
    }

    @Test
    void toFloatLimitedTestTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(48847, mantissa);
            Assertions.assertEquals(16, exponent);
        };
        Assertions.assertTrue(new MaximumPrecision(16).toDecimal(HARD_TO_DECIMALISE, check));
    }

    @Test
    void toFloatTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(48846946, mantissa);
            Assertions.assertEquals(19, exponent);
        };
        Assertions.assertTrue(GeneralDecimaliser.GENERAL.toDecimal(HARD_TO_DECIMALISE, check));
    }

    @Test
    void toFloatTest1e_6() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(1, mantissa);
            Assertions.assertEquals(6, exponent);
        };
        Assertions.assertTrue(GeneralDecimaliser.GENERAL.toDecimal(1e-6f, check));

        Assertions.assertTrue(new MaximumPrecision(7).toDecimal(1e-6f, check));
        Assertions.assertTrue(new MaximumPrecision(6).toDecimal(1e-6f, check));
        DecimalAppender check0 = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(0, mantissa);
            Assertions.assertEquals(0, exponent);
        };
        Assertions.assertTrue(new MaximumPrecision(5).toDecimal(1e-6f, check0));
    }

    @Test
    void toFloatTestRounding() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertFalse(negative);
            Assertions.assertEquals(1, mantissa);
            Assertions.assertEquals(0, exponent);
        };
        MaximumPrecision lp5 = new MaximumPrecision(5);
        Assertions.assertTrue(lp5.toDecimal(1.0000004, check));
        Assertions.assertTrue(lp5.toDecimal(0.9999996, check));
    }

    @Test
    void toFloatLiteAndBigDecimal() {
        IntStream.range(0, 100_000)
                .parallel()
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        float d = (float) x / f;
                        Assertions.assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(d, CHECK_OK));

                        // probably requires more precision
                        int l = Float.floatToRawIntBits(d);
                        float d2 = Float.intBitsToFloat(l + x);
                        Assertions.assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK));
                        f *= 10;
                    }
                });
    }

    @Test
    void toFloatLarge() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            Assertions.assertTrue(0 <= exponent);
            Assertions.assertTrue(exponent <= 18, "exponent: " + exponent);
        };
        LongStream.range(-46, 39)
                .forEach(x -> {
                    float f = (float) Math.pow(10, x);
                    float lower = 1e-18f;
                    Assertions.assertEquals(f == 0 || (lower <= f && f < 1e18), SimpleDecimaliser.SIMPLE.toDecimal(f, check), "x: " + x);
                });
    }

    @Test
    void testNegativeValue() {
        SimpleDecimaliser.SIMPLE.toDecimal(-3.14f, CHECK_NEG314);
    }

    @Test
    void testPositive() {
        SimpleDecimaliser.SIMPLE.toDecimal(123456.789f, CHECK_123456_789);
    }

    @Test
    void testPositiveBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(123456.789f, CHECK_123456_789);
    }

    @Test
    void testNegativePI() {
        SimpleDecimaliser.SIMPLE.toDecimal((float) -Math.PI, CHECK_NEG_PI);
    }

    @Test
    void testNegativePIBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal((float) -Math.PI, CHECK_NEG_PI);
    }

    @Test
    void testZero() {
        SimpleDecimaliser.SIMPLE.toDecimal(0.0f, CHECK_ZERO);
    }

    @Test
    void testZeroBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(0.0f, CHECK_ZERO);
    }

    @Test
    void testNegZero() {
        SimpleDecimaliser.SIMPLE.toDecimal(-0.0f, CHECK_NEG_ZERO);
    }

    @Test
    void testNegLongMinValueBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            // -9223372036854775808
            Assertions.assertTrue(negative);
            Assertions.assertEquals(9223372L, mantissa);
            Assertions.assertEquals(-12, exponent);
        };
        Assertions.assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal((float) Long.MIN_VALUE, check));
    }
}
