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
 * Implementation of {@link Decimaliser} that converts floating-point numbers to their decimal representation in
 * standard form. This class ensures that large numbers are represented in full, while small values are rounded
 * to a maximum of 18 decimal places.
 * <p>
 * Note: The conversion process first tries to utilize the {@link MaximumPrecision} with a precision of 18,
 * and if unsuccessful, falls back to a {@link UsesBigDecimal} strategy.
 */
public class StandardDecimaliser implements Decimaliser {

    /**
     * Singleton instance of StandardDecimaliser.
     */
    public static final StandardDecimaliser STANDARD = new StandardDecimaliser();

    /**
     * Instance of {@link MaximumPrecision} with a precision of 18, used for the initial attempt at conversion.
     */
    static final MaximumPrecision PRECISION_18 = new MaximumPrecision(18);

    /**
     * Converts a double value to its decimal representation in standard form and appends it using the provided
     * {@link DecimalAppender}. The conversion process will round small values to a maximum of 18 decimal places,
     * while large numbers will be represented in full.
     *
     * @param value           the double value to be converted.
     * @param decimalAppender the {@link DecimalAppender} used to store and append the converted decimal value.
     * @return true if the conversion and appending were successful; false otherwise.
     */
    @Override
    public boolean toDecimal(double value, DecimalAppender decimalAppender) {
        // Tries to convert using MaximumPrecision first, then falls back to UsesBigDecimal.
        return PRECISION_18.toDecimal(value, decimalAppender)
                || UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
    }

    /**
     * Converts a float value to its decimal representation in standard form and appends it using the provided
     * {@link DecimalAppender}. The conversion process will round small values to a maximum of 18 decimal places,
     * while large numbers will be represented in full.
     *
     * @param value           the float value to be converted.
     * @param decimalAppender the {@link DecimalAppender} used to store and append the converted decimal value.
     * @return true if the conversion and appending were successful; false otherwise.
     */
    @Override
    public boolean toDecimal(float value, DecimalAppender decimalAppender) {
        // Tries to convert using MaximumPrecision first, then falls back to UsesBigDecimal.
        return PRECISION_18.toDecimal(value, decimalAppender)
                || UsesBigDecimal.USES_BIG_DECIMAL.toDecimal(value, decimalAppender);
    }
}
