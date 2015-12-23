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
    private final MappedFile mappedFile;

    // assume the mapped file is reserved already.
    public MappedBytes(MappedFile mappedFile) throws IllegalStateException {
        super(NoBytesStore.noBytesStore(), NoBytesStore.noBytesStore().writePosition(), NoBytesStore.noBytesStore().writeLimit());
        this.mappedFile = mappedFile;
        clear();
    }

    public MappedFile mappedFile() {
        return mappedFile;
    }

    public MappedBytes withSizes(long chunkSize, long overlapSize) {
        MappedFile mappedFile2 = this.mappedFile.withSizes(chunkSize, overlapSize);
        if (mappedFile2 == this.mappedFile)
            return this;
        try {
            return new MappedBytes(mappedFile2);
        } finally {
            release();
        }
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull String filename, long chunkSize) throws FileNotFoundException, IllegalStateException {
        return mappedBytes(new File(filename), chunkSize);
    }

    @NotNull
    public static MappedBytes mappedBytes(@NotNull File file, long chunkSize) throws FileNotFoundException, IllegalStateException {
        MappedFile rw = MappedFile.of(file, chunkSize, OS.pageSize());
        return new MappedBytes(rw);
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
    public long refCount() {
        return Math.max(super.refCount(), mappedFile.refCount());
    }

    @Override
    protected void readCheckOffset(long offset, long adding) throws BufferUnderflowException, IORuntimeException {
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
    }

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
