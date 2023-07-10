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

import java.lang.reflect.InvocationHandler;
/**
 * Interface for handling invocations in a MethodWriter.
 * This provides advanced configurations for MethodWriters, including
 * history recording, resource cleanup, generic event specification,
 * and the option to use method IDs.
 */
public interface MethodWriterInvocationHandler extends InvocationHandler {

    /**
     * Enable or disable the recording of method invocation history.
     *
     * @param recordHistory boolean flag to indicate if history should be recorded
     */
    void recordHistory(boolean recordHistory);

    /**
     * Attach a Closeable resource that will be closed when this handler is closed.
     * This is useful for handling resource cleanup after the handler is done.
     *
     * @param closeable the Closeable resource to be managed
     */
    void onClose(Closeable closeable);

    /**
     * Specify the identifier for generic events.
     * A generic event uses the first argument as the method name, providing a flexible way to write arbitrary methods at runtime.
     *
     * @param genericEvent the identifier used for generic events
     */
    void genericEvent(String genericEvent);

    /**
     * Enable or disable the use of method IDs.
     * When enabled, methods can be encoded with a numeric ID instead of a string for efficiency.
     *
     * @param useMethodIds boolean flag to indicate if method IDs should be used
     */
    void useMethodIds(boolean useMethodIds);

}
