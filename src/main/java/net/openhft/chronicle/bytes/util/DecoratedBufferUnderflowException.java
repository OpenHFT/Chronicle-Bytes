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
package net.openhft.chronicle.bytes.util;

import java.nio.BufferUnderflowException;

/**
 * A specialized {@link BufferUnderflowException} that allows for custom messages.
 */
public final class DecoratedBufferUnderflowException extends BufferUnderflowException {

    /**
     * The custom message describing this exception.
     */
    private final String message;

    /**
     * Constructs a DecoratedBufferUnderflowException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public DecoratedBufferUnderflowException(final String message) {
        this.message = message;
    }

    /**
     * Returns the detail message string of this exception.
     *
     * @return the detail message string of this exception
     */
    @Override
    public String getMessage() {
        return message;
    }
}
