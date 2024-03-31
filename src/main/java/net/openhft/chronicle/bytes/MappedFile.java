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

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.internal.CanonicalPathUtil;
import net.openhft.chronicle.bytes.internal.ChunkedMappedFile;
import net.openhft.chronicle.bytes.internal.SingleMappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.annotation.Positive;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;

import static net.openhft.chronicle.bytes.internal.BytesInternal.uncheckedCast;

/**
 * Represents a memory-mapped file that can be randomly accessed in chunks. The file is divided
 * into chunks, and each chunk has an overlapping region with the next chunk to avoid wasting bytes
 * at the end of each chunk. This class provides methods for locking regions of the file, acquiring
 * bytes for read or write operations, and managing the reference counts of the underlying mapped byte stores.
 * <p>
 * The class is abstract, and specific implementations may differ in how they manage the chunks
 * and underlying file storage.
 */
@SuppressWarnings({"rawtypes", "restriction"})
public abstract class MappedFile extends AbstractCloseableReferenceCounted {

    /**
     * The default disk synchronization mode for this mapped file.
     */
    public static final SyncMode DEFAULT_SYNC_MODE = SyncMode.valueOf(System.getProperty("mappedFile.defaultSyncMode", "ASYNC"));

    /**
     * Flag to indicate if the mapped file should be retained.
     */
    protected static final boolean RETAIN = Jvm.getBoolean("mappedFile.retain");

    /**
     * The default capacity of the mapped file.
     */
    private static final long DEFAULT_CAPACITY = 128L << 40;

    /**
     * A token that represents the internalized file path.
     */
    private final String internalizedToken;

    /**
     * The actual file that is memory-mapped.
     */
    @NotNull
    private final File file;

    /**
     * Flag indicating if the file is read-only.
     */
    private final boolean readOnly;

    /**
     * Listener for the event when a new chunk is created.
     */
    protected NewChunkListener newChunkListener = MappedFile::logNewChunk;

    /**
     * Constructs a new MappedFile with the specified file and read-only flag.
     *
     * @param file     The file to be memory-mapped.
     * @param readOnly True if the file should be read-only, false otherwise.
     * @throws IORuntimeException if an I/O error occurs.
     */
    protected MappedFile(@NotNull final File file,
                         final boolean readOnly)
            throws IORuntimeException {
        this.file = file;
        this.internalizedToken = CanonicalPathUtil.of(file);
        this.readOnly = readOnly;
    }

    /**
     * Logs information about the allocation of a new chunk.
     *
     * @param filename    The name of the file for which the chunk was allocated.
     * @param chunk       The size of the chunk.
     * @param delayMicros The delay in microseconds it took to allocate the chunk.
     */
    static void logNewChunk(final String filename,
                            @NonNegative final int chunk,
                            final long delayMicros) {
        if (delayMicros < 100 || !Jvm.isDebugEnabled(MappedFile.class))
            return;

        // avoid a GC while trying to memory map.
        try (ScopedResource<StringBuilder> stlSb = BytesInternal.acquireStringBuilderScoped()) {
            final String message = stlSb.get()
                    .append("Allocation of ").append(chunk)
                    .append(" chunk in ").append(filename)
                    .append(" took ").append(delayMicros / 1e3).append(" ms.")
                    .toString();
            Jvm.perf().on(ChunkedMappedFile.class, message);
        }
    }

    /**
     * @see #of(File, long, long, int, boolean)
     */
    @NotNull
    public static MappedFile of(@NotNull final File file,
                                @NonNegative final long chunkSize,
                                @NonNegative final long overlapSize,
                                final boolean readOnly)
            throws FileNotFoundException {
        return of(file, chunkSize, overlapSize, PageUtil.getPageSize(file.getAbsolutePath()), readOnly);
    }

    /**
     * Creates and returns a MappedFile instance with the specified file, chunk size, overlap size, pageSize
     * and read-only mode.
     *
     * @param file        The file to be memory-mapped.
     * @param chunkSize   The size of each chunk in bytes.
     * @param overlapSize The size of the overlapping regions between chunks in bytes.
     * @param pageSize The custom page size in bytes.
     * @param readOnly    If true, the file is opened in read-only mode; if false, it is opened for read-write.
     * @return A new MappedFile instance.
     * @throws FileNotFoundException If the specified file does not exist.
     */
    @NotNull
    public static MappedFile of(@NotNull final File file,
                                @NonNegative final long chunkSize,
                                @NonNegative final long overlapSize,
                                @Positive final int pageSize,
                                final boolean readOnly)
            throws FileNotFoundException {

        @NotNull RandomAccessFile raf = new CleaningRandomAccessFile(file, readOnly ? "r" : "rw");
        return new ChunkedMappedFile(file, raf, chunkSize, overlapSize, pageSize, DEFAULT_CAPACITY, readOnly);
    }

