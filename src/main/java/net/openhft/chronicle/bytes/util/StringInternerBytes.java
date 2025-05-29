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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.BytesUtil.toCharArray;

/**
 * {@link net.openhft.chronicle.core.pool.StringInterner} specialised for
 * {@link Bytes} instances. Byte sequences are interpreted as 8-bit characters
 * (for example ISO-8859-1) when forming {@link String} objects.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * StringInternerBytes interner = new StringInternerBytes(64);
 * String s = interner.intern(bytes, length);
 * }</pre>
 */
public class StringInternerBytes extends StringInterner {

    /**
     * Constructs a new {@code StringInternerBytes} instance with the specified capacity.
     *
     * @param capacity the number of strings that can be stored in the interner.
     * @throws IllegalArgumentException If the specified capacity is negative.
     */
    public StringInternerBytes(@NonNegative int capacity)
            throws IllegalArgumentException {
        super(capacity);
    }

    /**
     * Interns the string representation of the given bytes. The length of the string
     * is automatically determined based on the remaining bytes to read.
     *
     * @param bytes the bytes to be converted and interned as a string.
     * @return the interned string representation of the bytes.
     * @throws ArithmeticException      If there is an integer overflow when calculating the length.
     * @throws BufferUnderflowException If there are not enough bytes remaining to read.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public String intern(@NotNull final Bytes<?> bytes)
            throws ArithmeticException, IllegalStateException, BufferUnderflowException {
        return intern(bytes, Maths.toUInt31(bytes.readRemaining()));
    }

    /**
     * Reads {@code length} bytes from {@code bytes.readPosition()}, converts them
     * using an 8-bit character encoding and interns the resulting {@link String}.
     * The read position of {@code bytes} is advanced after the conversion.
     *
     * @param bytes  the bytes to convert
     * @param length the number of bytes to read from {@code bytes}
     * @return the interned string
     * @throws IllegalArgumentException if {@code length} is negative
     * @throws BufferUnderflowException If there are not enough bytes remaining to read.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
