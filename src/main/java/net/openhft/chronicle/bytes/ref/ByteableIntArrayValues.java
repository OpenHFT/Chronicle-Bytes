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
import net.openhft.chronicle.core.values.IntArrayValues;

/**
 * Interface for a resizable array of integer values that can be represented as bytes.
 * This provides methods to get and set values, as well as to query and modify the size of the array.
 * It extends the {@link IntArrayValues}, {@link Byteable}, and {@link DynamicallySized} interfaces.
 */
@SuppressWarnings("rawtypes")
public interface ByteableIntArrayValues extends IntArrayValues, Byteable, DynamicallySized {

    /**
     * Returns the size in bytes of the array for a given number of elements.
     *
     * @param sizeInBytes The number of elements for which to compute the size in bytes.
     * @return The size in bytes for the given number of elements.
     * @throws IllegalStateException if the array is closed.
     */
    @Override
    long sizeInBytes(@NonNegative long sizeInBytes)
            throws IllegalStateException;

    /**
     * Adjusts the capacity of the array.
     *
     * @param arrayLength The new capacity of the array.
     * @return The current instance, adjusted to the new capacity.
     * @throws IllegalStateException if the array is closed.
     */
    ByteableIntArrayValues capacity(@NonNegative long arrayLength)
            throws IllegalStateException;
}
