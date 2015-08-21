/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytesStore;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteOrder;

import static net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash.*;
import static net.openhft.chronicle.core.Maths.agitate;

/**
 * Created by peter on 28/06/15.
 */
public enum OptimisedBytesHash implements BytesStoreHash<Bytes> {
    INSTANCE;

    public static final Memory MEMORY = OS.memory();
    private static final int TOP_BYTES = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 4 : 0;

    static long applyAsLong1to7(@NotNull Bytes store, int remaining) {
        final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());

        long l0 = readIncompleteLong(address, remaining);
        long l0a = l0 >> 32;

        long h0 = (long) remaining * K0 + l0 * M0;
        long h1 = l0a * M1;

        return agitate(h0) ^ agitate(h1);
    }

    static long applyAsLong8(@NotNull Bytes store) {
        final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());

        long l0 = MEMORY.readLong(address);
        long l0a = MEMORY.readInt(address + TOP_BYTES);

        long h0 = 8L * K0 + l0 * M0;
        long h1 = l0a * M1;

        return agitate(h0) ^ agitate(h1);
    }

    public static long hash(long l) {
        long h0 = 8L * K0 + l * M0;
        long h1 = (l >> 32) * M1;

        return agitate(h0) ^ agitate(h1);
    }

    static long applyAsLong9to16(@NotNull Bytes store, int remaining) {
        final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());
        long h0 = (long) remaining * K0, h1 = 0;

        int left = remaining;
        long addrI = address;

        long l0 = readIncompleteLong(addrI, left);
        int l0a = (int) (l0 >> 32);
        long l1 = readIncompleteLong(addrI + 8, left - 8);
        int l1a = (int) (l1 >> 32);

        h0 += (l0 + l1a) * M0;
        h1 += (l1 + l0a) * M1;

        return agitate(h0) ^ agitate(h1);
    }

    static long applyAsLong17to32(@NotNull Bytes store, int remaining) {
        final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());
        long h0 = (long) remaining * K0, h1 = 0;

        int left = remaining;
        long addrI = address;

        long l0 = readIncompleteLong(addrI, left);
        int l0a = (int) (l0 >> 32);
        long l1 = readIncompleteLong(addrI + 8, left - 8);
        int l1a = (int) (l1 >> 32);
        long l2 = readIncompleteLong(addrI + 16, left - 16);
        int l2a = (int) (l2 >> 32);
        long l3 = readIncompleteLong(addrI + 24, left - 24);
        int l3a = (int) (l3 >> 32);

        h0 += (l0 + l1a) * M0 + (l2 + l3a) * M2;
        h1 += (l1 + l0a) * M1 + (l3 + l2a) * M3;

        return agitate(h0) ^ agitate(h1);
    }

    static long applyAsLongAny(@NotNull Bytes store, int remaining) {
        final NativeBytesStore bytesStore = (NativeBytesStore) store.bytesStore();
        final long address = bytesStore.address(store.readPosition());
        long h0 = remaining, h1 = 0;

        int i;
        for (i = 0; i < remaining - 31; i += 32) {
            h0 *= K0;
            h1 *= K1;
            long addrI = address + i;
            long l0 = MEMORY.readLong(addrI);
            int l0a = MEMORY.readInt(addrI + TOP_BYTES);
            long l1 = MEMORY.readLong(addrI + 8);
            int l1a = MEMORY.readInt(addrI + 8 + TOP_BYTES);
            long l2 = MEMORY.readLong(addrI + 16);
            int l2a = MEMORY.readInt(addrI + 16 + TOP_BYTES);
            long l3 = MEMORY.readLong(addrI + 24);
            int l3a = MEMORY.readInt(addrI + 24 + TOP_BYTES);

            h0 += (l0 + l1a) * M0 + (l2 + l3a) * M2;
            h1 += (l1 + l0a) * M1 + (l3 + l2a) * M3;
        }
        int left = remaining - i;
        if (left > 0) {
            h0 *= K0;
            h1 *= K1;
            long addrI = address + i;
            if (left <= 16) {
                long l0 = readIncompleteLong(addrI, left);
                int l0a = (int) (l0 >> 32);
                long l1 = readIncompleteLong(addrI + 8, left - 8);
                int l1a = (int) (l1 >> 32);

                h0 += (l0 + l1a) * M0;
                h1 += (l1 + l0a) * M1;

            } else {
                long l0 = MEMORY.readLong(addrI);
                int l0a = MEMORY.readInt(addrI + TOP_BYTES);
                long l1 = MEMORY.readLong(addrI + 8);
                int l1a = MEMORY.readInt(addrI + 8 + TOP_BYTES);
                long l2 = readIncompleteLong(addrI + 16, left - 16);
                int l2a = (int) (l2 >> 32);
                long l3 = readIncompleteLong(addrI + 24, left - 24);
                int l3a = (int) (l3 >> 32);

                h0 += (l0 + l1a) * M0 + (l2 + l3a) * M2;
                h1 += (l1 + l0a) * M1 + (l3 + l2a) * M3;
            }
        }

        return agitate(h0) ^ agitate(h1);
    }

    static long readIncompleteLong(long address, int len) {
        if (len >= 8)
            return MEMORY.readLong(address);
        if (len == 4)
            return MEMORY.readInt(address);
        long l = 0;
        for (int i = 0; i < len; i++) {
            byte b = MEMORY.readByte(address + i);
            l |= (long) (b & 0xFF) << (i * 8);
        }
        return l;
    }

    @Override
    public long applyAsLong(@NotNull Bytes store) {
        final int remaining = Maths.toInt32(store.readRemaining());
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
        } else {
            return applyAsLongAny(store, remaining);
        }
    }
}
