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
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.NativeBytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;

import java.util.function.ToLongFunction;

/**
 * Simple function to derive a long hash from a BytesStore
 */
public interface BytesStoreHash<B extends BytesStore> extends ToLongFunction<B> {
    static long hash(VanillaBytes b) {
        return OptimisedBytesStoreHash.INSTANCE.applyAsLong(b);
    }

    static long hash(BytesStore b) {
        return b instanceof Bytes && b.bytesStore() instanceof NativeBytesStore
                ? OptimisedBytesStoreHash.INSTANCE.applyAsLong((Bytes) b)
                : VanillaBytesStoreHash.INSTANCE.applyAsLong(b);
    }

    static int hash32(Bytes b) {
        long hash = hash(b);
        return (int) (hash ^ (hash >>> 32));
    }

    static long hash(Bytes b, int length) {
        return b.bytesStore() instanceof NativeBytesStore
                ? OptimisedBytesStoreHash.INSTANCE.applyAsLong(b, length)
                : VanillaBytesStoreHash.INSTANCE.applyAsLong(b, length);
    }

    static int hash32(Bytes b, int length) {
        long hash = hash(b, length);
        return (int) (hash ^ (hash >>> 32));
    }

}
