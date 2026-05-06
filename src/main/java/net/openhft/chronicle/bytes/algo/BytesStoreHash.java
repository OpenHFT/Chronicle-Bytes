/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
 * Implementations provide fast, deterministic hashing based on little-endian
 * variants of the Murmur3 algorithm.
 * This interface also exposes static helpers for computing 32-bit and 64-bit
 * hashes using the bundled implementations.
 *
 * <p> Implementations should avoid allocating memory and may assume that
 * {@code length} bytes can be read without extra bounds checks.
 *
 * See {@code algo-overview.adoc} for usage examples.
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
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    static long hash(@NotNull BytesStore<?, ?> b) {
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
     * @throws BufferUnderflowException       if the length specified is greater than the available bytes
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    static long hash(@NotNull BytesStore<?, ?> b, @NonNegative long length) throws IllegalStateException, BufferUnderflowException {
        return b.isDirectMemory()
                ? OptimisedBytesStoreHash.INSTANCE.applyAsLong(b, length)
                : VanillaBytesStoreHash.INSTANCE.applyAsLong(b, length);
    }

    /**
     * Computes a 32-bit hash value of the given {@link BytesStore}.
     *
     * @param b the {@link BytesStore} to compute the hash for.
     * @return the 32-bit hash value.
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    static int hash32(BytesStore<?, ?> b) {
        long hash = hash(b);
        return (int) (hash ^ (hash >>> 32));
    }

    /**
     * Computes a 32-bit hash value of the given {@link BytesStore} with a specified length.
     *
     * @param b      the {@link BytesStore} to compute the hash for.
     * @param length the number of bytes to include in the hash computation.
     * @return the 32-bit hash value.
     * @throws BufferUnderflowException       if the length specified is greater than the available bytes
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     */
    static int hash32(@NotNull BytesStore<?, ?> b, @NonNegative int length) throws IllegalStateException, BufferUnderflowException {
        long hash = hash(b, length);
        return (int) (hash ^ (hash >>> 32));
    }

    /**
     * Computes a 64-bit hash value of the given {@link BytesStore} with a specified length.
     *
     * @param bytes  the {@link BytesStore} to compute the hash for.
     * @param length the number of bytes to include in the hash computation.
     * @return the 64-bit hash value.
     * @throws BufferUnderflowException       if the length specified is greater than the available bytes
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if this resource was accessed by multiple threads in an unsafe way
     *
     * <p> Implementations are typically branch-free and perform no allocations.
     */
    long applyAsLong(BytesStore<?, ?> bytes, long length) throws IllegalStateException, BufferUnderflowException;
}
