/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceCounted;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;

/**
 * A foundational interface for random access to a byte sequence or buffer.
 * It defines start position, capacity and cursor operations.
 * Extends {@link net.openhft.chronicle.core.io.ReferenceCounted} for lifecycle management and underpins {@link RandomDataInput} and {@link RandomDataOutput}.
 */
public interface RandomCommon extends ReferenceCounted {
    /**
     * @return The smallest position allowed in this buffer.
     */
    @NonNegative
    default long start() {
        return 0L;
    }

    /**
     * @return the maximum addressable capacity.
     * May be as large as {@link Bytes#MAX_CAPACITY} for virtual mappings.
     */
    @NonNegative
    default long capacity() {
        return Bytes.MAX_CAPACITY;
    }
    /**
     * @return the current allocated capacity of the underlying storage.
     * For elastic buffers this may be less than {@link #capacity()} and can grow on demand.
     */
    @NonNegative
    default long realCapacity() {
        return capacity();
    }

    /**
    /**
     * @return the current read position.
     * Typically {@code start() <= readPosition() <= writePosition()} and {@code readPosition() <= readLimit()}.
     */
    @NonNegative
    default long readPosition() {
        return start();
    }

    /**
     * @return the current write position.
     * Typically {@code readPosition() <= writePosition() <= writeLimit()}.
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
     */
    long addressForRead(@NonNegative long offset)
            throws UnsupportedOperationException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Retrieves the underlying memory address for reading. This is for expert users only.
     *
     * @param offset the logical offset within this buffer relative to {@link #start()}.
     * @param buffer the buffer index if this store is backed by multiple buffers.
     * @return the native address for the specified offset.
     * @throws UnsupportedOperationException if the buffer uses heap memory.
     * @throws BufferUnderflowException      if the offset is outside the allowed range.
     * @throws ClosedIllegalStateException   if the resource has been released or closed.
     * @throws ThreadingIllegalStateException if accessed concurrently in an unsafe way.
     */
    default long addressForRead(@NonNegative long offset, @NonNegative int buffer)
            throws UnsupportedOperationException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return addressForRead(offset);
    }

    /**
     * Retrieves the underlying memory address for writing.  This is for expert users only.
     *
     * @param offset within this buffer. addressForRead(start()) is the actual addressForRead of the first byte.
     * @return the underlying addressForRead of the buffer
     * @throws UnsupportedOperationException If the underlying buffer is on the heap
     * @throws BufferOverflowException       If the offset is before the start() or the after the capacity()
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long addressForWrite(@NonNegative long offset)
            throws UnsupportedOperationException, BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Retrieves the underlying memory address for writing at the current write position.  This is for expert users only.
     *
     * @return The underlying memory address for writing at the current write position.
     * @throws UnsupportedOperationException If the underlying buffer is on the heap.
     * @throws BufferOverflowException       If the current write position is before the start or after the capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException, ClosedIllegalStateException;

    /**
     * Retrieves the byte order used by the buffer.
     *
     * @return The byte order used by the buffer.
     */
    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    /**
     * @return a {@link Bytes} view for reading from this buffer.
     * The returned view reflects the current positions and limits.
     */
    @NotNull
    Bytes<?> bytesForRead()
            throws ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Retrieves a Bytes object for writing.
     *
     * @return A Bytes object for writing.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    Bytes<?> bytesForWrite()
            throws ClosedIllegalStateException;

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
