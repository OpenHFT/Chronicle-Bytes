/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
/**
 * Strategy for formatting offsets when dumping bytes.
 */
@FunctionalInterface
public interface OffsetFormat {

    /**
     * Formats {@code offset} and appends it to {@code bytes}.
     */
    void append(@NonNegative long offset, Bytes<?> bytes);
}
