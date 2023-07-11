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

import java.math.BigDecimal;

/**
 * A versatile implementation of {@link Decimaliser} which employs a hybrid approach to convert floating-point numbers
 * to decimal representation. Initially, it attempts a lightweight conversion strategy. If that fails or is unsuitable
 * due to the number's magnitude, it then falls back on a {@link BigDecimal}-based strategy for higher precision.
 * <p>
 * This implementation is designed to achieve a balance between performance and precision. It is particularly well-suited
 * for converting numbers that are usually within a certain range, but occasionally can be very large or have a high degree
 * of precision.
 * <p>
 * Note: Numbers are represented in scientific notation if they are 1e45 or larger, or 1e-29 or smaller, to maintain compactness.
 */
public class GeneralDecimaliser implements Decimaliser {

    /**
     * A singleton instance of {@link GeneralDecimaliser} for convenient reuse.
     * This instance combines both lightweight and {@link BigDecimal}-based conversion strategies.
     * It is thread-safe and can be used across multiple threads without synchronization.
     */
    public static final Decimaliser GENERAL = new GeneralDecimaliser();

    /**
     * Converts a double value to its decimal representation by first trying a lightweight approach and then,
     * if necessary, falling back to a {@link BigDecimal}-based approach for higher precision. Appends the result
     * to the provided {@link DecimalAppender}.
     * <p>
     * The conversion is attempted if the absolute value is 0, or if it's in the range of 1e-29 to 1e45 (both inclusive).
     *
     * @param value           The double value to be converted.
     * @param decimalAppender The {@link DecimalAppender} to which the converted decimal value is appended.
     * @return {@code true} if the conversion and appending were successful, {@code false} otherwise.
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
     * Converts a float value to its decimal representation by first trying a lightweight approach and then,
     * if necessary, falling back to a {@link BigDecimal}-based approach for higher precision. Appends the result
     * to the provided {@link DecimalAppender}.
     * <p>
     * The conversion is attempted if the absolute value is 0, or if it's equal to or larger than 1e-29f.
     *
     * @param value           The float value to be converted.
     * @param decimalAppender The {@link DecimalAppender} to which the converted decimal value is appended.
     * @return {@code true} if the conversion and appending were successful, {@code false} otherwise.
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
