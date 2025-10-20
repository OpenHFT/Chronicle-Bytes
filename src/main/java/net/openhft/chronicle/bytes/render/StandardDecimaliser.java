/*
 * Copyright 2016-2025 chronicle.software
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
