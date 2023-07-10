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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.BytesUtil.toCharArray;

/**
 * A specialized {@link StringInterner} to intern strings represented in {@link Bytes}.
 */
public class StringInternerBytes extends StringInterner {

    /**
     * Constructs a StringInternerBytes with the specified capacity.
     *
     * @param capacity the capacity of the interner
     * @throws IllegalArgumentException if the capacity is negative
     */
    public StringInternerBytes(@NonNegative int capacity)
            throws IllegalArgumentException {
        super(capacity);
    }

    /**
     * Interns the string represented in the provided bytes.
     *
     * @param bytes the bytes representing the string to be interned
     * @return the interned string
     * @throws ArithmeticException        if the length overflows integer size
     * @throws IllegalStateException      if the underlying data structure is invalid
     * @throws BufferUnderflowException   if the buffer's limits are exceeded
     */
    public String intern(@NotNull final Bytes<?> bytes)
            throws ArithmeticException, IllegalStateException, BufferUnderflowException {
        return intern(bytes, Maths.toUInt31(bytes.readRemaining()));
    }

    /**
     * Interns the string represented in the provided bytes up to the specified length.
     * <p>
     * The bytes are converted to an ISO-8859-1 string. If a string equivalent to the bytes is
     * already in the pool, that string is returned; otherwise, the new string is added to the pool.
     *
     * @param bytes  the bytes representing the string to be interned
     * @param length the length up to which to parse the string
     * @return the interned string made from bytes
     * @throws IllegalStateException      if the underlying data structure is invalid
     * @throws BufferUnderflowException   if the buffer's limits are exceeded
     */
    public String intern(@NotNull final Bytes<?> bytes, @NonNegative int length)
            throws IllegalStateException, BufferUnderflowException {
        try {

            // Throw exception if length is negative
            if (length < 0) {
                throw new IllegalArgumentException("length=" + length);
            }

            // Calculate hash code of the bytes
            int hash32 = BytesStoreHash.hash32(bytes, length);
            int h = hash32 & mask;
            String s = interner[h];
            long position = bytes.readPosition();

            // Check if the bytes match an existing string in the pool
            if (bytes.isEqual(position, length, s)) {
                return s;
            }

            // Calculate secondary hash
            int h2 = (hash32 >> shift) & mask;
            String s2 = interner[h2];
            if (bytes.isEqual(position, length, s2)) {
                return s2;
            }

            // Convert bytes to characters
            char[] chars = toCharArray(bytes, position, length);

            // Determine where to place the new string in the interner array
            final int toPlace = s == null || (s2 != null && toggle()) ? h : h2;

            // Create a new string and add to the pool
            String result = StringUtils.newString(chars);
            interner[toPlace] = result;
            return result;

        } finally {
            // Skip read position by length
            bytes.readSkip(length);
        }
    }
}
