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
package net.openhft.chronicle.bytes.render;

/**
 * Implementation of {@link Decimaliser} that converts floating-point numbers to decimal representation with a specified
 * maximum number of decimal places. During conversion, the number of decimal places will be limited to this maximum precision.
 * <p>
 * For example, if a maximum precision of 2 is specified, the number 1.238 will be converted to 1.24.
 */
public class MaximumPrecision implements Decimaliser {

    private final int precision;

    /**
     * Creates a new MaximumPrecision object with the specified maximum precision.
     *
     * @param precision the maximum number of decimal places to be used in the conversion.
     *                  Must be between 0 and 18, inclusive.
     * @throws IllegalArgumentException If the precision is not between 0 and 18.
     */
    public MaximumPrecision(int precision) {
        if (precision < 0 || precision > 18) {
            throw new IllegalArgumentException("Precision must be between 0 and 18, inclusive.");
        }
        this.precision = precision;
    }

    /**
     * Converts a double value to its decimal representation using the specified maximum precision, and appends it
     * to the provided {@link DecimalAppender}.
     *
     * @param value           the double value to be converted.
     * @param decimalAppender the {@link DecimalAppender} used to store and append the converted decimal value.
     * @return true if the conversion and appending were successful, false if the absolute value is greater than 1e18.
     * @throws AssertionError if the conversion fails unexpectedly.
     */
    @Override
    public boolean toDecimal(double value, DecimalAppender decimalAppender) {
        // Determine if the input value is negative.
        boolean isNegative = Double.doubleToLongBits(value) < 0;
        // Take the absolute value for conversion.
        double absValue = Math.abs(value);

        // Ensure the value is not too large for conversion.
        if (!(absValue <= 1e18)) {
            return false;
        }
        // Factor to multiply the value to achieve the desired precision.
        long factor = 1;
        for (int exponent = 0; exponent <= precision; exponent++) {
            // Calculate mantissa based on the current factor.
            long mantissa = Math.round(absValue * factor);
            // Check if rounding is accurate for this precision.
            if ((double) mantissa / factor == absValue) {
                // Append to decimal appender.
                decimalAppender.append(isNegative, mantissa, exponent);
                return true;
            }
            // Reduce precision if mantissa gets too large.
            if (mantissa >= Long.MAX_VALUE / 10 || exponent == precision) {
                while (exponent > 0) {
                    if (mantissa % 10 == 0) {
                        mantissa /= 10;
                        exponent--;
                    } else {
                        break;
                    }
                }
                // Append with reduced precision.
                decimalAppender.append(isNegative, mantissa, exponent);
                return true;
            }
            factor *= 10;
        }
        return false; // shouldn't happen
    }

    /**
     * Converts a float value to its decimal representation using the specified maximum precision, and appends it
     * to the provided {@link DecimalAppender}.
     *
     * @param value           the float value to be converted.
     * @param decimalAppender the {@link DecimalAppender} used to store and append the converted decimal value.
     * @return true if the conversion and appending were successful, false if the absolute value is greater or equal to 1e18.
     * @throws AssertionError if the conversion fails unexpectedly.
     */
    @Override
    public boolean toDecimal(float value, DecimalAppender decimalAppender) {
        // Determine if the input value is negative.
        boolean isNegative = Double.doubleToLongBits(value) < 0;
        // Take the absolute value for conversion.
        float absValue = Math.abs(value);

        // Ensure the value is not too large for conversion.
        if (!(absValue < 1e18)) {
            return false;
        }
        // Factor to multiply the value to achieve the desired precision.
        long factor = 1;
        for (int exponent = 0; exponent <= precision; exponent++) {
            // Calculate mantissa based on the current factor.
            long mantissa = Math.round((double) absValue * factor);
            // Check if rounding is accurate for this precision.
            if ((float) ((double) mantissa / factor) == absValue) {
                // Append to decimal appender.
                decimalAppender.append(isNegative, mantissa, exponent);
                return true;
            }
            // Reduce precision if mantissa gets too large.
            if (mantissa >= Long.MAX_VALUE / 10 || exponent == precision) {
                while (exponent > 0) {
                    if (mantissa % 10 == 0) {
                        mantissa /= 10;
                        exponent--;
                    } else {
                        break;
                    }
                }
                // Append with reduced precision.
                decimalAppender.append(isNegative, mantissa, exponent);
                return true;
            }
            factor *= 10;
        }
        return false; // shouldn't happen
    }
}
