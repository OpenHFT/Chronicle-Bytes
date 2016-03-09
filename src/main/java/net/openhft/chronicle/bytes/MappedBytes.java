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
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Bytes to wrap memory mapped data.
 */
public class MappedBytes extends AbstractBytes<Void> {
    public static boolean CHECKING = true;
    private final MappedFile mappedFile;

    // assume the mapped file is reserved already.
    protected MappedBytes(MappedFile mappedFile) throws IllegalStateException {
        super(NoBytesStore.noBytesStore(), NoBytesStore.noBytesStore().writePosition(), NoBytesStore.noBytesStore().writeLimit());
        this.mappedFile = mappedFile;
        clear();
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull String filename, long chunkSize) throws FileNotFoundException, IllegalStateException {
        return mappedBytes(new File(filename), chunkSize);
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull File file, long chunkSize) throws FileNotFoundException, IllegalStateException {
        return mappedBytes(file, chunkSize, OS.pageSize());
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull File file, long chunkSize, long overlapSize) throws FileNotFoundException, IllegalStateException {
        MappedFile rw = MappedFile.of(file, chunkSize, overlapSize);
        return mappedBytes(rw);
    }

    @NotNull
    public static MappedBytes mappedBytes(MappedFile rw) {
        return CHECKING ? new CheckingMappedBytes(rw) : new MappedBytes(rw);
    }

    public void setNewChunkListener(NewChunkListener listener) {
        mappedFile.setNewChunkListener(listener);
    }

    public MappedFile mappedFile() {
        return mappedFile;
    }

    public MappedBytes withSizes(long chunkSize, long overlapSize) {
        MappedFile mappedFile2 = this.mappedFile.withSizes(chunkSize, overlapSize);
        if (mappedFile2 == this.mappedFile)
            return this;
        try {
            return mappedBytes(mappedFile2);
        } finally {
            release();
        }
    }

    @Override
    public BytesStore<Bytes<Void>, Void> copy() {
        return NativeBytes.copyOf(this);
    }

    @Override
    public long capacity() {
        return mappedFile == null ? 0L : mappedFile.capacity();
    }

    @Override
    public long realCapacity() {
        return mappedFile.actualSize();
    }

    @Override
    public long refCount() {
        return Math.max(super.refCount(), mappedFile.refCount());
    }

    @Override
    protected void readCheckOffset(long offset, long adding, boolean given) throws BufferUnderflowException, IORuntimeException {
        if (!bytesStore.inside(offset)) {
            BytesStore oldBS = bytesStore;
            try {
                bytesStore = (BytesStore) mappedFile.acquireByteStore(offset);
                oldBS.release();
            } catch (IOException | IllegalStateException e) {
                throw new IORuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new BufferUnderflowException();
            }
        }
    }

    @Override
    protected void writeCheckOffset(long offset, long adding) throws BufferOverflowException, IORuntimeException {
        if (offset < 0 || offset >= capacity())
            throw new IllegalArgumentException("Offset out of bound " + offset);
        if (!bytesStore.inside(offset)) {
            BytesStore oldBS = bytesStore;
            try {
                bytesStore = (BytesStore) mappedFile.acquireByteStore(offset);
                oldBS.release();
            } catch (IOException | IllegalStateException e) {
                throw new IORuntimeException(e);
            } catch (IllegalArgumentException e) {
                throw new BufferOverflowException();
            }
        }
    }// 07721192269

    @Override
    public long start() {
        return 0L;
    }

    @Override
    protected void performRelease() {
        super.performRelease();
        mappedFile.close();
    }

    @Override
    public boolean isElastic() {
        return true;
    }
}
