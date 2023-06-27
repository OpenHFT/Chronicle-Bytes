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

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.Assert.*;

class DecimaliserFloatTest {

    public static final DecimalAppender CHECK_OK = (negative, mantissa, exponent) -> {
        // ok
    };
    public static final DecimalAppender CHECK_NEG314 = (negative, mantissa, exponent) -> {
        assertTrue(negative);
        assertEquals(314, mantissa);
        assertEquals(2, exponent);
    };
    public static final DecimalAppender CHECK_123456_789 = (negative, mantissa, exponent) -> {
        assertFalse(negative);
        assertEquals(12345679L, mantissa);
        assertEquals(2, exponent);
        assertEquals(123456.79f, mantissa / 1e2f, 0.0f);
    };
    public static final DecimalAppender CHECK_NEG_PI = (negative, mantissa, exponent) -> {
        assertTrue(negative);
        assertEquals(31415927, mantissa);
        assertEquals(7, exponent);
        assertEquals((float) Math.PI, mantissa / 1e7f, 0.0f);
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
    public static final float HARD_TO_DECIMALISE = 4.8846945805332034E-12f;

    @Test
    public void toFloatTestTest() {
        assertFalse(Decimalizer.LITE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK));
    }

    @Test
    public void toFloatLimitedTestTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(48847, mantissa);
            assertEquals(16, exponent);
        };
        assertTrue(new Decimalizer.MaximumPrecisionOnly(16).toDecimal((float) HARD_TO_DECIMALISE, check));
    }

    @Test
    public void toFloatTest() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(48846946, mantissa);
            assertEquals(19, exponent);
        };
        assertTrue(Decimalizer.INSTANCE.toDecimal(HARD_TO_DECIMALISE, check));
    }

    @Test
    public void toFloatTest1e_6() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(1, mantissa);
            assertEquals(6, exponent);
        };
        assertTrue(Decimalizer.INSTANCE.toDecimal(1e-6f, check));

        assertTrue(new Decimalizer.MaximumPrecisionOnly(7).toDecimal(1e-6f, check));
        assertTrue(new Decimalizer.MaximumPrecisionOnly(6).toDecimal(1e-6f, check));
        DecimalAppender check0 = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(0, mantissa);
            assertEquals(1, exponent);
        };
        assertTrue(new Decimalizer.MaximumPrecisionOnly(5).toDecimal(1e-6f, check));
    }

    @Test
    public void toFloatTestRounding() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertFalse(negative);
            assertEquals(1, mantissa);
            assertEquals(0, exponent);
        };
        Decimalizer.MaximumPrecisionOnly lp5 = new Decimalizer.MaximumPrecisionOnly(5);
        assertTrue(lp5.toDecimal(1.0000004, check));
        assertTrue(lp5.toDecimal(0.9999996, check));
    }

    @Test
    public void toFloatLiteAndBigDecimal() {
        IntStream.range(0, 100_000)
                .parallel()
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        float d = (float) x / f;
                        assertTrue(Decimalizer.LITE.toDecimal(d, CHECK_OK));

                        // probably requires more precision
                        int l = Float.floatToRawIntBits(d);
                        float d2 = Float.intBitsToFloat(l + x);
                        assertTrue(Decimalizer.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK));
                        f *= 10;
                    }
                });
    }

    @Test
    public void toFloatLarge() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            assertTrue(0 <= exponent);
            assertTrue("exponent: " + exponent, exponent <= 18);
        };
        LongStream.range(-46, 39)
                .forEach(x -> {
                    float f = (float) Math.pow(10, x);
                    float lower = 1e-18f;
                    assertEquals("x: " + x,
                            f == 0 || (lower <= f && f < 1e18),
                            Decimalizer.LITE.toDecimal(f, check));
                });
    }

    @Test
    public void testNegativeValue() {
        Decimalizer.LITE.toDecimal(-3.14f, CHECK_NEG314);
    }

    @Test
    public void testPositive() {
        Decimalizer.LITE.toDecimal(123456.789f, CHECK_123456_789);
    }

    @Test
    public void testPositiveBD() {
        Decimalizer.USES_BIG_DECIMAL.toDecimal(123456.789f, CHECK_123456_789);
    }

    @Test
    public void testNegativePI() {
        Decimalizer.LITE.toDecimal((float) -Math.PI, CHECK_NEG_PI);
    }

    @Test
    public void testNegativePIBD() {
        Decimalizer.USES_BIG_DECIMAL.toDecimal((float) -Math.PI, CHECK_NEG_PI);
    }

    @Test
    public void testZero() {
        Decimalizer.LITE.toDecimal(0.0f, CHECK_ZERO);
    }

    @Test
    public void testZeroBD() {
        Decimalizer.USES_BIG_DECIMAL.toDecimal(0.0f, CHECK_ZERO);
    }

    @Test
    public void testNegZero() {
        Decimalizer.LITE.toDecimal(-0.0f, CHECK_NEG_ZERO);
    }

    @Test
    public void testNegLongMinValueBD() {
        DecimalAppender check = (negative, mantissa, exponent) -> {
            // -9223372036854775808
            assertTrue(negative);
            assertEquals(9223372L, mantissa);
            assertEquals(-12, exponent);
        };
        assertTrue(Decimalizer.USES_BIG_DECIMAL.toDecimal((float) Long.MIN_VALUE, check));
    }
}
