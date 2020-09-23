/*
 * Copyright 2016-2020 Chronicle Software
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

import net.openhft.chronicle.core.CleaningRandomAccessFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.AbstractCloseableReferenceCounted;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.onoes.Slf4jExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * A memory mapped files which can be randomly accessed in chunks. It has overlapping regions to
 * avoid wasting bytes at the end of chunks.
 */
@SuppressWarnings({"rawtypes", "unchecked", "restriction"})
public class MappedFile extends AbstractCloseableReferenceCounted {
    static final boolean RETAIN = Jvm.getBoolean("mappedFile.retain");
    private static final long DEFAULT_CAPACITY = 128L << 40;

    // A single JVM cannot lock a distinct canonical file more than once
    private static final Map<String, WeakReference<NamedObject>> FILE_LOCKS = new HashMap<>();
    private static final int EXPUNGE_MODULO = 64;
    private static int EXPUNGE_COUNTER = 0;

    @NotNull
    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final long chunkSize;
    private final long overlapSize;
    private final List<MappedBytesStore> stores = new ArrayList<>();
    private final long capacity;
    @NotNull
    private final File file;
    private final String canonicalPath;
    private final boolean readOnly;
    private NewChunkListener newChunkListener = MappedFile::logNewChunk;

    protected MappedFile(@NotNull final File file,
                         @NotNull final RandomAccessFile raf,
                         final long chunkSize,
                         final long overlapSize,
                         final long capacity,
                         final boolean readOnly) {
        this.file = file;
        try {
            this.canonicalPath = file.getCanonicalPath();
        } catch (IOException ioe) {
            throw new IllegalStateException("Unable to obtain the canonical path for " + file.getAbsolutePath(), ioe);
        }
        this.raf = raf;
        this.fileChannel = raf.getChannel();
        this.chunkSize = OS.mapAlign(chunkSize);
        this.overlapSize = overlapSize > 0 && overlapSize < 64 << 10 ? chunkSize : OS.mapAlign(overlapSize);
        this.capacity = capacity;
        this.readOnly = readOnly;

        Jvm.doNotCloseOnInterrupt(getClass(), this.fileChannel);
    }

    private static void logNewChunk(final String filename,
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
        Jvm.debug().on(MappedFile.class, message);
    }

    @NotNull
    public static MappedFile of(@NotNull final File file,
                                final long chunkSize,
                                final long overlapSize,
                                final boolean readOnly) throws FileNotFoundException {
//        if (readOnly && OS.isWindows()) {
//            Jvm.warn().on(MappedFile.class, "Read only mode not supported on Windows, defaulting to read/write");
//            readOnly = false;
//        }

        @NotNull RandomAccessFile raf = new CleaningRandomAccessFile(file, readOnly ? "r" : "rw");
//        try {
        final long capacity = /*readOnly ? raf.length() : */DEFAULT_CAPACITY;
        return new MappedFile(file, raf, chunkSize, overlapSize, capacity, readOnly);
/*
        } catch (IOException e) {
            Closeable.closeQuietly(raf);
            @NotNull FileNotFoundException fnfe = new FileNotFoundException("Unable to open " + file);
            fnfe.initCause(e);
            throw fnfe;
        }
*/
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final File file, final long chunkSize) throws FileNotFoundException {
        return mappedFile(file, chunkSize, OS.pageSize());
    }

/*
    private void check(Throwable throwable, int[] count) {
        for (int i = 0; i < stores.size(); i++) {
            WeakReference<MappedBytesStore> storeRef = stores.get(i);
            if (storeRef == null)
                continue;
            @Nullable MappedBytesStore mbs = storeRef.get();
            if (mbs != null && mbs.refCount() > 0) {
                mbs.releaseLast();
                throwable.printStackTrace();
                count[0]++;
            }
        }
    }
*/

