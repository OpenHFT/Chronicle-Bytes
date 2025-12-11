/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
     *
     * @param filename   path of the mapped file
     * @param chunk      chunk index that was mapped
     * @param delayMicros time taken to map in microseconds
     */
    void onNewChunk(String filename, @NonNegative int chunk, @NonNegative long delayMicros);
}
