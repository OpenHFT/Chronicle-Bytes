/*
 * Copyright (c) 2016-2022 chronicle.software
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

import net.openhft.chronicle.bytes.internal.ChunkedMappedBytes;
import net.openhft.chronicle.bytes.internal.SingleMappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.ManagedCloseable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Bytes to wrap memory mapped data.
 * <p>
 * Memory is grouped in chunks of 64 MB by default.
 * The chunk size can be increased greatly if the OS.isSparseFileSupported()
 * e.g. blockSize(512 << 30)
 * <p>
 * The chunk containing the most recent accessed memory is reserved and the previous chunk is released.
 * For random access, you can reserve a chunk by obtaining the bytesStore() and using reserve(owner) on it,
 * remembering to release(owner) the same BytesStore before closing the file.
 * <p>
 * NOTE These MappedBytes are single threaded as are all Bytes.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class MappedBytes extends AbstractBytes<Void> implements Closeable, ManagedCloseable {

    protected static final boolean TRACE = Jvm.getBoolean("trace.mapped.bytes");

    // assume the mapped file is reserved already.
    protected MappedBytes()
            throws IllegalStateException {
        this("");
    }

    protected MappedBytes(final String name)
            throws IllegalStateException {
        super(BytesStore.empty(),
                BytesStore.empty().writePosition(),
                BytesStore.empty().writeLimit(),
                name);
    }

    @NotNull
    public static MappedBytes singleMappedBytes(@NotNull final String filename, @NonNegative final long capacity)
            throws FileNotFoundException, IllegalStateException {
        return singleMappedBytes(new File(filename), capacity);
    }

    @NotNull
    public static MappedBytes singleMappedBytes(@NotNull final File file, @NonNegative final long capacity)
            throws FileNotFoundException, IllegalStateException {
        return singleMappedBytes(file, capacity, false);
    }

    @NotNull
    public static MappedBytes singleMappedBytes(@NotNull File file, @NonNegative long capacity, boolean readOnly)
            throws FileNotFoundException, IllegalStateException {
        final MappedFile rw = MappedFile.ofSingle(file, capacity, readOnly);
        try {
            return new SingleMappedBytes(rw);
        } finally {
            rw.release(INIT);
        }
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final String filename, @NonNegative final long chunkSize)
            throws FileNotFoundException, IllegalStateException {
        return mappedBytes(new File(filename), chunkSize);
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file, @NonNegative final long chunkSize)
            throws FileNotFoundException, IllegalStateException {
        return mappedBytes(file, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file, @NonNegative final long chunkSize, @NonNegative final long overlapSize)
            throws FileNotFoundException, IllegalStateException {
        final MappedFile rw = MappedFile.of(file, chunkSize, overlapSize, false);
        try {
            return mappedBytes(rw);
        } finally {
            rw.release(INIT);
        }
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final File file,
                                          @NonNegative final long chunkSize,
                                          @NonNegative final long overlapSize,
                                          final boolean readOnly)
            throws FileNotFoundException,
            IllegalStateException {
        final MappedFile rw = MappedFile.of(file, chunkSize, overlapSize, readOnly);
        try {
            return mappedBytes(rw);
        } finally {
            rw.release(INIT);
        }
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull final MappedFile rw)
            throws IllegalStateException {
        return rw.createBytesFor();
    }

    @NotNull
    public static MappedBytes readOnly(@NotNull final File file)
            throws FileNotFoundException {
        final MappedFile mappedFile = MappedFile.readOnly(file);
        try {
            try {
                return new ChunkedMappedBytes(mappedFile);
            } finally {
                mappedFile.release(INIT);
            }
        } catch (IllegalStateException e) {
            throw new AssertionError(e);
        }
    }

    public abstract boolean isBackingFileReadOnly();

    @Override
    public boolean sharedMemory() {
        return true;
    }

    @Deprecated(/* to be removed in x.25 */)
    public abstract MappedBytes disableThreadSafetyCheck(boolean disableThreadSafetyCheck);

    public abstract void chunkCount(long[] chunkCount);

    public abstract MappedFile mappedFile();

}
