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

import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteOrder;

/**
 * Created by peter on 28/06/15.
 */
public enum VanillaBytesStoreHash implements BytesStoreHash<BytesStore> {
    INSTANCE;

    public static final int K0 = 0x6d0f27bd;
    public static final int K1 = 0xc1f3bfc9;
    public static final int K2 = 0x6b192397;
    public static final int K3 = 0x6b915657;
    public static final int M0 = 0x5bc80bad;
    public static final int M1 = 0xea7585d7;
    public static final int M2 = 0x7a646e19;
    public static final int M3 = 0x855dd4db;
    private static final int HI_BYTES = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 4 : 0;

    public static long agitate(long l) {
        l += l >>> 22;
        l ^= Long.rotateRight(l, 17);
        return l;
    }

    @Override
    public long applyAsLong(@NotNull BytesStore store) {
        long start = store.readPosition();
        int remaining = (int) store.readRemaining();
        // use two hashes so that when they are combined the 64-bit hash is more random.
        long h0 = (long) remaining * K0;
        long h1 = 0, h2 = 0, h3 = 0;
        int i;
        // optimise chunks of 32 bytes but this is the same as the next loop.
        for (i = 0; i < remaining - 31; i += 32) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }
            long addrI = start + i;
            long l0 = store.readLong(addrI);
            int l0a = store.readInt(addrI + HI_BYTES);
            long l1 = store.readLong(addrI + 8);
            int l1a = store.readInt(addrI + 8 + HI_BYTES);
            long l2 = store.readLong(addrI + 16);
            int l2a = store.readInt(addrI + 16 + HI_BYTES);
            long l3 = store.readLong(addrI + 24);
            int l3a = store.readInt(addrI + 24 + HI_BYTES);

            h0 += (l0 + l1a - l2a) * M0;
            h1 += (l1 + l2a - l3a) * M1;
            h2 += (l2 + l3a - l0a) * M2;
            h3 += (l3 + l0a - l1a) * M3;
            }

        // perform a hash of the end.
        int left = remaining - i;
        if (left > 0) {
            if (i > 0) {
                h0 *= K0;
                h1 *= K1;
                h2 *= K2;
                h3 *= K3;
            }

            long addrI = start + i;
            long l0 = store.readIncompleteLong(addrI);
            int l0a = (int) (l0 >> 32);
            long l1 = store.readIncompleteLong(addrI + 8);
            int l1a = (int) (l1 >> 32);
            long l2 = store.readIncompleteLong(addrI + 16);
            int l2a = (int) (l2 >> 32);
            long l3 = store.readIncompleteLong(addrI + 24);
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
