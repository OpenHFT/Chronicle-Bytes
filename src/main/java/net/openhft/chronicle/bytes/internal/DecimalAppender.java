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
package net.openhft.chronicle.bytes.internal;
/**
 * Interface representing a handler for decimal numbers, allowing for the appending of decimal values in various forms.
 * Implementations may decide how to handle the components of the decimal number (sign, mantissa, and exponent).
 * Additionally, there is support for handling high-precision numbers separately.
 */
public interface DecimalAppender {

    /**
     * Appends a decimal number represented by its sign, mantissa, and exponent components.
     *
     * @param isNegative Whether the number is negative; true if negative, false if positive.
     * @param mantissa   The significant digits of the decimal number.
     * @param exponent   The power of 10 by which the mantissa should be multiplied to obtain the actual number.
     */
    void append(boolean isNegative, long mantissa, int exponent);

    /**
     * Appends a high-precision double value. This method should be overridden if high-precision handling is required.
     * It is called when the Decimalizer fails to convert the double into sign, mantissa, and exponent components.
     *
     * @param value The high-precision double value to be appended.
     * @throws UnsupportedOperationException if this operation is not supported by the implementing class.
     */
    default void appendHighPrecision(double value) {
        throw new UnsupportedOperationException("High-precision double appending is not supported. Value: " + value);
    }

    /**
     * Appends a high-precision float value. This method should be overridden if high-precision handling is required.
     * It is called when the Decimalizer fails to convert the float into sign, mantissa, and exponent components.
     *
     * @param value The high-precision float value to be appended.
     * @throws UnsupportedOperationException if this operation is not supported by the implementing class.
     */
    default void appendHighPrecision(float value) {
        throw new UnsupportedOperationException("High-precision float appending is not supported. Value: " + value);
    }
}
