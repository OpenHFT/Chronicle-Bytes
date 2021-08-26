/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.MappedBytesStore;
import net.openhft.chronicle.bytes.NoBytesStore;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;

@SuppressWarnings("rawtypes")
public abstract class AbstractReference extends AbstractCloseable implements Byteable, Closeable {

    @Nullable
    protected BytesStore<?, ?> bytes;
    protected long offset;

    @Override
    public void bytesStore(@NotNull final BytesStore bytes, final long offset, final long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosedInSetter();
        // trigger it to move to this
        bytes.readInt(offset);
        BytesStore bytesStore = bytes.bytesStore();

        acceptNewBytesStore(bytesStore);
        this.offset = offset;
    }

    @Nullable
    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public abstract long maxSize();

    protected void acceptNewBytesStore(@NotNull final BytesStore bytes)
            throws IllegalStateException {
        if (this.bytes != null) {
            this.bytes.release(this);
        }
        bytes.readByte(offset); // wake it up if it's not pointing to anything yet.
        this.bytes = bytes.bytesStore();

        this.bytes.reserve(this);
    }

    @Override
    protected void performClose()
            throws IllegalStateException {
        if (this.bytes != null) {
            this.bytes.release(this);
            this.bytes = null;
        }
    }

    public long address()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore().addressForRead(offset);
    }

/* TODO FIX in x.22
        @Override
    protected void finalize()
throws Throwable {
        warnIfNotClosed();
        super.finalize();
    }*/

    @Override
    protected boolean threadSafetyCheck(boolean isUsed) {
        // References are thread safe
        return true;
    }

    @Override
    public FileLock lock(boolean shared) throws IOException {
        if (bytesStore() instanceof MappedBytesStore) {
            final MappedBytesStore mbs = (MappedBytesStore) bytesStore();
            return mbs.lock(offset, maxSize(), shared);
        }
        return Byteable.super.lock(shared);
    }

    @Override
    public FileLock tryLock(boolean shared) throws IOException {
        if (bytesStore() instanceof MappedBytesStore) {
            final MappedBytesStore mbs = (MappedBytesStore) bytesStore();
            return mbs.tryLock(offset, maxSize(), shared);
        }
        return Byteable.super.tryLock(shared);
    }
}
