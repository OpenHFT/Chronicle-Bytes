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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MappedBytes extends AbstractBytes<Void> {
    private final MappedFile mappedFile;

    // assume the mapped file is reserved already.
    MappedBytes(MappedFile mappedFile) {
        super(NoBytesStore.noBytesStore(), NoBytesStore.noBytesStore().writePosition(), NoBytesStore.noBytesStore().writeLimit());
        this.mappedFile = mappedFile;
        clear();
    }

    public static MappedBytes mappedBytes(String filename, long chunkSize) throws FileNotFoundException {
        return mappedBytes(new File(filename), chunkSize);
    }

    public static MappedBytes mappedBytes(File file, long chunkSize) throws FileNotFoundException {
        MappedFile rw = new MappedFile(file, chunkSize, OS.pageSize());
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
    protected void readCheckOffset(long offset, long adding) {
        checkOffset(offset);
    }

    @Override
    protected void writeCheckOffset(long offset, long adding) {
        checkOffset(offset);
    }

    private void checkOffset(long offset) {
        if (!bytesStore.inside(offset)) {
            BytesStore oldBS = bytesStore;
            try {
                bytesStore = mappedFile.acquireByteStore(offset);
                oldBS.release();
            } catch (IOException e) {
                throw new IORuntimeException(e);
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

    @Override
    public Bytes<Void> write(BytesStore buffer, long offset, long length) {
        throw new UnsupportedOperationException("todo");
    }
}
