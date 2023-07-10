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
 * Provides functionality for converting floating-point values (double and float) into a
 * decimal representation that can be appended to a target.
 * <p>
 * This interface offers multiple strategies for converting floating-point numbers, including
 * a light-weight approach and a more precise, BigDecimal-based implementation.
 *
 * @see DecimalAppender
 */
public interface Decimaliser {

    /**
     * Converts a double value to its decimal representation and appends it using the provided DecimalAppender.
     * <p>
     * The DecimalAppender should have been implemented to accept the sign, mantissa, and exponent of the decimal representation,
     * and append them accordingly.
     *
     * @param value           The double value to be converted.
     * @param decimalAppender The DecimalAppender used to store and append the converted decimal value.
     * @return {@code true} if the conversion and appending were successful, {@code false} otherwise.
     */
    boolean toDecimal(double value, DecimalAppender decimalAppender);

    /**
     * Converts a float value to its decimal representation and appends it using the provided DecimalAppender.
     * <p>
     * The DecimalAppender should have been implemented to accept the sign, mantissa, and exponent of the decimal representation,
     * and append them accordingly.
     *
     * @param value           The float value to be converted.
     * @param decimalAppender The DecimalAppender used to store and append the converted decimal value.
     * @return {@code true} if the conversion and appending were successful, {@code false} otherwise.
     */
    boolean toDecimal(float value, DecimalAppender decimalAppender);

}
