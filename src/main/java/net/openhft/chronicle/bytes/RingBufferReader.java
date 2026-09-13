/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.Closeable;

/**
 * Reader facade for a Chronicle ring buffer with read-once semantics.
 * <p>
 * Supports concurrent producers and consumers without blocking writers; consumers call
 * {@link #beforeRead(Bytes)} / {@link #afterRead(long)} to advance safely and can gather stats via
 * {@link RingBufferReaderStats}. Once stopped, the reader keeps writers unblocked and may be
 * restarted if supported by the implementation.
 */
public interface RingBufferReader extends RingBufferReaderStats, Closeable {

    /**
     * Constant representing an undefined or unknown index within the ring buffer.
     */
    long UNKNOWN_INDEX = -1;

    /**
     * Checks if the Ring Buffer is empty.
     *
     * @return true if the Ring Buffer is empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * Checks if the Ring Buffer reader has been stopped.
     *
     * @return true if the Ring Buffer reader has been stopped, false otherwise.
     */
    boolean isStopped();

    /**
     * Stops the reader. After being stopped, the reader will not block writers. The reader can be re-opened after being stopped.
     */
    void stop();

    /**
     * Prepares the reader to read data from the Ring Buffer. This method adjusts the read position and read limit
     * in the provided {@link Bytes} object, allowing the client to read the data.
     *
     * @param bytes The {@link Bytes} instance backed by the Ring Buffer.
     * @return The position from where next read operation should be performed, this should be passed to
     * {@link RingBufferReader#afterRead(long)} after the read operation.
     */
    @NonNegative
    long beforeRead(Bytes<?> bytes);

    /**
     * Updates the reader's state after a read operation. The parameter is usually the return value of the preceding {@link #beforeRead(Bytes)} call.
     *
     * @param next The position after the last read operation.
     */
    void afterRead(@NonNegative long next);

    /**
     * Overloaded method of {@link #afterRead(long)} providing additional details about the read operation.
     *
     * @param next            The position after the last read operation.
     * @param payloadStart    The starting position of the payload that was read.
     * @param underlyingIndex The index in the underlying data structure from where the data was read.
     */
    void afterRead(@NonNegative long next, long payloadStart, long underlyingIndex);

    /**
     * Retrieves the index in the underlying data structure where the last read operation occurred.
     *
     * @return The index of the last read operation.
     */
    long underlyingIndex();

    /**
     * A convenience method that reads data from the Ring Buffer by internally calling both {@link #beforeRead(Bytes)} and {@link #afterRead(long)}.
     *
     * @param bytes to read a message into
     * @return True if the read operation succeeded, false otherwise.
     */
    boolean read(BytesOut<?> bytes);

    /**
     * Retrieves the {@link BytesStore} instance that backs the Ring Buffer.
     *
     * @return The backing {@link BytesStore} instance.
     */
    BytesStore<?, ?> byteStore();

    /**
     * Adjusts the reader's position to just past the end of the Ring Buffer, effectively making it read any new data that gets written.
     */
    void toEnd();
}
