/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;

public interface MethodWriterInvocationHandler extends InvocationHandler {
    void recordHistory(boolean recordHistory);

    void onClose(Closeable closeable);

    @Deprecated
    default void methodWriterInterceptorReturns(MethodWriterListener methodWriterListener, @Nullable MethodWriterInterceptorReturns interceptor)
            throws IllegalArgumentException {
        if (methodWriterListener != null || interceptor != null)
            methodWriterInterceptorReturns(MethodWriterInterceptorReturns.of(methodWriterListener, interceptor));
    }

    void methodWriterInterceptorReturns(MethodWriterInterceptorReturns methodWriterInterceptor);

    void genericEvent(String genericEvent);

    void useMethodIds(boolean useMethodIds);

}
