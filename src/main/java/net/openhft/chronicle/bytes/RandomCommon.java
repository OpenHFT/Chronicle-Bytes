/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.io.ReferenceCounted;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;

interface RandomCommon extends ReferenceCounted {
    /**
     * @return The smallest position allowed in this buffer.
     */
    @NonNegative
    default long start() {
        return 0L;
    }

    /**
     * @return the highest limit allowed for this buffer.
     */
    @NonNegative
    default long capacity() {
        return Bytes.MAX_CAPACITY;
    }

    /**
     * @return the limit for this buffer without resizing
     */
    @NonNegative
    default long realCapacity() {
        return capacity();
    }

    /**
     * Returns the read position.
     * <p>
     * The read position is {@code start() <= readPosition() && readPosition() <= readLimit() && readPosition < safeLimit()}
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return position to read from.
     */
    @NonNegative
    default long readPosition() {
        return start();
    }

    /**
     * Returns the write position.
     *
     * The write position is {@code readPosition() <= writePosition() && writePosition() <= writeLimit()}
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return position to write to.
     */
    @NonNegative
    default long writePosition() {
        return start();
    }

    /**
     * Typically this calculates the difference however for HexDumpBytes it's not as simple.
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @param startPosition to compare against
     * @return the length from the startPosition
     */
    default long lengthWritten(@NonNegative long startPosition) {
        return writePosition() - startPosition;
    }

    /**
     * Returns the remaining bytes that can be read.
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return How many more bytes can we read.
     */
    default long readRemaining() {
        return readLimit() - readPosition();
    }

    /**
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return how much can be safely read directly.
     */
    default long realReadRemaining() {
        return Math.min(realCapacity(), readLimit()) - readPosition();
    }

    /**
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return How many more bytes can we written.
     */
    default long writeRemaining() {
        return writeLimit() - writePosition();
    }

    /**
     *
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return writeRemaining with resize
     */
    default long realWriteRemaining() {
        return Math.min(realCapacity(), writeLimit()) - writePosition();
    }

    /**
     *
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return the highest offset or position allowed for this buffer.
     */
    @NonNegative
    default long readLimit() {
        return realCapacity();
    }


    /**
     * Returns the write limit.
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return the write limit
     */
    @NonNegative
    default long writeLimit() {
        return realCapacity();
    }

    /**
     * Obtain the underlying addressForRead.  This is for expert users only.
     *
     * @param offset within this buffer. addressForRead(start()) is the actual addressForRead of the first byte.
     * @return the underlying addressForRead of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     * @throws BufferUnderflowException      if the offset is before the start() or the after the capacity()
     */
    long addressForRead(@NonNegative long offset)
            throws UnsupportedOperationException, BufferUnderflowException, IllegalStateException;

    default long addressForRead(@NonNegative long offset, @NonNegative int buffer)
            throws UnsupportedOperationException, BufferUnderflowException, IllegalStateException {
        return addressForRead(offset);
    }

    /**
     * Obtain the underlying addressForRead.  This is for expert users only.
     *
     * @param offset within this buffer. addressForRead(start()) is the actual addressForRead of the first byte.
     * @return the underlying addressForRead of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     * @throws BufferOverflowException       if the offset is before the start() or the after the capacity()
     */
    long addressForWrite(@NonNegative long offset)
            throws UnsupportedOperationException, BufferOverflowException, IllegalStateException;

    long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException, IllegalStateException;

    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    /**
     * @return the streaming bytes for reading.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    Bytes bytesForRead()
            throws IllegalStateException;

    /**
     * @return the streaming bytes for writing.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    Bytes bytesForWrite()
            throws IllegalStateException;

    /**
     * Perform a 32-bit CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    boolean compareAndSwapInt(@NonNegative long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException;

    void testAndSetInt(@NonNegative long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Perform a 64-bit CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    boolean compareAndSwapLong(@NonNegative long offset, long expected, long value)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Perform a 32-bit float CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    default boolean compareAndSwapFloat(@NonNegative long offset, float expected, float value)
            throws BufferOverflowException, IllegalStateException {
        return compareAndSwapInt(offset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(value));
    }

    /**
     * Perform a 64-bit double CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    default boolean compareAndSwapDouble(@NonNegative long offset, double expected, double value)
            throws BufferOverflowException, IllegalStateException {
        return compareAndSwapLong(offset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value));
    }

    /**
     * @return true if these Bytes use shared memory.
     */
    boolean sharedMemory();

    boolean isDirectMemory();
}
