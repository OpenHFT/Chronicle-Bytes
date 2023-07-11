/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;

/**
 * Functional interface defining a listener that gets invoked when a new chunk is added to a {@link MappedFile} in {@link MappedBytes}.
 * <p>
 * This listener provides a mechanism to perform actions or react to events whenever a new chunk is added to a {@link MappedFile}.
 * <p>
 * Implementations should define the {@link #onNewChunk(String, int, long)} method to handle the event.
 */
@FunctionalInterface
public interface NewChunkListener {

    /**
     * This method is invoked when a new chunk is added to a {@link MappedFile} in {@link MappedBytes}.
     *
     * @param filename    the name of the file to which a new chunk has been added
     * @param chunk       the number (or identifier) of the new chunk
     * @param delayMicros the delay in microseconds from when the chunk was scheduled to be added, to when it was actually added
     */
    void onNewChunk(String filename, @NonNegative int chunk, @NonNegative long delayMicros);
}
