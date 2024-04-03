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

import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * An unchecked exception thrown when there is an issue with encoding or decoding UTF-8 data.
 * This exception extends {@link IORuntimeException}, which is a base class for exceptions that can
 * be thrown during reading and writing operations.
 */
public class UTFDataFormatRuntimeException extends IORuntimeException {
    private static final long serialVersionUID = 0L;
    /**
     * Constructs a new UTFDataFormatRuntimeException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public UTFDataFormatRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs a new UTFDataFormatRuntimeException with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is not automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public UTFDataFormatRuntimeException(String message, Exception cause) {
        super(message, cause);
    }
}
