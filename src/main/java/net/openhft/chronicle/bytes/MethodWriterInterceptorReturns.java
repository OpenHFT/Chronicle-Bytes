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
import java.util.function.BiFunction;

/*
 * Peter Lawrey so interceptors can return an object to use for chaining.
 * <p>
 * Invoked around method writing allowing you to take action before or after method invocation,
 * or even not to call the method
 */
@FunctionalInterface
public interface MethodWriterInterceptorReturns {
    static MethodWriterInterceptorReturns of(MethodWriterInterceptor interceptor) {
        return (m, a, i) -> {
            interceptor.intercept(m, a, i::apply);
            return null;
        };
    }

    static MethodWriterInterceptorReturns of(@Nullable final MethodWriterListener methodWriterListener, @Nullable final MethodWriterInterceptorReturns interceptor) {
        if (methodWriterListener == null && interceptor == null)
            throw new IllegalArgumentException("both methodWriterListener and interceptor are NULL");

        if (methodWriterListener == null)
            return interceptor;

        if (interceptor == null)
            return (method, args, invoker) -> {
                methodWriterListener.onWrite(method.getName(), args);
                return invoker.apply(method, args);
            };

        return (method, args, invoker) -> {
            methodWriterListener.onWrite(method.getName(), args);
            return interceptor.intercept(method, args, invoker);
        };
    }


    Object intercept(Method method, Object[] args, BiFunction<Method, Object[], Object> invoker);
}
