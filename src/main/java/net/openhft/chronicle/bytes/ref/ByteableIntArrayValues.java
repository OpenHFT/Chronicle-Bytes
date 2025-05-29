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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.DynamicallySized;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.values.IntArrayValues;

/**
 * Represents an array of integer values, where each integer in the array is byteable and dynamically sized.
 * <p>
 * Implementations of this interface should provide means to manage the array of integers, with
 * support for resizing the array dynamically. It is meant to be used where direct, low-level access
 * to the bytes representing the integer values is needed.
 *
 * @see IntArrayValues
 * @see Byteable
 * @see DynamicallySized
 */
@SuppressWarnings("rawtypes")
public interface ByteableIntArrayValues extends IntArrayValues, Byteable, DynamicallySized {

    /**
     * Calculates the byte size required to hold the provided element capacity.
     *
     * @param capacity the number of elements
     * @return total bytes needed for this capacity
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    long sizeInBytes(@NonNegative long capacity)
            throws IllegalStateException;

    /**
     * Sets the capacity of the array as a count of elements, not bytes.
     *
     * @param arrayLength the desired element count
     * @return this instance for chaining
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    ByteableIntArrayValues capacity(@NonNegative long arrayLength)
            throws IllegalStateException;
}
