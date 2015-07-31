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

public class MappedFile implements ReferenceCounted {
    @NotNull
    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final long chunkSize;
    private final long overlapSize;
    private final List<WeakReference<MappedBytesStore>> stores = new ArrayList<>();
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final long capacity;

    MappedFile(@NotNull File file, long chunkSize, long overlapSize) throws FileNotFoundException {
        this.raf = new RandomAccessFile(file, "rw");
        this.fileChannel = raf.getChannel();
        this.chunkSize = OS.mapAlign(chunkSize);
        this.overlapSize = overlapSize == 0 ? 0 : OS.mapAlign(overlapSize);
        capacity = 1L << 40;
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
        return new MappedFile(file, chunkSize, overlapSize);
    }

    @Nullable
    public MappedBytesStore acquireByteStore(long position) throws IOException {
        if (closed.get())
            throw new IOException("Closed");
        int chunk = (int) (position / chunkSize);
        synchronized (stores) {
            while (stores.size() <= chunk) {
                stores.add(null);
            }
            WeakReference<MappedBytesStore> mbsRef = stores.get(chunk);
            if (mbsRef != null) {
                MappedBytesStore mbs = mbsRef.get();
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
            MappedBytesStore mbs2 = new MappedBytesStore(this, chunk * chunkSize, address, mappedSize, chunkSize);
            stores.set(chunk, new WeakReference<>(mbs2));
            mbs2.reserve();
            System.out.printf("Took %,d us to acquire chunk %,d%n", (System.nanoTime() - start) / 1000, chunk);
//            new Throwable("chunk "+chunk).printStackTrace();
            return mbs2;
        }
    }

    /**
     * Convenience method so you don't need to release the BytesStore
     */

    public Bytes acquireBytesForRead(long position) throws IOException {
        MappedBytesStore mbs = acquireByteStore(position);
        Bytes bytes = mbs.bytesForRead();
        bytes.readPosition(position);
        mbs.release();
        return bytes;
    }

    public void acquireBytesForRead(long position, @NotNull VanillaBytes bytes) throws IOException {
        MappedBytesStore mbs = acquireByteStore(position);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
    }

    public Bytes acquireBytesForWrite(long position) throws IOException {
        MappedBytesStore mbs = acquireByteStore(position);
        Bytes bytes = mbs.bytesForWrite();
        bytes.writePosition(position);
        mbs.release();
        return bytes;
    }

    public void acquireBytesForWrite(long position, @NotNull VanillaBytes bytes) throws IOException {
        MappedBytesStore mbs = acquireByteStore(position);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
        bytes.writePosition(position);
    }

    @Override
    public void reserve() {
        refCount.reserve();
    }

    @Override
    public void release() {
        refCount.release();
    }

    @Override
    public long refCount() {
        return refCount.get();
    }

    public void close() {
        if (!closed.compareAndSet(false, true))
            return;
        synchronized (stores) {
            ReferenceCounted.releaseAll((List) stores);
        }
        release();
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
                    mbs.release();
                    if (count > 1)
                        continue;
                }
            }
            stores.set(i, null);
        }
        try {
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
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
}
