/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
     * Moves the queue to read a message if there is one, but is more expensive than {@link #lazyReadOne()}.
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
     * Does a quick read which is simpler but might not read the next message. readOne() has to be called periodically.
     * See also javadoc for {@link #readOne()}
     *
     * @return true if there was a message, false if there is probably not a message.
     */
    @Deprecated(/* to be removed in x.23*/)
    default boolean lazyReadOne()
            throws IllegalStateException, BufferUnderflowException {
        return readOne();
    }

    /**
     * Call close on the input when closed
     */
    MethodReader closeIn(boolean closeIn);
}
