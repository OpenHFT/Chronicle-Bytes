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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.MappedBytesStore;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;

/**
 * This abstract class represents a reference to a byte store. It is designed
 * to be extended by classes that need to provide byte store operations and
 * lifecycle management.
 *
 * @author peter.lawrey
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractReference extends AbstractCloseable implements Byteable, Closeable {

    /**
     * BytesStore associated with this reference
     */
    @Nullable
    protected BytesStore<?, ?> bytes;

    /**
     * Offset within the BytesStore for this reference
     */
    protected long offset;

    protected AbstractReference() {
        // assume thread safe.
        singleThreadedCheckDisabled(true);
    }

    /**
     * Sets the BytesStore and the offset within the store for this reference.
     *
     * @param bytes  the BytesStore
     * @param offset the offset within the BytesStore
     * @throws IllegalStateException if this reference has been closed
     */
    @Override
    public void bytesStore(final @NotNull BytesStore bytes, @NonNegative final long offset, @NonNegative final long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosedInSetter();
        // trigger it to move to this
        bytes.readInt(offset);
        BytesStore bytesStore = bytes.bytesStore();

        acceptNewBytesStore(bytesStore);
        this.offset = offset;
    }

    /**
     * @return the BytesStore associated with this reference
     */
    @Nullable
    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    /**
     * @return the offset within the BytesStore for this reference
     */
    @Override
    public long offset() {
        return offset;
    }

    /**
     * Updates the BytesStore for this reference, releasing any previous BytesStore
     *
     * @param bytes the new BytesStore
     * @throws IllegalStateException if this reference has been closed
     */
    protected void acceptNewBytesStore(@NotNull final BytesStore bytes)
            throws IllegalStateException {
        if (this.bytes != null) {
            this.bytes.release(this);
        }
        this.bytes = bytes.bytesStore();

        this.bytes.reserve(this);
    }

    /**
     * Closes this reference, releasing any associated BytesStore
     *
     * @throws IllegalStateException if this reference has already been closed
     */
    @Override
    protected void performClose() {
        if (this.bytes == null)
            return;
        
        try {
            this.bytes.release(this);
        } catch (ClosedIllegalStateException ignored) {
            // ignored
        }
        this.bytes = null;
    }

    /**
     * @return the address of the start of this reference in the BytesStore
     * @throws IllegalStateException if this reference has been closed
     */
    @Override
    public long address()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore().addressForRead(offset);
    }

    /**
     * Acquires a file lock on this reference.
     *
     * @param shared if the lock is shared
     * @return the FileLock acquired
     * @throws IOException if an I/O error occurs
     */
    @Override
    public FileLock lock(boolean shared) throws IOException {
        if (bytesStore() instanceof MappedBytesStore) {
            final MappedBytesStore mbs = (MappedBytesStore) bytesStore();
            return mbs.lock(offset, maxSize(), shared);
        }
        return Byteable.super.lock(shared);
    }

    /**
     * Attempts to acquire a file lock on this reference.
     *
     * @param shared if the lock is shared
     * @return the FileLock acquired, or null if the lock could not be acquired
     * @throws IOException if an I/O error occurs
     */
    @Override
    public FileLock tryLock(boolean shared) throws IOException {
        if (bytesStore() instanceof MappedBytesStore) {
            final MappedBytesStore mbs = (MappedBytesStore) bytesStore();
            return mbs.tryLock(offset, maxSize(), shared);
        }
        return Byteable.super.tryLock(shared);
    }
}
