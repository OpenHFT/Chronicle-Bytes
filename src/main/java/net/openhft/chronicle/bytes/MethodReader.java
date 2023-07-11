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
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;

import java.nio.BufferUnderflowException;

/**
 * Interface defining the required operations for a method reader.
 * <p>
 * A method reader is responsible for reading serialized method calls, typically from a queue or a stream,
 * and then dispatching those method calls to their intended target.
 * <p>
 * The reader provides a means to intercept method calls and control whether the underlying input should be closed
 * when the reader itself is closed.
 * <p>
 * This interface extends {@link java.io.Closeable}, so any class implementing this interface can also be safely closed.
 */
public interface MethodReader extends Closeable {
    String HISTORY = "history";
    int MESSAGE_HISTORY_METHOD_ID = -1;

    /**
     * Returns a MethodReaderInterceptorReturns instance which can be used to control the flow of method calls.
     *
     * @return a MethodReaderInterceptorReturns instance
     */
    MethodReaderInterceptorReturns methodReaderInterceptorReturns();

    /**
     * Attempts to read a single message from the input, if one is available.
     * <p>
     * If an exception is thrown while dispatching the method call, this should be caught and logged.
     * If an exception is thrown by the target method itself, this is wrapped in an {@link InvocationTargetRuntimeException} and rethrown.
     *
     * @return {@code true} if a message was successfully read, {@code false} if no more messages are available
     * @throws InvocationTargetRuntimeException if an exception is thrown by the target method
     * @throws IllegalStateException            if this method is invoked at an inappropriate time
     * @throws BufferUnderflowException         if there is not enough data available in the buffer to read the next message
     */
    boolean readOne()
            throws InvocationTargetRuntimeException, IllegalStateException, BufferUnderflowException;

    /**
     * Determines whether the underlying input should be closed when the MethodReader is closed.
     * <p>
     * If {@code closeIn} is set to {@code true}, closing the MethodReader will also close the underlying input.
     * If {@code closeIn} is set to {@code false}, the underlying input will remain open after the MethodReader is closed.
     *
     * @param closeIn {@code true} to close the input when this reader is closed, {@code false} otherwise
     * @return the MethodReader itself, for method chaining
     */
    MethodReader closeIn(boolean closeIn);
}
