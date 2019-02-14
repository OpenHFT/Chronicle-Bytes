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

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.NotNull;

import java.nio.ByteOrder;

/*
 * Created by Peter Lawrey on 28/06/15.
 */
@SuppressWarnings("rawtypes")
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
        l ^= Long.rotateLeft(l, 26);
        l ^= Long.rotateRight(l, 17);
        return l;
    }

    @Override
    public long applyAsLong(@NotNull BytesStore store) {
        int remaining = Maths.toInt32(store.readRemaining());
        return applyAsLong(store, remaining);
    }

    @Override
    public long applyAsLong(BytesStore bytes, long length) {
        long start = bytes.readPosition();
        if (length <= 8) {
            long l = bytes.readIncompleteLong(start);
            return agitate(l * K0 + (l >> 32) * K1);
        }
        // use two hashes so that when they are combined the 64-bit hash is more random.
        long h0 = (long) length * K0;
        long h1 = 0, h2 = 0, h3 = 0;
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
