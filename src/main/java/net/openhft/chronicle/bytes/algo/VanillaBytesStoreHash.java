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
import net.openhft.chronicle.core.Maths;

import java.nio.ByteOrder;

/**
 * Created by peter on 28/06/15.
 */
public enum VanillaBytesStoreHash implements BytesStoreHash<BytesStore> {
    INSTANCE;

    private static final int TOP_BYTES = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 4 : 0;

    static final int K0 = 0xc5b0_3135;
    static final int K1 = 0x1d56_2d7b;
    static final int M0 = 0x4932_5e2f;
    static final int M1 = 0x3275_2743;
    static final int M2 = 0xf4bb_2e2f;
    static final int M3 = 0x4a64_17c9;

    @Override
    public long applyAsLong(BytesStore store) {
        long start = store.readPosition();
        int remaining = (int) store.readRemaining();
        long h0 = remaining;
        long h1 = 0;
        int i;
        for (i = 0; i < remaining - 31; i += 32) {
            h0 *= K0;
            h1 *= K1;
            long addrI = start + i;
            long l0 = store.readLong(addrI);
            int l0a = store.readInt(addrI + TOP_BYTES);
            long l1 = store.readLong(addrI + 8);
            int l1a = store.readInt(addrI + 8 + TOP_BYTES);
            long l2 = store.readLong(addrI + 16);
            int l2a = store.readInt(addrI + 16 + TOP_BYTES);
            long l3 = store.readLong(addrI + 24);
            int l3a = store.readInt(addrI + 24 + TOP_BYTES);

            h0 += (l0 + l1a) * M0 + (l2 + l3a) * M2;
            h1 += (l1 + l0a) * M1 + (l3 + l2a) * M3;
        }
        int left = remaining - i;
        if (left > 0) {
            h0 *= K0;
            h1 *= K1;
            long addrI = start + i;
            long l0 = store.readIncompleteLong(addrI);
            int l0a = (int) (l0 >> 32);
            long l1 = store.readIncompleteLong(addrI + 8);
            int l1a = (int) (l1 >> 32);
            long l2 = store.readIncompleteLong(addrI + 16);
            int l2a = (int) (l2 >> 32);
            long l3 = store.readIncompleteLong(addrI + 24);
            int l3a = (int) (l3 >> 32);

            h0 += (l0 + l1a) * M0 + (l2 + l3a) * M2;
            h1 += (l1 + l0a) * M1 + (l3 + l2a) * M3;
        }
        return Maths.agitate(h0) ^ Maths.agitate(h1);
    }
}
