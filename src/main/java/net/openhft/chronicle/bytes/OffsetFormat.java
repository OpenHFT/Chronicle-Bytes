/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
/**
 * Strategy for formatting offsets when dumping bytes into readable aligned output.
 */
@FunctionalInterface
@Deprecated(/* to be removed in 2027, as it is only used in tests */)
public interface OffsetFormat {

    /**
     * Formats {@code offset} and appends it to {@code bytes}.
     *
     * @param offset offset to render
     * @param bytes  destination buffer for the formatted value
     */
    void append(@NonNegative long offset, Bytes<?> bytes);
}
