/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.render;

import java.math.BigDecimal;

/**
 * Decimaliser that first tries {@link SimpleDecimaliser} and then
 * {@link UsesBigDecimal} if more precision is needed.
 * Values outside the range [1e-29, 1e45) are rejected.
 */
public class GeneralDecimaliser implements Decimaliser {

    /**
     * Preferred entry point combining lightweight and {@link BigDecimal} strategies.
     * This singleton is stateless and thread-safe.
     */
    public static final Decimaliser GENERAL = new GeneralDecimaliser();

    /**
     * Convert {@code value} using the simple decimaliser then fall back to
     * {@link UsesBigDecimal} if necessary.
     * Values outside [1e-29, 1e45) are rejected.
     */
    @Override
    public boolean toDecimal(double value, DecimalAppender decimalAppender) {
        double absValue = Math.abs(value);

        // Check if the absolute value is 0 or in the valid range
        if (value == 0 || (1e-29 <= absValue && absValue < 1e45)) {
            // First try the lightweight approach, if that fails use the BigDecimal-based approach
            return SimpleDecimaliser.SIMPLE.toDecimal(value, decimalAppender)
                    || UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
        }

        // If the value is outside the valid range, return false
        return false;
    }

    /**
     * Convert {@code value} using the simple decimaliser then fall back to
     * {@link UsesBigDecimal} if necessary. Values with absolute value below
     * {@code 1e-29f} are rejected.
     */
    @Override
    public boolean toDecimal(float value, DecimalAppender decimalAppender) {
        float absValue = Math.abs(value);

        // Check if the absolute value is 0 or equal to or larger than the threshold
        if (value == 0 || 1e-29f <= absValue) {
            // First try the lightweight approach, if that fails use the BigDecimal-based approach
            return SimpleDecimaliser.SIMPLE.toDecimal(value, decimalAppender)
                    || UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
        }

        // If the value is smaller than the threshold, return false
        return false;
    }
}