    /**
     * Creates and returns a MappedFile instance representing a single chunk of the specified file.
     *
     * @param file     The file to be memory-mapped.
     * @param capacity The capacity of the memory-mapped file in bytes.
     * @param readOnly If true, the file is opened in read-only mode; if false, it is opened for read-write.
     * @return A new MappedFile instance.
     * @throws FileNotFoundException If the specified file does not exist.
     */
    @NotNull
    public static MappedFile ofSingle(@NotNull final File file,
                                      @NonNegative final long capacity,
                                      final boolean readOnly)
            throws FileNotFoundException {

        @NotNull RandomAccessFile raf = new CleaningRandomAccessFile(file, readOnly ? "r" : "rw");
        return new SingleMappedFile(file, raf, capacity, readOnly);
    }

    /**
     * Creates and returns a MappedFile instance for the specified file with the given chunk size.
     * The overlap size is set to the default page size of the operating system.
     *
     * @param file      The file to be memory-mapped.
     * @param chunkSize The size of each chunk in bytes.
     * @return A new MappedFile instance.
     * @throws FileNotFoundException If the specified file does not exist.
     */
    @NotNull
    public static MappedFile mappedFile(@NotNull final File file, @NonNegative final long chunkSize)
            throws FileNotFoundException {
        return mappedFile(file, chunkSize, OS.pageSize());
    }

    /**
     * Creates and returns a MappedFile instance for the specified file with the given chunk size.
     * The overlap size is set to the default page size of the operating system.
     *
     * @param filename  The name of the file to be memory-mapped.
     * @param chunkSize The size of each chunk in bytes.
     * @return A new MappedFile instance.
     * @throws FileNotFoundException If the specified file does not exist.
     */
    @NotNull
    public static MappedFile mappedFile(@NotNull final String filename, @NonNegative final long chunkSize)
            throws FileNotFoundException {
        return mappedFile(filename, chunkSize, OS.pageSize());
    }

    /**
     * Creates and returns a MappedFile instance for the specified file with the given chunk size and overlap size.
     *
     * @param filename    The name of the file to be memory-mapped.
     * @param chunkSize   The size of each chunk in bytes.
     * @param overlapSize The size of the overlapping regions between chunks in bytes.
     * @return A new MappedFile instance.
     * @throws FileNotFoundException If the specified file does not exist.
     */
    @NotNull
    public static MappedFile mappedFile(@NotNull final String filename,
                                        @NonNegative final long chunkSize,
                                        @NonNegative final long overlapSize)
            throws FileNotFoundException {
        return mappedFile(new File(filename), chunkSize, overlapSize);
    }

    /**
     * Creates and returns a MappedFile instance for the specified file with the given chunk size and overlap size.
     *
     * @param file        The file to be memory-mapped.
     * @param chunkSize   The size of each chunk in bytes.
     * @param overlapSize The size of the overlapping regions between chunks in bytes.
     * @return A new MappedFile instance.
     * @throws FileNotFoundException If the specified file does not exist.
     */
    @NotNull
    public static MappedFile mappedFile(@NotNull final File file,
                                        @NonNegative final long chunkSize,
                                        @NonNegative final long overlapSize)
            throws FileNotFoundException {
        return mappedFile(file, chunkSize, overlapSize, false);
    }

    /**
     * Creates and returns a MappedFile instance for the specified file with the given chunk size, overlap size, and read-only mode.
     *
     * @param file        The file to be memory-mapped.
     * @param chunkSize   The size of each chunk in bytes.
     * @param overlapSize The size of the overlapping regions between chunks in bytes.
     * @param readOnly    If true, the file is opened in read-only mode; if false, it is opened for read-write.
     * @return A new MappedFile instance.
     * @throws FileNotFoundException If the specified file does not exist.
     */
    @NotNull
    public static MappedFile mappedFile(@NotNull final File file,
                                        @NonNegative final long chunkSize,
                                        @NonNegative final long overlapSize,
                                        final boolean readOnly)
            throws FileNotFoundException {
        return MappedFile.of(file, chunkSize, overlapSize, readOnly);
    }

