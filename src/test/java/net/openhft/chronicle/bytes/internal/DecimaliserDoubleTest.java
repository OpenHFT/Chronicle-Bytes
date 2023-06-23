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

import java.util.stream.LongStream;

import static org.junit.Assert.*;

class DecimaliserDoubleTest {

    public static final DecimalAppender CHECK_OK = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            // ok
        }

        @Override
        public void appendHighPrecision(double d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final DecimalAppender CHECK_NEG314 = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            assertTrue(negative);
            assertEquals(314, mantissa);
            assertEquals(2, exponent);
        }

        @Override
        public void appendHighPrecision(double d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final DecimalAppender CHECK_123456789_012345 = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            assertFalse(negative);
            assertEquals(123456789012345L, mantissa);
            assertEquals(6, exponent);
            assertEquals(123456789.012345, mantissa / 1e6, 0.0);
        }

        @Override
        public void appendHighPrecision(double d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final DecimalAppender CHECK_NEG_PI = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            assertTrue(negative);
            assertEquals(3141592653589793L, mantissa);
            assertEquals(15, exponent);
            assertEquals(Math.PI, mantissa / 1e15, 0.0);
        }

        @Override
        public void appendHighPrecision(double d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final DecimalAppender CHECK_ZERO = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            assertFalse(negative);
            assertEquals(0, mantissa);
            assertEquals(1, exponent);
        }

        @Override
        public void appendHighPrecision(double d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final DecimalAppender CHECK_NEG_ZERO = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            assertTrue(negative);
            assertEquals(0, mantissa);
            assertEquals(1, exponent);
        }

        @Override
        public void appendHighPrecision(double d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final double HARD_TO_DECIMALISE = 4.8846945805332034E-12;

    @Test
    public void toDoubleTestTest() {
        try {
            Decimalizer.LITE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK);
            fail("Test the test failed");
        } catch (AssertionError e) {
            assertEquals("Unable to decimalise " + HARD_TO_DECIMALISE, e.getMessage());
        }
    }

    @Test
    public void toDoubleTest() {
        DecimalAppender check = new DecimalAppender() {
            @Override
            public void append(boolean negative, long mantissa, int exponent) {
                assertFalse(negative);
                assertEquals(48846945805332034L, mantissa);
                assertEquals(28, exponent);
            }

            @Override
            public void appendHighPrecision(double d) {
                fail("Unable to decimalise " + d);
            }
        };
        Decimalizer.INSTANCE.toDecimal(HARD_TO_DECIMALISE, check);
    }

    @Test
    public void toDoubleLiteAndBigDecimal() {
        LongStream.range(0, 1_000_000L)
                .parallel()
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        double d = (double) x / f;
                        Decimalizer.LITE.toDecimal(d, CHECK_OK);

                        // probably requires more precision
                        long l = Double.doubleToLongBits(d);
                        double d2 = Double.longBitsToDouble(l + x);
                        Decimalizer.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK);
                        f *= 10;
                    }
                });
    }

    @Test
    public void toDoubleLarge() {
        DecimalAppender check = new DecimalAppender() {
            @Override
            public void append(boolean negative, long mantissa, int exponent) {
                assertTrue(0 <= exponent);
                assertTrue(exponent <= 18);
            }

            @Override
            public void appendHighPrecision(double d) {
                assertFalse("Unexpected " + d, 1e-18 <= d && d < 1e18);
            }
        };
        LongStream.range(-325, 309)
                .forEach(x -> {
                    double d = Math.pow(10, x);
                    Decimalizer.INSTANCE.toDecimal(d, check);
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
        DecimalAppender check = new DecimalAppender() {
            @Override
            public void append(boolean negative, long mantissa, int exponent) {
                // -9223372036854775808
                assertTrue(negative);
                assertEquals(9223372036854776L, mantissa);
                assertEquals(-3, exponent);
            }

            @Override
            public void appendHighPrecision(double d) {
                fail("Unable to decimalise " + d);
            }
        };
        Decimalizer.USES_BIG_DECIMAL.toDecimal((double) Long.MIN_VALUE, check);
    }

    @Test
    public void testVerySmallValue() {
        DecimalAppender check = new DecimalAppender() {
            @Override
            public void append(boolean negative, long mantissa, int exponent) {
                fail("Expected high precision, but received normal precision.");
            }

            @Override
            public void appendHighPrecision(double d) {
                // ok, expecting high precision call
                assertEquals(1e-20, d, 0.0);
            }
        };
        Decimalizer.INSTANCE.toDecimal(1e-20, check);
    }
}
