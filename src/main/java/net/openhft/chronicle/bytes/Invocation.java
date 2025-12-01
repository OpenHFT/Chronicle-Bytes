/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Functional callback representing a method invocation.
 * <p>
 * Used by method reader interceptors to delegate the actual call while still allowing wrappers to
 * log, short-circuit or alter arguments/return values.
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
