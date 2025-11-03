/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.render;

/**
 * Strategy interface that decomposes a floating point number into sign,
 * mantissa and exponent suitable for textual serialisation.
 * Implementations may trade performance against precision.
 *
 * <p>NaN, infinity and negative zero values are not converted and should cause
 * the method to return {@code false}.
 *
 * @see DecimalAppender
 */
public interface Decimaliser {

    /**
     * Convert {@code value} to a decimal representation and append it.
     *
     * @param value           the double value to serialise; must be finite and not negative zero
     * @param decimalAppender the target receiving sign, mantissa and exponent
     * @return {@code true} if the value could be represented and appended
     */
    boolean toDecimal(double value, DecimalAppender decimalAppender);

    /**
     * Convert {@code value} to a decimal representation and append it.
     *
     * @param value           the float value to serialise; must be finite and not negative zero
     * @param decimalAppender the target receiving sign, mantissa and exponent
     * @return {@code true} if the value could be represented and appended
     */
    boolean toDecimal(float value, DecimalAppender decimalAppender);
}
