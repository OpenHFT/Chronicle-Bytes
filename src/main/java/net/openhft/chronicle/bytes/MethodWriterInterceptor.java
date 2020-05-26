/*
 * Copyright 2016-2020 Chronicle Software
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

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

/**
 * Invoked around method writing allowing you to take action before or after method invocation,
 * or even not to call the method
 *
 * @deprecated Use MethodWriterInterceptorReturns
 */
@FunctionalInterface
@Deprecated
public interface MethodWriterInterceptor {

    static MethodWriterInterceptor of(@Nullable final MethodWriterListener methodWriterListener, @Nullable final MethodWriterInterceptor interceptor) {
        if (methodWriterListener == null && interceptor == null)
            throw new IllegalArgumentException("both methodWriterListener and interceptor are NULL");

        if (methodWriterListener == null)
            return interceptor::intercept;

        if (interceptor == null)
            return (method, args, invoker) -> {
                methodWriterListener.onWrite(method.getName(), args);
                invoker.accept(method, args);
            };

        return (method, args, invoker) -> {
            interceptor.intercept(method, args, invoker);
            methodWriterListener.onWrite(method.getName(), args);
        };
    }

    void intercept(Method method, Object[] args, BiConsumer<Method, Object[]> invoker);
}
