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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;

/**
 * Abstract BytesStore
 *
 * @param <B> ByteStore type
 * @param <U> Underlying type
 */

public abstract class AbstractBytesStore<B extends BytesStore<B, U>, U>
        extends AbstractReferenceCounted
        implements BytesStore<B, U> {

    protected AbstractBytesStore() {
    }

    protected AbstractBytesStore(boolean monitored) {
        super(monitored);
    }

    @Override
    public int peekUnsignedByte(@NonNegative long offset)
            throws IllegalStateException {
        return offset < start() || readLimit() <= offset ? -1 : readUnsignedByte(offset);
    }

    @Override
    public int hashCode() {
        return BytesStoreHash.hash32(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore<?, ?>) obj);
    }

    @Override
    public @NonNegative long readPosition() {
        return 0L;
    }

    @Override
    public long readRemaining() {
        return readLimit() - readPosition();
    }

    @Override
    public long writeRemaining() {
        return writeLimit() - writePosition();
    }

    @Override
    public @NonNegative long start() {
        return 0L;
    }

    @Override
    protected boolean canReleaseInBackground() {
        return isDirectMemory();
    }
}
