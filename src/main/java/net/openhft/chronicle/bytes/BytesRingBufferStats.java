/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;

import java.util.List;

/**
 * This interface provides statistics about a {@link BytesRingBuffer}.
 */
public interface BytesRingBufferStats {
    /**
     * Minimum free space observed for the writer since the last call. Reset on
     * each invocation. Returns {@link Long#MAX_VALUE} if no reads occurred in
     * that period.
     */
    @NonNegative
    long minNumberOfWriteBytesRemaining();

    /**
     * @return the total capacity of the ring buffer in bytes.
     */
    @NonNegative
    long capacity();

    /**
     * Calling this method resets the number.
     *
     * @return the number of write operations performed since the last call to this method.
     */
    @NonNegative
    long getAndClearWriteCount();

    /**
     * Calling this method resets the number.
     *
     * @return the number of missed write operations since the last call to this method.
     */
    @NonNegative
    long getAndClearMissedWriteCount();

    /**
     * Calling this method resets the number.
     *
     * @return the number of contentions since the last call to this method.
     */
    @NonNegative
    long getAndClearContentionCount();

    /**
     * @return a list of {@link RingBufferReaderStats} objects, each representing the statistics
     * for a reader of the ring buffer.
     */
    List<RingBufferReaderStats> readers();
}
