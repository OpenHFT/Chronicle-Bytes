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
 * This interface represents a handler for decimal numbers, and defines how they should be appended in various forms.
 * Implementations of this interface should provide strategies for handling the individual components of a decimal number,
 * including its sign, mantissa, and exponent.
 * <p>
 * A decimal number can be represented as: <code>decimal = (isNegative ? -1 : +1) * mantissa * 10^-exponent</code>, where mantissa contains the
 * significant digits and the exponent scales the number by a power of ten. The sign denotes whether the decimal is positive or negative.
 */
@FunctionalInterface
public interface DecimalAppender {

    /**
     * Appends a decimal number represented by its sign, mantissa, and exponent to a target.
     *
     * @param isNegative Whether the number is negative; true indicates that the number is negative, false indicates positive.
     * @param mantissa   The significant digits of the decimal number, represented as a long.
     * @param exponent   The exponent to which 10 must be raised and then multiplied with the mantissa to obtain the actual decimal number.
     */
    void append(boolean isNegative, long mantissa, int exponent);
}
