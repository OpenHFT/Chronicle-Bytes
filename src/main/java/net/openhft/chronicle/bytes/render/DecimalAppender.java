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
 * This interface represents a handler for decimal numbers, and defines how they should be appended in various forms.
 * Implementations of this interface should provide strategies for handling the individual components of a decimal number,
 * including its sign, mantissa, and exponent.
 * <p>
 * A decimal number is represented as: <code>decimal = sign * mantissa * 10 ^ (-exponent)</code>,
 * where:
 * <ul>
 *   <li><code>sign</code> is -1 if the number is negative, +1 otherwise.</li>
 *   <li><code>mantissa</code> holds the significant digits of the decimal number.</li>
 *   <li><code>exponent</code> denotes the power of 10 by which the mantissa is scaled.</li>
 * </ul>
 * Implementations of this interface should provide strategies to handle these individual components.
 */
@FunctionalInterface
public interface DecimalAppender {

    /**
     * Appends a decimal number, represented by its sign, mantissa, and exponent, to a target.
     * The target can be any object that consumes these components, such as a StringBuilder or a file stream.
     *
     * @param isNegative Whether the number is negative. <code>true</code> indicates a negative number,
     *                   <code>false</code> indicates a positive number.
     * @param mantissa   The significant digits of the decimal number, represented as a long integer.
     * @param exponent   The power of 10 by which the mantissa is scaled to obtain the actual decimal number.
     */
    void append(boolean isNegative, long mantissa, int exponent);
}
