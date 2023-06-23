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
 * Interface representing a DecimalAppender, which is responsible for appending decimal values.
 * The implementation can decide how to handle negative numbers, mantissa and exponent.
 * Additionally, high-precision numbers can be handled separately through the default methods.
 */
public interface DecimalAppender {

    /**
     * Appends a decimal number broken down into its sign, mantissa, and exponent components.
     *
     * @param negative Indicates if the number is negative (true if negative, false if positive).
     * @param mantissa The mantissa of the decimal number.
     * @param exponent The exponent of the decimal number. It represents the power of 10 by which the mantissa should be multiplied.
     */
    void append(boolean negative, long mantissa, int exponent);

    /**
     * Appends a high precision double value.
     * This is called when the Decimalizer fails to turn the double into a sign/mantissa/exponent
     *
     * @param d The high precision double value to be appended.
     * @throws UnsupportedOperationException if not overridden by implementing class.
     */
    default void appendHighPrecision(double d) {
        throw new UnsupportedOperationException("d: " + d);
    }

    /**
     * Appends a high precision float value.
     * This is called when the Decimalizer fails to turn the double into a sign/mantissa/exponent
     *
     * @param f The high precision float value to be appended.
     * @throws UnsupportedOperationException if not overridden by implementing class.
     */
    default void appendHighPrecision(float f) {
        throw new UnsupportedOperationException("f: " + f);
    }
}
