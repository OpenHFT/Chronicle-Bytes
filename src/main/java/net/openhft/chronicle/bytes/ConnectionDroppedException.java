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
 * Exception thrown when the TcpChannelHub loses its connection to the server.
 * <p>
 * This exception is a runtime exception, which means that it does not need to be
 * declared in a method or constructor's {@code throws} clause if it can be thrown
 * by the execution of the method or constructor and propagate outside the method
 * or constructor boundary.
 * 
 * TODO Move to network where it is used.
 */
public class ConnectionDroppedException extends IORuntimeException {

    /**
     * Constructs a {@code ConnectionDroppedException} with the specified detail message.
     *
     * @param message the detail message saved for later retrieval by the {@link #getMessage()} method.
     */
    public ConnectionDroppedException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code ConnectionDroppedException} with the specified cause.
     *
     * @param e the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *           (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ConnectionDroppedException(Throwable e) {
        super(e);
    }

    /**
     * Constructs a {@code ConnectionDroppedException} with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     * 
     *
     * @param message the detail message saved for later retrieval by the {@link #getMessage()} method.
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ConnectionDroppedException(String message, Throwable cause) {
        super(message, cause);
    }
}
