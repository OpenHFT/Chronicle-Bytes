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

import java.nio.BufferUnderflowException;

/**
 * This class implements the xxHash algorithm for hashing byte stores.
 * xxHash is a non-cryptographic hash function known for its speed.
 *
 * <p>Migrated from Zero-Allocation-Hashing.
 *
 * @see BytesStoreHash
 * @see BytesStore
 */
// Migration of XxHash from Zero-Allocation-Hashing
@SuppressWarnings("rawtypes")
public class XxHash implements BytesStoreHash<BytesStore> {
    // Primes if treated as unsigned
    private static final long P1 = -7046029288634856825L;
    private static final long P2 = -4417276706812531889L;
    private static final long P3 = 1609587929392839161L;
    private static final long P4 = -8796714831421723037L;
    private static final long P5 = 2870177450012600261L;

    /**
     * Singleton instance of XxHash with seed P4.
     */
    public static final XxHash INSTANCE = new XxHash(P4);

    private final long seed;

    /**
     * Constructs a new instance of XxHash with the specified seed.
     *
     * @param seed the seed for hash computation.
     */
    public XxHash(long seed) {
        this.seed = seed;
    }

    /**
     * Finalizes the hash calculation.
     *
     * @param hash the hash to finalize.
     * @return the finalized hash.
     */
    private static long finishUp(long hash) {
        hash ^= hash >>> 33;
        hash *= P2;
        hash ^= hash >>> 29;
        hash *= P3;
        hash ^= hash >>> 32;
        return hash;
    }

    /**
     * Fetches 64 bits from the byte store at the given offset.
     *
     * @param bytes the byte store.
     * @param off   the offset.
     * @return the fetched 64 bits.
     * @throws BufferUnderflowException If there are not enough bytes remaining in the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long fetch64(BytesStore bytes, @NonNegative long off) throws IllegalStateException, BufferUnderflowException {
        return bytes.readLong(off);
    }

    /**
     * Fetches 32 bits from the byte store at the given offset.
     *
     * @param bytes the byte store.
     * @param off   the offset.
     * @return the fetched 32 bits.
     * @throws BufferUnderflowException If there are not enough bytes remaining in the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long fetch32(BytesStore bytes, @NonNegative long off) throws IllegalStateException, BufferUnderflowException {
        return bytes.readUnsignedInt(off);
    }

    /**
     * Fetches 8 bits from the byte store at the given offset.
     *
     * @param bytes the byte store.
     * @param off   the offset.
     * @return the fetched 8 bits.
     * @throws BufferUnderflowException If there are not enough bytes remaining in the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long fetch8(BytesStore bytes, @NonNegative long off) throws IllegalStateException, BufferUnderflowException {
        return bytes.readUnsignedByte(off);
    }

    /**
     * Calculates the hash code of the given byte store.
     *
     * @param bytes the byte store to be hashed.
     * @return the hash code.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public long applyAsLong(BytesStore bytes) {
        return applyAsLong(bytes, bytes.readRemaining());
    }

    /**
     * Computes a hash value for the given byte store with a specified length.
     *
     * @param bytes  the byte store.
     * @param length the number of bytes to hash.
     * @return the hash value.
     * @throws BufferUnderflowException If there is not enough data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public long applyAsLong(BytesStore bytes, @NonNegative long length) throws IllegalStateException, BufferUnderflowException {
        long hash;
        long remaining = length;
        long off = bytes.readPosition();

        if (remaining >= 32) {
            long v1 = seed + P1 + P2;
            long v2 = seed + P2;
            long v3 = seed;
            long v4 = seed - P1;

            do {
                v1 += fetch64(bytes, off) * P2;
                v1 = Long.rotateLeft(v1, 31);
                v1 *= P1;

                v2 += fetch64(bytes, off + 8) * P2;
                v2 = Long.rotateLeft(v2, 31);
                v2 *= P1;

                v3 += fetch64(bytes, off + 16) * P2;
                v3 = Long.rotateLeft(v3, 31);
                v3 *= P1;

                v4 += fetch64(bytes, off + 24) * P2;
                v4 = Long.rotateLeft(v4, 31);
                v4 *= P1;

                off += 32;
                remaining -= 32;
            } while (remaining >= 32);

            hash = Long.rotateLeft(v1, 1)
                    + Long.rotateLeft(v2, 7)
                    + Long.rotateLeft(v3, 12)
                    + Long.rotateLeft(v4, 18);

            v1 *= P2;
            v1 = Long.rotateLeft(v1, 31);
            v1 *= P1;
            hash ^= v1;
            hash = hash * P1 + P4;

            v2 *= P2;
            v2 = Long.rotateLeft(v2, 31);
            v2 *= P1;
            hash ^= v2;
            hash = hash * P1 + P4;

            v3 *= P2;
            v3 = Long.rotateLeft(v3, 31);
            v3 *= P1;
            hash ^= v3;
            hash = hash * P1 + P4;

            v4 *= P2;
            v4 = Long.rotateLeft(v4, 31);
            v4 *= P1;
            hash ^= v4;
            hash = hash * P1 + P4;
        } else {
            hash = seed + P5;
        }

        hash += length;

        while (remaining >= 8) {
            long k1 = fetch64(bytes, off);
            k1 *= P2;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= P1;
            hash ^= k1;
            hash = Long.rotateLeft(hash, 27) * P1 + P4;
            off += 8;
            remaining -= 8;
        }

        if (remaining >= 4) {
            hash ^= fetch32(bytes, off) * P1;
            hash = Long.rotateLeft(hash, 23) * P2 + P3;
            off += 4;
            remaining -= 4;
        }

        while (remaining != 0) {
            hash ^= fetch8(bytes, off) * P5;
            hash = Long.rotateLeft(hash, 11) * P1;
            --remaining;
            ++off;
        }

        return finishUp(hash);
    }
}
