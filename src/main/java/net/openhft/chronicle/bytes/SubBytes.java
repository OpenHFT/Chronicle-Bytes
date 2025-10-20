/*
 * Copyright 2016-2025 chronicle.software
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
 * Represents a fixed size view over a region of another {@link BytesStore}.
 * The view has its own start offset and capacity, effectively creating a slice
 * of the parent store. It is non elastic and extends {@link VanillaBytes}.
 *
 * @param <U> the type of the underlying object
 */
@SuppressWarnings("rawtypes")
public class SubBytes<U> extends VanillaBytes<U> {
    private final long start;
    private final long capacity;

    /**
     * Creates a sub region view of the supplied {@code bytesStore}.
     * The new view spans from {@code start} for {@code capacity} bytes.
     * The initial read and write positions are set to {@code start} and the
     * write limit to {@code start + capacity}.
     *
     * @param bytesStore the parent store
     * @param start      absolute start offset within {@code bytesStore}
     * @param capacity   number of bytes in the sub region
     * @throws BufferUnderflowException       if {@code start + capacity} exceeds the parent capacity
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if accessed by multiple threads in an unsafe way
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
     * Returns the fixed capacity supplied at construction.
     */
    @NonNegative
    @Override
    public long capacity() {
        return capacity;
    }

    /**
     * Returns the absolute start offset within the parent store.
     */
    @NonNegative
    @Override
    public long start() {
        return start;
    }

    /**
     * Returns the real capacity, which is identical to {@link #capacity()} for this view.
     */
    @NonNegative
    @Override
    public long realCapacity() {
        return capacity;
    }
}
