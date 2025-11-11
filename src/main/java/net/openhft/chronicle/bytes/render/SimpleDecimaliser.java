/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.render;

/**
 * A lightweight {@link Decimaliser} using simple rounding.
 * It attempts to represent numbers with up to {@link #LARGEST_EXPONENT_IN_LONG}
 * decimal places. If the scaled mantissa exceeds {@link #MANTISSA_LIMIT} the
 * method gives up and returns {@code false}.
 *
 * <p>This approach is optimised for speed and is thread-safe.
 * See {@link MaximumPrecision} for a more accurate alternative.
 */
public class SimpleDecimaliser implements Decimaliser {

    /**
     * The largest exponent that can be represented using a long integer. Beyond this value, the Decimaliser gives up.
     */
    public static final int LARGEST_EXPONENT_IN_LONG = 18;

    /**
     * Largest mantissa handled by the simple algorithm before precision is lost.
     */
    static final long MANTISSA_LIMIT = 1_000_000_000_000_000L; // 1e15

    /**
     * A singleton instance of {@link SimpleDecimaliser} for convenient reuse.
     * This instance is thread-safe and can be used across multiple threads without synchronization.
     */
    public static final Decimaliser SIMPLE = new SimpleDecimaliser();

    /**
     * Convert {@code value} using a simple rounding approach and append the result.
     * <p>
     * This method iteratively scales the input value by powers of 10, and performs rounding to attempt finding a precise
     * representation. If such representation is found, it is appended using the provided {@link DecimalAppender}.
     *
     * @param value           the double value to convert; must be finite
     * @param decimalAppender the appender that receives sign, mantissa and exponent
     * @return {@code true} if the value was represented within {@link #MANTISSA_LIMIT}
     *         and {@link #LARGEST_EXPONENT_IN_LONG}
     */
    public boolean toDecimal(double value, DecimalAppender decimalAppender) {
        // Determine if the input value is negative.
        boolean isNegative = Double.doubleToLongBits(value) < 0;
        // Take the absolute value for conversion.
        double absValue = Math.abs(value);

        // Initialize the factor used to scale the input value.
        long factor = 1;
        // Iterate through the exponents to find a precise representation.
        for (int exponent = 0; exponent <= LARGEST_EXPONENT_IN_LONG; exponent++) {
            // Scale and round the value.
            long mantissa = Math.round(absValue * factor);
            // Check if the scaled and rounded value matches the original.
            if ((double) mantissa / factor == absValue) {
                // Append the representation to the decimal appender.
                decimalAppender.append(isNegative, mantissa, exponent);
                return true;
            }
            // this is over the edge of precision
            if (mantissa >= MANTISSA_LIMIT)
                return false;
            // Increase the factor for the next iteration.
            factor *= 10;
        }
        return false;
    }

    /**
     * Convert {@code value} using a simple rounding approach and append the result.
     *
     * @param value           the float value to convert; must be finite
     * @param decimalAppender the appender that receives sign, mantissa and exponent
     * @return {@code true} if the value was represented within {@link #LARGEST_EXPONENT_IN_LONG}
     */
    public boolean toDecimal(float value, DecimalAppender decimalAppender) {
        // Determine if the input value is negative.
        boolean sign = Float.floatToRawIntBits(value) < 0;
        // Take the absolute value for conversion.
        float absValue = Math.abs(value);

        // Initialize the factor used to scale the input value.
        long factor = 1;
        // Iterate through the exponents to find a precise representation.
        for (int exponent = 0; exponent <= LARGEST_EXPONENT_IN_LONG; exponent++) {
            // Scale and round the value.
            long mantissa = Math.round(absValue * (double) factor);
            // Check if the scaled and rounded value matches the original.
            if ((float) mantissa / factor == absValue) {
                // Append the representation to the decimal appender.
                decimalAppender.append(sign, mantissa, exponent);
                return true;
            }
            if (mantissa >= MANTISSA_LIMIT)
                return false;
            // Increase the factor for the next iteration.
            factor *= 10;
        }
        return false;
    }
}
