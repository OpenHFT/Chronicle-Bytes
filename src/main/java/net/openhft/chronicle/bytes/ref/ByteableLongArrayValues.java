/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.DynamicallySized;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.values.LongArrayValues;

/**
 * Off-heap, contiguous array of signed 64-bit values.
 * <p>
 * Implementations must store each value contiguously according to
 * {@link BinaryLongArrayReference#SHIFT} and are expected to use
 * little-endian aligned storage.
 *
 * @see LongArrayValues
 * @see Byteable
 * @see DynamicallySized
 * @see BinaryLongArrayReference
 */
@SuppressWarnings("rawtypes")
public interface ByteableLongArrayValues extends LongArrayValues, Byteable, DynamicallySized {

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

    ByteableLongArrayValues capacity(@NonNegative long arrayLength)
            throws IllegalStateException;
}
