/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

/**
 * Parses fallback messages read from a {@link BytesIn} when no specific handler
 * method is available for routing during message decoding.
 */
@FunctionalInterface
public interface BytesParselet {
    /**
     * Consumes a message of the given type from the input stream.
     *
     * @param messageType numeric identifier for the message
     * @param in          stream containing the message payload
     */
    void accept(long messageType, BytesIn<?> in);
}
