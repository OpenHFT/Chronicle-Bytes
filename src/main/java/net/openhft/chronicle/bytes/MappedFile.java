/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.internal.CanonicalPathUtil;
import net.openhft.chronicle.bytes.internal.ChunkedMappedFile;
import net.openhft.chronicle.bytes.internal.SingleMappedFile;
import net.openhft.chronicle.core.CleaningRandomAccessFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.AbstractCloseableReferenceCounted;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;

/**
 * A memory mapped files which can be randomly accessed in chunks. It has overlapping regions to
 * avoid wasting bytes at the end of chunks.
 */
@SuppressWarnings({"rawtypes", "unchecked", "restriction"})
public abstract class MappedFile extends AbstractCloseableReferenceCounted {
    static final boolean RETAIN = Jvm.getBoolean("mappedFile.retain");
    private static final long DEFAULT_CAPACITY = 128L << 40;
    protected final String internalizedToken;
    @NotNull
    private final File file;
    private final boolean readOnly;
    protected NewChunkListener newChunkListener = MappedFile::logNewChunk;

    protected MappedFile(@NotNull final File file,
                         final boolean readOnly)
            throws IORuntimeException {
        this.file = file;
        this.internalizedToken = CanonicalPathUtil.of(file);
        this.readOnly = readOnly;
    }

    static void logNewChunk(final String filename,
                            final int chunk,
                            final long delayMicros) {
        if (delayMicros < 100 || !Jvm.isDebugEnabled(MappedFile.class))
            return;

        // avoid a GC while trying to memory map.
        final String message = BytesInternal.acquireStringBuilder()
                .append("Allocation of ").append(chunk)
                .append(" chunk in ").append(filename)
                .append(" took ").append(delayMicros / 1e3).append(" ms.")
                .toString();
        Jvm.perf().on(ChunkedMappedFile.class, message);
    }

    @NotNull
    public static MappedFile of(@NotNull final File file,
                                final long chunkSize,
                                final long overlapSize,
                                final boolean readOnly)
            throws FileNotFoundException {

        @NotNull RandomAccessFile raf = new CleaningRandomAccessFile(file, readOnly ? "r" : "rw");
        final long capacity = /*readOnly ? raf.length() : */DEFAULT_CAPACITY;
        return new ChunkedMappedFile(file, raf, chunkSize, overlapSize, capacity, readOnly);
    }

    @NotNull
    public static MappedFile ofSingle(@NotNull final File file,
                                      final long capacity,
                                      final boolean readOnly)
            throws FileNotFoundException {

        @NotNull RandomAccessFile raf = new CleaningRandomAccessFile(file, readOnly ? "r" : "rw");
        return new SingleMappedFile(file, raf, capacity, readOnly);
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final File file, final long chunkSize)
            throws FileNotFoundException {
        return mappedFile(file, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final String filename, final long chunkSize)
            throws FileNotFoundException {
        return mappedFile(filename, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final String filename,
                                        final long chunkSize,
                                        final long overlapSize)
            throws FileNotFoundException {
        return mappedFile(new File(filename), chunkSize, overlapSize);
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final File file,
                                        final long chunkSize,
                                        final long overlapSize)
            throws FileNotFoundException {
        return mappedFile(file, chunkSize, overlapSize, false);
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final File file,
                                        final long chunkSize,
                                        final long overlapSize,
                                        final boolean readOnly)
            throws FileNotFoundException {
        return MappedFile.of(file, chunkSize, overlapSize, readOnly);
    }

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

    @NotNull
    public static MappedFile mappedFile(@NotNull final File file,
                                        final long capacity,
                                        final long chunkSize,
                                        final long overlapSize,
                                        final boolean readOnly)
            throws IOException {
        final RandomAccessFile raf = new CleaningRandomAccessFile(file, readOnly ? "r" : "rw");
        // Windows throws an exception when setting the length when you re - open
        if (raf.length() < capacity)
            raf.setLength(capacity);
        return new ChunkedMappedFile(file, raf, chunkSize, overlapSize, capacity, readOnly);
    }

    public static void warmup() {
        ChunkedMappedFile.warmup();
    }

    @NotNull
    public File file() {
        return file;
    }

    public boolean readOnly() {
        return readOnly;
    }

    /**
     * @throws IllegalStateException if closed.
     */
    @NotNull
    public MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            final long position)
            throws IOException, IllegalArgumentException, IllegalStateException {
        return acquireByteStore(owner, position, null, MappedBytesStore::new);
    }

    @NotNull
    public MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            final long position,
            BytesStore oldByteStore)
            throws IOException, IllegalStateException {
        try {
            return acquireByteStore(owner, position, oldByteStore, MappedBytesStore::new);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    public abstract MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            final long position,
            BytesStore oldByteStore,
            @NotNull final MappedBytesStoreFactory mappedBytesStoreFactory)
            throws IOException,
            IllegalArgumentException,
            IllegalStateException;

    /**
     * Convenience method so you don't need to release the BytesStore
     */
    @NotNull
    public Bytes acquireBytesForRead(ReferenceOwner owner, final long position)
            throws IOException, IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        final Bytes bytes = mbs.bytesForRead();
        bytes.readPositionUnlimited(position);
        bytes.reserveTransfer(INIT, owner);
        mbs.release(owner);
        return bytes;
    }

    public void acquireBytesForRead(ReferenceOwner owner, final long position, @NotNull final VanillaBytes bytes)
            throws IOException, IllegalStateException, IllegalArgumentException, BufferUnderflowException, BufferOverflowException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
    }

    @NotNull
    public Bytes acquireBytesForWrite(ReferenceOwner owner, final long position)
            throws IOException, IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosed();

        @Nullable MappedBytesStore mbs = acquireByteStore(owner, position, null);
        @NotNull Bytes bytes = mbs.bytesForWrite();
        bytes.writePosition(position);
        bytes.reserveTransfer(INIT, owner);
        mbs.release(owner);
        return bytes;
    }

    public void acquireBytesForWrite(ReferenceOwner owner, final long position, @NotNull final VanillaBytes bytes)
            throws IOException, IllegalStateException, IllegalArgumentException, BufferUnderflowException, BufferOverflowException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
        bytes.writePosition(position);
    }

    @Override
    protected boolean canReleaseInBackground() {
        // don't perform the close in the background as that just sets a flag. This does the real work.
        return true;
    }

    @NotNull
    public abstract String referenceCounts();

    public abstract long capacity();

    public abstract long chunkSize();

    public abstract long overlapSize();

    public NewChunkListener getNewChunkListener() {
        return newChunkListener;
    }

    public void setNewChunkListener(final NewChunkListener listener) {
        this.newChunkListener = listener;
    }

    public abstract long actualSize();

    @NotNull
    public abstract RandomAccessFile raf();

    @Override
    protected void finalize()
            throws Throwable {
        warnAndReleaseIfNotReleased();
        super.finalize();
    }

    @Override
    protected boolean threadSafetyCheck(boolean isUsed) {
        // component is thread safe
        return true;
    }

    /**
     * Calls lock on the underlying file channel
     */
    public abstract FileLock lock(long position, long size, boolean shared) throws IOException;

    /**
     * Calls tryLock on the underlying file channel
     */
    public abstract FileLock tryLock(long position, long size, boolean shared) throws IOException;

    public abstract long chunkCount();

    public abstract void chunkCount(long[] chunkCount);

    public abstract MappedBytes createBytesFor();
}