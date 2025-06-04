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
