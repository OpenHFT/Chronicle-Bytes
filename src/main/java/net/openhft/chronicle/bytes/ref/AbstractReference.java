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
 * Represents an abstract reference to a {@link BytesStore}.
 *
 * <p>This class provides an abstraction for managing a reference to a BytesStore. It provides
 * functionality to read and write data from/to the BytesStore, manage a reference count, and lock
 * resources.</p>
 *
 * @see BytesStore
 * @see Byteable
 * @see Closeable
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

    /**
     * Constructor initializes the reference assuming thread safety.
     */
    protected AbstractReference() {
        // assume thread safe.
        singleThreadedCheckDisabled(true);
    }

    /**
     * Sets the underlying BytesStore to work with, along with the offset and length.
     *
     * @param bytes  the BytesStore to set
     * @param offset the offset to set
     * @param length the length to set
     * @throws IllegalStateException        if the state is invalid
     * @throws IllegalArgumentException     if the arguments are invalid
     * @throws BufferOverflowException     if the provided buffer is too small
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
     */
    @Override
    protected void performClose() {
        if (this.bytes == null)
            return;

        BytesStore<?, ?> bytes0 = this.bytes;
        this.bytes = null;
        try {
            bytes0.release(this);
        } catch (ClosedIllegalStateException ignored) {
            // ignored
        }
    }

    /**
     * Retrieves the memory address for reading.
     *
     * @return the memory address
     * @throws IllegalStateException if the state is invalid
     * @throws BufferUnderflowException if the buffer does not have enough content
     */
    @Override
    public long address()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        return bytesStore().addressForRead(offset);
    }

    /**
     * Attempts to lock a region in the file in either shared or exclusive mode.
     *
     * @param shared if true the lock will be shared, otherwise it will be exclusive.
     * @return a FileLock object representing the locked region
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
     * Attempts to lock a region in the file in either shared or exclusive mode,
     * but does not block waiting for the lock.
     *
     * @param shared if true the lock will be shared, otherwise it will be exclusive.
     * @return a FileLock object representing the locked region or null if the lock could not be acquired
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
