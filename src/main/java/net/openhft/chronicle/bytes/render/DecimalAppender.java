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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;

/**
 * Handler for decimal numbers extracted by a {@link Decimaliser}.
 * Implementations must document their threading guarantees. In Chronicle Bytes
 * a typical implementation appends directly to a byte buffer without
 * allocation.
 * <p>
 * A decimal number is represented as: {@code decimal = sign * mantissa * 10 ^ (-exponent)},
 * where:
 * <ul>
 *   <li>{@code sign} is -1 if the number is negative, +1 otherwise.</li>
 *   <li>{@code mantissa} holds the significant digits of the decimal number.</li>
 *   <li>{@code exponent} denotes the power of 10 by which the mantissa is scaled.</li>
 * </ul>
 * Implementations of this interface should provide strategies to handle these individual components.
 */
@FunctionalInterface
public interface DecimalAppender {

    /**
     * Append a decimal number, represented by its sign, mantissa and exponent, to a target.
     * The target is often a {@link net.openhft.chronicle.bytes.BytesOut} buffer.
     *
     * @param isNegative Whether the number is negative. {@code true} indicates a negative number,
     *                   {@code false} indicates a positive number.
     * @param mantissa   The significant digits of the decimal number, represented as a long integer.
     * @param exponent   The power of ten by which the mantissa is scaled, typically in the range 0-18.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    void append(boolean isNegative, long mantissa, int exponent)
            throws ClosedIllegalStateException, ThreadingIllegalStateException;
}
