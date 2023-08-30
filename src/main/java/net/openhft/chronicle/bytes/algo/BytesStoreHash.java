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
package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;
import java.util.function.ToLongFunction;

/**
 * Represents a function that computes a 64-bit hash value from a {@link BytesStore}.
 * This interface provides static methods for computing 32-bit and 64-bit hash values,
 * using either optimized or vanilla implementations.
 *
 * @param <B> the type of {@link BytesStore} that this function can compute hash values for.
 */
@SuppressWarnings("rawtypes")
public interface BytesStoreHash<B extends BytesStore> extends ToLongFunction<B> {

    /**
     * Computes a 64-bit hash value of the given {@link BytesStore}.
     *
     * @param b the {@link BytesStore} to compute the hash for.
     * @return the 64-bit hash value.
     */
    static long hash(@NotNull BytesStore b) {
        return b.isDirectMemory()
                ? OptimisedBytesStoreHash.INSTANCE.applyAsLong(b)
                : VanillaBytesStoreHash.INSTANCE.applyAsLong(b);
    }

    /**
     * Computes a 64-bit hash value of the given {@link BytesStore} with a specified length.
     *
     * @param b      the {@link BytesStore} to compute the hash for.
     * @param length the number of bytes to include in the hash computation.
     * @return the 64-bit hash value.
     * @throws BufferUnderflowException If the length specified is greater than the available bytes.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    static long hash(@NotNull BytesStore b, @NonNegative long length) throws IllegalStateException, BufferUnderflowException {
        return b.isDirectMemory()
                ? OptimisedBytesStoreHash.INSTANCE.applyAsLong(b, length)
                : VanillaBytesStoreHash.INSTANCE.applyAsLong(b, length);
    }

    /**
     * Computes a 32-bit hash value of the given {@link BytesStore}.
     *
     * @param b the {@link BytesStore} to compute the hash for.
     * @return the 32-bit hash value.
     */
    static int hash32(BytesStore b) {
        long hash = hash(b);
        return (int) (hash ^ (hash >>> 32));
    }

    /**
     * Computes a 32-bit hash value of the given {@link BytesStore} with a specified length.
     *
     * @param b      the {@link BytesStore} to compute the hash for.
     * @param length the number of bytes to include in the hash computation.
     * @return the 32-bit hash value.
     * @throws BufferUnderflowException If the length specified is greater than the available bytes.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    static int hash32(@NotNull BytesStore b, @NonNegative int length) throws IllegalStateException, BufferUnderflowException {
        long hash = hash(b, length);
        return (int) (hash ^ (hash >>> 32));
    }

    /**
     * Computes a 64-bit hash value of the given {@link BytesStore} with a specified length.
     *
     * @param bytes  the {@link BytesStore} to compute the hash for.
     * @param length the number of bytes to include in the hash computation.
     * @return the 64-bit hash value.
     * @throws BufferUnderflowException If the length specified is greater than the available bytes.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long applyAsLong(BytesStore bytes, long length) throws IllegalStateException, BufferUnderflowException;
}
