/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.render;

/**
 * Default decimaliser that attempts {@link MaximumPrecision} with precision 18
 * and falls back to {@link UsesBigDecimal} for large numbers.
 */
@SuppressWarnings("deprecation")
public class StandardDecimaliser implements Decimaliser {

    /**
     * Singleton instance of StandardDecimaliser.
     */
    public static final StandardDecimaliser STANDARD = new StandardDecimaliser();

    /**
     * Creates the default decimaliser combining maximum precision and BigDecimal fallback.
     */
    public StandardDecimaliser() {
        // default
    }

    /**
     * Initial strategy rounding to eighteen decimal places.
     */
    static final MaximumPrecision PRECISION_18 = new MaximumPrecision(18);

    /**
     * Convert {@code value} using {@link #PRECISION_18} then {@link UsesBigDecimal}.
     */
    @Override
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public boolean toDecimal(double value, DecimalAppender decimalAppender) {
        // Tries to convert using MaximumPrecision first, then falls back to UsesBigDecimal.
        return PRECISION_18.toDecimal(value, decimalAppender)
                || UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
    }

    /**
     * Convert {@code value} using {@link #PRECISION_18} then {@link UsesBigDecimal}.
     */
    @Override
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public boolean toDecimal(float value, DecimalAppender decimalAppender) {
        // Tries to convert using MaximumPrecision first, then falls back to UsesBigDecimal.
        return PRECISION_18.toDecimal(value, decimalAppender)
                || UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
    }
}
