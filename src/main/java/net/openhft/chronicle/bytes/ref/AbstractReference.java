/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.MappedBytesStore;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;

/**
 * Base class for references backed by a {@link BytesStore}.
 * <p>{@link #acceptNewBytesStore(BytesStore)} reserves the store and
 * {@link #performClose()} releases it. Subclasses must call
 * {@code throwExceptionIfClosed...()} before mutating state.</p>
 *
 * <p> {@link #unmonitor()} propagates to the wrapped store.
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
    protected BytesStore bytesStore;

    /**
     * Offset within the BytesStore for this reference
     */
    protected long offset;

    /**
     * Constructor initializes the reference assuming thread safety.
     */
    @SuppressWarnings("this-escape")
    protected AbstractReference() {
        // assume thread safe.
        singleThreadedCheckDisabled(true);
    }

    /**
     * Sets the underlying {@link BytesStore} together with the offset and length.
     *
     * @param bytes  the {@code BytesStore} providing the backing memory
     * @param offset the non-negative offset within the store
     * @param length the number of bytes this reference spans
     * @throws IllegalArgumentException if {@code length} does not match {@link #maxSize()}
     * @throws BufferOverflowException  if the region exceeds the store capacity
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
     * Returns the {@link BytesStore} that backs this reference, or {@code null} if none is set.
     */
    @Nullable
    @Override
    public BytesStore<?, ?> bytesStore() {
        return bytesStore;
    }

    /**
     * Returns the byte offset relative to the start of the associated store.
     */
    @Override
    public long offset() {
        return offset;
    }

    /**
     * Updates the BytesStore for this reference, releasing any previous BytesStore
     *
     * @param bytes the new BytesStore
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    protected void acceptNewBytesStore(@NotNull final BytesStore<?, ?> bytes)
            throws IllegalStateException {
        if (this.bytesStore != null) {
            this.bytesStore.release(this);
        }
        this.bytesStore = bytes.bytesStore();

        this.bytesStore.reserve(this);
    }

    /**
     * Closes this reference, releasing any associated BytesStore
     */
    @Override
    protected void performClose() {
        if (this.bytesStore == null)
            return;

        BytesStore<?, ?> bytes0 = this.bytesStore;
        this.bytesStore = null;
        try {
            bytes0.release(this);
        } catch (ClosedIllegalStateException e) {
            Jvm.debug().on(AbstractReference.class, "release after close", e);
        }
    }

    /**
     * Retrieves the memory address for reading.
     *
     * @return the memory address
     * @throws BufferUnderflowException If the buffer does not have enough content
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
     * @throws IOException If an I/O error occurs
     */
    @Override
    public FileLock lock(boolean shared) throws IOException {
        if (bytesStore() instanceof MappedBytesStore) {
            final MappedBytesStore mbs = (MappedBytesStore) bytesStore();
            return mbs.lock(offset, maxSize(), shared);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to lock a region in the file in either shared or exclusive mode,
     * but does not block waiting for the lock.
     *
     * @param shared if true the lock will be shared, otherwise it will be exclusive.
     * @return a FileLock object representing the locked region or null if the lock could not be acquired
     * @throws IOException If an I/O error occurs
     */
    @Override
    public FileLock tryLock(boolean shared) throws IOException {
        if (bytesStore() instanceof MappedBytesStore) {
            final MappedBytesStore mbs = (MappedBytesStore) bytesStore();
            return mbs.tryLock(offset, maxSize(), shared);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void unmonitor() {
        super.unmonitor();
        Monitorable.unmonitor(bytesStore);
    }
}
