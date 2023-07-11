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
import net.openhft.chronicle.core.pool.StringBuilderPool;
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
    private static final StringBuilderPool SBP = new StringBuilderPool();

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
     * @throws IllegalStateException    if this method is called in an inappropriate state
     * @throws BufferUnderflowException if the BytesStore doesn't have enough remaining capacity
     */
    @SuppressWarnings("rawtypes")
    @Override
    @NotNull
    protected String getValue(@NotNull BytesStore cs, @NonNegative int length) throws IllegalStateException, BufferUnderflowException {
        StringBuilder sb = SBP.acquireStringBuilder();
        for (int i = 0; i < length; i++)
            sb.append((char) cs.readUnsignedByte(cs.readPosition() + i));
        return sb.toString();
    }
}
