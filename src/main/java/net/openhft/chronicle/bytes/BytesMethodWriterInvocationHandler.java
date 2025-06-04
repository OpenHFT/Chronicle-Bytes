/*
 * Copyright 2016-2025 chronicle.software
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
 * An {@link java.lang.reflect.InvocationHandler} for method writers operating
 * on {@link BytesOut}. It extends the standard handler contract by exposing a
 * hook for resource cleanup via {@link #onClose(Closeable)}.
 */
public interface BytesMethodWriterInvocationHandler extends InvocationHandler {

    /**
     * Register a {@link Closeable} resource that should be closed when the
     * associated writer is closed.
     *
     * @param closeable resource to close with the writer
     */
    void onClose(Closeable closeable);
}
