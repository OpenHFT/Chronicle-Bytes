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
 * Interface for converting floating-point values (double and float) into a decimal representation
 * which is suitable for appending. Multiple strategies are provided for the conversion process
 * including a light-weight and a BigDecimal based implementation.
 */
public interface Decimalizer {

    /**
     * Default instance of Decimalizer which combines both the light-weight and BigDecimal-based strategies.
     */
    Decimalizer INSTANCE = new Instance();

    /**
     * Light-weight instance of Decimalizer, which uses a simple rounding approach for the conversion.
     */
    Decimalizer LITE = new Decimalizer.Lite();

    /**
     * Decimalizer instance that utilizes BigDecimal for higher precision conversion.
     */
    Decimalizer USES_BIG_DECIMAL = new Decimalizer.UsesBigDecimal();

    /**
     * Converts a double value to its decimal representation and appends it using the provided DecimalAppender.
     *
     * @param value           the double value to be converted.
     * @param decimalAppender the DecimalAppender used to store the converted decimal value.
     * @return true if the conversion and appending was successful, false otherwise.
     */
    boolean toDecimal(double value, DecimalAppender decimalAppender);

    /**
     * Converts a float value to its decimal representation and appends it using the provided DecimalAppender.
     *
     * @param value           the float value to be converted.
     * @param decimalAppender the DecimalAppender used to store the converted decimal value.
     * @return true if the conversion and appending was successful, false otherwise.
     */
    boolean toDecimal(float value, DecimalAppender decimalAppender);

    /**
     * The default implementation of Decimalizer which attempts the conversion using the light-weight strategy first,
     * falling back to the BigDecimal-based strategy if necessary.
     */
    class Instance implements Decimalizer {
        @Override
        public boolean toDecimal(double value, DecimalAppender decimalAppender) {
            return LITE.toDecimal(value, decimalAppender)
                    || USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
        }


        @Override
        public boolean toDecimal(float value, DecimalAppender decimalAppender) {
            return LITE.toDecimal(value, decimalAppender)
                    || USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
        }
    }

    /**
     * Light-weight implementation of Decimalizer, which uses a simple rounding approach for conversion.
     */
    class Lite implements Decimalizer {

        public static final int LARGEST_EXPONENT_IN_LONG = 18;

        /**
         * Converts a double value to its decimal representation using a simple rounding approach and appends it.
         *
         * @param value           the double value to be converted.
         * @param decimalAppender the DecimalAppender used to store the converted decimal value.
         * @return true if the conversion and appending was successful, false otherwise.
         */
        public boolean toDecimal(double value, DecimalAppender decimalAppender) {
            boolean isNegative = Double.doubleToLongBits(value) < 0;
            double absValue = Math.abs(value);

            long factor = 1;
            for (int exponent = 0; exponent <= LARGEST_EXPONENT_IN_LONG; exponent++) {
                long mantissa = Math.round(absValue * factor);
                if ((double) mantissa / factor == absValue) {
                    decimalAppender.append(isNegative, mantissa, exponent);
                    return true;
                }
                factor *= 10;
            }
            return false;
        }

        /**
         * Converts a float value to its decimal representation using a simple rounding approach and appends it.
         *
         * @param value           the float value to be converted.
         * @param decimalAppender the DecimalAppender used to store the converted decimal value.
         * @return true if the conversion and appending was successful, false otherwise.
         */
        public boolean toDecimal(float value, DecimalAppender decimalAppender) {
            boolean sign = Double.doubleToLongBits(value) < 0;
            float absValue = Math.abs(value);

            long factor = 1;
            for (int exponent = 0; exponent <= LARGEST_EXPONENT_IN_LONG; exponent++) {
                long mantissa = Math.round(absValue * (double) factor);
                if ((float) mantissa / factor == absValue) {
                    decimalAppender.append(sign, mantissa, exponent);
                    return true;
                }
                factor *= 10;
            }
            return false;
        }
    }

    /**
     * BigDecimal-based implementation of Decimalizer for higher precision conversions.
     */
    class UsesBigDecimal implements Decimalizer {

        /**
         * Field reference to the internal storage of BigDecimal values.
         */
        static final java.lang.reflect.Field INT_COMPACT = Jvm.getFieldOrNull(BigDecimal.class, "intCompact");

        /**
         * Converts a double value to its decimal representation using BigDecimal and appends it.
         *
         * @param value           the double value to be converted.
         * @param decimalAppender the DecimalAppender used to store the converted decimal value.
         * @return true if the conversion and appending was successful, false otherwise.
         */
        public boolean toDecimal(double value, DecimalAppender decimalAppender) {
            double abs = Math.abs(value);
            if (!Double.isFinite(value) || abs < 1e-30 || abs > 1e46)
                return false;
            BigDecimal bd = BigDecimal.valueOf(value);
            int exp = bd.scale();
            try {
                if (INT_COMPACT == null) {
                    // shouldn't happen unless the JVM is implemented differently.
                    BigInteger bi = bd.unscaledValue();
                    long l = bi.longValueExact();
                    decimalAppender.append(l < 0, Math.abs(l), exp);
                    return true;

                } else {
                    long l = INT_COMPACT.getLong(bd);
                    if (l != Long.MIN_VALUE) {
                        decimalAppender.append(l < 0, Math.abs(l), exp);
                        return true;
                    }
                }
            } catch (ArithmeticException | IllegalAccessException ae) {
                // fall back
            }
            return false;
        }

        /**
         * Converts a float value to its decimal representation using BigDecimal and appends it.
         *
         * @param value           the float value to be converted.
         * @param decimalAppender the DecimalAppender used to store the converted decimal value.
         * @return true if the conversion and appending was successful, false otherwise.
         */
        public boolean toDecimal(float value, DecimalAppender decimalAppender) {
            if (!Float.isFinite(value))
                return false;
            BigDecimal bd = new BigDecimal(Float.toString(value));
            int exp = bd.scale();
            try {
                if (INT_COMPACT == null) {
                    // shouldn't happen unless the JVM is implemented differently.
                    BigInteger bi = bd.unscaledValue();
                    long l = bi.longValueExact();
                    decimalAppender.append(l < 0, Math.abs(l), exp);
                    return true;

                } else {
                    long l = INT_COMPACT.getLong(bd);
                    if (l != Long.MIN_VALUE) {
                        decimalAppender.append(l < 0, Math.abs(l), exp);
                        return true;
                    }

                }
            } catch (ArithmeticException | IllegalAccessException ae) {
                // fall back
            }
            return false;
        }
    }

}
