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

import net.openhft.chronicle.core.io.Closeable;

/**
 * Defines a context for operating with bytes. This interface provides methods to retrieve bytes
 * and keys. It also provides utility methods to check if the context is closed and to perform
 * a rollback on close.
 */
public interface BytesContext extends Closeable {

    /**
     * Retrieves the bytes to be written.
     *
     * @return the {@link Bytes} object to be written to.
     */
    Bytes<?> bytes();

    /**
     * Retrieves the key to be written.
     *
     * @return the key to be written.
     */
    int key();

    /**
     * Checks whether the context is closed.
     *
     * @return {@code true} if the context is closed, {@code false} otherwise.
     * @throws UnsupportedOperationException if the method is not overridden.
     */
    @Override
    default boolean isClosed() {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * Performs a rollback operation when the context is closed. The exact behavior of this
     * method is left to the implementing class.
     */
    default void rollbackOnClose() {
    }
}
