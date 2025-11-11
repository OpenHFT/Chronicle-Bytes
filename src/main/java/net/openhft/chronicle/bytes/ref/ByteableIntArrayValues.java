/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.DynamicallySized;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.values.IntArrayValues;

/**
 * Off-heap, contiguous array of signed 32-bit values.
 * <p>
 * Values must be stored contiguously following
 * {@link BinaryIntArrayReference#SHIFT} in little-endian order.
 *
 * @see IntArrayValues
 * @see Byteable
 * @see DynamicallySized
 * @see BinaryIntArrayReference
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
