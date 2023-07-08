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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
/**
 * This enum provides hashing functionality for byte stores.
 * Hashing is a process of converting data of arbitrary size to fixed-size values.
 *
 * @see BytesStoreHash
 * @see BytesStore
 */
@SuppressWarnings("rawtypes")
public enum VanillaBytesStoreHash implements BytesStoreHash<BytesStore> {
    /**
     * Singleton instance of VanillaBytesStoreHash.
     */
    INSTANCE;

    public static final int K0 = 0x6d0f27bd;
    public static final int K1 = 0xc1f3bfc9;
    public static final int K2 = 0x6b192397;
    public static final int K3 = 0x6b915657;
    public static final int M0 = 0x5bc80bad;
    public static final int M1 = 0xea7585d7;
    public static final int M2 = 0x7a646e19;
    public static final int M3 = 0x855dd4db;

    /**
     * Constant indicating the byte order for reading multi-byte values.
     */
    private static final int HI_BYTES = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 4 : 0;

    /**
     * Agitates the given long value to generate a hash value.
     *
     * @param l The input value.
     * @return The agitated hash value.
     */
    public static long agitate(long l) {
        l ^= Long.rotateLeft(l, 26);
        l ^= Long.rotateRight(l, 17);
        return l;
    }

    /**
     * Computes a 64-bit hash value for the given BytesStore.
     *
     * @param store The {@link BytesStore} to compute the hash for.
     * @return A 64-bit hash value.
     * @throws IllegalStateException     If the state is illegal.
     * @throws BufferUnderflowException If there is not enough data.
     */
    @Override
    public long applyAsLong(@NotNull BytesStore store) {
        int remaining = Math.toIntExact(store.realReadRemaining());
        try {
            return applyAsLong(store, remaining);
        } catch (IllegalStateException | BufferUnderflowException e) {
            throw Jvm.rethrow(e);
        }
    }

    /**
     * Computes a 64-bit hash value for the given BytesStore.
     *
     * @param bytes  The {@link BytesStore} to compute the hash for.
     * @param length The number of bytes to process.
     * @return A 64-bit hash value.
     * @throws IllegalStateException     If the state is illegal.
     * @throws BufferUnderflowException If there is not enough data.
     */
    @Override
    public long applyAsLong(BytesStore bytes, @NonNegative long length) throws IllegalStateException, BufferUnderflowException {
        long start = bytes.readPosition();
        if (length <= 8) {
            if (length == 0)
                return 0;
            long l = bytes.readIncompleteLong(start);
            return agitate(l * K0 + (l >> 32) * K1);
        }
        // use two hashes so that when they are combined the 64-bit hash is more random.
        long h0 = length * K0;
        long h1 = 0;
        long h2 = 0;
        long h3 = 0;
        int i;
        // optimise chunks of 32 bytes but this is the same as the next loop.
        for (i = 0; i < length - 31; i += 32) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }
            long addrI = start + i;
            long l0 = bytes.readLong(addrI);
            int l0a = bytes.readInt(addrI + HI_BYTES);
            long l1 = bytes.readLong(addrI + 8);
            int l1a = bytes.readInt(addrI + 8 + HI_BYTES);
            long l2 = bytes.readLong(addrI + 16);
            int l2a = bytes.readInt(addrI + 16 + HI_BYTES);
            long l3 = bytes.readLong(addrI + 24);
            int l3a = bytes.readInt(addrI + 24 + HI_BYTES);

            h0 += (l0 + l1a - l2a) * M0;
            h1 += (l1 + l2a - l3a) * M1;
            h2 += (l2 + l3a - l0a) * M2;
            h3 += (l3 + l0a - l1a) * M3;
        }

        // perform a hash of the end.
        long left = length - i;
        if (left > 0) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }

            long addrI = start + i;
            long l0 = bytes.readIncompleteLong(addrI);
            int l0a = (int) (l0 >> 32);
            long l1 = bytes.readIncompleteLong(addrI + 8);
            int l1a = (int) (l1 >> 32);
            long l2 = bytes.readIncompleteLong(addrI + 16);
            int l2a = (int) (l2 >> 32);
            long l3 = bytes.readIncompleteLong(addrI + 24);
            int l3a = (int) (l3 >> 32);

            h0 += (l0 + l1a - l2a) * M0;
            h1 += (l1 + l2a - l3a) * M1;
            h2 += (l2 + l3a - l0a) * M2;
            h3 += (l3 + l0a - l1a) * M3;
        }
        return agitate(h0) ^ agitate(h1)
                ^ agitate(h2) ^ agitate(h3);
    }
}
