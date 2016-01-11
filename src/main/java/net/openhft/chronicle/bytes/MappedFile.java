/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.ReferenceCounted;
import net.openhft.chronicle.core.ReferenceCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A memory mapped files which can be randomly accessed in chunks.
 * It has overlapping regions to avoid wasting bytes at the end of chunks.
 */
public class MappedFile implements ReferenceCounted {

    public static final long DEFAULT_CAPACITY = 1L << 40;
    private static final Logger LOG = LoggerFactory.getLogger(MappedFile.class);
    @NotNull
    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final long chunkSize;
    private final long overlapSize;
    private final List<WeakReference<MappedBytesStore>> stores = new ArrayList<>();
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final long capacity;
    @NotNull
    private final File file;

    protected MappedFile(@NotNull File file, @NotNull RandomAccessFile raf, long chunkSize, long overlapSize, long capacity) {
        this.file = file;
        this.raf = raf;
        this.fileChannel = raf.getChannel();
        this.chunkSize = OS.mapAlign(chunkSize);
        this.overlapSize = OS.mapAlign(overlapSize);
        this.capacity = capacity;
    }

    public static MappedFile of(@NotNull File file, long chunkSize, long overlapSize) throws FileNotFoundException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        return new MappedFile(file, raf, chunkSize, overlapSize, DEFAULT_CAPACITY);
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
        return MappedFile.of(file, chunkSize, overlapSize);
    }

    public MappedFile withSizes(long chunkSize, long overlapSize) {
        chunkSize = OS.mapAlign(chunkSize);
        overlapSize = OS.mapAlign(overlapSize);
        if (chunkSize == this.chunkSize && overlapSize == this.overlapSize)
            return this;
        try {
            return new MappedFile(file, raf, chunkSize, overlapSize, capacity);
        } finally {
            release();
        }
    }

    public File file() {
        return file;
    }

    @Nullable
    public MappedBytesStore acquireByteStore(long position) throws IOException, IllegalArgumentException, IllegalStateException {
        return acquireByteStore(position, (owner, start, address, capacity1, safeCapacity) -> new
                MappedBytesStore(owner, start, address, capacity1, safeCapacity, this));
    }

    @Nullable
    public <T extends MappedBytesStore> T acquireByteStore(long position, MappedBytesStoreFactory<T> mappedBytesStoreFactory) throws IOException, IllegalArgumentException, IllegalStateException {
        if (closed.get())
            throw new IOException("Closed");
        int chunk = (int) (position / chunkSize);
        synchronized (stores) {
            while (stores.size() <= chunk) {
                stores.add(null);
            }
            WeakReference<MappedBytesStore> mbsRef = stores.get(chunk);
            if (mbsRef != null) {
                T mbs = (T) mbsRef.get();
                if (mbs != null && mbs.tryReserve()) {
                    return mbs;
                }
            }
            long minSize = (chunk + 1L) * chunkSize + overlapSize;
            long size = fileChannel.size();
            if (size < minSize) {
                // handle a possible race condition between processes.
                try (FileLock lock = fileChannel.lock()) {
                    size = fileChannel.size();
                    if (size < minSize) {
                        raf.setLength(minSize);
                    }
                }
            }
            long start = System.nanoTime();
            long mappedSize = chunkSize + overlapSize;
            long address = OS.map(fileChannel, FileChannel.MapMode.READ_WRITE, chunk * chunkSize, mappedSize);
            T mbs2 = mappedBytesStoreFactory.create(this, chunk * chunkSize, address, mappedSize, chunkSize);
            stores.set(chunk, new WeakReference<>(mbs2));
            mbs2.reserve();
            LOG.warn(String.format("Took %,d us to acquire chunk %,d",
                    (System.nanoTime() - start) / 1000,
                    chunk));
//            new Throwable("chunk "+chunk).printStackTrace();
            return mbs2;
        }
    }

    /**
     * Convenience method so you don't need to release the BytesStore
     */
    public Bytes acquireBytesForRead(long position)
            throws IOException, IllegalStateException, IllegalArgumentException {
        MappedBytesStore mbs = acquireByteStore(position);
        Bytes bytes = mbs.bytesForRead();
        bytes.readPosition(position);
        mbs.release();
        return bytes;
    }

    public void acquireBytesForRead(long position, @NotNull VanillaBytes bytes)
            throws IOException, IllegalStateException, IllegalArgumentException {
        MappedBytesStore mbs = acquireByteStore(position);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
    }

    public Bytes acquireBytesForWrite(long position)
            throws IOException, IllegalStateException, IllegalArgumentException {
        MappedBytesStore mbs = acquireByteStore(position);
        MappedBytes bytes = mbs.bytesForWrite();
        bytes.writePosition(position);
        mbs.release();
        return bytes;
    }

    public void acquireBytesForWrite(long position, @NotNull VanillaBytes bytes)
            throws IOException, IllegalStateException, IllegalArgumentException {
        MappedBytesStore mbs = acquireByteStore(position);
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

    public void close() {
        if (!closed.compareAndSet(false, true))
            return;
        try {
            synchronized (stores) {
                ReferenceCounted.releaseAll((List) stores);
            }
            release();
        } catch (IllegalStateException e) {
            LOG.error("", e);
        }
    }

    private void performRelease() {
        for (int i = 0; i < stores.size(); i++) {
            WeakReference<MappedBytesStore> storeRef = stores.get(i);
            if (storeRef == null)
                continue;
            MappedBytesStore mbs = storeRef.get();
            if (mbs != null) {
                long count = mbs.refCount();
                if (count > 0) {
                    try {
                        mbs.release();
                    } catch (IllegalStateException e) {
                        LOG.error("", e);
                    }
                    if (count > 1)
                        continue;
                }
            }
            stores.set(i, null);
        }
        try {
            fileChannel.close();
        } catch (IOException e) {
            LOG.error("", e);
        }
    }

    @NotNull
    public String referenceCounts() {
        StringBuilder sb = new StringBuilder();
        sb.append("refCount: ").append(refCount());
        for (WeakReference<MappedBytesStore> store : stores) {
            long count = 0;
            if (store != null) {
                MappedBytesStore mbs = store.get();
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
}