    @NotNull
    public static MappedFile mappedFile(@NotNull final String filename, final long chunkSize) throws FileNotFoundException {
        return mappedFile(filename, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final String filename,
                                        final long chunkSize,
                                        final long overlapSize) throws FileNotFoundException {
        return mappedFile(new File(filename), chunkSize, overlapSize);
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final File file,
                                        final long chunkSize,
                                        final long overlapSize) throws FileNotFoundException {
        return mappedFile(file, chunkSize, overlapSize, false);
    }

    @NotNull
    public static MappedFile mappedFile(@NotNull final File file,
                                        final long chunkSize,
                                        final long overlapSize,
                                        final boolean readOnly) throws FileNotFoundException {
        return MappedFile.of(file, chunkSize, overlapSize, readOnly);
    }

    @NotNull
    public static MappedFile readOnly(@NotNull final File file) throws FileNotFoundException {
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
                                        final boolean readOnly) throws IOException {
        final RandomAccessFile raf = new CleaningRandomAccessFile(file, readOnly ? "r" : "rw");
        // Windows throws an exception when setting the length when you re-open
        if (raf.length() < capacity)
            raf.setLength(capacity);
        return new MappedFile(file, raf, chunkSize, overlapSize, capacity, readOnly);
    }

    public static void warmup() {
        final List<IOException> errorsDuringWarmup = new ArrayList<>();
        try {
            Jvm.setExceptionHandlers(Slf4jExceptionHandler.FATAL, null, null);

            @NotNull final File file = File.createTempFile("delete_warming_up", "me");
            file.deleteOnExit();
            final long mapAlignment = OS.mapAlignment();
            final int chunks = 64;
            final int compileThreshold = Jvm.compileThreshold();
            for (int j = 0; j <= compileThreshold; j += chunks) {
                try {
                    try (@NotNull RandomAccessFile raf = new CleaningRandomAccessFile(file, "rw")) {
                        try (final MappedFile mappedFile = new MappedFile(file, raf, mapAlignment, 0, mapAlignment * chunks, false)) {
                            warmup0(mapAlignment, chunks, mappedFile);
                        }
                    }
                    Thread.yield();
                } catch (IOException e) {
                    errorsDuringWarmup.add(e);
                }
            }
            Thread.yield();
            Files.delete(file.toPath());
        } catch (IOException e) {
            Jvm.resetExceptionHandlers();
            Jvm.warn().on(MappedFile.class, "Error during warmup", e);
        } finally {
            Jvm.resetExceptionHandlers();
            if (errorsDuringWarmup.size() > 0)
                Jvm.warn().on(MappedFile.class, errorsDuringWarmup.size() + " errors during warmup: " + errorsDuringWarmup);
        }
    }

    private static void warmup0(final long mapAlignment,
                                final int chunks,
                                @NotNull final MappedFile mappedFile) throws IOException {
        ReferenceOwner warmup = ReferenceOwner.temporary("warmup");
        for (int i = 0; i < chunks; i++) {
            mappedFile.acquireBytesForRead(warmup, i * mapAlignment)
                    .release(warmup);
            mappedFile.acquireBytesForWrite(warmup, i * mapAlignment)
                    .release(warmup);
        }
    }

    @NotNull
    public File file() {
        return file;
    }

    /**
     * @throws IllegalStateException if closed.
     */
    @NotNull
    public MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            final long position)
            throws IOException, IllegalArgumentException, IllegalStateException {
        return acquireByteStore(owner, position, null, readOnly ? ReadOnlyMappedBytesStore::new : MappedBytesStore::new);
    }

