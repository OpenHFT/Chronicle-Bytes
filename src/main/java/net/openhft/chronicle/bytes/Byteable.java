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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;

/**
 * An interface for a reference to off-heap memory, acting as a proxy for memory residing outside the heap.
 * This allows the reference to be reassigned, facilitating dynamic memory management.
 *
 * @param <B> the BytesStore type
 * @param <U> the underlying type that the BytesStore manages
 */
public interface Byteable<B extends BytesStore<B, U>, U> {
    /**
     * Sets the reference to a data type that points to the underlying ByteStore.
     *
     * @param bytesStore the fixed-point ByteStore
     * @param offset     the offset within the ByteStore, indicating the starting point of the memory section
     * @param length     the length of the memory section within the ByteStore
     * @throws ClosedIllegalStateException if it is closed
     * @throws IllegalArgumentException if the provided arguments are invalid
     * @throws BufferOverflowException if the new memory section extends beyond the end of the ByteStore
     * @throws BufferUnderflowException if the new memory section starts before the start of the ByteStore
     */
    void bytesStore(@NotNull BytesStore<B, U> bytesStore, @NonNegative long offset, @NonNegative long length)
            throws ClosedIllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException;

    /**
     * Returns the ByteStore to which this object currently points.
     *
     * @return the ByteStore or null if it's not set
     */
    @Nullable
    BytesStore<B, U> bytesStore();

    /**
     * Returns the offset within the ByteStore to which this object currently points.
     *
     * @return the offset within the ByteStore (not the physical memory address)
     */
    long offset();

    /**
     * Returns the absolute address in the memory to which this object currently points.
     *
     * @return the absolute address in the memory
     * @throws UnsupportedOperationException if the address is not set or the underlying ByteStore isn't native
     */
    default long address() throws UnsupportedOperationException {
        return bytesStore().addressForRead(offset());
    }

    /**
     * Returns the maximum size in bytes that this reference can point to.
     *
     * @return the maximum size in bytes for this reference
     */
    long maxSize();

    /**
     * Locks the underlying file.
     *
     * @param shared true if the lock is shared, false if it's exclusive
     * @return the FileLock object representing the lock
     * @throws IOException if an error occurs while locking the file
     * @throws UnsupportedOperationException if the underlying implementation does not support file locking
     */
    // TODO move to implementations in x.25
    default FileLock lock(boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to lock the underlying file without blocking.
     *
     * @param shared true if the lock is shared, false if it's exclusive
     * @return the FileLock object if the lock was acquired successfully; null otherwise
     * @throws IOException if an error occurs while trying to lock the file
     * @throws UnsupportedOperationException if the underlying implementation does not support file locking
     */
    // TODO move to implementations in x.25
    default FileLock tryLock(boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }
}