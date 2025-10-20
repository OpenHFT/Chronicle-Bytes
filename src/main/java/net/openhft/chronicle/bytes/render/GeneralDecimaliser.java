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
