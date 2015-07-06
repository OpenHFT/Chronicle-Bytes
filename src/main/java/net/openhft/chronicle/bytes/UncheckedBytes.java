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

/**
 * Fast unchecked version of AbstractBytes
 */
public class UncheckedBytes<Underlying> extends AbstractBytes<Underlying> {
    public UncheckedBytes(Bytes underlyingBytes) {
        super(underlyingBytes.bytesStore(), underlyingBytes.writePosition(), underlyingBytes.writeLimit());
        readPosition(underlyingBytes.readPosition());
    }

    public Bytes<Underlying> unchecked(boolean unchecked) {
        return this;
    }


    @Override
    void writeCheckOffset(long offset, long adding) {
    }

    @Override
    void readCheckOffset(long offset, long adding) {
    }

    @Override
    public Bytes<Underlying> readPosition(long position) {
        readPosition = position;
        return this;
    }

    @Override
    public Bytes<Underlying> readLimit(long limit) {
        writePosition = limit;
        return this;
    }

    @Override
    public Bytes<Underlying> writePosition(long position) {
        writePosition = position;
        return this;
    }

    @Override
    public Bytes<Underlying> readSkip(long bytesToSkip) {
        readPosition += bytesToSkip;
        return this;
    }

    @Override
    public Bytes<Underlying> writeSkip(long bytesToSkip) {
        writePosition += bytesToSkip;
        return this;
    }

    @Override
    public Bytes<Underlying> writeLimit(long limit) {
        writeLimit = limit;
        return this;
    }

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

    @Override
    public Bytes<Underlying> write(BytesStore bytes, long offset, long length) {
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else if (bytes.underlyingObject() == null && length >= 64) {
            rawCopy(bytes, offset, length);

        } else {
            super.write(bytes, offset, length);
        }
        return this;
    }

    public void rawCopy(BytesStore bytes, long offset, long length) {
        long len = Math.min(writeRemaining(), Math.min(bytes.readRemaining(), length));
        if (len > 0) {
            writeCheckOffset(writePosition(), len);
            OS.memory().copyMemory(bytes.address(offset), address(writePosition()), len);
            writeSkip(len);
        }
    }
}