    /**
     * Creates and returns a read-only MappedFile instance for the specified file.
     * The chunk size is set to the length of the file and overlap size is set to 0.
     * On Windows, chunks of 4 GB or larger are not supported, so the chunk size is capped at 2^31 bytes
     * and the overlap size is set to the default page size of the operating system.
     *
     * @param file The file to be memory-mapped.
     * @return A new read-only MappedFile instance.
     * @throws FileNotFoundException If the specified file does not exist.
     */
    @NotNull
    public static MappedFile readOnly(@NotNull final File file)
            throws FileNotFoundException {
        long chunkSize = file.length();
        long overlapSize = 0;
        // Chunks of 4 GB+ not supported on Windows.
        if (OS.isWindows() && chunkSize > 2L << 30) {
            chunkSize = 2L << 30;
            overlapSize = OS.pageSize();
        }
        return MappedFile.of(file, chunkSize, overlapSize, true);
    }

    /**
     * Creates and returns a MappedFile instance with the specified file, capacity, chunk size,
     * overlap size, and read-only mode.
     *
     * @param file        The file to be memory-mapped.
     * @param capacity    The total size that the file should take up, in bytes.
     * @param chunkSize   The size of each chunk in bytes.
     * @param overlapSize The size of the overlapping regions between chunks in bytes.
     * @param readOnly    If true, the file is opened in read-only mode; if false, it is opened for read-write.
     * @return A new MappedFile instance.
     * @throws IOException If an I/O error occurs.
     */
    @NotNull
    public static MappedFile mappedFile(@NotNull final File file,
                                        @NonNegative final long capacity,
                                        @NonNegative final long chunkSize,
                                        @NonNegative final long overlapSize,
                                        final boolean readOnly)
            throws IOException {
        final RandomAccessFile raf = new CleaningRandomAccessFile(file, readOnly ? "r" : "rw");
        // Windows throws an exception when setting the length when you reopen
        if (raf.length() < capacity)
            raf.setLength(OS.mapAlign(capacity, PageUtil.getPageSize(file.getAbsolutePath())));
        return new ChunkedMappedFile(file, raf, chunkSize, overlapSize, capacity, readOnly);
    }

    /**
     * Warms up the memory-mapped file by accessing its contents.
     */
    public static void warmup() {
        ChunkedMappedFile.warmup();
    }

    /**
     * Returns the File object associated with this MappedFile.
     *
     * @return The File object.
     */
    @NotNull
    public File file() {
        return file;
    }

    /**
     * Checks if this MappedFile is read-only.
     *
     * @return True if this MappedFile is read-only, false otherwise.
     */
    public boolean readOnly() {
        return readOnly;
    }

