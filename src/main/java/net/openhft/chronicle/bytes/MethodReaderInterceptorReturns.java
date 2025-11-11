/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
