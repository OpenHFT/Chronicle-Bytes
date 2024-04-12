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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

/**
 * This class provides a mechanism for interning Strings with 8-bit characters.
 * Interning is a method of storing only one copy of each distinct String value,
 * which must be immutable.
 *
 * @see AbstractInterner
 */
public class Bit8StringInterner extends AbstractInterner<String> {

    /**
     * A pool of StringBuilder objects used to construct the interned Strings.
     */
    private static final ScopedResourcePool<StringBuilder> SBP = StringBuilderPool.createThreadLocal(1);

    /**
     * Constructs a new Bit8StringInterner with the specified capacity.
     *
     * @param capacity the initial capacity for the interner
     */
    public Bit8StringInterner(int capacity) {
        super(capacity);
    }

    /**
     * Returns a String value from the provided {@link BytesStore} object.
     *
     * @param cs     the BytesStore object from which to get the String value
     * @param length the length of the string to be interned
     * @return the interned String value
     * @throws BufferUnderflowException       If the BytesStore doesn't have enough remaining capacity
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @SuppressWarnings("rawtypes")
    @Override
    @NotNull
    protected String getValue(@NotNull BytesStore<?, ?> cs, @NonNegative int length) throws IllegalStateException, BufferUnderflowException {
        try (ScopedResource<StringBuilder> sbTl = SBP.get()) {
            StringBuilder sb = sbTl.get();
            for (int i = 0; i < length; i++)
                sb.append((char) cs.readUnsignedByte(cs.readPosition() + i));
            return sb.toString();
        }
    }
}
