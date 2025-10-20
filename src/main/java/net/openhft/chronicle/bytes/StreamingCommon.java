/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

/**
 * A common base interface for streaming data access. It manages fundamental
 * buffer properties such as positions and limits and supports random access
 * semantics. Implementations are mainly used by higher level streaming
 * interfaces like {@link StreamingDataInput} and {@link StreamingDataOutput}.
 *
 * @param <S> type of the implementing class
 */
public interface StreamingCommon<S extends StreamingCommon<S>> extends RandomCommon {

    /**
     * Resets the read and write positions to {@link #start()} and sets the
     * effective limits to {@link #capacity()}. The underlying data is left
     * unchanged but the buffer behaves as if newly allocated.
     *
     * @return this instance for chaining
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if accessed by multiple threads in an unsafe way
     */
    @NotNull
    S clear() throws ClosedIllegalStateException, ThreadingIllegalStateException;
}
