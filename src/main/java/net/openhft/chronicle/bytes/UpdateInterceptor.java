/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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
 * Represents an operation that accepts a single input argument then modifies that same instance.
 */
@FunctionalInterface
public interface UpdateInterceptor {

    /**
     * modifies {@code t} with changed data
     *
     * @param methodName the name of the method
     * @param t          the input argument - for a method call with multiple arguments, the last one is passed
     * @return whether to proceed. If false, don't write
     */
    boolean update(String methodName, Object t);

}
