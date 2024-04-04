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

import net.openhft.chronicle.bytes.internal.ChunkedMappedBytes;
import net.openhft.chronicle.bytes.internal.SingleMappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A specialized implementation of {@link AbstractBytes} that wraps memory-mapped data for efficient random file access.
 *
 * <p>Memory is grouped in chunks of 64 MB by default. The chunk size can be significantly increased if the
 * OS supports sparse files via the {@link OS#isSparseFileSupported()} method, e.g. {@code blockSize(512 << 30)}.
 *
 * <p>Only the most recently accessed memory chunk is reserved, and the previous chunk is released. For random access,
 * a chunk can be manually reserved by obtaining the bytesStore() and using reserve(owner) on it.
 * However, it is crucial to call release(owner) on the same BytesStore before closing the file to avoid memory leaks.
 *
 * <p>Several factory methods are provided to create different types of MappedBytes, including single mapped bytes
 * and chunked mapped bytes, with optional settings for read-only mode and chunk overlap size.
 *
 * <p>Note: MappedBytes, like all Bytes, are single-threaded. Also, it is recommended to ensure the mapped file
 * is reserved before using MappedBytes.
 *
 * @see BytesStore
 * @see MappedFile
 * @see AbstractBytes
 */
@SuppressWarnings("rawtypes")
public abstract class MappedBytes extends AbstractBytes<Void> implements Closeable, ManagedCloseable, Syncable {

    protected static final boolean TRACE = Jvm.getBoolean("trace.mapped.bytes");

    // assume the mapped file is reserved already.
    protected MappedBytes()
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        this("");
    }

    protected MappedBytes(final String name)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        super(BytesStore.empty(),
                BytesStore.empty().writePosition(),
                BytesStore.empty().writeLimit(),
                name);
    }

    /**
     * Creates a MappedBytes instance that wraps a single memory-mapped file.
     *
     * @param filename The name of the file to be memory-mapped.
     * @param capacity The maximum number of bytes that can be read from or written to the mapped file.
     * @return A new MappedBytes instance.
     * @throws FileNotFoundException If the file does not exist.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public static MappedBytes singleMappedBytes(@NotNull final String filename, @NonNegative final long capacity)
            throws FileNotFoundException, IllegalStateException {
        return singleMappedBytes(new File(filename), capacity);
    }

    /**
     * Creates a MappedBytes instance that wraps a single memory-mapped file.
     *
     * @param file     The name of the file to be memory-mapped.
     * @param capacity The maximum number of bytes that can be read from or written to the mapped file.
     * @return A new MappedBytes instance.
     * @throws FileNotFoundException If the file does not exist.
     */
    @NotNull
    public static MappedBytes singleMappedBytes(@NotNull final File file, @NonNegative final long capacity)
            throws FileNotFoundException {
        return singleMappedBytes(file, capacity, false);
    }

    /**
     * Creates a MappedBytes instance that wraps a single memory-mapped file.
     *
     * @param file     The name of the file to be memory-mapped.
     * @param capacity The maximum number of bytes that can be read from or written to the mapped file.
     * @param readOnly read only is true, read-write if false
     * @return A new MappedBytes instance.
     * @throws FileNotFoundException If the file does not exist.
     */

    @NotNull
    public static MappedBytes singleMappedBytes(@NotNull File file, @NonNegative long capacity, boolean readOnly)
            throws FileNotFoundException {
        final MappedFile rw = MappedFile.ofSingle(file, capacity, readOnly);
        try {
            return new SingleMappedBytes(rw);
        } finally {
            rw.release(INIT);
        }
    }

    /**
     * Creates a MappedBytes instance that wraps a memory-mapped file divided into chunks of a specified size.
     *
     * @param filename  The name of the file to be memory-mapped.
     * @param chunkSize The size of each chunk in bytes.
     * @return A new MappedBytes instance.
     * @throws FileNotFoundException          If the file does not exist.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public static MappedBytes mappedBytes(@NotNull final String filename, @NonNegative final long chunkSize)
            throws FileNotFoundException, ClosedIllegalStateException {
        return mappedBytes(new File(filename), chunkSize);
    }

    /**
     * Creates a MappedBytes instance that wraps a memory-mapped file divided into chunks of a specified size.
     *
     * @param file      The name of the file to be memory-mapped.
     * @param chunkSize The size of each chunk in bytes.
     * @return A new MappedBytes instance.
     * @throws FileNotFoundException          If the file does not exist.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file, @NonNegative final long chunkSize)
            throws FileNotFoundException, ClosedIllegalStateException {
        return mappedBytes(file, chunkSize, OS.pageSize());
    }

    /**
     * Creates a MappedBytes instance that wraps a memory-mapped file divided into chunks of a specified size.
     *
     * @param file        The name of the file to be memory-mapped.
     * @param chunkSize   The size of each chunk in bytes.
     * @param overlapSize The size of overlap of chunks in bytes.
     * @return A new MappedBytes instance.
     * @throws FileNotFoundException          If the file does not exist.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file, @NonNegative final long chunkSize, @NonNegative final long overlapSize)
            throws FileNotFoundException, ClosedIllegalStateException {
        final MappedFile rw = MappedFile.of(file, chunkSize, overlapSize, false);
        try {
            return mappedBytes(rw);
        } finally {
            rw.release(INIT);
        }
    }

    /**
     * Creates a MappedBytes instance that wraps a memory-mapped file divided into chunks of a specified size.
     *
     * @param file        The name of the file to be memory-mapped.
     * @param chunkSize   The size of each chunk in bytes.
     * @param overlapSize The size of overlap of chunks in bytes.
     * @param pageSize    The custom page size in bytes.
     * @param readOnly    read only is true, read-write if false
     * @return A new MappedBytes instance.
     * @throws FileNotFoundException          If the file does not exist.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file,
                                          @NonNegative final long chunkSize,
                                          @NonNegative final long overlapSize,
                                          @NonNegative final int pageSize,
                                          final boolean readOnly)
            throws FileNotFoundException, ClosedIllegalStateException {
        final MappedFile rw = MappedFile.of(file, chunkSize, overlapSize, pageSize, readOnly);
        try {
            return mappedBytes(rw);
        } finally {
            rw.release(INIT);
        }
    }

    /**
     * @see  #mappedBytes(File, long, long, int, boolean)
     */
    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file,
                                          @NonNegative final long chunkSize,
                                          @NonNegative final long overlapSize,
                                          final boolean readOnly)
            throws FileNotFoundException, ClosedIllegalStateException {
        return mappedBytes(file, chunkSize, overlapSize, PageUtil.getPageSize(file.getAbsolutePath()), readOnly);
    }

    /**
     * Create a MappedBytes for a MappedFile
     *
     * @param rw MappedFile to use
     * @return the MappedBytes
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    public static MappedBytes mappedBytes(@NotNull final MappedFile rw)
            throws ClosedIllegalStateException {
        return rw.createBytesFor();
    }

    /**
     * Creates a MappedBytes instance that wraps a read-only memory-mapped file.
     *
     * @param file The file to be memory-mapped in read-only mode.
     * @return A new MappedBytes instance.
     * @throws FileNotFoundException          If the file does not exist.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public static MappedBytes readOnly(@NotNull final File file)
            throws FileNotFoundException, ClosedIllegalStateException, ThreadingIllegalStateException {
        final MappedFile mappedFile = MappedFile.readOnly(file);
        try {
            return new ChunkedMappedBytes(mappedFile);
        } finally {
            mappedFile.release(INIT);
        }
    }

    /**
     * Checks if the backing file is read-only.
     *
     * @return true if the backing file is read-only, false otherwise.
     */
    public abstract boolean isBackingFileReadOnly();

    /**
     * Checks if the bytes are stored in shared memory.
     *
     * @return true if the bytes are stored in shared memory, false otherwise.
     */
    @Override
    public boolean sharedMemory() {
        return true;
    }

    /**
     * Updates the number of chunks in the mapped file.
     *
     * @param chunkCount The new number of chunks.
     */
    public abstract void chunkCount(long[] chunkCount);

    /**
     * Retrieves the mapped file.
     *
     * @return the MappedFile instance.
     */
    public abstract MappedFile mappedFile();

    /**
     * Ensures that any modifications to this MappedBytes instance are written to the storage device containing the mapped file.
     */
    @Override
    public void sync() {
        final BytesStore bs = bytesStore;
        if (bs instanceof MappedBytesStore) {
            MappedBytesStore mbs = (MappedBytesStore) bs;
            mbs.syncUpTo(writePosition());
        }
    }

    /**
     * Provides a bytes object for read operations. This object is backed by the current MappedBytes instance.
     *
     * @return a Bytes instance for read operations.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @Override
    public @NotNull Bytes<Void> bytesForRead() throws ClosedIllegalStateException {
        throwExceptionIfReleased();

        // MappedBytes don't have a backing BytesStore so we have to give out bytesForRead|Write backed by this
        return isClear()
                ? new VanillaBytes<>(this, writePosition(), bytesStore.writeLimit())
                : new SubBytes<>(this, readPosition(), readLimit() + start());
    }

    /**
     * Provides a bytes object for write operations. This object is backed by the current MappedBytes instance.
     *
     * @return a Bytes instance for write operations.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @Override
    public @NotNull Bytes<Void> bytesForWrite() throws ClosedIllegalStateException {
        throwExceptionIfReleased();

        // MappedBytes don't have a backing BytesStore so we have to give out bytesForRead|Write backed by this
        return new VanillaBytes<>(this, writePosition(), writeLimit());
    }
}
