/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.render;

/**
 * Default decimaliser that attempts {@link MaximumPrecision} with precision 18
 * and falls back to {@link UsesBigDecimal} for large numbers.
 */
public class StandardDecimaliser implements Decimaliser {

    /**
     * Singleton instance of StandardDecimaliser.
     */
    public static final StandardDecimaliser STANDARD = new StandardDecimaliser();

    /**
     * Initial strategy rounding to eighteen decimal places.
     */
    static final MaximumPrecision PRECISION_18 = new MaximumPrecision(18);

    /**
     * Convert {@code value} using {@link #PRECISION_18} then {@link UsesBigDecimal}.
     */
    @Override
    public boolean toDecimal(double value, DecimalAppender decimalAppender) {
        // Tries to convert using MaximumPrecision first, then falls back to UsesBigDecimal.
        return PRECISION_18.toDecimal(value, decimalAppender)
                || UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
    }

    /**
     * Convert {@code value} using {@link #PRECISION_18} then {@link UsesBigDecimal}.
     */
    @Override
    public boolean toDecimal(float value, DecimalAppender decimalAppender) {
        // Tries to convert using MaximumPrecision first, then falls back to UsesBigDecimal.
        return PRECISION_18.toDecimal(value, decimalAppender)
                || UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
    }
}
