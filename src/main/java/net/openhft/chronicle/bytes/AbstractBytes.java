/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.ReferenceCounter;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

import static java.lang.Math.min;
import static net.openhft.chronicle.bytes.Accessor.byteArrayAccessor;
import static net.openhft.chronicle.bytes.Accessor.uncheckedByteBufferAccessor;

public abstract class AbstractBytes<Underlying> implements Bytes<Underlying> {
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);

    protected BytesStore<Bytes<Underlying>, Underlying> bytesStore = NoBytesStore.noBytesStore();
    long mark = -1;
    private long position;
    private long limit;
    private UnderflowMode underflowMode;

    public AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
        this.bytesStore = bytesStore;
        bytesStore.reserve();
        clear();
    }

    public Bytes clear() {
        position = start();
        limit = isElastic() ? capacity() : realCapacity();
        return this;
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
    public long position() {
        return position;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        writeCheckOffset(offset, 4);
        return bytesStore.compareAndSwapInt(offset, expected, value);
    }

    @Override
    public long limit() {
        return limit;
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        writeCheckOffset(offset, 8);
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    @Override
    public Bytes position(long position) {
        if (position < start()) throw new BufferUnderflowException();
        if (position > limit())
            throw new BufferOverflowException();
        this.position = position;
        return this;
    }

    @Override
    public Bytes<Underlying> skip(long bytesToSkip) {
        readOffsetPositionMoved(bytesToSkip);
        return this;
    }

    @Override
    public Bytes limit(long limit) {
        if (limit < start()) throw
                new BufferUnderflowException();
        long capacity = capacity();
        if (limit > capacity) {
            assert false : "cant set limit=" + limit + " > " + "capacity=" + capacity;
            throw new BufferOverflowException();
        }
        this.limit = limit;
        return this;
    }

    @Override
    public Bytes<Underlying> flip() {
        limit = position;
        position = start();
        assert limit >= start();
        return this;
    }

    protected void performRelease() {
        this.bytesStore.release();
        this.bytesStore = NoBytesStore.noBytesStore();
    }

    @Override
    public UnderflowMode underflowMode() {
        return underflowMode;
    }

    @Override
    public Bytes<Underlying> underflowMode(UnderflowMode underflowMode) {
        this.underflowMode = underflowMode;
        return this;
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
            return remaining() > 0 ? bytesStore.readUnsignedByte(position) : -1;

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
    public int peakVolatileInt() {
        readCheckOffset(position, 4);
        return bytesStore.readVolatileInt(position);
    }

    @Override
    public void read(byte[] bytes) {
        read(byteArrayAccessor(), bytes, 0, bytes.length);
    }

    private <S, T> void read(Accessor.Full<S, T> accessor, S target, long off, long len) {
        long sourceOffset = accessPositionOffset();
        long size = accessor.size(len);
        skip(size);
        Access.copy(access(), accessHandle(), sourceOffset,
                accessor.access(target), accessor.handle(target), accessor.offset(target, off),
                size);
    }

    @Override
    public void read(ByteBuffer buffer) {
        long read = min(remaining(), buffer.remaining());
        read(uncheckedByteBufferAccessor(buffer), buffer, buffer.position(), read);
        buffer.position((int) (buffer.position() + read));
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

    protected long readOffsetPositionMoved(long adding) {
        long offset = position;
        readCheckOffset(position, adding);
        position += adding;
        assert position <= limit();
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
    public Bytes writeByte(long offset, byte i) {
        writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset, i);
        return this;
    }

    @Override
    public Bytes writeShort(long offset, short i) {
        writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @Override
    public Bytes writeInt(long offset, int i) {
        writeCheckOffset(offset, 4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @Override
    public Bytes writeOrderedInt(long offset, int i) {
        writeCheckOffset(offset, 4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @Override
    public Bytes writeLong(long offset, long i) {
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
    public Bytes writeFloat(long offset, float d) {
        writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset, d);
        return this;
    }

    @Override
    public Bytes writeDouble(long offset, double d) {
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
    public Bytes<Underlying> write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    public Bytes<Underlying> write(long offsetInRDO, Bytes bytes, long offset, long length) {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    protected void writeCheckOffset(long offset, long adding) {
        assert writeCheckOffset0(offset, adding);
    }

    private boolean writeCheckOffset0(long offset, long adding) {
        if (offset < start())
            throw new BufferUnderflowException();
        if (offset + adding > limit()) {
            assert offset + adding <= limit() : "cant add bytes past the limit : limit=" + limit +
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

    protected void readCheckOffset(long offset, long adding) {
        assert readCheckOffset0(offset, adding);
    }

    protected boolean readCheckOffset0(long offset, long adding) {
        if (offset < start())
            throw new BufferUnderflowException();
        long limit0 = limit();
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

    protected long writeOffsetPositionMoved(long adding) {
        long oldPosition = position;
        writeCheckOffset(position, adding);
        position += adding;
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
    public Bytes<Underlying> write(BytesStore bytes) {
        write(bytes, 0, bytes.capacity());
        return this;
    }

    @Override
    public Bytes<Underlying> write(Bytes bytes) {
        long write = min(remaining(), bytes.remaining());
        long offset = bytes.position();
        bytes.skip(write);
        write(bytes, offset, write);
        return this;
    }

    @Override
    public Bytes<Underlying> write(BytesStore bytes, long offset, long length) {
        long targetOffset = writeOffsetPositionMoved(length);
        Access.copy(bytes.access(), bytes.accessHandle(), bytes.accessOffset(offset),
                access(), accessHandle(), accessOffset(targetOffset), length);
        return this;
    }

    @Override
    public Bytes<Underlying> write(Bytes bytes, long offset, long length) {
        long write = min(remaining(), length);
        long targetOffset = writeOffsetPositionMoved(write);
        Access.copy(bytes.access(), bytes.accessHandle(), bytes.accessOffset(offset),
                access(), accessHandle(), accessOffset(targetOffset), write);
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
        bytesStore.write(position, buffer, buffer.position(), buffer.limit());
        position += buffer.remaining();
        assert position <= limit();
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

    private long bufferOverflowOnWrite() {
        throw new BufferOverflowException();
    }

    private long bufferUnderflowOnRead() {
        throw new BufferUnderflowException();
    }

    @Override
    public long address() {
        return bytesStore.address();
    }

    @Override
    public Underlying accessHandle() {
        return bytesStore.accessHandle();
    }

    @Override
    public long accessOffset(long randomOffset) {
        return bytesStore.accessOffset(randomOffset);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bytes)) return false;
        Bytes b2 = (Bytes) obj;
        long remaining = remaining();
        if (b2.remaining() != remaining) return false;
        return Access.equivalent(access(), accessHandle(), accessPositionOffset(),
                b2.access(), b2.accessHandle(), b2.accessPositionOffset(), remaining);
    }

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
    public final Bytes mark() {
        mark = position;
        return this;
    }

    @Override
    public final Bytes reset() {
        long m = mark;
        if (m < 0)
            throw new InvalidMarkException();
        assert position <= limit();
        position = m;
        return this;
    }

    @Override
    public Access<Underlying> access() {
        return bytesStore.access();
    }

    @Override
    public void nativeRead(long address, long size) {
        bytesStore.nativeRead(position(), address, size);
        skip(size);
    }

    @Override
    public void nativeWrite(long address, long size) {
        bytesStore.nativeWrite(address, position(), size);
        skip(size);
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

