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
 * Represents an interface for byteable long array values,
 * extending LongArrayValues, Byteable, and DynamicallySized interfaces.
 * This interface allows for handling long array values that are byteable
 * and dynamically sized.
 */
@SuppressWarnings("rawtypes")
public interface ByteableLongArrayValues extends LongArrayValues, Byteable, DynamicallySized {

    /**
     * Returns the size in bytes for the given number of elements.
     *
     * @param sizeInBytes The number of elements.
     * @return The size in bytes needed to store the given number of elements.
     * @throws IllegalStateException If an illegal state occurs during the operation.
     */
    @Override
    long sizeInBytes(@NonNegative long sizeInBytes)
            throws IllegalStateException;

    /**
     * Sets the capacity of the long array values.
     *
     * @param arrayLength The desired capacity of the long array values.
     * @return The instance of ByteableLongArrayValues with the set capacity.
     * @throws IllegalStateException If an illegal state occurs during the operation.
     */
    ByteableLongArrayValues capacity(@NonNegative long arrayLength) throws IllegalStateException;
}