    /**
     * Acquires a byte store at the specified position.
     *
     * @param owner    The owner of the byte store.
     * @param position The position at which the byte store should be acquired.
     * @return A MappedBytesStore object.
     * @throws IOException                    If an I/O error occurs.
     * @throws IllegalArgumentException       If the position is negative.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            @NonNegative final long position)
            throws IOException, IllegalArgumentException, IllegalStateException {
        return acquireByteStore(owner, position, null, MappedBytesStore::new);
    }

    /**
     * Acquires a byte store at the specified position with the ability to re-use an existing byte store.
     *
     * @param owner        The owner of the byte store.
     * @param position     The position at which the byte store should be acquired.
     * @param oldByteStore The old byte store that can be re-used, or null if not available.
     * @return A MappedBytesStore object.
     * @throws IOException                    If an I/O error occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            @NonNegative final long position,
            BytesStore oldByteStore)
            throws IOException, IllegalStateException {
        return acquireByteStore(owner, position, oldByteStore, MappedBytesStore::new);
    }

    /**
     * Acquires a byte store at the specified position with the ability to re-use an existing byte store,
     * and specify a custom byte store factory.
     *
     * @param owner                   The owner of the byte store.
     * @param position                The position at which the byte store should be acquired.
     * @param oldByteStore            The old byte store that can be re-used, or null if not available.
     * @param mappedBytesStoreFactory The factory to use for creating the MappedBytesStore.
     * @return A MappedBytesStore object.
     * @throws IOException                    If an I/O error occurs.
     * @throws IllegalArgumentException       If an illegal argument is provided.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public abstract MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            @NonNegative final long position,
            BytesStore<?,?> oldByteStore,
            @NotNull final MappedBytesStoreFactory mappedBytesStoreFactory)
            throws IOException, IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Acquires bytes for read at the specified position, without the need to release the BytesStore.
     *
     * @param owner    The owner of the bytes.
     * @param position The position at which the bytes should be acquired.
     * @return A Bytes object ready for read.
     * @throws IOException                    If an I/O error occurs.
     * @throws BufferUnderflowException       If the position is beyond the limit.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public Bytes<?> acquireBytesForRead(ReferenceOwner owner, @NonNegative final long position)
            throws IOException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        final Bytes<?> bytes = mbs.bytesForRead();
        bytes.readPositionUnlimited(position);
        bytes.reserveTransfer(INIT, owner);
        mbs.release(owner);
        return bytes;
    }

    /**
     * Acquires bytes for read at the specified position and stores them in the provided VanillaBytes object.
     *
     * @param owner    The owner of the bytes.
     * @param position The position at which the bytes should be acquired.
     * @param bytes    The VanillaBytes object to store the acquired bytes.
     * @throws IOException                    If an I/O error occurs.
     * @throws IllegalArgumentException       If an illegal argument is provided.
     * @throws BufferUnderflowException       If there is not enough data available.
     * @throws BufferOverflowException        If there is too much data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public void acquireBytesForRead(ReferenceOwner owner, @NonNegative final long position, @NotNull final VanillaBytes<?> bytes)
            throws IOException, IllegalStateException, IllegalArgumentException, BufferUnderflowException, BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        bytes.bytesStore(uncheckedCast(mbs), position, mbs.capacity() - position);
    }

    /**
     * Acquires bytes for write at the specified position.
     *
     * @param owner    The owner of the bytes.
     * @param position The position at which the bytes should be acquired for write.
     * @return A Bytes object ready for write.
     * @throws IOException                    If an I/O error occurs.
     * @throws IllegalArgumentException       If an illegal argument is provided.
     * @throws BufferOverflowException        If there is too much data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    public Bytes<?> acquireBytesForWrite(ReferenceOwner owner, @NonNegative final long position)
            throws IOException, ClosedIllegalStateException, IllegalArgumentException, BufferOverflowException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        @Nullable MappedBytesStore mbs = acquireByteStore(owner, position, null);
        @NotNull Bytes<?> bytes = mbs.bytesForWrite();
        bytes.writePosition(position);
        bytes.reserveTransfer(INIT, owner);
        mbs.release(owner);
        return bytes;
    }

    /**
     * Acquires bytes for write at the specified position and stores them in the provided VanillaBytes object.
     *
     * @param owner    The owner of the bytes.
     * @param position The position at which the bytes should be acquired for write.
     * @param bytes    The VanillaBytes object to store the acquired bytes.
     * @throws IOException                    If an I/O error occurs.
     * @throws IllegalArgumentException       If an illegal argument is provided.
     * @throws BufferUnderflowException       If there is not enough data available.
     * @throws BufferOverflowException        If there is too much data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public void acquireBytesForWrite(ReferenceOwner owner, @NonNegative final long position, @NotNull final VanillaBytes<?> bytes)
            throws IOException, ClosedIllegalStateException, IllegalArgumentException, BufferUnderflowException, BufferOverflowException, ThreadingIllegalStateException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        bytes.bytesStore(uncheckedCast(mbs), position, mbs.capacity() - position);
        bytes.writePosition(position);
    }

    /**
     * Indicates whether the release operation can be performed in the background.
     *
     * @return True if release operation can be performed in the background; otherwise False.
     */
    @Override
    protected boolean canReleaseInBackground() {
        // don't perform the close in the background as that just sets a flag. This does the real work.
        return true;
    }

    /**
     * Returns a string representation of the reference counts for the underlying mapped byte stores.
     *
     * @return A string representing the reference counts.
     */
    @NotNull
    public abstract String referenceCounts();

    /**
     * Returns the capacity of the mapped file in bytes.
     *
     * @return The capacity in bytes.
     */
    public abstract long capacity();

