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
     * @param value           the double value to be converted.
     * @param decimalAppender the appender used to store the converted decimal value.
     */
    void toDecimal(double value, DecimalAppender decimalAppender);

    /**
     * Converts a float value to its decimal representation.
     *
     * @param value           the float value to be converted.
     * @param decimalAppender the appender used to store the converted decimal value.
     */
    void toDecimal(float value, DecimalAppender decimalAppender);

    /**
     * Default implementation of Decimalizer.
     */
    class Instance implements Decimalizer {

        public static final double LARGEST_POW10_IN_LONG = 1e18;
        public static final double INV_LARGEST_POWRE10_IN_LONG = Jvm.isArm() ? 1e-17 : 1e-18;

        @Override
        public void toDecimal(double value, DecimalAppender decimalAppender) {
            double abs = Math.abs(value);

            // Revert to fail-safe writing for very large or small values.
            if (!(INV_LARGEST_POWRE10_IN_LONG <= abs && abs < LARGEST_POW10_IN_LONG)) {
                decimalAppender.appendHighPrecision(value);
                return;
            }

            // assume it's probably easily decimalized
            LITE.toDecimal(value, new DecimalAppender() {
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
        public void toDecimal(float value, DecimalAppender decimalAppender) {
            float abs = Math.abs(value);

            // Revert to fail-safe writing for very large or small values.
            if (!(INV_LARGEST_POWRE10_IN_LONG <= abs && abs < LARGEST_POW10_IN_LONG)) {
                decimalAppender.appendHighPrecision(value);
                return;
            }

            // assume it's probably easily decimalized
            LITE.toDecimal(value, new DecimalAppender() {
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
         * @param value           the double value to be converted.
         * @param decimalAppender the appender used to store the converted decimal value.
         */
        public void toDecimal(double value, DecimalAppender decimalAppender) {
            boolean isNegative = Double.doubleToLongBits(value) < 0;
            double absValue = Math.abs(value);

            long factor = 1;
            for (int exponent = 0; exponent <= LARGEST_EXPONENT_IN_LONG; exponent++) {
                long mantissa = Math.round(absValue * factor);
                if ((double) mantissa / factor == absValue) {
                    decimalAppender.append(isNegative, mantissa, exponent);
                    return;
                }
                factor *= 10;
            }
            decimalAppender.appendHighPrecision(value);
        }

        /**
         * Converts a float value to its decimal representation using a simple rounding approach.
         *
         * @param value           the double value to be converted.
         * @param decimalAppender the appender used to store the converted decimal value.
         */
        public void toDecimal(float value, DecimalAppender decimalAppender) {
            boolean sign = Double.doubleToLongBits(value) < 0;
            float absValue = Math.abs(value);

            long factor = 1;
            for (int exponent = 0; exponent <= LARGEST_EXPONENT_IN_LONG; exponent++) {
                long mantissa = Math.round(absValue * (double) factor);
                if ((float) mantissa / factor == absValue) {
                    decimalAppender.append(sign, mantissa, exponent);
                    return;
                }
                factor *= 10;
            }
            decimalAppender.appendHighPrecision(value);
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
         * @param value           the double value to be converted.
         * @param decimalAppender the appender used to store the converted decimal value.
         */
        public void toDecimal(double value, DecimalAppender decimalAppender) {
            BigDecimal bd = BigDecimal.valueOf(value);
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
                        decimalAppender.appendHighPrecision(value); // appendHighPrecision(double)
                    else
                        decimalAppender.append(l < 0, Math.abs(l), exp);
                }
            } catch (ArithmeticException | IllegalAccessException ae) {
                decimalAppender.appendHighPrecision(value);
            }
        }

        /**
         * Converts a float value to its decimal representation using BigDecimal.
         *
         * @param value           the double value to be converted.
         * @param decimalAppender the appender used to store the converted decimal value.
         */
        public void toDecimal(float value, DecimalAppender decimalAppender) {
            BigDecimal bd = new BigDecimal(Float.toString(value));
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
                        decimalAppender.appendHighPrecision(value); // appendHighPrecision(float)
                    else
                        decimalAppender.append(l < 0, Math.abs(l), exp);
                }
            } catch (ArithmeticException | IllegalAccessException ae) {
                decimalAppender.appendHighPrecision(value);
            }
        }
    }

}
