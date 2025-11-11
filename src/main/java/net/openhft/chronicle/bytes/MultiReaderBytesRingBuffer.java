/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

/**
 * Extension of the BytesRingBuffer interface that supports multiple readers.
 * Each reader in a MultiReaderBytesRingBuffer has a separate read position,
 * allowing independent progression through the buffer.
 */
public interface MultiReaderBytesRingBuffer extends BytesRingBuffer {

    /**
     * Creates a RingBufferReader with a default ID of 0.
     *
     * @return a new RingBufferReader
     */
    @NotNull
    default RingBufferReader createReader() {
        return createReader(0);
    }

    /**
     * Creates a RingBufferReader with a specified reader ID.
     * Each reader has a separate read position, which allows independent progression through the buffer.
     *
     * @param id the identifier for the new reader
     * @return a new RingBufferReader with the given ID
     */
    @NotNull
    RingBufferReader createReader(int id);
}
