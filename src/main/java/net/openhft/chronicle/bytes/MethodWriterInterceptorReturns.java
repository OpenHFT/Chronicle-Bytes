/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
