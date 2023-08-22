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

import java.nio.BufferOverflowException;

/**
 * A functional interface for consuming bytes.
 * The interface defines a single method {@code read} for retrieving and removing the head of a queue into a given {@code BytesOut} object.
 */
@FunctionalInterface
public interface BytesConsumer {

    /**
     * Retrieves and removes the head of this queue, or returns {@code false} if this queue is
     * empty.
     *
     * @param bytes the {@code BytesOut} object to read into.
     * @return {@code false} if this queue is empty, {@code true} otherwise.
     * @throws BufferOverflowException If there is insufficient space left in the buffer.
     */
    boolean read(BytesOut<?> bytes)
            throws BufferOverflowException;
}