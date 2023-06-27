/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.Assert.*;

class DecimaliserDoubleTest {

    public static final DecimalAppender CHECK_OK = (negative, mantissa, exponent) -> {
        // ok
    };
    public static final DecimalAppender CHECK_NEG314 = (negative, mantissa, exponent) -> {
        assertTrue(negative);
        assertEquals(314, mantissa);
        assertEquals(2, exponent);
    };
    public static final DecimalAppender CHECK_123456789_012345 = (negative, mantissa, exponent) -> {
        assertFalse(negative);
        assertEquals(123456789012345L, mantissa);
        assertEquals(6, exponent);
        assertEquals(123456789.012345, mantissa / 1e6, 0.0);
    };
    public static final DecimalAppender CHECK_NEG_PI = (negative, mantissa, exponent) -> {
        assertTrue(negative);
        assertEquals(3141592653589793L, mantissa);
        assertEquals(15, exponent);
        assertEquals(Math.PI, mantissa / 1e15, 0.0);
    };
    public static final DecimalAppender CHECK_ZERO = (negative, mantissa, exponent) -> {
        assertFalse(negative);
        assertEquals(0, mantissa);
        if (exponent != 0)
            assertEquals(1, exponent);
    };
    public static final DecimalAppender CHECK_NEG_ZERO = (negative, mantissa, exponent) -> {
        assertTrue(negative);
        assertEquals(0, mantissa);
        if (exponent != 0)
            assertEquals(1, exponent);
    };
    public static final double HARD_TO_DECIMALISE = 4.8846945805332034E-12;

    @Test
    public void toDoubleTestTest() {
        assertFalse(Decimalizer.LITE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK));
    }

    @Test
    public void toDoubleLimitedTestTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(48847, mantissa);
            assertEquals(16, exponent);
        };
        assertTrue(new Decimalizer.MaximumPrecisionOnly(16).toDecimal(HARD_TO_DECIMALISE, check));
    }

    @Test
    public void toDoubleTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(48846945805332034L, mantissa);
            assertEquals(28, exponent);
        };
        assertTrue(Decimalizer.INSTANCE.toDecimal(HARD_TO_DECIMALISE, check));
    }

    @Test
    public void toDoubleTest1e_6() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(1, mantissa);
            assertEquals(6, exponent);
        };
        assertTrue(Decimalizer.INSTANCE.toDecimal(1e-6, check));

        assertTrue(new Decimalizer.MaximumPrecisionOnly(7).toDecimal(1e-6, check));
        assertTrue(new Decimalizer.MaximumPrecisionOnly(6).toDecimal(1e-6, check));
        DecimalAppender check0 = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(0, mantissa);
            assertEquals(1, exponent);
        };
        assertTrue(new Decimalizer.MaximumPrecisionOnly(5).toDecimal(1e-6, check));
    }

    @Test
    public void toDoubleTestRounding() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(1, mantissa);
            assertEquals(0, exponent);
        };
        Decimalizer.MaximumPrecisionOnly lp7 = new Decimalizer.MaximumPrecisionOnly(7);
        assertTrue(lp7.toDecimal(1.000000004, check));
        assertTrue(lp7.toDecimal(0.999999996, check));
    }

    @Test
    public void toDoubleLiteAndBigDecimal() {
        LongStream.range(0, 100_000L)
//                .parallel()
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        double d = (double) x / f;
                        assertTrue(Decimalizer.LITE.toDecimal(d, CHECK_OK));

                        // probably requires more precision
                        long l = Double.doubleToLongBits(d);
                        double d2 = Double.longBitsToDouble(l + x);
                        boolean decimal = Decimalizer.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK);
                        boolean notZero = d > 0; // BigDecimal doesn't handle negative 0
                        assertEquals("d: " + d, notZero, decimal);
                        f *= 10;
                    }
                });
    }

    @Test
    public void toDoubleLarge() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertTrue(0 <= exponent);
            assertTrue("exponent: " + exponent, exponent <= 18);
        };
        IntStream.range(-325, 309)
                .forEach(x -> {
                    double d = (-18 < x && x < -1) ? 1.0 / Maths.tens(-x) : Math.pow(10, x);
                    double lower = 1e-18;
                    assertEquals("x: " + x,
                            d == 0.0 || (lower <= d && d <= 1e18),
                            Decimalizer.LITE.toDecimal(d, check));
                });
    }

    @Test
    public void testNegativeValue() {
        Decimalizer.LITE.toDecimal(-3.14, CHECK_NEG314);
    }

    @Test
    public void testPositive() {
        Decimalizer.LITE.toDecimal(123456789.012345, CHECK_123456789_012345);
    }

    @Test
    public void testPositiveBD() {
        Decimalizer.USES_BIG_DECIMAL.toDecimal(123456789.012345, CHECK_123456789_012345);
    }

    @Test
    public void testNegativePI() {
        Decimalizer.LITE.toDecimal(-Math.PI, CHECK_NEG_PI);
    }

    @Test
    public void testNegativePIBD() {
        Decimalizer.USES_BIG_DECIMAL.toDecimal(-Math.PI, CHECK_NEG_PI);
    }

    @Test
    public void testZero() {
        Decimalizer.LITE.toDecimal(0.0, CHECK_ZERO);
    }

    @Test
    public void testZeroBD() {
        Decimalizer.USES_BIG_DECIMAL.toDecimal(0.0, CHECK_ZERO);
    }

    @Test
    public void testNegZero() {
        Decimalizer.LITE.toDecimal(-0.0, CHECK_NEG_ZERO);
    }

    @Test
    public void testNegLongMinValueBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            // -9223372036854775808
            assertTrue(negative);
            assertEquals(9223372036854776L, mantissa);
            assertEquals(-3, exponent);
        };
        assertTrue(Decimalizer.USES_BIG_DECIMAL.toDecimal((double) Long.MIN_VALUE, check));
    }
}
