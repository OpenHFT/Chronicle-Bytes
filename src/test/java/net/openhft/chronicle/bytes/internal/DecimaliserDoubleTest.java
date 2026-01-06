/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.render.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"squid:S2699", "squid:S5786", "deprecation"})
@DisplayName("Double decimaliser behaviour and formatting checks")
class DecimaliserDoubleTest extends BytesTestCommon {

    private static final DecimalAppender CHECK_OK = (negative, mantissa, exponent) -> {
        // ok
    };
    private static final DecimalAppender CHECK_NEG314 = (negative, mantissa, exponent) -> {
        assertTrue(negative,
                "Negative flag is true for -3.14");
        assertEquals(314, mantissa,
                "Mantissa matches 3.14 scaled");
        assertEquals(2, exponent,
                "Exponent matches scale for -3.14");
    };
    private static final DecimalAppender CHECK_123456789_012345 = (negative, mantissa, exponent) -> {
        assertFalse(negative,
                "Positive values keep negative flag false");
        assertEquals(123456789012345L, mantissa,
                "Mantissa matches 123456789.012345 scaled");
        assertEquals(6, exponent,
                "Exponent matches scale for 123456789.012345");
        assertEquals(123456789.012345, mantissa / 1e6, 0.0,
                "Scaled mantissa reconstructs expected value");
    };
    private static final DecimalAppender CHECK_NEG_PI = (negative, mantissa, exponent) -> {
        assertTrue(negative,
                "Negative flag is true for -PI");
        assertEquals(3141592653589793L, mantissa,
                "Mantissa matches PI scaled to 15 digits");
        assertEquals(15, exponent,
                "Exponent matches scale for PI");
        assertEquals(Math.PI, mantissa / 1e15, 0.0,
                "Scaled mantissa reconstructs PI");
    };
    private static final DecimalAppender CHECK_ZERO = (negative, mantissa, exponent) -> {
        assertFalse(negative,
                "Zero keeps negative flag false");
        assertEquals(0, mantissa,
                "Zero keeps mantissa at zero");
        if (exponent != 0)
            assertEquals(1, exponent,
                    "Zero exponent uses one when non-zero");
    };
    private static final DecimalAppender CHECK_NEG_ZERO = (negative, mantissa, exponent) -> {
        assertTrue(negative,
                "Negative zero keeps negative flag true");
        assertEquals(0, mantissa,
                "Negative zero keeps mantissa at zero");
        if (exponent != 0)
            assertEquals(1, exponent,
                    "Negative zero exponent uses one when non-zero");
    };
    private static final double HARD_TO_DECIMALISE = 4.8846945805332034E-12;

