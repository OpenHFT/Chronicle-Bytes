/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash;

/*
 * Created by peter on 07/05/16.
 */
public abstract class AbstractBytesStore<B extends BytesStore<B, Underlying>, Underlying>
        implements BytesStore<B, Underlying> {
    @Override
    public int peekUnsignedByte(long offset) {
        return offset >= readLimit() ? -1 : readUnsignedByte(offset);
    }

    @Override
    public int hashCode() {
        long h = VanillaBytesStoreHash.INSTANCE.applyAsLong(this);
        h ^= h >> 32;
        return (int) h;
    }

    @Override
    public long readPosition() {
        return 0L;
    }

    @Override
    public long readRemaining() {
        return this.capacity();
    }

    @Override
    public long start() {
        return 0L;
    }
}
