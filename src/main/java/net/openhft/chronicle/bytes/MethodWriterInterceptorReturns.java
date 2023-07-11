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

import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * Functional interface representing an interceptor for methods invoked through a MethodWriter.
 * <p>
 * The interceptor provides a means to manipulate or monitor the invocation of methods. It allows for actions
 * to be taken before or after a method invocation, or to bypass the method invocation entirely.
 * <p>
 * This can be particularly useful for scenarios such as logging method invocations, modifying method parameters,
 * changing return values, or implementing pre- and post-method invocation actions.
 */
@FunctionalInterface
public interface MethodWriterInterceptorReturns {

    /**
     * Intercepts a method invocation.
     *
     * @param method  the method that is being invoked
     * @param args    the arguments used for the method call
     * @param invoker a functional interface representing the invocation of the method
     * @return the next object to use if there is any chaining, either this, null if no chaining, or another object.
     */
    Object intercept(Method method, Object[] args, BiFunction<Method, Object[], Object> invoker);
}
