package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.ReferenceCounter;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractBytes<Underlying> implements Bytes<Underlying> {
    private static final AtomicBoolean MEMORY_BARRIER = new AtomicBoolean();
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);

    protected BytesStore<Bytes<Underlying>, Underlying> bytesStore = NoBytesStore.noBytesStore();

    private long position, limit;

    public AbstractBytes(BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
        this.bytesStore = bytesStore;
        bytesStore.reserve();
        clear();
    }

    @Override
    public long realCapacity() {
        return bytesStore.capacity();
    }

    protected void performRelease() {
        this.bytesStore.release();
        this.bytesStore = NoBytesStore.noBytesStore();
    }

    @Override
    public void storeFence() {
        MEMORY_BARRIER.lazySet(true);
    }

    @Override
    public void loadFence() {
        MEMORY_BARRIER.get();
    }

    public Bytes clear() {
        position = start();
        limit = capacity();
        return this;
    }

    @Override
    public long start() {
        return bytesStore.start();
    }

    @Override
    public long capacity() {
        return bytesStore.capacity();
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public long limit() {
        return limit;
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
    public Bytes limit(long limit) {
        if (limit < start()) throw new BufferUnderflowException();
        if (limit > capacity())
            throw new BufferOverflowException();
        this.limit = limit;
        return this;
    }

    @Override
    public UnderflowMode underflowMode() {
        return UnderflowMode.BOUNDED;
    }

    @Override
    public long refCount() {
        return refCount.get();
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
    public Bytes writeByte(long offset, byte i) {
        long offset2 = writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset2, i);
        return this;
    }

    @Override
    public byte readByte(long offset) {
        long offset2 = readCheckOffset(offset, 1);
        return bytesStore.readByte(offset2);
    }

    @Override
    public Bytes writeShort(long offset, short i) {
        long offset2 = writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset2, i);
        return this;
    }

    @Override
    public short readShort(long offset) {
        long offset2 = readCheckOffset(offset, 2);
        return bytesStore.readShort(offset2);
    }

    @Override
    public Bytes writeInt(long offset, int i) {
        bytesStore.writeInt(writeCheckOffset(offset, 4), i);
        return this;
    }

    @Override
    public Bytes writeOrderedInt(long offset, int i) {
        bytesStore.writeOrderedInt(writeCheckOffset(offset, 4), i);
        return this;
    }

    @Override
    public int readInt(long offset) {
        long offset2 = readCheckOffset(offset, 4);
        return bytesStore.readInt(offset2);
    }

    @Override
    public Bytes writeLong(long offset, long i) {
        long offset2 = writeCheckOffset(offset, 8);
        bytesStore.writeLong(offset2, i);
        return this;
    }

    @Override
    public long readLong(long offset) {
        long offset2 = readCheckOffset(offset, 8);
        return bytesStore.readLong(offset2);
    }

    @Override
    public float readFloat(long offset) {
        long offset2 = readCheckOffset(offset, 4);
        return bytesStore.readFloat(offset2);
    }

    @Override
    public double readDouble(long offset) {
        long offset2 = readCheckOffset(offset, 8);
        return bytesStore.readDouble(offset2);
    }

    @Override
    public Bytes writeFloat(long offset, float d) {
        long offset2 = writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset2, d);
        return this;
    }

    @Override
    public Bytes writeDouble(long offset, double d) {
        long offset2 = writeCheckOffset(offset, 8);
        bytesStore.writeDouble(offset2, d);
        return this;
    }

    @Override
    public byte readByte() {
        long offset = readOffsetPositionMoved(1);
        return bytesStore.readByte(offset);
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
        long offset = readCheckOffset(position, 4);
        return bytesStore.readVolatileInt(offset);
    }

    protected long writeOffsetPositionMoved(int adding) {
        long offset = writeCheckOffset(position, adding);
        position += adding;
        return offset;
    }

    protected long readOffsetPositionMoved(int adding) {
        long offset = readCheckOffset(position, adding);
        position += adding;
        return offset;
    }

    @Override
    public Bytes<Underlying> writeByte(byte i8) {
        long offset = writeOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    private long bufferOverflowOnWrite() {
        throw new BufferOverflowException();
    }

    private long bufferUnderflowOnRead() {
        throw new BufferUnderflowException();
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
    public Bytes<Underlying> write(Bytes bytes) {
        while (bytes.remaining() > 7 && remaining() > 7)
            writeLong(bytes.readLong());
        while (bytes.remaining() > 0 && remaining() > 0)
            writeByte(bytes.readByte());
        return this;
    }

    @Override
    public Bytes<Underlying> skip(long bytesToSkip) {
        position += bytesToSkip;
        readOffsetPositionMoved(0);
        return this;
    }

    @Override
    public Bytes<Underlying> flip() {
        limit = position;
        position = start();
        return this;
    }

    public String toString() {
        return remaining() > 1 << 30 ? "[Bytes too large]" : BytesUtil.toString(this);
    }

    @Override
    public Bytes<Underlying> write(byte[] bytes, int offset, int length) {
        long offsetInRDO = writeOffsetPositionMoved(length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    public Bytes<Underlying> write(long offsetInRDO, byte[] bytes, int offset, int length) {
        long offsetInRDO1 = writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO1, bytes, offset, length);
        return this;
    }

    @Override
    public Bytes<Underlying> write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        long offsetInRDO1 = writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO1, bytes, offset, length);
        return this;
    }

    @Override
    public Bytes<Underlying> writeOrderedLong(long offset, long i) {
        long offset2 = writeCheckOffset(offset, 8);
        bytesStore.writeOrderedLong(offset2, i);
        return this;
    }

    @Override
    public void read(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = readByte();
    }

    @Override
    public void read(ByteBuffer buffer) {
        while (remaining() > 0 && buffer.remaining() > 0)
            buffer.put(readByte());
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

    @Override
    public Bytes<Underlying> write(ByteBuffer buffer) {
        bytesStore.write(position, buffer, buffer.position(), buffer.limit());
        position += buffer.remaining();
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
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        long offset2 = writeCheckOffset(offset, 4);
        return bytesStore.compareAndSwapInt(offset2, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        long offset2 = writeCheckOffset(offset, 8);
        return bytesStore.compareAndSwapLong(offset2, expected, value);
    }

    @Override
    public long address() {
        return bytesStore.address();
    }

    protected long readCheckOffset(long offset, int adding) {
        if (offset < start()) throw new BufferUnderflowException();
        if (offset + adding > capacity()) throw new BufferOverflowException();
        return offset;
    }

    protected long writeCheckOffset(long offset, int adding) {
        if (offset < start()) throw new BufferUnderflowException();
        if (offset + adding > capacity()) throw new BufferOverflowException();
        return offset;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        refCount.releaseAll();
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
        long i;
        for (i = 0; i < remaining - 7; i += 8)
            if (readLong(i) != b2.readLong(i))
                return false;
        for (i = 0; i < remaining; i++)
            if (readByte(i) != b2.readByte(i))
                return false;
        return true;
    }


    @Override
    public Underlying underlyingObject() {
        return bytesStore.underlyingObject();
    }
}
