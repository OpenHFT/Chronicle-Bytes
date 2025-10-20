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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A functional interface representing the act of invoking a method.
 * <p>
 * It is primarily used as a callback within interceptor patterns such as
 * {@link net.openhft.chronicle.bytes.MethodReaderInterceptorReturns} to
 * allow the interceptor to control or augment the actual method execution.
 */
@FunctionalInterface
public interface Invocation {
    /**
     * Invokes the supplied method.
     *
     * @param m    the {@link Method} to invoke, must not be {@code null}
     * @param o    the instance on which the method should be invoked, {@code null} for static methods
     * @param args the arguments to be passed to the method, may be {@code null}
     * @return the result of the invocation or {@code null} if the method returns void
     * @throws InvocationTargetException if the underlying method throws an exception
     * @throws IllegalArgumentException  if the arguments do not match the method signature
     * @throws NullPointerException      if {@code m} is {@code null}
     */
    Object invoke(Method m, Object o, Object[] args)
            throws InvocationTargetException;
}
