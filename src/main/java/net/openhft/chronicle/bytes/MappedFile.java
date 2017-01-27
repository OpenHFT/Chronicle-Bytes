/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.ReferenceCounted;
import net.openhft.chronicle.core.ReferenceCounter;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A memory mapped files which can be randomly accessed in chunks. It has overlapping regions to
 * avoid wasting bytes at the end of chunks.
 */
public class MappedFile implements ReferenceCounted {
    public static final long DEFAULT_CAPACITY = 128L << 40;
    // A single JVM cannot lock a file more than once.
    private static final Object GLOBAL_FILE_LOCK = new Object();
    @NotNull
    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final long chunkSize;
    private final long overlapSize;
    private final List<WeakReference<MappedBytesStore>> stores = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);
    private final long capacity;
    private final boolean readOnly;
    @NotNull
    private final File file;
    private NewChunkListener newChunkListener = (filename, chunk, delayMicros) ->
            Jvm.debug().on(MappedFile.class, "Allocation of " + chunk + " chunk in " + filename + " took " + delayMicros / 1e3 + " ms.");

    protected MappedFile(@NotNull File file, @NotNull RandomAccessFile raf, long chunkSize, long overlapSize, long capacity, boolean readOnly) {
        this.file = file;
        this.raf = raf;
        this.fileChannel = raf.getChannel();
        this.chunkSize = OS.mapAlign(chunkSize);
        this.overlapSize = OS.mapAlign(overlapSize);
        this.capacity = capacity;
        this.readOnly = readOnly;
    }

    @NotNull
    public static MappedFile of(@NotNull File file, long chunkSize, long overlapSize, boolean readOnly) throws FileNotFoundException {
        @NotNull RandomAccessFile raf = new RandomAccessFile(file, readOnly ? "r" : "rw");
        try {
            long capacity = readOnly ? raf.length() : DEFAULT_CAPACITY;
            return new MappedFile(file, raf, chunkSize, overlapSize, capacity, readOnly);
        } catch (IOException e) {
            Closeable.closeQuietly(raf);
            @NotNull FileNotFoundException fnfe = new FileNotFoundException("Unable to open " + file);
            fnfe.initCause(e);
            throw fnfe;
        }
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull File file, long chunkSize) throws FileNotFoundException {
        return mappedFile(file, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull String filename, long chunkSize) throws FileNotFoundException {
        return mappedFile(filename, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull String filename, long chunkSize, long overlapSize) throws FileNotFoundException {
        return mappedFile(new File(filename), chunkSize, overlapSize);
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull File file, long chunkSize, long overlapSize) throws FileNotFoundException {
        return mappedFile(file, chunkSize, overlapSize, false);
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull File file, long chunkSize, long overlapSize, boolean readOnly) throws FileNotFoundException {
        return MappedFile.of(file, chunkSize, overlapSize, readOnly);
    }

    @NotNull
    public static MappedFile readOnly(@NotNull File file) throws FileNotFoundException {
        long chunkSize = file.length();
        long overlapSize = 0;
        // Chunks of 4 GB+ not supported on Windows.
        if (OS.isWindows() && chunkSize > 2 << 30) {
            chunkSize = 2 << 30;
            overlapSize = OS.pageSize();
        }
        return MappedFile.of(file, chunkSize, overlapSize, true);
    }

    public static void warmup() {
        try {
            @NotNull File file = File.createTempFile("delete", ".me");
            file.deleteOnExit();
            long mapAlignment = OS.mapAlignment();
            int chunks = 64;
            int compileThreshold = Jvm.compileThreshold();
            for (int j = 0; j <= compileThreshold; j += chunks) {
                try {
                    try (@NotNull RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                        @NotNull MappedFile mappedFile = new MappedFile(file, raf, mapAlignment, 0, mapAlignment * chunks, false);
                        warmup0(mapAlignment, chunks, mappedFile);
                        mappedFile.release();
                    }
                    Thread.yield();
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    Jvm.debug().on(MappedFile.class, "Error during warmup", e);
                }
            }

        } catch (IOException e) {
            Jvm.warn().on(MappedFile.class, "Error during warmup", e);
        }
    }

    private static void warmup0(long mapAlignment, int chunks, @NotNull MappedFile mappedFile) throws IOException {
        for (int i = 0; i < chunks; i++) {
            mappedFile.acquireBytesForRead(i * mapAlignment).release();
            mappedFile.acquireBytesForWrite(i * mapAlignment).release();
        }

    }

    @NotNull
    public MappedFile withSizes(long chunkSize, long overlapSize) {
        chunkSize = OS.mapAlign(chunkSize);
        overlapSize = OS.mapAlign(overlapSize);
        if (chunkSize == this.chunkSize && overlapSize == this.overlapSize)
            return this;
        try {
            return new MappedFile(file, raf, chunkSize, overlapSize, capacity, readOnly);
        } finally {
            release();
        }
    }

    @NotNull
    public File file() {
        return file;
    }

    @Nullable
    public MappedBytesStore acquireByteStore(long position) throws IOException, IllegalArgumentException, IllegalStateException {
        return acquireByteStore(position, readOnly ? MappedBytesStore::new : ReadOnlyMappedBytesStore::new);
    }

    @Nullable
    public <T extends MappedBytesStore> T acquireByteStore(long position, @NotNull MappedBytesStoreFactory<T> mappedBytesStoreFactory) throws IOException, IllegalArgumentException, IllegalStateException {
        if (closed.get())
            throw new IOException("Closed");
        if (position < 0)
            throw new IOException("Attempt to access a negative position: " + position);
        int chunk = (int) (position / chunkSize);
        synchronized (stores) {
            while (stores.size() <= chunk) {
                stores.add(null);
            }
            WeakReference<MappedBytesStore> mbsRef = stores.get(chunk);
            if (mbsRef != null) {
                @NotNull T mbs = (T) mbsRef.get();
                if (mbs != null && mbs.tryReserve()) {
                    return mbs;
                }
            }
            long start = System.nanoTime();
            long minSize = (chunk + 1L) * chunkSize + overlapSize;
            long size = fileChannel.size();
            if (size < minSize) {
                // handle a possible race condition between processes.
                try {
                    synchronized (GLOBAL_FILE_LOCK) {
                        size = fileChannel.size();
                        if (size < minSize) {
                            try (FileLock ignore = fileChannel.lock()) {
                                size = fileChannel.size();
                                if (size < minSize) {
                                    raf.setLength(minSize);
                                }
                            }
                        }
                    }

                } catch (IOException ioe) {
                    throw new IOException("Failed to resize to " + minSize, ioe);
                }
            }
            long mappedSize = chunkSize + overlapSize;
            FileChannel.MapMode mode = readOnly ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE;
            long address = OS.map(fileChannel, mode, chunk * chunkSize, mappedSize);
            final long safeCapacity = this.chunkSize + overlapSize / 2;
            T mbs2 = mappedBytesStoreFactory.create(this, chunk * this.chunkSize, address, mappedSize, safeCapacity);
            stores.set(chunk, new WeakReference<>(mbs2));
            mbs2.reserve();
            if (newChunkListener != null)
                newChunkListener.onNewChunk(file.getPath(), chunk, (System.nanoTime() - start) / 1000);
//            new Throwable("chunk "+chunk).printStackTrace();
            return mbs2;
        }
    }

    /**
     * Convenience method so you don't need to release the BytesStore
     */
    public Bytes acquireBytesForRead(long position)
            throws IOException, IllegalStateException, IllegalArgumentException {
        @Nullable MappedBytesStore mbs = acquireByteStore(position);
        Bytes bytes = mbs.bytesForRead();
        bytes.readPositionUnlimited(position);
        mbs.release();
        return bytes;
    }

    public void acquireBytesForRead(long position, @NotNull VanillaBytes bytes)
            throws IOException, IllegalStateException, IllegalArgumentException {
        @Nullable MappedBytesStore mbs = acquireByteStore(position);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
    }

    @NotNull
    public Bytes acquireBytesForWrite(long position)
            throws IOException, IllegalStateException, IllegalArgumentException {
        @Nullable MappedBytesStore mbs = acquireByteStore(position);
        @NotNull Bytes bytes = mbs.bytesForWrite();
        bytes.writePosition(position);
        mbs.release();
        return bytes;
    }

    public void acquireBytesForWrite(long position, @NotNull VanillaBytes bytes)
            throws IOException, IllegalStateException, IllegalArgumentException {
        @Nullable MappedBytesStore mbs = acquireByteStore(position);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
        bytes.writePosition(position);
    }

    @Override
    public void reserve() throws IllegalStateException {
        refCount.reserve();
    }

    @Override
    public void release() throws IllegalStateException {
        refCount.release();
    }

    @Override
    public long refCount() {
        return refCount.get();
    }


    private void performRelease() {
        for (int i = 0; i < stores.size(); i++) {
            WeakReference<MappedBytesStore> storeRef = stores.get(i);
            if (storeRef == null)
                continue;
            @Nullable MappedBytesStore mbs = storeRef.get();
            if (mbs != null) {
                long count = mbs.refCount();
                if (count > 0) {
                    try {
                        mbs.release();

                    } catch (IllegalStateException e) {
                        Jvm.debug().on(getClass(), e);
                    }
                    if (count > 1)
                        continue;
                }
            }
            stores.set(i, null);
        }
        try {
            raf.close();
            closed.set(true);
        } catch (IOException e) {
            Jvm.debug().on(getClass(), e);
        }
    }

    @NotNull
    public String referenceCounts() {
        @NotNull StringBuilder sb = new StringBuilder();
        sb.append("refCount: ").append(refCount());
        for (@Nullable WeakReference<MappedBytesStore> store : stores) {
            long count = 0;
            if (store != null) {
                @Nullable MappedBytesStore mbs = store.get();
                if (mbs != null)
                    count = mbs.refCount();
            }
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

    public NewChunkListener getNewChunkListener() {
        return newChunkListener;
    }

    public void setNewChunkListener(NewChunkListener listener) {
        this.newChunkListener = listener;
    }

    public long actualSize() throws IORuntimeException {
        try {
            return fileChannel.size();

        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    public boolean isClosed() {
        return closed.get();
    }
}
