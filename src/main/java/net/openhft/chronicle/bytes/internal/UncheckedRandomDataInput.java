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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.annotation.NonNegative;

import java.nio.BufferUnderflowException;

public interface UncheckedRandomDataInput {

    /**
     * Read a byte at an offset possibly not checking memory bounds.
     * <p>
     * Memory bounds must be checked before invoking this method or else the result
     * is undefined and may lead to JVM crashes.
     *
     * @param offset to read
     * @return the long
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    byte readByte(@NonNegative long offset);

    /**
     * Read a short at an offset possibly not checking memory bounds.
     * <p>
     * Memory bounds must be checked before invoking this method or else the result
     * is undefined and may lead to JVM crashes.
     *
     * @param offset to read
     * @return the short
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    short readShort(@NonNegative long offset);

    /**
     * Read an int at an offset possibly not checking memory bounds.
     * <p>
     * Memory bounds must be checked before invoking this method or else the result
     * is undefined and may lead to JVM crashes.
     *
     * @param offset to read
     * @return the int
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    int readInt(@NonNegative long offset);

    /**
     * Read a long at an offset possibly not checking memory bounds.
     * <p>
     * Memory bounds must be checked before invoking this method or else the result
     * is undefined and may lead to JVM crashes.
     *
     * @param offset to read
     * @return the long
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    long readLong(@NonNegative long offset);

}