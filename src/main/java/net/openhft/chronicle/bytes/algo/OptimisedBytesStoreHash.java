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
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;

import static net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash.*;

/**
 * Optimized hashing algorithm for BytesStore.
 * <p>
 * This enumeration implements BytesStoreHash for optimized hashing of BytesStore
 * depending on the size of the data and architecture of the system.
 */
@SuppressWarnings("rawtypes")
public enum OptimisedBytesStoreHash implements BytesStoreHash<BytesStore> {
    INSTANCE;

    public static final Memory MEMORY = OS.memory();
    public static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final int TOP_BYTES = IS_LITTLE_ENDIAN ? 4 : 0;

    /**
     * Computes a 64-bit hash value for the given BytesStore for data sizes between 1 to 7 bytes.
     *
     * @param store     The {@link BytesStore} to compute the hash for.
     * @param remaining The number of bytes to process.
     * @return A 64-bit hash value.
     * @throws BufferUnderflowException If there is not enough data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    static long applyAsLong1to7(@NotNull BytesStore store, @NonNegative int remaining) throws IllegalStateException, BufferUnderflowException {
        final long address = store.addressForRead(store.readPosition());

        return hash(readIncompleteLong(address, remaining));
    }

    /**
     * Computes a 64-bit hash value for the given BytesStore for exactly 8 bytes of data.
     *
     * @param store The {@link BytesStore} to compute the hash for.
     * @return A 64-bit hash value.
     * @throws BufferUnderflowException If there is not enough data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    static long applyAsLong8(@NotNull BytesStore store) throws IllegalStateException, BufferUnderflowException {
        final long address = store.addressForRead(store.readPosition());

        return hash0(MEMORY.readLong(address), MEMORY.readInt(address + TOP_BYTES));
    }

    /**
     * Performs a hash on a 64-bit long value.
     *
     * @param l The long value to hash.
     * @return A 64-bit hash value.
     */
    public static long hash(long l) {
        return hash0(l, l >> 32);
    }

    /**
     * Computes a 64-bit hash value using two long integers.
     *
     * @param l  The lower 64 bits.
     * @param hi The upper 64 bits.
     * @return A 64-bit hash value.
     */
    static long hash0(long l, long hi) {
        return agitate(l * K0 + hi * K1);
    }

    /**
     * Computes a 64-bit hash value for the given BytesStore for data sizes between 9 to 16 bytes.
     *
     * @param store     The {@link BytesStore} to compute the hash for.
     * @param remaining The number of bytes to process.
     * @return A 64-bit hash value.
     * @throws BufferUnderflowException If there is not enough data.
     */
    static long applyAsLong9to16(@NotNull BytesStore store, @NonNegative int remaining) throws BufferUnderflowException {
        @NotNull final BytesStore bytesStore = store.bytesStore();
        final long address = bytesStore.addressForRead(store.readPosition());
        long h0 = (long) remaining * K0;

        int left = remaining;
        long addrI = address;

        long l0 = readIncompleteLong(addrI, left);
        int l0a = (int) (l0 >> 32);
        long l1 = readIncompleteLong(addrI + 8, left - 8);
        int l1a = (int) (l1 >> 32);
        final long l2 = 0;
        final int l2a = 0;
        final long l3 = 0;
        final int l3a = 0;

        h0 += (l0 + l1a - l2a) * M0;
        long h1 = (l1 + l2a - l3a) * M1;
        long h2 = ((long) -l0a) * M2;
        long h3 = (l3 + l0a - l1a) * M3;

        return agitate(h0) ^ agitate(h1)
                ^ agitate(h2) ^ agitate(h3);
    }

    /**
     * Computes a 64-bit hash value for the given BytesStore for data sizes between 17 to 32 bytes.
     *
     * @param store     The {@link BytesStore} to compute the hash for.
     * @param remaining The number of bytes to process.
     * @return A 64-bit hash value.
     * @throws BufferUnderflowException If there is not enough data.
     */
    static long applyAsLong17to32(@NotNull BytesStore store, @NonNegative int remaining) throws BufferUnderflowException {
        @NotNull final BytesStore bytesStore = store.bytesStore();
        final long address = bytesStore.addressForRead(store.readPosition());
        long h0 = (long) remaining * K0;

        int left = remaining;
        long addrI = address;

        long l0 = MEMORY.readLong(addrI);
        int l0a = MEMORY.readInt(addrI + TOP_BYTES);
        long l1 = MEMORY.readLong(addrI + 8);
        int l1a = MEMORY.readInt(addrI + 8 + TOP_BYTES);
        long l2 = readIncompleteLong(addrI + 16, left - 16);
        int l2a = (int) (l2 >> 32);
        long l3 = readIncompleteLong(addrI + 24, left - 24);
        int l3a = (int) (l3 >> 32);

        h0 += (l0 + l1a - l2a) * M0;
        long h1 = (l1 + l2a - l3a) * M1;
        long h2 = (l2 + l3a - l0a) * M2;
        long h3 = (l3 + l0a - l1a) * M3;

        return agitate(h0) ^ agitate(h1)
                ^ agitate(h2) ^ agitate(h3);
    }

