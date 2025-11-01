/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import java.nio.BufferOverflowException;

/**
 * Consumes bytes from a source and writes them to a {@link BytesOut} instance.
 * Implementations typically pull data from a queue or ring buffer.
 */
@FunctionalInterface
public interface BytesConsumer {

    /**
     * Attempts to pull data from the source into {@code bytes}.
     *
     * @param bytes destination for the consumed data, positioned for writing
     * @return {@code true} if bytes were written, {@code false} if no data was available
     * @throws BufferOverflowException if {@code bytes} lacks capacity for the read data
     */
    boolean read(BytesOut<?> bytes) throws BufferOverflowException;
}
