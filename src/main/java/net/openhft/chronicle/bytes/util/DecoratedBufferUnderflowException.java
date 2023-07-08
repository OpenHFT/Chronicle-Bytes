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
 * Customized {@link BufferUnderflowException} with a descriptive message.
 * This exception is thrown to indicate that there is an attempt to read
 * data from a buffer beyond its limit.
 */
public final class DecoratedBufferUnderflowException extends BufferUnderflowException {

    /**
     * The custom message describing this exception.
     */
    private final String message;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public DecoratedBufferUnderflowException(final String message) {
        this.message = message;
    }

    /**
     * Returns the detail message of this exception.
     *
     * @return the detail message
     */
    @Override
    public String getMessage() {
        return message;
    }
}
