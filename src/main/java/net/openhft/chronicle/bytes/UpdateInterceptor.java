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
     * @param t the input argument to the method - for a method call with multiple arguments,
     *           only the last one is passed. This object may be modified by this method.
     * @throws IllegalArgumentException if {@code t} is an instance of Validatable and its validation fails
     * @return a boolean value indicating whether to proceed with the operation. If false,
     *         the operation that was intercepted will not be carried out.
     */
    boolean update(String methodName, Object t) throws IllegalArgumentException;

}
