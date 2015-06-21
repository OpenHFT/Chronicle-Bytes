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

import net.openhft.chronicle.core.ReferenceCounter;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public abstract class AbstractBytes<Underlying> implements Bytes<Underlying> {
    BytesStore<Bytes<Underlying>, Underlying> bytesStore = NoBytesStore.noBytesStore();
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);
    private long readPosition;
    private long writePosition;
    private long writeLimit;

    AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
        this.bytesStore = bytesStore;
        bytesStore.reserve();
        clear();
    }

    public Bytes<Underlying> clear() {
        readPosition = writePosition = start();
        writeLimit = capacity();
        return this;
    }

    @Override
    public long readLimit() {
        return writePosition;
    }

    public long writeLimit() {
        return writeLimit;
    }

    @Override
    public long realCapacity() {
        return bytesStore.capacity();
    }

    @Override
    public long capacity() {
        return bytesStore.capacity();
    }

    @Override
    public Underlying underlyingObject() {
        return bytesStore.underlyingObject();
    }

    @Override
    public long start() {
        return bytesStore.start();
    }

    public Bytes<Underlying> zeroOut(long start, long end) {
        bytesStore.zeroOut(start, end);
        return this;
    }

    @Override
    public long readPosition() {
        return readPosition;
    }

    @Override
    public long writePosition() {
        return writePosition;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        writeCheckOffset(offset, 4);
        return bytesStore.compareAndSwapInt(offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        writeCheckOffset(offset, 8);
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    @Override
    public Bytes<Underlying> readPosition(long position) {
        if (position < start()) throw new BufferUnderflowException();
        if (position > readLimit())
            throw new BufferOverflowException();
        this.readPosition = position;
        return this;
    }

    @Override
    public Bytes<Underlying> readLimit(long limit) {
        if (limit < start()) throw new BufferUnderflowException();
        if (limit > writeLimit())
            throw new BufferOverflowException();
        this.writePosition = limit;
        return this;
    }

    @Override
    public Bytes<Underlying> writePosition(long position) {
        if (position < readPosition()) throw new BufferUnderflowException();
        if (position > writeLimit())
            throw new BufferOverflowException();
        this.writePosition = position;
        return this;
    }

    @Override
    public Bytes<Underlying> readSkip(long bytesToSkip) {
        readOffsetPositionMoved(bytesToSkip);
        return this;
    }

    @Override
    public Bytes<Underlying> writeSkip(long bytesToSkip) {
        writeOffsetPositionMoved(bytesToSkip);
        return this;
    }

    @Override
    public Bytes<Underlying> writeLimit(long limit) {
        if (limit < start()) throw
                new BufferUnderflowException();
        long capacity = capacity();
        if (limit > capacity) {
            assert false : "cant set limit=" + limit + " > " + "capacity=" + capacity;
            throw new BufferOverflowException();
        }
        this.writeLimit = limit;
        return this;
    }

    void performRelease() {
        this.bytesStore.release();
        this.bytesStore = NoBytesStore.noBytesStore();
    }

    @Override
    public byte readByte() {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readByte(offset);

        } catch (BufferOverflowException e) {
            return 0;
        }
    }

    @Override
    public int peekUnsignedByte() {
        try {
            return readRemaining() > 0 ? bytesStore.readUnsignedByte(readPosition) : -1;

        } catch (BufferOverflowException e) {
            return -1;
        }
    }

    @Override
    public short readShort() {
        long offset = readOffsetPositionMoved(2);
        return bytesStore.readShort(offset);
    }

    @Override
    public int readInt() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readInt(offset);
    }

    @Override
    public long readLong() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readLong(offset);
    }

    @Override
    public float readFloat() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readFloat(offset);
    }

    @Override
    public double readDouble() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readDouble(offset);
    }

    @Override
    public int readVolatileInt() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readVolatileLong(offset);
    }

    private long readOffsetPositionMoved(long adding) {
        long offset = readPosition;
        readCheckOffset(readPosition, adding);
        readPosition += adding;
        assert readPosition <= readLimit();
        return offset;
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

    @Override
    public Bytes<Underlying> writeByte(long offset, byte i) {
        writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> writeShort(long offset, short i) {
        writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> writeInt(long offset, int i) {
        writeCheckOffset(offset, 4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> writeOrderedInt(long offset, int i) {
        writeCheckOffset(offset, 4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> writeLong(long offset, long i) {
        writeCheckOffset(offset, 8);
        bytesStore.writeLong(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> writeOrderedLong(long offset, long i) {
        writeCheckOffset(offset, 8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> writeFloat(long offset, float d) {
        writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset, d);
        return this;
    }

    @Override
    public Bytes<Underlying> writeDouble(long offset, double d) {
        writeCheckOffset(offset, 8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @Override
    public Bytes<Underlying> write(long offsetInRDO, byte[] bytes, int offset, int length) {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
    }

    @Override
    public Bytes<Underlying> write(long offsetInRDO, Bytes bytes, long offset, long length) {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    void writeCheckOffset(long offset, long adding) {
        assert writeCheckOffset0(offset, adding);
    }

    private boolean writeCheckOffset0(long offset, long adding) {
        if (offset < start())
            throw new BufferUnderflowException();
        if (offset + adding > writeLimit()) {
            assert offset + adding <= writeLimit() : "cant add bytes past the limit : limit=" + writeLimit() +
                    ",offset=" +
                    offset +
                    ",adding=" + adding;
            throw new BufferOverflowException();
        }
        return true;
    }

    @Override
    public byte readByte(long offset) {
        readCheckOffset(offset, 1);
        return bytesStore.readByte(offset);
    }

    @Override
    public short readShort(long offset) {
        readCheckOffset(offset, 2);
        return bytesStore.readShort(offset);
    }

    @Override
    public int readInt(long offset) {
        readCheckOffset(offset, 4);
        return bytesStore.readInt(offset);
    }

    @Override
    public long readLong(long offset) {
        readCheckOffset(offset, 8);
        return bytesStore.readLong(offset);
    }

    @Override
    public float readFloat(long offset) {
        readCheckOffset(offset, 4);
        return bytesStore.readFloat(offset);
    }

    @Override
    public double readDouble(long offset) {
        readCheckOffset(offset, 8);
        return bytesStore.readDouble(offset);
    }

    void readCheckOffset(long offset, long adding) {
        assert readCheckOffset0(offset, adding);
    }

    private boolean readCheckOffset0(long offset, long adding) {
        if (offset < start())
            throw new BufferUnderflowException();
        long limit0 = readLimit();
        if (offset + adding > limit0) {
//          assert false : "can't read bytes past the limit : limit=" + limit0 + ",offset=" +
            //                  offset +
            //                ",adding=" + adding;
            throw new BufferUnderflowException();
        }
        return true;
    }

    @Override
    public Bytes<Underlying> writeByte(byte i8) {
        long offset = writeOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    private long writeOffsetPositionMoved(long adding) {
        long oldPosition = writePosition;
        writeCheckOffset(writePosition, adding);
        writePosition += adding;
        return oldPosition;
    }

    @Override
    public Bytes<Underlying> writeShort(short i16) {
        long offset = writeOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @Override
    public Bytes<Underlying> writeInt(int i) {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> writeLong(long i64) {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @Override
    public Bytes<Underlying> writeFloat(float f) {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeFloat(offset, f);
        return this;
    }

    @Override
    public Bytes<Underlying> writeDouble(double d) {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @Override
    public Bytes<Underlying> write(byte[] bytes, int offset, int length) {
        long offsetInRDO = writeOffsetPositionMoved(length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    public Bytes<Underlying> write(ByteBuffer buffer) {
        bytesStore.write(writePosition, buffer, buffer.position(), buffer.limit());
        writePosition += buffer.remaining();
        assert writePosition <= writeLimit();
        return this;
    }

    @Override
    public Bytes<Underlying> writeOrderedInt(int i) {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> writeOrderedLong(long i) {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @Override
    public long address() {
        return bytesStore.address();
    }

    @Override
    public long accessOffset(long randomOffset) {
        return bytesStore.accessOffset(randomOffset);
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bytes)) return false;
        Bytes b2 = (Bytes) obj;
        long remaining = readRemaining();
        if (b2.readRemaining() != remaining) return false;
        long i = 0;
        for (; i < remaining - 7; i++)
            if (readLong(readPosition() + i) != b2.readLong(b2.readPosition() + i))
                return false;
        for (; i < remaining; i++)
            if (readByte(readPosition() + i) != b2.readByte(b2.readPosition() + i))
                return false;
        return true;
    }

    @NotNull
    @Override
    public String toString() {
        return BytesUtil.toString(this);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        refCount.releaseAll();
    }

    @Override
    public void nativeRead(long address, long size) {
        bytesStore.nativeRead(readPosition(), address, size);
        readSkip(size);
    }

    @Override
    public void nativeWrite(long address, long size) {
        bytesStore.nativeWrite(address, writePosition(), size);
        writeSkip(size);
    }

    @Override
    public void nativeRead(long position, long address, long size) {
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        bytesStore.nativeWrite(address, position, size);
    }
}

