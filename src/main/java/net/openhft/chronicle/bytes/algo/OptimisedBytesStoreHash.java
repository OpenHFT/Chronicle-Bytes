/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytesStore;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteOrder;

import static net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash.*;

/*
 * Created by peter on 28/06/15.
 */
public enum OptimisedBytesStoreHash implements BytesStoreHash<Bytes> {
    INSTANCE;

    @Nullable
    public static final Memory MEMORY = OS.memory();
    public static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final int TOP_BYTES = IS_LITTLE_ENDIAN ? 4 : 0;

    static long applyAsLong1to7(@NotNull Bytes store, int remaining) {
        @NotNull final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());

        return hash(readIncompleteLong(address, remaining));
    }

    static long applyAsLong8(@NotNull Bytes store) {
        @NotNull final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());

        return hash0(MEMORY.readLong(address), MEMORY.readInt(address + TOP_BYTES));
    }

    public static long hash(long l) {
        return hash0(l, l >> 32);
    }

    static long hash0(long l, long hi) {
        return agitate(l * K0 + hi * K1);
    }

    static long applyAsLong9to16(@NotNull Bytes store, int remaining) {
        @NotNull final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());
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
        long h2 = (l2 + l3a - l0a) * M2;
        long h3 = (l3 + l0a - l1a) * M3;

        return agitate(h0) ^ agitate(h1)
                ^ agitate(h2) ^ agitate(h3);
    }

    static long applyAsLong17to32(@NotNull Bytes store, int remaining) {
        @NotNull final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());
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

    public static long applyAsLong32bytesMultiple(@NotNull Bytes store, int remaining) {
        @NotNull final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());
        long h0 = remaining * K0, h1 = 0, h2 = 0, h3 = 0;

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

    public static long applyAsLongAny(@NotNull Bytes store, int remaining) {
        @NotNull final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());
        long h0 = (long) remaining * K0, h1 = 0, h2 = 0, h3 = 0;

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
        int left = remaining - i;
        if (left > 0) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }
            long addrI = address + i;
            if (left <= 16) {

                long l0 = readIncompleteLong(addrI, left);
                int l0a = (int) (l0 >> 32);
                long l1 = readIncompleteLong(addrI + 8, left - 8);
                int l1a = (int) (l1 >> 32);
                final long l2 = 0;
                final int l2a = 0;
                final long l3 = 0;
                final int l3a = 0;

                h0 += (l0 + l1a - l2a) * M0;
                h1 += (l1 + l2a - l3a) * M1;
                h2 += (l2 + l3a - l0a) * M2;
                h3 += (l3 + l0a - l1a) * M3;

            } else {
                long l0 = MEMORY.readLong(addrI);
                int l0a = MEMORY.readInt(addrI + TOP_BYTES);
                long l1 = MEMORY.readLong(addrI + 8);
                int l1a = MEMORY.readInt(addrI + 8 + TOP_BYTES);
                long l2 = readIncompleteLong(addrI + 16, left - 16);
                int l2a = (int) (l2 >> 32);
                long l3 = readIncompleteLong(addrI + 24, left - 24);
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

    static long readIncompleteLong(long address, int len) {
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

    @Override
    public long applyAsLong(@NotNull Bytes store) {
        final int remaining = Maths.toInt32(store.readRemaining());
        return applyAsLong(store, remaining);
    }

    public long applyAsLong(@NotNull Bytes store, int remaining) {
        if (remaining <= 16) {
            if (remaining == 0) {
                return 0;
            } else if (remaining < 8) {
                return applyAsLong1to7(store, remaining);
            } else if (remaining == 8) {
                return applyAsLong8(store);
            } else {
                return applyAsLong9to16(store, remaining);
            }
        } else if (remaining <= 32) {
            return applyAsLong17to32(store, remaining);
        } else if ((remaining & 31) == 0) {
            return applyAsLong32bytesMultiple(store, remaining);
        } else {
            return applyAsLongAny(store, remaining);
        }
    }
}
