/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;

/**
 * This Interface allows a reference to off heap memory to be reassigned.
 * <p></p>
 * A reference to off heap memory is a proxy for some memory which sits outside the heap.
 *
 * @param <B> Bytes type
 * @param <U> Underlying type
 */
public interface Byteable<B extends BytesStore<B, U>, U> {
    /**
     * This setter for a data type which points to an underlying ByteStore.
     *  @param bytesStore the fix point ByteStore
     * @param offset     the offset within the ByteStore
     * @param length     the length in the ByteStore
     */
    void bytesStore(@NotNull BytesStore<B, U> bytesStore, @NonNegative long offset, @NonNegative long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException;

    @Nullable
    BytesStore<B, U> bytesStore();

    /**
     * @return The offset within the BytesStore (not the address)
     */
    long offset();

    /**
     * @return The absolute address
     * @throws UnsupportedOperationException if not set ot the underlying byteStore isn't native.
     */
    default long address() throws UnsupportedOperationException {
        return bytesStore().addressForRead(offset());
    }

    /**
     * @return the maximum size in byte for this reference.
     */
    long maxSize();

    /**
     * Calls lock on the underlying file
     * @param shared if the lock is shared or not.
     * @return the FileLock
     */
    default FileLock lock(boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Calls lock on the underlying file
     * @param shared if the lock is shared or not.
     * @return the FileLock
     */
    default FileLock tryLock(boolean shared) throws IOException {
        throw new UnsupportedOperationException();
    }
}