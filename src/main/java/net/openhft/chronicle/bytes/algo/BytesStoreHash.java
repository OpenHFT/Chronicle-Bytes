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
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;
import java.util.function.ToLongFunction;

/**
 * An interface to compute a hash value for a given {@link BytesStore}.
 *
 * @param <B> Type parameter that extends BytesStore
 */
@SuppressWarnings("rawtypes")
public interface BytesStoreHash<B extends BytesStore> extends ToLongFunction<B> {

    /**
     * Computes a hash value for the given {@link BytesStore}.
     *
     * @param b the {@link BytesStore} for which the hash is to be computed
     * @return a long representing the hash value
     */
    static long hash(@NotNull BytesStore b) {
        return b.isDirectMemory()
                ? OptimisedBytesStoreHash.INSTANCE.applyAsLong(b)
                : VanillaBytesStoreHash.INSTANCE.applyAsLong(b);
    }

    /**
     * Computes a hash value for the given {@link BytesStore}, considering the specified length.
     *
     * @param b      the {@link BytesStore} for which the hash is to be computed
     * @param length the length to be considered for hashing
     * @return a long representing the hash value
     * @throws IllegalStateException    if any state specific exception occurs
     * @throws BufferUnderflowException if there's less data than expected
     */
    static long hash(@NotNull BytesStore b, @NonNegative long length) throws IllegalStateException, BufferUnderflowException {
        return b.isDirectMemory()
                ? OptimisedBytesStoreHash.INSTANCE.applyAsLong(b, length)
                : VanillaBytesStoreHash.INSTANCE.applyAsLong(b, length);
    }

    /**
     * Computes a 32-bit hash value for the given {@link BytesStore}.
     *
     * @param b the {@link BytesStore} for which the hash is to be computed
     * @return an int representing the 32-bit hash value
     */
    static int hash32(BytesStore b) {
        long hash = hash(b);
        return (int) (hash ^ (hash >>> 32));
    }

    /**
     * Computes a 32-bit hash value for the given {@link BytesStore}, considering the specified length.
     *
     * @param b      the {@link BytesStore} for which the hash is to be computed
     * @param length the length to be considered for hashing
     * @return an int representing the 32-bit hash value
     * @throws IllegalStateException    if any state specific exception occurs
     * @throws BufferUnderflowException if there's less data than expected
     */
    static int hash32(@NotNull BytesStore b, @NonNegative int length) throws IllegalStateException, BufferUnderflowException {
        long hash = hash(b, length);
        return (int) (hash ^ (hash >>> 32));
    }

    /**
     * Applies the hash function to the given {@link BytesStore} with a specified length.
     *
     * @param bytes  the {@link BytesStore} for which the hash is to be computed
     * @param length the length to be considered for hashing
     * @return a long representing the hash value
     * @throws IllegalStateException    if any state specific exception occurs
     * @throws BufferUnderflowException if there's less data than expected
     */
    long applyAsLong(BytesStore bytes, long length) throws IllegalStateException, BufferUnderflowException;
}
