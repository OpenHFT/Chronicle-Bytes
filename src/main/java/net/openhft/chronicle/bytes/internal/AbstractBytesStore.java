/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;

import java.nio.BufferUnderflowException;

/**
 * Abstract BytesStore
 * @param <B> ByteStore type
 * @param <Underlying> Underlying type
 */

public abstract class AbstractBytesStore<B extends BytesStore<B, Underlying>, Underlying>
        extends AbstractReferenceCounted
        implements BytesStore<B, Underlying> {

    protected AbstractBytesStore() {
    }

    protected AbstractBytesStore(boolean monitored) {
        super(monitored);
    }

    @Override
    public int peekUnsignedByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return offset >= readLimit() ? -1 : readUnsignedByte(offset);
    }

    @Override
    public int hashCode() {
        return BytesStoreHash.hash32(this);
    }

    @Override
    public long readPosition() {
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
    public long start() {
        return 0L;
    }

    @Override
    protected boolean canReleaseInBackground() {
        return isDirectMemory();
    }
}
