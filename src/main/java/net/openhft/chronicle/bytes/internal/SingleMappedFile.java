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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.domestic.ReentrantFileLock;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;

import static net.openhft.chronicle.bytes.MappedBytesStore.MAPPED_BYTES_STORE_FACTORY;
import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * A memory mapped files which can be randomly accessed in chunks. It has overlapping regions to
 * avoid wasting bytes at the end of chunks.
 */
@SuppressWarnings({"rawtypes", "unchecked", "restriction"})
public class SingleMappedFile extends MappedFile {
    @NotNull
    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final MappedBytesStore store;
    private final long capacity;

    public SingleMappedFile(@NotNull final File file,
                            @NotNull final RandomAccessFile raf,
                            @NonNegative final long capacity,
                            final boolean readOnly)
            throws IORuntimeException {
        super(file, readOnly);

        this.raf = raf;
        this.fileChannel = raf.getChannel();
        this.capacity = capacity;

        final MapMode mode = readOnly() ? MapMode.READ_ONLY : MapMode.READ_WRITE;

        final long beginNs = System.nanoTime();
        boolean ok = false;
        try {
            Jvm.doNotCloseOnInterrupt(getClass(), this.fileChannel);

            resizeRafIfTooSmall(capacity);
            final long address = OS.map(fileChannel, mode, 0, capacity);
            final MappedBytesStore mbs2 = MappedBytesStore.create(this, this, 0, address, capacity, capacity);
            mbs2.syncMode(DEFAULT_SYNC_MODE);

            final long elapsedNs = System.nanoTime() - beginNs;
            if (newChunkListener != null)
                newChunkListener.onNewChunk(file().getPath(), 0, elapsedNs / 1000);
            if (elapsedNs >= 2_000_000L)
                Jvm.perf().on(getClass(), "Took " + elapsedNs / 1_000_000L + " ms to add mapping for " + file());

            store = mbs2;

            ok = true;

        } catch (IOException ioe) {
            throw new IORuntimeException(ioe);

        } finally {
            if (!ok)
                close();
        }
    }

    @Override
    public void syncMode(SyncMode syncMode) {
        store.syncMode(syncMode);
    }

    @NotNull
    public MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            @NonNegative final long position,
            BytesStore oldByteStore,
            @NotNull final MappedBytesStoreFactory mappedBytesStoreFactory)
            throws IllegalArgumentException {

        if (position != 0)
            throw new IllegalArgumentException();
        store.reserve(owner);
        return store;
    }

    private void resizeRafIfTooSmall(@NonNegative final long minSize)
            throws IOException {
        Jvm.safepoint();

        long size = fileChannel.size();
        Jvm.safepoint();
        if (size >= minSize || readOnly())
            return;

        // handle a possible race condition between processes.
        try {
            // A single JVM cannot lock a distinct canonical file more than once.

            // We might have several MappedFile objects that maps to
            // the same underlying file (possibly via hard or soft links)
            // so we use the canonical path as a lock key

            // Ensure exclusivity for any and all MappedFile objects handling
            // the same canonical file.
            synchronized (internalizedToken()) {
                size = fileChannel.size();
                if (size < minSize) {
                    final long beginNs = System.nanoTime();
                    try (FileLock ignore = ReentrantFileLock.lock(file(), fileChannel)) {
                        size = fileChannel.size();
                        if (size < minSize) {
                            Jvm.safepoint();
                            raf.setLength(minSize);
                            Jvm.safepoint();
                        }
                    }
                    final long elapsedNs = System.nanoTime() - beginNs;
                    if (elapsedNs >= 1_000_000L) {
                        Jvm.perf().on(getClass(), "Took " + elapsedNs / 1000L + " us to grow file " + file());
                    }
                }
            }
        } catch (IOException ioe) {
            throw new IOException("Failed to resize to " + minSize, ioe);
        }
    }

    protected void performRelease() {
        try {
            final MappedBytesStore mbs = store;
            if (mbs != null && RETAIN) {
                // this MappedFile is the only referrer to the MappedBytesStore at this point,
                // so ensure that it is released
                try {
                    mbs.release(this);
                } catch (IllegalStateException e) {
                    Jvm.debug().on(getClass(), e);
                }
            }
        } finally {
            closeQuietly(raf);
            setClosed();
        }
    }

    @NotNull
    public String referenceCounts() {
        @NotNull final StringBuilder sb = new StringBuilder();
        sb.append("refCount: ").append(refCount());
        @Nullable final MappedBytesStore mbs = store;
        long count = 0;
        if (mbs != null)
            count = mbs.refCount();
        sb.append(", ").append(count);

        return sb.toString();
    }

    public long capacity() {
        return capacity;
    }

    public long chunkSize() {
        return capacity;
    }

    public long overlapSize() {
        return 0;
    }

    @Override
    public NewChunkListener getNewChunkListener() {
        return newChunkListener;
    }

    @Override
    public void setNewChunkListener(final NewChunkListener listener) {
        this.newChunkListener = listener;
    }

    public long actualSize()
            throws IORuntimeException, IllegalStateException {

        boolean interrupted = Thread.interrupted();
        try {
            return fileChannelSize();

            // this was seen once deep in the JVM.
        } catch (ArrayIndexOutOfBoundsException aiooe) {
            // try again.
            return actualSize();

        } catch (ClosedByInterruptException cbie) {
            close();
            interrupted = true;
            throw new IllegalStateException(cbie);

        } catch (IOException e) {
            final boolean open = fileChannel.isOpen();
            if (open) {
                throw new IORuntimeException(e);
            } else {
                close();
                throw new IllegalStateException(e);
            }
        } finally {
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }

    private long fileChannelSize()
            throws IOException, ArrayIndexOutOfBoundsException {
        return fileChannel.size();
    }

    @NotNull
    public RandomAccessFile raf() {
        return raf;
    }

    /**
     * This finalize() is used to detect when a component is not released deterministically. It is not required to be run, but provides a warning
     */
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
    public FileLock lock(@NonNegative long position, @NonNegative long size, boolean shared) throws IOException {
        return fileChannel.lock(position, size, shared);
    }

    /**
     * Calls tryLock on the underlying file channel
     */
    public FileLock tryLock(@NonNegative long position, @NonNegative long size, boolean shared) throws IOException {
        return fileChannel.tryLock(position, size, shared);
    }

    public long chunkCount() {
        return 1;
    }

    public void chunkCount(long[] chunkCount) {
        chunkCount[0] = 1;
    }

    @Override
    public MappedBytes createBytesFor() {
        return new SingleMappedBytes(this);
    }
}