    @BeforeEach
    void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for decimaliser tests");
    }

    @Test
    @DisplayName("simple decimaliser fails for hard to decimalise values")
    void toDoubleTestTest() {
        assertFalse(SimpleDecimaliser.SIMPLE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK),
                "Simple decimaliser should reject hard to decimalise value");
    }

    @Test
    @DisplayName("maximum precision handles hard to decimalise values")
    void toDoubleLimitedTestTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative,
                    "Hard value stays positive");
            assertEquals(48847, mantissa,
                    "Mantissa matches limited precision");
            assertEquals(16, exponent,
                    "Exponent matches limited precision scale");
        };
        assertTrue(new MaximumPrecision(16).toDecimal(HARD_TO_DECIMALISE, check),
                "Maximum precision should convert hard value");
    }

    @Test
    @DisplayName("general decimaliser handles hard value conversion")
    void toDoubleTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative,
                    "Hard value remains positive in general decimaliser");
            assertEquals(48846945805332034L, mantissa,
                    "Mantissa matches hard value scale");
            assertEquals(28, exponent,
                    "Exponent matches hard value scale");
        };
        assertTrue(GeneralDecimaliser.GENERAL.toDecimal(HARD_TO_DECIMALISE, check),
                "General decimaliser converts hard value");
    }

    @Test
    @DisplayName("precision tiers handle 1e-6 value")
    void toDoubleTest1e_6() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative,
                    "1e-6 stays positive");
            assertEquals(1, mantissa,
                    "Mantissa matches 1e-6 scale");
            assertEquals(6, exponent,
                    "Exponent matches 1e-6 scale");
        };
        assertTrue(GeneralDecimaliser.GENERAL.toDecimal(1e-6, check),
                "General decimaliser handles 1e-6 value");

        assertTrue(new MaximumPrecision(7).toDecimal(1e-6, check),
                "Maximum precision 7 handles 1e-6 value");
        assertTrue(new MaximumPrecision(6).toDecimal(1e-6, check),
                "Maximum precision 6 handles 1e-6 value");
        DecimalAppender check0 = (negative, mantissa, exponent) -> {
            assertFalse(negative,
                    "Rounded 1e-6 stays positive");
            assertEquals(0, mantissa,
                    "Mantissa rounds to zero with limited precision");
            assertEquals(0, exponent,
                    "Exponent resets to zero with limited precision");
        };
        assertTrue(new MaximumPrecision(5).toDecimal(1e-6, check0),
                "Maximum precision 5 rounds 1e-6 to zero");
    }

    @Test
    @DisplayName("rounding handles values near one safely")
    void toDoubleTestRounding() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative,
                    "Rounded values stay positive");
            assertEquals(1, mantissa,
                    "Mantissa rounds to one");
            assertEquals(0, exponent,
                    "Exponent stays zero after rounding");
        };
        MaximumPrecision lp7 = new MaximumPrecision(7);
        assertTrue(lp7.toDecimal(1.000000004, check),
                "Rounding handles value above one");
        assertTrue(lp7.toDecimal(0.999999996, check),
                "Rounding handles value below one");
    }

    @Test
    @DisplayName("simple and big decimal agree across range")
    void toDoubleLiteAndBigDecimal() {
        LongStream.range(0, 100_000L)
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        double d = (double) x / f;
                        assertTrue(SimpleDecimaliser.SIMPLE.toDecimal(d, CHECK_OK),
                                "Simple decimaliser handles value at index " + i);

                        // probably requires more precision
                        long l = Double.doubleToLongBits(d);
                        double d2 = -Double.longBitsToDouble(l + x);
                        boolean decimal = UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK);
                        boolean notZero = d2 < 0; // BigDecimal doesn't handle negative 0
                        assertEquals(notZero, decimal,
                                "Big decimal handles adjusted value at index " + i + " for d " + d);
                        f *= 10;
                    }
                });
    }

    @Test
    @DisplayName("large exponent handling uses simple decimaliser")
    void toDoubleLarge() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertTrue(0 <= exponent,
                    "Exponent is non-negative: " + exponent);
            assertTrue(exponent <= 18,
                    "Exponent is within limit: " + exponent + " <= 18");
        };
        IntStream.range(-325, 309)
                .forEach(x -> {
                    double d = (-18 < x && x < -1) ? 1.0 / Maths.tens(-x) : Math.pow(10, x);
                    double lower = 1e-18;
                    assertEquals(d == 0.0 || (lower <= d && d <= 1e18),
                            SimpleDecimaliser.SIMPLE.toDecimal(d, check),
                            "Simple decimaliser matches large exponent for x " + x);
                });
    }

    @Test
    @DisplayName("simple decimaliser converts negative decimal value")
    void testNegativeValue() {
        SimpleDecimaliser.SIMPLE.toDecimal(-3.14, CHECK_NEG314);
    }

    @Test
    @DisplayName("simple decimaliser converts positive decimal value")
    void testPositive() {
        SimpleDecimaliser.SIMPLE.toDecimal(123456789.012345, CHECK_123456789_012345);
    }

    @Test
    @DisplayName("big decimal decimaliser converts positive decimal value")
    void testPositiveBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(123456789.012345, CHECK_123456789_012345);
    }

    @Test
    @DisplayName("simple decimaliser converts negative pi value")
    void testNegativePI() {
        SimpleDecimaliser.SIMPLE.toDecimal(-Math.PI, CHECK_NEG_PI);
    }

    @Test
    @DisplayName("big decimal decimaliser converts negative pi")
    void testNegativePIBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(-Math.PI, CHECK_NEG_PI);
    }

    @Test
    @DisplayName("simple decimaliser converts zero value correctly")
    void testZero() {
        SimpleDecimaliser.SIMPLE.toDecimal(0.0, CHECK_ZERO);
    }

    @Test
    @DisplayName("big decimal decimaliser converts zero value")
    void testZeroBD() {
        UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(0.0, CHECK_ZERO);
    }

    @Test
    @DisplayName("big decimal decimaliser rejects negative zero")
    void testNegZeroBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            throw new AssertionError("Negative zero should not be appended");
        };
        assertFalse(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(-0.0, check),
                "Big decimal decimaliser should reject negative zero");
    }

    @Test
    @DisplayName("big decimal decimaliser rejects NaN and infinity")
    void testNonFiniteBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            throw new AssertionError("Non-finite values should not be appended");
        };
        assertFalse(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(Double.NaN, check),
                "Big decimal decimaliser should reject NaN");
        assertFalse(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(Double.POSITIVE_INFINITY, check),
                "Big decimal decimaliser should reject infinity");
    }

    @Test
    @DisplayName("simple decimaliser converts negative zero value")
    void testNegZero() {
        SimpleDecimaliser.SIMPLE.toDecimal(-0.0, CHECK_NEG_ZERO);
    }

    @Test
    @DisplayName("big decimal decimaliser handles long min value")
    void testNegLongMinValueBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            // -9223372036854775808
            assertTrue(negative,
                    "Long min value should keep negative flag");
            assertEquals(9223372036854776L, mantissa,
                    "Mantissa matches long min value scale");
            assertEquals(-3, exponent,
                    "Exponent matches long min value scale");
        };
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal((double) Long.MIN_VALUE, check),
                "Big decimal decimaliser converts long min value");
    }

    @Test
    @DisplayName("big decimal and general handle repeating decimal")
    void testDouble1() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative,
                    "Repeating decimal stays positive");
            assertEquals(16666666666666785L, mantissa,
                    "Mantissa matches repeating decimal scale");
            assertEquals(17, exponent,
                    "Exponent matches repeating decimal scale");
        };
        double value = 0.16666666666666785d;
        assertTrue(UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, check),
                "Big decimal handles repeating decimal value");
        assertTrue(GeneralDecimaliser.GENERAL.toDecimal(value, check),
                "General decimaliser handles repeating decimal value");
        assertFalse(SimpleDecimaliser.SIMPLE.toDecimal(value, check),
                "Simple decimaliser rejects repeating decimal value");
    }
}
