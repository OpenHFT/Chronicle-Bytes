/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;

/**
 * Listener notified when a new chunk is allocated by a {@link MappedFile}.
 */
@FunctionalInterface
public interface NewChunkListener {

    /**
     * Invoked after mapping a new chunk.
     */
    void onNewChunk(String filename, @NonNegative int chunk, @NonNegative long delayMicros);
}
