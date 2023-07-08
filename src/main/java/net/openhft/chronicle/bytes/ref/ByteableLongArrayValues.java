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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.DynamicallySized;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.values.LongArrayValues;

/**
 * Represents an array of long integer values, where each long integer in the array is byteable and dynamically sized.
 * <p>
 * Implementations of this interface should provide means to manage the array of long integers, with
 * support for resizing the array dynamically. It is meant to be used where direct, low-level access
 * to the bytes representing the long integer values is needed.
 * </p>
 *
 * @see LongArrayValues
 * @see Byteable
 * @see DynamicallySized
 */
@SuppressWarnings("rawtypes")
public interface ByteableLongArrayValues extends LongArrayValues, Byteable, DynamicallySized {

    /**
     * Calculates the size in bytes needed to store the given number of long integers.
     *
     * @param sizeInBytes the number of long integers to be stored.
     * @return the size in bytes needed to store the specified number of long integers.
     * @throws IllegalStateException if the size cannot be determined.
     */
    @Override
    long sizeInBytes(@NonNegative long sizeInBytes)
            throws IllegalStateException;

    /**
     * Sets the capacity of the array, in terms of the number of long integers it can hold.
     *
     * @param arrayLength the desired array capacity, in number of long integers.
     * @return this {@code ByteableLongArrayValues} instance.
     * @throws IllegalStateException if the capacity cannot be set, for example, due to insufficient memory.
     */

    ByteableLongArrayValues capacity(@NonNegative long arrayLength)
            throws IllegalStateException;
}
