/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;

import java.nio.BufferUnderflowException;

public interface MethodReader extends Closeable {
    String HISTORY = "history";
    int MESSAGE_HISTORY_METHOD_ID = -1;

    MethodReaderInterceptorReturns methodReaderInterceptorReturns();

    /**
     * Moves the queue to read a message if there is one.
     * If there is an exception in the dispatching mechanics then this should be caught and Jvm.warn'd.
     * If there is an exception in the invocation then this is wrapped in a {@link InvocationTargetRuntimeException}
     * and thrown.
     *
     * @return true if there was a message, false if no more messages.
     * @throws InvocationTargetRuntimeException if the receiver (target method) throws
     */
    boolean readOne()
            throws InvocationTargetRuntimeException, IllegalStateException, BufferUnderflowException;

    /**
     * Call close on the input when closed
     */
    MethodReader closeIn(boolean closeIn);
}
