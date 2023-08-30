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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;

import java.nio.BufferUnderflowException;

/**
 * This interface defines methods for reading data of different types (byte, short, int, long) from
 * a certain offset position. The methods do not necessarily perform boundary checks, so it's
 * crucial to ensure memory bounds are validated before invoking these methods.
 * <p>
 * Note: Calling these methods without proper boundary checks can lead to undefined results
 * and even JVM crashes.
 */
public interface UncheckedRandomDataInput {

    /**
     * Read a byte at an offset possibly not checking memory bounds.
     * <p>
     * Memory bounds must be checked before invoking this method or else the result
     * is undefined and may lead to JVM crashes.
     *
     * @param offset Offset position from which the byte is to be read.
     * @return The byte read from the specified offset.
     * @throws BufferUnderflowException       If the offset is beyond the boundary of the data source.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    byte readByte(@NonNegative long offset) throws ClosedIllegalStateException;

    /**
     * Read a short at an offset possibly not checking memory bounds.
     * <p>
     * Memory bounds must be checked before invoking this method or else the result
     * is undefined and may lead to JVM crashes.
     *
     * @param offset Offset position from which the short is to be read.
     * @return The short read from the specified offset.
     * @throws BufferUnderflowException       If the offset is beyond the boundary of the data source.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    short readShort(@NonNegative long offset) throws ClosedIllegalStateException;

    /**
     * Read an int at an offset possibly not checking memory bounds.
     * <p>
     * Memory bounds must be checked before invoking this method or else the result
     * is undefined and may lead to JVM crashes.
     *
     * @param offset Offset position from which the integer is to be read.
     * @return The integer read from the specified offset.
     * @throws BufferUnderflowException       If the offset is beyond the boundary of the data source.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    int readInt(@NonNegative long offset) throws ClosedIllegalStateException;

    /**
     * Read a long at an offset possibly not checking memory bounds.
     * <p>
     * Memory bounds must be checked before invoking this method or else the result
     * is undefined and may lead to JVM crashes.
     *
     * @param offset Offset position from which the long is to be read.
     * @return The long read from the specified offset.
     * @throws BufferUnderflowException       If the offset is beyond the boundary of the data source.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long readLong(@NonNegative long offset) throws ClosedIllegalStateException;

}