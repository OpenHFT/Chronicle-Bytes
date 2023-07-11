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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a streaming interface for handling streaming data with support for random access.
 * The interface is used for managing reading and writing positions within the streaming data.
 *
 * @param <S> Type of the implementing class, extending StreamingCommon
 */
public interface StreamingCommon<S extends StreamingCommon<S>> extends RandomCommon {

    /**
     * Resets the read and write positions to the start of the streaming buffer,
     * and sets the write limit to the capacity of the buffer. This effectively
     * clears any existing data in the buffer and prepares it for new data to be written.
     *
     * <p>This operation is similar to rewinding the tape to the beginning,
     * and having it ready to record new data over anything that was there before.</p>
     *
     * @return A reference to this object, allowing for method chaining.
     * @throws ClosedIllegalStateException If this buffer has been closed
     */
    @NotNull
    S clear() throws ClosedIllegalStateException;
}
