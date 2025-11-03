/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;

/**
 * Allows a Java object to view a slice of a {@link BytesStore}. Implementations
 * may be remapped to different offsets to avoid copying.
 */
public interface Byteable {
    /**
     * Map this object onto a region of {@code bytesStore}.
     *
     * @param bytesStore backing store
     * @param offset     non-negative offset
     * @param length     non-negative length which should match {@link #maxSize()}
     * @throws IllegalArgumentException       if parameters are out of range
     * @throws BufferOverflowException        if the region would extend past the end of the store
     * @throws BufferUnderflowException       if the region would start before {@code 0}
     * @throws ClosedIllegalStateException    if the store is closed
     * @throws ThreadingIllegalStateException if accessed from the wrong thread
     */
    @SuppressWarnings("rawtypes")
    void bytesStore(@NotNull BytesStore bytesStore, @NonNegative long offset, @NonNegative long length)
            throws ClosedIllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException, ThreadingIllegalStateException;

    /**
     * @return current backing {@link BytesStore} or {@code null} if unmapped
     */
    @Nullable
    BytesStore<?, ?> bytesStore();

    /**
     * @return offset within the current {@link BytesStore}
     */
    long offset();

    /**
     * @return absolute address of the mapped data if supported
     * @throws UnsupportedOperationException if not backed by native memory
     */
    default long address() throws UnsupportedOperationException {
        return bytesStore().addressForRead(offset());
    }

    /**
     * @return fixed byte size represented by this object
     */
    long maxSize();

    /**
     * Locks the underlying file.
     *
     * @param shared true if the lock is shared, false if it's exclusive
     * @return the FileLock object representing the lock
     * @throws IOException                   If an error occurs while locking the file
     * @throws UnsupportedOperationException If the underlying implementation does not support file locking
     */
    default FileLock lock(boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to lock the underlying file without blocking.
     *
     * @param shared true if the lock is shared, false if it's exclusive
     * @return the FileLock object if the lock was acquired successfully; null otherwise
     * @throws IOException                   If an error occurs while trying to lock the file
     * @throws UnsupportedOperationException If the underlying implementation does not support file locking
     */
    default FileLock tryLock(boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }
}