    @NotNull
    public MappedBytesStore acquireByteStore(
            ReferenceOwner owner,
            final long position,
            BytesStore oldByteStore)
            throws IOException, IllegalArgumentException, IllegalStateException {
        return acquireByteStore(owner, position, oldByteStore, readOnly ? ReadOnlyMappedBytesStore::new : MappedBytesStore::new);
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
        Jvm.safepoint();

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
            //resizeRafIfTooSmall(chunk);

            final long mappedSize = chunkSize + overlapSize;
            final MapMode mode = readOnly ? MapMode.READ_ONLY : MapMode.READ_WRITE;
            final long startOfMap = chunk * chunkSize;

            final long beginNs = System.nanoTime();

            final long address = OS.map(fileChannel, mode, startOfMap, mappedSize);
            final MappedBytesStore mbs2 = mappedBytesStoreFactory.create(owner, this, chunk * this.chunkSize, address, mappedSize, this.chunkSize);
            if (RETAIN)
                mbs2.reserve(this);
            stores.set(chunk, mbs2);

            final long elapsedNs = System.nanoTime() - beginNs;
            if (newChunkListener != null)
                newChunkListener.onNewChunk(file.getPath(), chunk, elapsedNs / 1000);

            if (elapsedNs >= 2_000_000L)
                Jvm.warn().on(getClass(), "Took " + elapsedNs / 1_000_000L + " ms to add mapping for " + file());

            return mbs2;
        }
    }

    private void resizeRafIfTooSmall(final int chunk) throws IOException {
        Jvm.safepoint();

        final long minSize = (chunk + 1L) * chunkSize + overlapSize;
        long size = fileChannel.size();
        Jvm.safepoint();
        if (size >= minSize || readOnly)
            return;

        // handle a possible race condition between processes.
        try {

            // We might have several MappedFile objects that maps to
            // the same underlying file (possibly via hard or soft links)
            // so we use the canonical path as a lock key

            NamedObject namedObject;
            synchronized (FILE_LOCKS) {
                if (++EXPUNGE_COUNTER % EXPUNGE_MODULO == 0) {
                    // Occasionally expunge all stale entries.
                    final Set<String> expiredKeys = FILE_LOCKS.entrySet()
                            .stream()
                            .filter(e -> e.getValue().get() == null)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());
                    expiredKeys.forEach(FILE_LOCKS::remove);
                }
                do {
                    final WeakReference<NamedObject> namedObjectRef = FILE_LOCKS.computeIfAbsent(canonicalPath, k -> new WeakReference<>(new NamedObject(k)));
                    namedObject = namedObjectRef.get();
                    if (namedObject == null)
                        FILE_LOCKS.remove(canonicalPath); // Expunge a stale entry
                } while (namedObject == null);
            }

            // Ensure exclusivity for any and all MappedFile objects handling
            // the same canonical file.
            synchronized (namedObject) {
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
                        Jvm.warn().on(getClass(), "Took " + elapsedNs / 1000L + " us to grow file " + file());
                    }
                }
            }
        } catch (IOException ioe) {
            throw new IOException("Failed to resize to " + minSize, ioe);
        }
    }

    /**
     * Convenience method so you don't need to release the BytesStore
     */
    @NotNull
    public Bytes acquireBytesForRead(ReferenceOwner owner, final long position)
            throws IOException, IllegalStateException, IllegalArgumentException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        final Bytes bytes = mbs.bytesForRead();
        bytes.readPositionUnlimited(position);
        bytes.reserveTransfer(INIT, owner);
        mbs.release(owner);
        return bytes;
    }

    public void acquireBytesForRead(ReferenceOwner owner, final long position, @NotNull final VanillaBytes bytes)
            throws IOException, IllegalStateException, IllegalArgumentException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
    }

    @NotNull
    public Bytes acquireBytesForWrite(ReferenceOwner owner, final long position)
            throws IOException, IllegalStateException, IllegalArgumentException {
        throwExceptionIfClosed();

        @Nullable MappedBytesStore mbs = acquireByteStore(owner, position, null);
        @NotNull Bytes bytes = mbs.bytesForWrite();
        bytes.writePosition(position);
        bytes.reserveTransfer(INIT, owner);
        mbs.release(owner);
        return bytes;
    }

    public void acquireBytesForWrite(ReferenceOwner owner, final long position, @NotNull final VanillaBytes bytes)
            throws IOException, IllegalStateException, IllegalArgumentException {
        throwExceptionIfClosed();

        @Nullable final MappedBytesStore mbs = acquireByteStore(owner, position, null);
        bytes.bytesStore(mbs, position, mbs.capacity() - position);
        bytes.writePosition(position);
    }

    @Override
    protected boolean performReleaseInBackground() {
        // don't perform the close in the background as that just sets a flag. This does the real work.
        return true;
    }

    protected void performRelease() {
        try {
            synchronized (stores) {
                for (int i = 0; i < stores.size(); i++) {
                    final MappedBytesStore mbs = stores.get(i);
                    if (mbs != null && RETAIN) {
                        // this MappedFile is the only referrer to the MappedBytesStore at this point,
                        // so ensure that it is released
                        mbs.release(this);
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

    public NewChunkListener getNewChunkListener() {
        return newChunkListener;
    }

    public void setNewChunkListener(final NewChunkListener listener) {
        throwExceptionIfClosedInSetter();

        this.newChunkListener = listener;
    }

    public long actualSize() throws IORuntimeException {

        boolean interrupted = Thread.interrupted();
        try {
            return fileChannel.size();

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

    @NotNull
    public RandomAccessFile raf() {
        return raf;
    }

    @Override
    protected void finalize() throws Throwable {
        warnAndReleaseIfNotReleased();
        super.finalize();
    }

    @Override
    protected boolean threadSafetyCheck(boolean isUsed) {
        // component is thread safe
        return true;
    }

    private static final class NamedObject {
        private final String name;

        public NamedObject(@NotNull final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "NamedObject{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

}