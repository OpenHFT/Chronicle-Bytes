/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

/**
 * Represents an operation that intercepts a method call, possibly modifies the input argument,
 * and determines whether to proceed with the original operation.
 *
 * <p>This interface can be used to implement custom behaviors before an operation is carried out,
 * such as validation, transformation, or cancellation of the operation based on the method
 * parameters or other conditions.
 */
@FunctionalInterface
public interface UpdateInterceptor {

    /**
     * Potentially modifies the provided argument and determines whether to proceed with
     * the operation that was intercepted.
     *
     * @param methodName the name of the method that was intercepted
     * @param t          the input argument to the method - for a method call with multiple arguments,
     *                   only the last one is passed. This object may be modified by this method.
     * @return a boolean value indicating whether to proceed with the operation. If false,
     * the operation that was intercepted will not be carried out.
     * @throws IllegalArgumentException if {@code t} is an instance of Validatable and its validation fails
     */
    boolean update(String methodName, Object t) throws IllegalArgumentException;

}
