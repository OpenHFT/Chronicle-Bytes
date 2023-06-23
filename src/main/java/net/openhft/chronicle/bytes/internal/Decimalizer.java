package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.Jvm;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Interface for converting double values to their decimal representation.
 */
public interface Decimalizer {

    /**
     * Default instance of Decimalizer.
     */
    Decimalizer INSTANCE = new Instance();

    /**
     * Light-weight instance of Decimalizer.
     */
    Decimalizer LITE = new Decimalizer.Lite();

    /**
     * Decimalizer instance that utilizes BigDecimal for conversion.
     */
    Decimalizer USES_BIG_DECIMAL = new Decimalizer.UsesBigDecimal();

    /**
     * Converts a double value to its decimal representation.
     *
     * @param d                the double value to be converted.
     * @param decimalAppender  the appender used to store the converted decimal value.
     */
    void toDecimal(double d, DecimalAppender decimalAppender);

    /**
     * Converts a double value to its decimal representation.
     *
     * @param f                the double value to be converted.
     * @param decimalAppender  the appender used to store the converted decimal value.
     */
    void toDecimal(float f, DecimalAppender decimalAppender);

    /**
     * Default implementation of Decimalizer.
     */
    class Instance implements Decimalizer {

        public static final double LARGEST_POW10_IN_LONG = 1e18;
        public static final double INV_LARGEST_POWRE10_IN_LONG = 1e-18;

        @Override
        public void toDecimal(double d, DecimalAppender decimalAppender) {
            // if very large or small reverse to fail-safe writing
            double abs = Math.abs(d);
            if (!(INV_LARGEST_POWRE10_IN_LONG <= abs && abs < LARGEST_POW10_IN_LONG)) {
                decimalAppender.appendHighPrecision(d);
                return;
            }

            // assume it's probably easily decimalized
            LITE.toDecimal(d, new DecimalAppender() {
                @Override
                public void append(boolean negative, long mantissa, int exponent) {
                    decimalAppender.append(negative, mantissa, exponent);
                }

                @Override
                public void appendHighPrecision(double d) {
                    // or try using BigDecimal
                    USES_BIG_DECIMAL.toDecimal(d, decimalAppender);
                }
            });
        }


        @Override
        public void toDecimal(float f, DecimalAppender decimalAppender) {
            // if very large or small reverse to fail-safe writing
            float abs = Math.abs(f);
            if (!(INV_LARGEST_POWRE10_IN_LONG <= abs && abs < LARGEST_POW10_IN_LONG)) {
                decimalAppender.appendHighPrecision(f);
                return;
            }

            // assume it's probably easily decimalized
            LITE.toDecimal(f, new DecimalAppender() {
                @Override
                public void append(boolean negative, long mantissa, int exponent) {
                    decimalAppender.append(negative, mantissa, exponent);
                }

                @Override
                public void appendHighPrecision(float f) {
                    // or try using BigDecimal
                    USES_BIG_DECIMAL.toDecimal(f, decimalAppender);
                }
            });
        }
    }

    /**
     * Light-weight implementation of Decimalizer.
     */
    class Lite implements Decimalizer {

        public static final int LARGEST_EXPONENT_IN_LONG = 18;

        /**
         * Converts a double value to its decimal representation using a simple rounding approach.
         *
         * @param d                the double value to be converted.
         * @param decimalAppender  the appender used to store the converted decimal value.
         */
        public void toDecimal(double d, DecimalAppender decimalAppender) {
            boolean sign = Double.doubleToLongBits(d) < 0;
            d = Math.abs(d);
            if (d == 0.0) {
                decimalAppender.append(sign, 0, 1);
                return;
            }
            long factor = 1;
            for (int exp = 0; exp <= LARGEST_EXPONENT_IN_LONG; exp++) {
                long num = Math.round(d * factor);
                if ((double) num / factor == d) {
                    decimalAppender.append(sign, num, exp);
                    return;
                }
                factor *= 10;
            }
            decimalAppender.appendHighPrecision(d);
        }

        /**
         * Converts a float value to its decimal representation using a simple rounding approach.
         *
         * @param f                the double value to be converted.
         * @param decimalAppender  the appender used to store the converted decimal value.
         */
        public void toDecimal(float f, DecimalAppender decimalAppender) {
            boolean sign = Double.doubleToLongBits(f) < 0;
            f = Math.abs(f);
            if (f == 0.0) {
                decimalAppender.append(sign, 0, 1);
                return;
            }
            long factor = 1;
            for (int exp = 0; exp <= LARGEST_EXPONENT_IN_LONG; exp++) {
                long num = Math.round(f * (double) factor);
                if ((float) num / factor == f) {
                    decimalAppender.append(sign, num, exp);
                    return;
                }
                factor *= 10;
            }
            decimalAppender.appendHighPrecision(f);
        }
    }

    /**
     * BigDecimal based implementation of Decimalizer.
     */
    class UsesBigDecimal implements Decimalizer {

        /**
         * Field reference to the internal storage of BigDecimal values.
         */
        static final java.lang.reflect.Field INT_COMPACT = Jvm.getFieldOrNull(BigDecimal.class, "intCompact");

        /**
         * Converts a double value to its decimal representation using BigDecimal.
         *
         * @param d                the double value to be converted.
         * @param decimalAppender  the appender used to store the converted decimal value.
         */
        public void toDecimal(double d, DecimalAppender decimalAppender) {
            BigDecimal bd = BigDecimal.valueOf(d);
            int exp = bd.scale();
            try {
                if (INT_COMPACT == null) {
                    // shouldn't happen unless the JVM is implemented differently.
                    BigInteger bi = bd.unscaledValue();
                    long l = bi.longValueExact();
                    decimalAppender.append(l < 0, Math.abs(l), exp);
                } else {
                    long l = INT_COMPACT.getLong(bd);
                    if (l == Long.MIN_VALUE)
                        decimalAppender.appendHighPrecision(d);
                    else
                        decimalAppender.append(l < 0, Math.abs(l), exp);
                }
            } catch (ArithmeticException | IllegalAccessException ae) {
                decimalAppender.appendHighPrecision(d);
            }
        }

        /**
         * Converts a float value to its decimal representation using BigDecimal.
         *
         * @param f                the double value to be converted.
         * @param decimalAppender  the appender used to store the converted decimal value.
         */
        public void toDecimal(float f, DecimalAppender decimalAppender) {
            BigDecimal bd = new BigDecimal(Float.toString(f));
            int exp = bd.scale();
            try {
                if (INT_COMPACT == null) {
                    // shouldn't happen unless the JVM is implemented differently.
                    BigInteger bi = bd.unscaledValue();
                    long l = bi.longValueExact();
                    decimalAppender.append(l < 0, Math.abs(l), exp);
                } else {
                    long l = INT_COMPACT.getLong(bd);
                    if (l == Long.MIN_VALUE)
                        decimalAppender.appendHighPrecision(f);
                    else
                        decimalAppender.append(l < 0, Math.abs(l), exp);
                }
            } catch (ArithmeticException | IllegalAccessException ae) {
                decimalAppender.appendHighPrecision(f);
            }
        }
    }

}
