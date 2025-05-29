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
 * Strategy interface that decomposes a floating point number into sign,
 * mantissa and exponent suitable for textual serialisation.
 * Implementations may trade performance against precision.
 *
 * <p>NaN, infinity and negative zero values are not converted and should cause
 * the method to return {@code false}.
 *
 * @see DecimalAppender
 */
public interface Decimaliser {

    /**
     * Convert {@code value} to a decimal representation and append it.
     *
     * @param value           the double value to serialise; must be finite and not negative zero
     * @param decimalAppender the target receiving sign, mantissa and exponent
     * @return {@code true} if the value could be represented and appended
     */
    boolean toDecimal(double value, DecimalAppender decimalAppender);

    /**
     * Convert {@code value} to a decimal representation and append it.
     *
     * @param value           the float value to serialise; must be finite and not negative zero
     * @param decimalAppender the target receiving sign, mantissa and exponent
     * @return {@code true} if the value could be represented and appended
     */
    boolean toDecimal(float value, DecimalAppender decimalAppender);
}
