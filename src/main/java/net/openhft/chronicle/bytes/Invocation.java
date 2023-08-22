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
 * Functional interface representing an invocation of a method.
 * <p>
 * This interface is designed to be used with the {@link MethodReaderInterceptorReturns} interface,
 * providing a flexible way to handle method invocations, such as performing the invocation,
 * or adding custom behaviors before or after the invocation.
 */
@FunctionalInterface
public interface Invocation {
    /**
     * Invokes a method with the specified parameters.
     *
     * @param m    the method to be invoked
     * @param o    the object from which the method is invoked
     * @param args the arguments to be passed to the method
     * @return the result of the method invocation
     * @throws InvocationTargetException If the invoked method throws an exception
     */
    Object invoke(Method m, Object o, Object[] args)
            throws InvocationTargetException;
}
