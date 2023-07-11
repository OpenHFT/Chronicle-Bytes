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
 * Interface for an invocation handler that provides a mechanism to handle the closure of resources.
 * Implementations of this interface must provide a concrete implementation of the onClose method,
 * which takes a Closeable as a parameter. This interface extends the InvocationHandler interface from the Java Reflection API.
 */
public interface BytesMethodWriterInvocationHandler extends InvocationHandler {

    /**
     * Handles the closure of the provided Closeable resource.
     *
     * @param closeable the resource that should be handled upon closure
     */
    void onClose(Closeable closeable);
}
