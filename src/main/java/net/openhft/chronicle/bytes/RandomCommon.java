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
import net.openhft.chronicle.core.io.ReferenceCounted;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;

/**
 * Interface defining methods for managing random access to a buffer. It extends ReferenceCounted to allow for reference counting features.
 */
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
     * <p>
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
     * Calculates the length of data written from the given start position.
     * <p>
     * Typically this calculates the difference however for HexDumpBytes it's not as simple.
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @param startPosition The position to calculate the length from.
     * @return The length of data written from the given start position.
     */
    default long lengthWritten(@NonNegative long startPosition) {
        return writePosition() - startPosition;
    }

    /**
     * Calculates the number of bytes remaining that can be read from the current read position.
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return The number of bytes that can still be read.
     */
    default long readRemaining() {
        return readLimit() - readPosition();
    }

    /**
     * Calculates the number of bytes that can be safely read directly.
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return The number of bytes that can be safely read directly.
     */
    default long realReadRemaining() {
        return Math.min(realCapacity(), readLimit()) - readPosition();
    }

    /**
     * Calculates the number of bytes remaining that can be written from the current write position.
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return The number of bytes that can still be written.
     */
    default long writeRemaining() {
        return writeLimit() - writePosition();
    }

    /**
     * Calculates the number of bytes remaining that can be written from the current write position with resizing.
     *
     * <p>
     * If the resource is closed, the returned value is unspecified.
     *
     * @return The number of bytes that can still be written with resizing.
     */
    default long realWriteRemaining() {
        return Math.min(realCapacity(), writeLimit()) - writePosition();
    }

    /**
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
     * Retrieves the maximum writable position within the buffer.
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
     * Retrieves the underlying memory address for reading. This is for expert users only.
     *
     * @param offset within this buffer. addressForRead(start()) is the actual addressForRead of the first byte.
     * @return the underlying addressForRead of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     * @throws BufferUnderflowException      if the offset is before the start() or the after the capacity()
     * @throws IllegalStateException         if the buffer has been closed.
     */
    long addressForRead(@NonNegative long offset)
            throws UnsupportedOperationException, BufferUnderflowException, IllegalStateException;

    /**
     * Retrieves the underlying memory address for reading. This is for expert users only.
     *
     * @param offset The offset within this buffer.
     * @param buffer The buffer index.
     * @return The underlying memory address for reading at the specified offset.
     * @throws UnsupportedOperationException if the underlying buffer is on the heap.
     * @throws BufferUnderflowException      if the offset is before the start or after the capacity.
     * @throws IllegalStateException         if the buffer has been closed.
     */
    default long addressForRead(@NonNegative long offset, @NonNegative int buffer)
            throws UnsupportedOperationException, BufferUnderflowException, IllegalStateException {
        return addressForRead(offset);
    }

    /**
     * Retrieves the underlying memory address for writing.  This is for expert users only.
     *
     * @param offset within this buffer. addressForRead(start()) is the actual addressForRead of the first byte.
     * @return the underlying addressForRead of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     * @throws BufferOverflowException       if the offset is before the start() or the after the capacity()
     */
    long addressForWrite(@NonNegative long offset)
            throws UnsupportedOperationException, BufferOverflowException, IllegalStateException;

    /**
     * Retrieves the underlying memory address for writing at the current write position.  This is for expert users only.
     *
     * @return The underlying memory address for writing at the current write position.
     * @throws UnsupportedOperationException if the underlying buffer is on the heap.
     * @throws BufferOverflowException       if the current write position is before the start or after the capacity.
     * @throws IllegalStateException         if the buffer state doesn't allow the operation.
     */
    long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException, IllegalStateException;

    /**
     * Retrieves the byte order used by the buffer.
     *
     * @return The byte order used by the buffer.
     */
    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    /**
     * Retrieves a Bytes object for reading.
     *
     * @return A Bytes object for reading.
     * @throws IllegalStateException if the buffer state doesn't allow the operation.
     */
    @NotNull
    Bytes<?> bytesForRead()
            throws IllegalStateException;

    /**
     * Retrieves a Bytes object for writing.
     *
     * @return A Bytes object for writing.
     * @throws IllegalStateException if the buffer state doesn't allow the operation.
     */
    @NotNull
    Bytes<?> bytesForWrite()
            throws IllegalStateException;

    /**
     * Checks if the Bytes use shared memory.
     *
     * @return True if the Bytes use shared memory, false otherwise.
     */
    boolean sharedMemory();

    /**
     * Checks if the buffer uses direct memory.
     *
     * @return True if the buffer uses direct memory, false otherwise.
     */
    boolean isDirectMemory();
}