    /**
     * Returns the size of each chunk in the mapped file in bytes.
     *
     * @return The chunk size in bytes.
     */
    public abstract long chunkSize();

    /**
     * Returns the size of the overlap between consecutive chunks in the mapped file in bytes.
     *
     * @return The overlap size in bytes.
     */
    public abstract long overlapSize();

    /**
     * Returns the listener that is notified when a new chunk is created.
     *
     * @return The listener for new chunks.
     */
    public NewChunkListener getNewChunkListener() {
        return newChunkListener;
    }

    /**
     * Sets the listener to be notified when a new chunk is created.
     *
     * @param listener The listener to be set.
     */
    public void setNewChunkListener(final NewChunkListener listener) {
        this.newChunkListener = listener;
    }

    /**
     * Returns the actual size of the mapped file in bytes, including any space reserved
     * for metadata, headers, etc.
     *
     * @return The actual size in bytes.
     */
    public abstract long actualSize();

    /**
     * Returns the RandomAccessFile associated with the mapped file. This can be used for
     * lower-level operations that are not directly supported by the MappedFile API.
     *
     * @return The RandomAccessFile associated with the mapped file.
     */
    @NotNull
    public abstract RandomAccessFile raf();

    /**
     * This finalize() is used to detect when a component is not released deterministically. It is not required to be run, but provides a warning
     */
    @SuppressWarnings("removal")
    @Override
    protected void finalize()
            throws Throwable {
        warnAndReleaseIfNotReleased();
        super.finalize();
    }

    /**
     * Performs a thread safety check on the component.
     *
     * @param isUsed Flag to indicate whether the component is used.
     * @return Always returns true as the component is thread-safe.
     */
    @Override
    protected boolean threadSafetyCheck(boolean isUsed) {
        // component is thread safe
        return true;
    }

    /**
     * Returns an internalized String that represents a token based on the
     * underlying file's canonical path and some other factors including a
     * per JVM random string.
     * <p>
     * The canonical path is pre-pended with static and random data to reduce the probability of
     * unrelated synchronization on internalized Strings.
     *
     * @return internalized token.
     */
    protected String internalizedToken() {
        return internalizedToken;
    }

    /**
     * Locks a specified region of this mapped file.
     *
     * @param position The byte position at which the locked region begins.
     * @param size     The size of the region to lock, in bytes.
     * @param shared   If {@code true}, the lock will be shared. Otherwise, it will be exclusive.
     * @return A FileLock object representing the lock on the specified region.
     * @throws IOException If an I/O error occurs.
     */
    public abstract FileLock lock(@NonNegative long position, @NonNegative long size, boolean shared) throws IOException;

    /**
     * Attempts to lock a specified region of this mapped file.
     *
     * @param position The byte position at which the locked region begins.
     * @param size     The size of the region to lock, in bytes.
     * @param shared   If {@code true}, the lock will be shared. Otherwise, it will be exclusive.
     * @return A FileLock object representing the lock on the specified region, or {@code null} if the lock could not be acquired.
     * @throws IOException If an I/O error occurs.
     */
    public abstract FileLock tryLock(@NonNegative long position, @NonNegative long size, boolean shared) throws IOException;

    /**
     * Returns the number of chunks in this mapped file.
     *
     * @return The number of chunks.
     */
    public abstract long chunkCount();

    /**
     * Populates the provided array with detailed chunk count information.
     * This may include, for example, counts of specific types of chunks.
     *
     * @param chunkCount The array to be populated with detailed chunk count information.
     */
    public abstract void chunkCount(long[] chunkCount);

    /**
     * Creates a MappedBytes object for this mapped file. The MappedBytes object
     * can be used for reading from and writing to the mapped file.
     *
     * @return A new MappedBytes object associated with this mapped file.
     * @throws ClosedIllegalStateException If the resource has been released or closed.
     * @throws IllegalArgumentException    If the write limit is negative.
     */
    public abstract MappedBytes createBytesFor() throws ClosedIllegalStateException;

    /**
     * This mode determines whether an MS_ASYNC or MS_SYNC should be performed on a chunk release.
     * <p>
     * Performs this sync on any open store as well
     *
     * @param syncMode of sync to perform.
     */
    public abstract void syncMode(SyncMode syncMode);

}
