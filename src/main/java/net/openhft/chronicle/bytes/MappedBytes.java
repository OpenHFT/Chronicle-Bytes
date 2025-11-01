/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
 * A specialised {@link Bytes} implementation backed by a memory-mapped file.
 * <p>
 * The underlying file is accessed in chunks (64&nbsp;MiB by default) so very
 * large files can be treated as if they were in memory. Only the most recently
 * accessed chunk is retained; previous chunks are released. Call
 * {@link #releaseLast()} when finished with a {@code MappedBytes} instance to
 * free system resources.
 * <p>
 * Instances are single threaded and should be reserved before use.
 */
@SuppressWarnings("rawtypes")
public abstract class MappedBytes extends AbstractBytes<Void> implements Closeable, ManagedCloseable, Syncable {

    /**
     * System property flag {@code trace.mapped.bytes} for enabling trace
     * logging of {@code MappedBytes} operations.
     */
    protected static final boolean TRACE = Jvm.getBoolean("trace.mapped.bytes");

    // assume the mapped file is reserved already.
    /**
     * Constructs an instance for use by subclasses. The resulting object is
     * backed by an empty {@link BytesStore} until a real mapping is assigned.
     */
    protected MappedBytes()
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        this("");
    }

    /**
     * Constructs an instance for use by subclasses with a descriptive name.
     * The instance initially references an empty store.
     */
    protected MappedBytes(final String name)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        super(BytesStore.empty(),
                BytesStore.empty().writePosition(),
                BytesStore.empty().writeLimit(),
                name);
    }

    /**
     * Creates a {@code MappedBytes} covering the entire file as a single mapping.
     *
     * @param filename path of the file to map
     * @param capacity total capacity to map
     * @return a new {@code MappedBytes}
     * @throws FileNotFoundException       if the file cannot be found
     * @throws ClosedIllegalStateException if the mapped file is already closed
     * @throws ThreadingIllegalStateException if accessed from multiple threads
     */
    @NotNull
    public static MappedBytes singleMappedBytes(@NotNull final String filename, @NonNegative final long capacity)
            throws FileNotFoundException, IllegalStateException {
        return singleMappedBytes(new File(filename), capacity);
    }

    /**
     * As {@link #singleMappedBytes(String, long)} but accepting a {@link File} instance.
     */
    @NotNull
    public static MappedBytes singleMappedBytes(@NotNull final File file, @NonNegative final long capacity)
            throws FileNotFoundException {
        return singleMappedBytes(file, capacity, false);
    }

    /**
     * Creates a single mapping for the whole file with explicit read-only option.
     *
     * @param file     file to map
     * @param capacity total capacity
     * @param readOnly {@code true} for read only
     * @return a new {@code MappedBytes}
     * @throws FileNotFoundException if the file does not exist
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
     * Creates a chunked mapping for the given file.
     *
     * @param filename  path of the file to map
     * @param chunkSize size of each chunk in bytes
     * @return a new {@code MappedBytes}
     * @throws FileNotFoundException          if the file does not exist
     * @throws ClosedIllegalStateException    if the mapped file is closed
     * @throws ThreadingIllegalStateException if accessed from multiple threads
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
     * Creates a chunked mapping with a specified overlap between chunks.
     *
     * @param file        file to map
     * @param chunkSize   size of each chunk
     * @param overlapSize overlap between adjacent chunks
     * @return a new {@code MappedBytes}
     * @throws FileNotFoundException          if the file does not exist
     * @throws ClosedIllegalStateException    if the mapped file is closed
     * @throws ThreadingIllegalStateException if accessed from multiple threads
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
     * Creates a chunked mapping with explicit overlap, page size and read-only option.
     *
     * @param file        file to map
     * @param chunkSize   size of each chunk
     * @param overlapSize overlap between adjacent chunks
     * @param pageSize    page size to align mappings
     * @param readOnly    {@code true} for read only
     * @return a new {@code MappedBytes}
     * @throws FileNotFoundException          if the file does not exist
     * @throws ClosedIllegalStateException    if the mapped file is closed
     * @throws ThreadingIllegalStateException if accessed from multiple threads
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
     * Convenience overload using the default page size.
     *
     * @see #mappedBytes(File, long, long, int, boolean)
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
     * Creates a {@code MappedBytes} view for a pre-existing {@link MappedFile}.
     *
     * @param rw the mapped file to use
     * @return the created {@code MappedBytes}
     * @throws ClosedIllegalStateException    if the mapped file has been closed
     * @throws ThreadingIllegalStateException if accessed from multiple threads
     */
    @NotNull
    public static MappedBytes mappedBytes(@NotNull final MappedFile rw)
            throws ClosedIllegalStateException {
        return rw.createBytesFor();
    }

    /**
     * Maps the given file in read-only mode.
     *
     * @param file file to map
     * @return a new read only {@code MappedBytes}
     * @throws FileNotFoundException          if the file does not exist
     * @throws ClosedIllegalStateException    if the mapped file is closed
     * @throws ThreadingIllegalStateException if accessed from multiple threads
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
     * @return {@code true} if the underlying file was mapped read only
     */
    public abstract boolean isBackingFileReadOnly();

    /**
     * Memory-mapped files are a form of shared memory, hence this returns {@code true}.
     */
    @Override
    public boolean sharedMemory() {
        return true;
    }

    /**
     * Populates the supplied array with the number of chunks held by the underlying {@link MappedFile}.
     */
    public abstract void chunkCount(long[] chunkCount);

    /**
     * @return the underlying {@link MappedFile}
     */
    public abstract MappedFile mappedFile();

    /**
     * Forces any changes made to this mapping to be written to the storage device.
     */
    @Override
    public void sync() {
        final BytesStore<?, ?> bs = bytesStore;
        if (bs instanceof MappedBytesStore) {
            MappedBytesStore mbs = (MappedBytesStore) bs;
            mbs.syncUpTo(writePosition());
        }
    }

    /**
     * Returns a view for reading from the current mapping.
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
     * Returns a view for writing to the current mapping.
     */
    @Override
    public @NotNull Bytes<Void> bytesForWrite() throws ClosedIllegalStateException {
        throwExceptionIfReleased();

        // MappedBytes don't have a backing BytesStore so we have to give out bytesForRead|Write backed by this
        return new VanillaBytes<>(this, writePosition(), writeLimit());
    }
}
