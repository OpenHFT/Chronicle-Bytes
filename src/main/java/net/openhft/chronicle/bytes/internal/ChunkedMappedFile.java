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

package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.CleaningRandomAccessFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.onoes.Slf4jExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * A memory mapped files which can be randomly accessed in chunks. It has overlapping regions to
 * avoid wasting bytes at the end of chunks.
 */
@SuppressWarnings({"restriction"})
public class ChunkedMappedFile extends MappedFile {
    static final boolean RETAIN = Jvm.getBoolean("mappedFile.retain");

    @NotNull
    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final long chunkSize;
    private final long overlapSize;
    private final List<MappedBytesStore> stores = new ArrayList<>();
    private final long capacity;
    private long[] chunkCount = {0L};

    public ChunkedMappedFile(@NotNull final File file,
                             @NotNull final RandomAccessFile raf,
                             final long chunkSize,
                             final long overlapSize,
                             final long capacity,
                             final boolean readOnly)
            throws IORuntimeException {
        super(file, readOnly);

        this.raf = raf;
        this.fileChannel = raf.getChannel();
        this.chunkSize = OS.mapAlign(chunkSize);
        this.overlapSize = overlapSize > 0 && overlapSize < 64 << 10 ? chunkSize : OS.mapAlign(overlapSize);
        this.capacity = capacity;

        Jvm.doNotCloseOnInterrupt(getClass(), this.fileChannel);
    }

    public static void warmup() {
        final List<IOException> errorsDuringWarmup = new ArrayList<>();
        try {
            Jvm.setExceptionHandlers(Slf4jExceptionHandler.ERROR, null, null);

            final Path path = Files.createTempDirectory("warmup");

            final File file = File.createTempFile("delete_warming_up", "me", path.toFile());
            file.deleteOnExit();
            final long mapAlignment = OS.mapAlignment();
            final int chunks = 64;
            final int compileThreshold = Jvm.compileThreshold();
            for (int j = 0; j <= compileThreshold; j += chunks) {
                warmupChunks(errorsDuringWarmup, file, mapAlignment, chunks);
            }
            Thread.yield();
            Files.delete(file.toPath());
        } catch (IOException e) {
            Jvm.resetExceptionHandlers();
            Jvm.warn().on(ChunkedMappedFile.class, "Error during warmup", e);
        } finally {
            Jvm.resetExceptionHandlers();
            if (!errorsDuringWarmup.isEmpty())
                Jvm.warn().on(ChunkedMappedFile.class, errorsDuringWarmup.size() + " errors during warmup: " + errorsDuringWarmup);
        }
    }

    private static void warmupChunks(List<IOException> errorsDuringWarmup,
                                     File file,
                                     long mapAlignment,
                                     int chunks) {
        try {
            try (@NotNull RandomAccessFile raf = new CleaningRandomAccessFile(file, "rw")) {
                try (final ChunkedMappedFile mappedFile = new ChunkedMappedFile(file, raf, mapAlignment, 0, mapAlignment * chunks, false)) {
                    warmup0(mapAlignment, chunks, mappedFile);
                }
            }
            Thread.yield();
        } catch (IOException e) {
            errorsDuringWarmup.add(e);
        }
    }

