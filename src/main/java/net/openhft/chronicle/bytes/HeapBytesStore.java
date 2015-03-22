package net.openhft.chronicle.bytes;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

public class HeapBytesStore<Underlying>
        implements BytesStore<HeapBytesStore<Underlying>, Underlying> {
    private static final AtomicBoolean MEMORY_BARRIER = new AtomicBoolean();
    private final AtomicLong refCount = new AtomicLong(1);
    private final Underlying underlyingObject;
    final Object realUnderlyingObject;
    final int dataOffset, capacity;

    private HeapBytesStore(ByteBuffer byteBuffer) {
        //noinspection unchecked
        this.underlyingObject = (Underlying) byteBuffer;
        this.realUnderlyingObject = byteBuffer.array();
        this.dataOffset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
        this.capacity = byteBuffer.capacity();
    }

    static HeapBytesStore<ByteBuffer> wrap(ByteBuffer bb) {
        return new HeapBytesStore<>(bb);
    }

    @Override
    public void storeFence() {
        MEMORY_BARRIER.lazySet(true);
    }

    @Override
    public void loadFence() {
        MEMORY_BARRIER.get();
    }

    @Override
    public void reserve() {
        refCount.incrementAndGet();
    }

    @Override
    public void release() {
        refCount.decrementAndGet();
    }

    @Override
    public long refCount() {
        return refCount.get();
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        return MEMORY.compareAndSwapInt(realUnderlyingObject, dataOffset + offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        return MEMORY.compareAndSwapLong(
                realUnderlyingObject, dataOffset + offset, expected, value);
    }

    @Override
    public byte readByte(long offset) {
        checkOffset(offset, 1);
        return MEMORY.readByte(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public short readShort(long offset) {
        checkOffset(offset, 2);
        return MEMORY.readShort(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public int readInt(long offset) {
        checkOffset(offset, 4);
        return MEMORY.readInt(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public long readLong(long offset) {
        checkOffset(offset, 8);
        return MEMORY.readLong(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public float readFloat(long offset) {
        checkOffset(offset, 4);
        return MEMORY.readFloat(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public double readDouble(long offset) {
        checkOffset(offset, 8);
        return MEMORY.readDouble(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public HeapBytesStore<Underlying> writeByte(long offset, byte b) {
        checkOffset(offset, 1);
        MEMORY.writeByte(realUnderlyingObject, dataOffset + offset, b);
        return this;
    }

    @Override
    public HeapBytesStore<Underlying> writeShort(long offset, short i16) {
        checkOffset(offset, 2);
        MEMORY.writeShort(realUnderlyingObject, dataOffset + offset, i16);
        return this;
    }

    @Override
    public HeapBytesStore writeInt(long offset, int i32) {
        checkOffset(offset, 4);
        MEMORY.writeInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @Override
    public HeapBytesStore<Underlying> writeLong(long offset, long i64) {
        checkOffset(offset, 8);
        MEMORY.writeLong(realUnderlyingObject, dataOffset + offset, i64);
        return this;
    }

    @Override
    public HeapBytesStore<Underlying> writeOrderedLong(long offset, long i) {
        checkOffset(offset, 8);
        MEMORY.writeOrderedLong(realUnderlyingObject, dataOffset + offset, i);
        return this;
    }

    @Override
    public HeapBytesStore<Underlying> writeFloat(long offset, float f) {
        checkOffset(offset, 4);
        MEMORY.writeFloat(realUnderlyingObject, dataOffset + offset, f);
        return this;
    }

    @Override
    public HeapBytesStore<Underlying> writeDouble(long offset, double d) {
        checkOffset(offset, 8);
        MEMORY.writeDouble(realUnderlyingObject, dataOffset + offset, d);
        return this;
    }

    public void checkOffset(long offset, int size) {
        if (offset < start() || offset + size > capacity) {
            throw new BufferOverflowException();
        }
    }

    @Override
    public HeapBytesStore<Underlying> writeOrderedInt(long offset, int i32) {
        checkOffset(offset, 4);
        MEMORY.writeOrderedInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @Override
    public HeapBytesStore<Underlying> write(
            long offsetInRDO, byte[] bytes, int offset, int length) {
        checkOffset(offset, length);
        MEMORY.copyMemory(
                bytes, offset, realUnderlyingObject, this.dataOffset + offsetInRDO, length);
        return this;
    }

    @Override
    public HeapBytesStore<Underlying> write(
            long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        checkOffset(offset, length);
        if (bytes.isDirect()) {
            MEMORY.copyMemory(((DirectBuffer) bytes).address(), realUnderlyingObject,
                    this.dataOffset + offsetInRDO, length);
        } else {
            MEMORY.copyMemory(bytes.array(), offset, realUnderlyingObject,
                    this.dataOffset + offsetInRDO, length);
        }
        return this;
    }

    @Override
    public HeapBytesStore<Underlying> write(long offsetInRDO, Bytes bytes, long offset, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long address() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Underlying underlyingObject() {
        return underlyingObject;
    }

    @Override
    public boolean isNative() {
        return false;
    }
}
