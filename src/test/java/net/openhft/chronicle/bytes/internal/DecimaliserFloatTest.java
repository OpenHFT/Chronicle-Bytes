package net.openhft.chronicle.bytes.internal;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.Assert.*;

class DecimaliserFloatTest {

    public static final DecimalAppender CHECK_OK = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            // ok
        }

        @Override
        public void appendHighPrecision(float d) {
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
        public void appendHighPrecision(float d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final DecimalAppender CHECK_123456_789 = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            assertFalse(negative);
            assertEquals(12345679L, mantissa);
            assertEquals(2, exponent);
            assertEquals(123456.79f, mantissa / 1e2f, 0.0f);
        }

        @Override
        public void appendHighPrecision(float d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final DecimalAppender CHECK_NEG_PI = new DecimalAppender() {
        @Override
        public void append(boolean negative, long mantissa, int exponent) {
            assertTrue(negative);
            assertEquals(31415927, mantissa);
            assertEquals(7, exponent);
            assertEquals((float) Math.PI, mantissa / 1e7f, 0.0f);
        }

        @Override
        public void appendHighPrecision(float d) {
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
        public void appendHighPrecision(float d) {
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
        public void appendHighPrecision(float d) {
            fail("Unable to decimalise " + d);
        }
    };
    public static final float HARD_TO_DECIMALISE = 4.8846945805332034E-12f;

    @Test
    public void toFloatTestTest() {
        try {
            Decimalizer.LITE.toDecimal(HARD_TO_DECIMALISE, CHECK_OK);
            fail("Test the test failed");
        } catch (AssertionError e) {
            assertEquals("Unable to decimalise " + HARD_TO_DECIMALISE, e.getMessage());
        }
    }

    @Test
    public void toFloatTest() {
        DecimalAppender check = new DecimalAppender() {
            @Override
            public void append(boolean negative, long mantissa, int exponent) {
                assertFalse(negative);
                assertEquals(48846946, mantissa);
                assertEquals(19, exponent);
            }

            @Override
            public void appendHighPrecision(float d) {
                fail("Unable to decimalise " + d);
            }
        };
        Decimalizer.INSTANCE.toDecimal(HARD_TO_DECIMALISE, check);
    }

    @Test
    public void toFloatLiteAndBigDecimal() {
        IntStream.range(0, 1_000_000)
                .parallel()
                .forEach(x -> {
                    long f = 1;
                    for (int i = 0; i <= 18; i++) {
                        // simple decimal is ok
                        float d = (float) x / f;
                        Decimalizer.LITE.toDecimal(d, CHECK_OK);

                        // probably requires more precision
                        int l = Float.floatToRawIntBits(d);
                        float d2 = Float.intBitsToFloat(l + x);
                        Decimalizer.USES_BIG_DECIMAL.toDecimal(d2, CHECK_OK);
                        f *= 10;
                    }
                });
    }

    @Test
    public void toFloatLarge() {
        DecimalAppender check = new DecimalAppender() {
            @Override
            public void append(boolean negative, long mantissa, int exponent) {
                assertTrue(0 <= exponent);
                assertTrue(exponent <= 18);
            }

            @Override
            public void appendHighPrecision(float d) {
                assertFalse("Unexpected " + d, 1e-18 <= d && d < 1e18);
            }
        };
        LongStream.range(-39, 39)
                .forEach(x -> {
                    float f = (float) Math.pow(10, x);
                    Decimalizer.INSTANCE.toDecimal(f, check);
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
        DecimalAppender check = new DecimalAppender() {
            @Override
            public void append(boolean negative, long mantissa, int exponent) {
                // -9223372036854775808
                assertTrue(negative);
                assertEquals(9223372L, mantissa);
                assertEquals(-12, exponent);
            }

            @Override
            public void appendHighPrecision(float d) {
                fail("Unable to decimalise " + d);
            }
        };
        Decimalizer.USES_BIG_DECIMAL.toDecimal((float) Long.MIN_VALUE, check);
    }

    @Test
    public void testVerySmallValue() {
        DecimalAppender check = new DecimalAppender() {
            @Override
            public void append(boolean negative, long mantissa, int exponent) {
                fail("Expected high precision, but received normal precision.");
            }

            @Override
            public void appendHighPrecision(float d) {
                // ok, expecting high precision call
                assertEquals(1e-20f, d, 0.0f);
            }
        };
        Decimalizer.INSTANCE.toDecimal(1e-20f, check);
    }
}
