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
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;

/**
 * Fast unchecked version of AbstractBytes
 */
public class UncheckedBytes<Underlying> extends AbstractBytes<Underlying> {
    public UncheckedBytes(@NotNull Bytes underlyingBytes) throws IllegalStateException {
        super(underlyingBytes.bytesStore(), underlyingBytes.writePosition(), underlyingBytes.writeLimit());
        readPosition(underlyingBytes.readPosition());
    }

    @NotNull
    public Bytes<Underlying> unchecked(boolean unchecked) {
        return this;
    }

    @Override
    void writeCheckOffset(long offset, long adding) {
    }

    @Override
    void readCheckOffset(long offset, long adding) {
    }

    @NotNull
    @Override
    public Bytes<Underlying> readPosition(long position) {
        readPosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readLimit(long limit) {
        writePosition = limit;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writePosition(long position) {
        writePosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readSkip(long bytesToSkip) {
        readPosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeSkip(long bytesToSkip) {
        writePosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLimit(long limit) {
        writeLimit = limit;
        return this;
    }

    @NotNull
    @Override
    public BytesStore<Bytes<Underlying>, Underlying> copy() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean isElastic() {
        return false;
    }

    @Override
    protected long readOffsetPositionMoved(long adding) {
        long offset = readPosition;
        readPosition += adding;
        return offset;
    }

    @Override
    protected long writeOffsetPositionMoved(long adding) {
        long oldPosition = writePosition;
        writePosition += adding;
        return oldPosition;
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull BytesStore bytes, long offset, long length)
            throws IORuntimeException, BufferOverflowException, IllegalArgumentException {
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else if (bytes.underlyingObject() == null && length >= 32) {
            rawCopy(bytes, offset, length);

        } else {
            super.write(bytes, offset, length);
        }
        return this;
    }

    public void rawCopy(@NotNull BytesStore bytes, long offset, long length)
            throws IORuntimeException, BufferOverflowException, IllegalArgumentException {
        long len = Math.min(writeRemaining(), Math.min(bytes.readRemaining(), length));
        if (len > 0) {
            writeCheckOffset(writePosition(), len);
            OS.memory().copyMemory(bytes.address(offset), address(writePosition()), len);
            writeSkip(len);
        }
    }
}