    private static void warmup0(final long mapAlignment,
                                final int chunks,
                                @NotNull final ChunkedMappedFile mappedFile) {
        try {
            ReferenceOwner warmup = ReferenceOwner.temporary("warmup");
            for (int i = 0; i < chunks; i++) {
                mappedFile.acquireBytesForRead(warmup, i * mapAlignment)
                        .release(warmup);
                mappedFile.acquireBytesForWrite(warmup, i * mapAlignment)
                        .release(warmup);
            }
        } catch (BufferUnderflowException | IllegalArgumentException | IOException | IllegalStateException | BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }


    @NotNull
    public MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            final long position,
            BytesStore oldByteStore,
            @NotNull final MappedBytesStoreFactory mappedBytesStoreFactory)
            throws IOException,
            IllegalArgumentException,
            IllegalStateException {

        throwExceptionIfClosed();

        if (position < 0)
            throw new IOException("Attempt to access a negative position: " + position);
        final int chunk = (int) (position / chunkSize);

        final MappedBytesStore mbs;
        synchronized (stores) {
            while (stores.size() <= chunk)
                stores.add(null);
            mbs = stores.get(chunk);
        }
        if (mbs != null) {
            // don't reserve it again if we are already holding it.
            if (mbs == oldByteStore) {
                return mbs;
            }
            if (mbs.tryReserve(owner)) {
                return mbs;
            }
        }

        // its important we perform this outside the synchronized below, as this operation can take a while and if synchronized can block slow tailer
        // from acquiring the next block
        resizeRafIfTooSmall(chunk);

        synchronized (stores) {

            // We are back, protected by synchronized, and need to
            // update our view on previous existence (we might have been stalled
            // for a long time since we last checked dues to resizing and another
            // thread might have added a MappedByteStore (very unlikely but still possible))
            final MappedBytesStore mbs1 = stores.get(chunk);
            if (mbs1 != null && mbs1.tryReserve(owner)) {
                return mbs1;
            }
            // *** THIS CAN TAKE A LONG TIME IF A RESIZE HAS TO OCCUR ***
            // let double check it to make sure no other thread change it in the meantime.
            // resizeRafIfTooSmall ( chunk )

            final long mappedSize = chunkSize + overlapSize;
            final MapMode mode = readOnly() ? MapMode.READ_ONLY : MapMode.READ_WRITE;
            final long startOfMap = chunk * chunkSize;

            final long beginNs = System.nanoTime();

            throwExceptionIfClosed();

            final long address = OS.map(fileChannel, mode, startOfMap, mappedSize);
            final MappedBytesStore mbs2 =
                    mappedBytesStoreFactory.create(owner, this, chunk * this.chunkSize, address, mappedSize, this.chunkSize);
            if (RETAIN)
                mbs2.reserve(this);
            stores.set(chunk, mbs2);

            final long elapsedNs = System.nanoTime() - beginNs;
            if (newChunkListener != null)
                newChunkListener.onNewChunk(file().getPath(), chunk, elapsedNs / 1000);
            chunkCount[0]++;
            if (elapsedNs >= 2_000_000L)
                Jvm.perf().on(getClass(), "Took " + elapsedNs / 1_000_000L + " ms to add mapping for " + file());

            return mbs2;
        }
    }

    private void resizeRafIfTooSmall(final int chunk)
            throws IOException {
        Jvm.safepoint();

        final long minSize = (chunk + 1L) * chunkSize + overlapSize;
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
                    try (FileLock ignore = fileChannel.lock()) {
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
            synchronized (stores) {
                for (int i = 0; i < stores.size(); i++) {
                    final MappedBytesStore mbs = stores.get(i);
                    if (mbs != null && RETAIN) {
                        // this MappedFile is the only referrer to the MappedBytesStore at this point,
                        // so ensure that it is released
                        try {
                            mbs.release(this);
                        } catch (IllegalStateException e) {
                            Jvm.debug().on(getClass(), e);
                        }
                    }
                    // Dereference released entities
                    stores.set(i, null);
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
        for (@Nullable final MappedBytesStore mbs : stores) {
            long count = 0;
            if (mbs != null)
                count = mbs.refCount();
            sb.append(", ").append(count);
        }
        return sb.toString();
    }

    public long capacity() {
        return capacity;
    }

    public long chunkSize() {
        return chunkSize;
    }

    public long overlapSize() {
        return overlapSize;
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
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return fileChannel.lock(position, size, shared);
    }

    /**
     * Calls tryLock on the underlying file channel
     */
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return fileChannel.tryLock(position, size, shared);
    }

    public long chunkCount() {
        return chunkCount[0];
    }

    public void chunkCount(long[] chunkCount) {
        this.chunkCount = chunkCount;
    }

    @Override
    public MappedBytes createBytesFor() {
        return new ChunkedMappedBytes(this);
    }
}