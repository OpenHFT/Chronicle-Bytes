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
import net.openhft.chronicle.core.annotation.ForceInline;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public abstract class AbstractBytes<Underlying> implements Bytes<Underlying> {
    protected BytesStore<Bytes<Underlying>, Underlying> bytesStore;
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);
    protected long readPosition;
    protected long writePosition;
    protected long writeLimit;

    AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore, long writePosition, long writeLimit) {
        this.bytesStore = bytesStore;
        bytesStore.reserve();
        readPosition = bytesStore.readPosition();
        this.writePosition = writePosition;
        this.writeLimit = writeLimit;
    }

    public Bytes<Underlying> clear() {
        readPosition = writePosition = start();
        writeLimit = capacity();
        return this;
    }

    @Override
    @ForceInline
    public long readLimit() {
        return writePosition;
    }

    @ForceInline
    public long writeLimit() {
        return writeLimit;
    }

    @Override
    @ForceInline
    public long realCapacity() {
        return bytesStore.capacity();
    }

    @Override
    @ForceInline
    public long capacity() {
        return bytesStore.capacity();
    }

    @Override
    public Underlying underlyingObject() {
        return bytesStore.underlyingObject();
    }

    @Override
    @ForceInline
    public long start() {
        return bytesStore.start();
    }

    @Override
    @ForceInline
    public long readPosition() {
        return readPosition;
    }

    @Override
    @ForceInline
    public long writePosition() {
        return writePosition;
    }

    @Override
    @ForceInline
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        writeCheckOffset(offset, 4);
        return bytesStore.compareAndSwapInt(offset, expected, value);
    }

    @Override
    @ForceInline
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        writeCheckOffset(offset, 8);
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    @Override
    @ForceInline
    public Bytes<Underlying> readPosition(long position) {
        if (position < start()) throw new BufferUnderflowException();
        if (position > readLimit())
            throw new BufferOverflowException();
        this.readPosition = position;
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> readLimit(long limit) {
        if (limit < start()) throw new BufferUnderflowException();
        if (limit > writeLimit())
            throw new BufferOverflowException();
        this.writePosition = limit;
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writePosition(long position) {
        if (position < readPosition())
            throw new BufferUnderflowException();
        if (position > writeLimit())
            throw new BufferOverflowException();
        this.writePosition = position;
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> readSkip(long bytesToSkip) {
        readOffsetPositionMoved(bytesToSkip);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeSkip(long bytesToSkip) {
        writeOffsetPositionMoved(bytesToSkip);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeLimit(long limit) {
        if (limit < start())
            throw                new BufferUnderflowException();
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
    @ForceInline
    public byte readByte() {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readByte(offset);

        } catch (BufferOverflowException e) {
            return 0;
        }
    }

    @Override
    @ForceInline
    public int peekUnsignedByte() {
        try {
            return readRemaining() > 0 ? bytesStore.readUnsignedByte(readPosition) : -1;

        } catch (BufferOverflowException e) {
            return -1;
        }
    }

    @Override
    @ForceInline
    public short readShort() {
        long offset = readOffsetPositionMoved(2);
        return bytesStore.readShort(offset);
    }

    @Override
    @ForceInline
    public int readInt() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readInt(offset);
    }

    @Override
    @ForceInline
    public long readLong() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readLong(offset);
    }

    @Override
    @ForceInline
    public float readFloat() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readFloat(offset);
    }

    @Override
    @ForceInline
    public double readDouble() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readDouble(offset);
    }

    @Override
    @ForceInline
    public int readVolatileInt() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    @ForceInline
    public long readVolatileLong() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readVolatileLong(offset);
    }

    protected long readOffsetPositionMoved(long adding) {
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
    @ForceInline
    public Bytes<Underlying> writeByte(long offset, byte i) {
        writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset, i);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeShort(long offset, short i) {
        writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeInt(long offset, int i) {
        writeCheckOffset(offset, 4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedInt(long offset, int i) {
        writeCheckOffset(offset, 4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeLong(long offset, long i) {
        writeCheckOffset(offset, 8);
        bytesStore.writeLong(offset, i);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedLong(long offset, long i) {
        writeCheckOffset(offset, 8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeFloat(long offset, float d) {
        writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset, d);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeDouble(long offset, double d) {
        writeCheckOffset(offset, 8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> write(long offsetInRDO, byte[] bytes, int offset, int length) {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    @ForceInline
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
    }

    @Override
    @ForceInline
    public Bytes<Underlying> write(long offsetInRDO,
                                   RandomDataInput bytes, long offset, long length) {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @ForceInline
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
    @ForceInline
    public byte readByte(long offset) {
        readCheckOffset(offset, 1);
        return bytesStore.readByte(offset);
    }

    @Override
    @ForceInline
    public short readShort(long offset) {
        readCheckOffset(offset, 2);
        return bytesStore.readShort(offset);
    }

    @Override
    @ForceInline
    public int readInt(long offset) {
        readCheckOffset(offset, 4);
        return bytesStore.readInt(offset);
    }

    @Override
    @ForceInline
    public long readLong(long offset) {
        readCheckOffset(offset, 8);
        return bytesStore.readLong(offset);
    }

    @Override
    @ForceInline
    public float readFloat(long offset) {
        readCheckOffset(offset, 4);
        return bytesStore.readFloat(offset);
    }

    @Override
    @ForceInline
    public double readDouble(long offset) {
        readCheckOffset(offset, 8);
        return bytesStore.readDouble(offset);
    }

    @ForceInline
    void readCheckOffset(long offset, long adding) {
        assert readCheckOffset0(offset, adding);
    }

    @ForceInline
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
    @ForceInline
    public Bytes<Underlying> writeByte(byte i8) {
        long offset = writeOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    protected long writeOffsetPositionMoved(long adding) {
        long oldPosition = writePosition;
        writeCheckOffset(writePosition, adding);
        writePosition += adding;
        return oldPosition;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeShort(short i16) {
        long offset = writeOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeInt(int i) {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeLong(long i64) {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeFloat(float f) {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeFloat(offset, f);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeDouble(double d) {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> write(byte[] bytes, int offset, int length) {
        long offsetInRDO = writeOffsetPositionMoved(length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> write(ByteBuffer buffer) {
        bytesStore.write(writePosition, buffer, buffer.position(), buffer.limit());
        writePosition += buffer.remaining();
        assert writePosition <= writeLimit();
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedInt(int i) {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedLong(long i) {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @Override
    public long address(long offset) throws UnsupportedOperationException {
        return bytesStore.address(offset);
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
        return b2.readRemaining() == remaining && equalsBytes(b2, remaining);
    }

    public boolean equalsBytes(Bytes b2, long remaining) {
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
    @ForceInline
    public void nativeRead(long address, long size) {
        bytesStore.nativeRead(readPosition(), address, size);
        readSkip(size);
    }

    @Override
    @ForceInline
    public void nativeWrite(long address, long size) {
        bytesStore.nativeWrite(address, writePosition(), size);
        writeSkip(size);
    }

    @Override
    @ForceInline
    public void nativeRead(long position, long address, long size) {
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    @ForceInline
    public void nativeWrite(long address, long position, long size) {
        bytesStore.nativeWrite(address, position, size);
    }

    @Override
    public BytesStore bytesStore() {
        return bytesStore;
    }
}

