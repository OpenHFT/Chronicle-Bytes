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

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

/**
 * A {@code SubBytes} object represents a subsection of a {@link BytesStore} from a given start index up to a specified capacity.
 * This is useful when you want to handle a specific part of the data within a larger BytesStore.
 *
 * @param <U> the type of the BytesStore
 */
@SuppressWarnings("rawtypes")
public class SubBytes<U> extends VanillaBytes<U> {
    private final long start;
    private final long capacity;

    /**
     * Class constructor. Creates a SubBytes from the bytes in a specified BytesStore from a specified Offset to
     * a specified index (excluding).
     *
     * @param bytesStore the parent BytesStore that contains the data
     * @param start      the start index in the parent BytesStore from which the SubBytes start
     * @param capacity   the number of elements from the start index that the SubBytes cover
     * @throws BufferUnderflowException       If the capacity is less than the start index
     * @throws IllegalArgumentException       If any other argument issue occurs
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @SuppressWarnings("this-escape")
    public SubBytes(@NotNull BytesStore<?, ?> bytesStore, @NonNegative long start, @NonNegative long capacity)
            throws IllegalStateException, IllegalArgumentException, BufferUnderflowException {
        super(bytesStore);
        this.start = start;
        this.capacity = capacity;
        clear();
        readLimit(writeLimit());
    }

    /**
     * @return the capacity as a long value
     */
    @NonNegative
    @Override
    public long capacity() {
        return capacity;
    }

    /**
     * @return the start index as a long value
     */
    @NonNegative
    @Override
    public long start() {
        return start;
    }

    /**
     * @return the capacity as a long value
     */
    @NonNegative
    @Override
    public long realCapacity() {
        return capacity;
    }
}