    /**
     * Computes a 64-bit hash value for the given BytesStore for data sizes that are multiple of 32 bytes.
     *
     * @param store     The {@link BytesStore} to compute the hash for.
     * @param remaining The number of bytes to process.
     * @return A 64-bit hash value.
     * @throws BufferUnderflowException If there is not enough data.
     */
    public static long applyAsLong32bytesMultiple(@NotNull BytesStore store, @NonNegative int remaining) throws BufferUnderflowException {
        @NotNull final BytesStore bytesStore = store.bytesStore();
        final long address = bytesStore.addressForRead(store.readPosition());
        long h0 = (long) remaining * K0;
        long h1 = 0;
        long h2 = 0;
        long h3 = 0;

        int i;
        for (i = 0; i < remaining - 31; i += 32) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }
            long addrI = address + i;
            long l0 = MEMORY.readLong(addrI);
            int l0a = MEMORY.readInt(addrI + TOP_BYTES);
            long l1 = MEMORY.readLong(addrI + 8);
            int l1a = MEMORY.readInt(addrI + 8 + TOP_BYTES);
            long l2 = MEMORY.readLong(addrI + 16);
            int l2a = MEMORY.readInt(addrI + 16 + TOP_BYTES);
            long l3 = MEMORY.readLong(addrI + 24);
            int l3a = MEMORY.readInt(addrI + 24 + TOP_BYTES);

