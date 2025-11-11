/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.render;

/**
 * Decimaliser that rounds values to a fixed number of decimal places.
 *
 * <p> For example a precision of two converts {@code 1.238} to {@code 1.24}.
 * Trailing zeros introduced by rounding are trimmed.
 */
public class MaximumPrecision implements Decimaliser {

    private final int precision;

    /**
     * Create an instance with the given precision.
     *
     * @param precision number of decimal places, 0-18 inclusive
     * @throws IllegalArgumentException if {@code precision} is outside that range
     */
    public MaximumPrecision(int precision) {
        if (precision < 0 || precision > 18) {
            throw new IllegalArgumentException("Precision must be between 0 and 18, inclusive.");
        }
        this.precision = precision;
    }

    /**
     * Convert {@code value} rounding to at most {@code precision} decimal places.
     *
     * @param value           the double to convert; must be finite
     * @param decimalAppender the appender receiving the components
     * @return {@code true} if the value was in range
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
     * Convert {@code value} rounding to at most {@code precision} decimal places.
     *
     * @param value           the float to convert; must be finite
     * @param decimalAppender the appender receiving the components
     * @return {@code true} if the value was in range
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
