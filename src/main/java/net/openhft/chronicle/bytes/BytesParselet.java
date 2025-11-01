/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

/**
 * Parses messages read from a {@link BytesIn} when no specific method handler
 * is available.
 */
@FunctionalInterface
public interface BytesParselet {
    /**
     * Handles a message of the supplied {@code messageType} using bytes from
     * {@code in}.
     */
    void accept(long messageType, BytesIn<?> in);
}