            h0 += (l0 + l1a - l2a) * M0;
            h1 += (l1 + l2a - l3a) * M1;
            h2 += (l2 + l3a - l0a) * M2;
            h3 += (l3 + l0a - l1a) * M3;
        }

        return agitate(h0) ^ agitate(h1)
                ^ agitate(h2) ^ agitate(h3);
    }

    /**
     * Computes a 64-bit hash value for the given BytesStore for any size of data.
     *
     * @param store     The {@link BytesStore} to compute the hash for.
     * @param remaining The number of bytes to process.
     * @return A 64-bit hash value.
     * @throws BufferUnderflowException If there is not enough data.
     */
    public static long applyAsLongAny(@NotNull BytesStore store, @NonNegative long remaining) throws BufferUnderflowException {
        @NotNull final BytesStore bytesStore = store.bytesStore();
        final long address = bytesStore.addressForRead(store.readPosition());
        long h0 = remaining * K0;
        long h1 = 0;
        long h2 = 0;
        long h3 = 0;

        int i;
        for (i = 0; i < remaining - 31; i += 32) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }
            long addrI = address + i;
            long l0 = MEMORY.readLong(addrI);
            int l0a = MEMORY.readInt(addrI + TOP_BYTES);
            long l1 = MEMORY.readLong(addrI + 8);
            int l1a = MEMORY.readInt(addrI + 8 + TOP_BYTES);
            long l2 = MEMORY.readLong(addrI + 16);
            int l2a = MEMORY.readInt(addrI + 16 + TOP_BYTES);
            long l3 = MEMORY.readLong(addrI + 24);
            int l3a = MEMORY.readInt(addrI + 24 + TOP_BYTES);

            h0 += (l0 + l1a - l2a) * M0;
            h1 += (l1 + l2a - l3a) * M1;
            h2 += (l2 + l3a - l0a) * M2;
            h3 += (l3 + l0a - l1a) * M3;
        }
        long left = remaining - i;
        if (left > 0) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }
            long addrI = address + i;
            if (left <= 16) {

                long l0 = readIncompleteLong(addrI, (int) left);
                int l0a = (int) (l0 >> 32);
                long l1 = readIncompleteLong(addrI + 8, (int) (left - 8));
                int l1a = (int) (l1 >> 32);
                final long l2 = 0;
                final int l2a = 0;
                final long l3 = 0;
                final int l3a = 0;

                h0 += (l0 + l1a - l2a) * M0;
                h1 += (l1 + l2a - l3a) * M1;
                h2 += ((long) -l0a) * M2;
                h3 += (l3 + l0a - l1a) * M3;

            } else {
                long l0 = MEMORY.readLong(addrI);
                int l0a = MEMORY.readInt(addrI + TOP_BYTES);
                long l1 = MEMORY.readLong(addrI + 8);
                int l1a = MEMORY.readInt(addrI + 8 + TOP_BYTES);
                long l2 = readIncompleteLong(addrI + 16, (int) (left - 16));
                int l2a = (int) (l2 >> 32);
                long l3 = readIncompleteLong(addrI + 24, (int) (left - 24));
                int l3a = (int) (l3 >> 32);

                h0 += (l0 + l1a - l2a) * M0;
                h1 += (l1 + l2a - l3a) * M1;
                h2 += (l2 + l3a - l0a) * M2;
                h3 += (l3 + l0a - l1a) * M3;
            }
        }

        return agitate(h0) ^ agitate(h1)
                ^ agitate(h2) ^ agitate(h3);
    }


    /**
     * Reads a long from the given address. If the length is less than 8,
     * it reads as many bytes as specified and converts them to a long.
     * Endianness is considered for reading bytes.
     *
     * @param address the starting address of the memory to read from.
     * @param len     the number of bytes to read (between 1 and 7, inclusive).
     * @return the value read from memory converted to a {@code long}.
     */
    static long readIncompleteLong(long address, @NonNegative int len) {
        switch (len) {
            case 1:
                return MEMORY.readByte(address);
            case 2:
                return MEMORY.readShort(address);
            case 3:
                return IS_LITTLE_ENDIAN
                        ? (MEMORY.readShort(address) & 0xFFFF) + ((MEMORY.readByte(address + 2) & 0xFF) << 16)
                        : ((MEMORY.readShort(address) & 0xFFFF) << 8) + (MEMORY.readByte(address + 2) & 0xFF);
            case 4:
                return MEMORY.readInt(address);
            case 5:
                return IS_LITTLE_ENDIAN
                        ? (MEMORY.readInt(address) & 0xFFFFFFFFL) + ((long) (MEMORY.readByte(address + 4) & 0xFF) << 32)
                        : ((MEMORY.readInt(address) & 0xFFFFFFFFL) << 8) + (MEMORY.readByte(address + 4) & 0xFF);
            case 6:
                return IS_LITTLE_ENDIAN
                        ? (MEMORY.readInt(address) & 0xFFFFFFFFL) + ((long) (MEMORY.readShort(address + 4) & 0xFFFF) << 32)
                        : ((MEMORY.readInt(address) & 0xFFFFFFFFL) << 16) + (MEMORY.readShort(address + 4) & 0xFFFF);
            case 7:
                return IS_LITTLE_ENDIAN
                        ? (MEMORY.readInt(address) & 0xFFFFFFFFL) + ((long) (MEMORY.readShort(address + 4) & 0xFFFF) << 32) + ((long) (MEMORY.readByte(address + 6) & 0xFF) << 48)
                        : ((MEMORY.readInt(address) & 0xFFFFFFFFL) << 24) + ((MEMORY.readShort(address + 4) & 0xFFFF) << 8) + (MEMORY.readByte(address + 6) & 0xFF);
            default:
                return len >= 8 ? MEMORY.readLong(address) : 0;
        }
    }

    /**
     * Hashes a BytesStore. This is an implementation of the BytesStoreHash interface.
     *
     * @param store the BytesStore to hash.
     * @return the hash value.
     */
    @Override
    public long applyAsLong(@NotNull BytesStore store) {
        final int remaining = Math.toIntExact(store.realReadRemaining());
        try {
            return applyAsLong(store, remaining);
        } catch (IllegalStateException | BufferUnderflowException e) {
            throw Jvm.rethrow(e);
        }
    }

    /**
     * Hashes a BytesStore of the given length. This is an implementation of the BytesStoreHash interface.
     *
     * @param store     the BytesStore to hash.
     * @param remaining the length of content in BytesStore to hash.
     * @return the hash value.
     * @throws BufferUnderflowException If buffer underflows during reading.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    public long applyAsLong(@NotNull BytesStore store, @NonNegative long remaining) throws IllegalStateException, BufferUnderflowException {
        if (remaining <= 16) {
            if (remaining == 0) {
                return 0;
            } else if (remaining < 8) {
                return applyAsLong1to7(store, (int) remaining);
            } else if (remaining == 8) {
                return applyAsLong8(store);
            } else {
                return applyAsLong9to16(store, (int) remaining);
            }
        } else if (remaining <= 32) {
            return applyAsLong17to32(store, (int) remaining);
        } else if ((remaining & 31) == 0) {
            return applyAsLong32bytesMultiple(store, (int) remaining);
        } else {
            return applyAsLongAny(store, remaining);
        }
    }
}
