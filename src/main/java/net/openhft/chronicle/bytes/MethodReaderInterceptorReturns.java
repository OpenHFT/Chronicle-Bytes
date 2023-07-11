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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Functional interface representing an interceptor for methods invoked through a MethodReader.
 * <p>
 * The intercept method is called whenever a method is invoked on the MethodReader. It provides
 * an opportunity to modify, inspect, or take action based on the method invocation.
 */
@FunctionalInterface
public interface MethodReaderInterceptorReturns {
    /**
     * Intercepts a method invocation.
     *
     * @param m          the method that is being invoked
     * @param o          the object the underlying method is invoked from
     * @param args       the arguments used for the method call
     * @param invocation a functional interface representing the invocation of the method
     * @return the result of the method invocation, which can be the original result or a modified one
     * @throws InvocationTargetException if the invoked method throws an exception
     */
    Object intercept(Method m, Object o, Object[] args, Invocation invocation)
            throws InvocationTargetException;
}
