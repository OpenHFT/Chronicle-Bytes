/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
