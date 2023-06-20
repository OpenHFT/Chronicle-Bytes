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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
/**
 * Functional interface defining a format for appending offset information to a byte sequence.
 *
 * This interface provides a standardized way to append offset information to {@link Bytes},
 * allowing different implementations to provide different formatting strategies.
 *
 * Implementations should define the {@link #append(long, Bytes)} method to handle the formatting and appending operation.
 */
@FunctionalInterface
public interface OffsetFormat {

    /**
     * Appends the given offset to the provided {@link Bytes} instance.
     *
     * This method allows the implementation to decide how the offset information should be formatted
     * and appended to the {@link Bytes} instance.
     *
     * @param offset the offset to be appended
     * @param bytes  the {@link Bytes} instance where the offset will be appended
     * @throws IllegalArgumentException if the offset is negative
     */
    void append(@NonNegative long offset, Bytes<?> bytes);
}
