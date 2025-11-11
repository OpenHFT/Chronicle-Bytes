/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;

/**
 * An interface to provide statistics about a {@link RingBufferReader}'s reading operations.
 * This includes the number of successful reads, missed reads and how far behind the reader is.
 */
public interface RingBufferReaderStats {

    /**
     * Retrieves and resets the count of successful read operations performed by the RingBufferReader.
     * Calling this method resets the number.
     *
     * @return The number of successful read operations since the last call to this method.
     */
    @NonNegative
    long getAndClearReadCount();

    /**
     * Retrieves and resets the count of missed read operations performed by the RingBufferReader.
     * Missed reads occur if there was no new data to be read.
     * Calling this method resets the number.
     *
     * @return The number of missed read operations since the last call to this method.
     */
    @NonNegative
    long getAndClearMissedReadCount();

    /**
     * Calculates how far behind the RingBufferReader is relative to the write position in the Ring Buffer.
     * This provides an indication of how much unread data remains in the buffer for this reader.
     *
     * @return The number of bytes yet to be read by the RingBufferReader.
     */
    @NonNegative
    long behind();
}
