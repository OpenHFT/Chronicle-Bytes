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

import net.openhft.chronicle.core.io.Closeable;

/**
 * Holds a {@link Bytes} buffer and optional key for a unit of work. Implementations
 * may support rollback of writes when the context is closed.
 */
public interface BytesContext extends Closeable {

    /**
     * Returns the buffer associated with this context.
     */
    Bytes<?> bytes();

    /**
     * Provides a context-dependent key, such as a message type.
     */
    int key();

    /**
     * Indicates whether this context has been closed. The default implementation
     * throws {@link UnsupportedOperationException} and should be overridden.
     */
    @Override
    default boolean isClosed() {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * Marks this context to roll back any writes when {@link #close()} is called.
     */
    default void rollbackOnClose() {
    }
}
