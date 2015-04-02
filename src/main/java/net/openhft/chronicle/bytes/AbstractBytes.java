package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.ReferenceCounter;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.min;
import static net.openhft.chronicle.bytes.Accessor.byteArrayAccessor;
import static net.openhft.chronicle.bytes.Accessor.uncheckedByteBufferAccessor;

public abstract class AbstractBytes<Underlying> implements Bytes<Underlying> {
    private static final AtomicBoolean MEMORY_BARRIER = new AtomicBoolean();
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);

    protected BytesStore<Bytes<Underlying>, Underlying> bytesStore = NoBytesStore.noBytesStore();
    private long position;
    private long limit;
    private UnderflowMode underflowMode;
    long mark = -1;

    public AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
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
        limit = isElastic() ? capacity() : realCapacity();
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
    public UnderflowMode underflowMode() {
        return underflowMode;
    }

    @Override
    public Bytes<Underlying> underflowMode(UnderflowMode underflowMode) {
        this.underflowMode = underflowMode;
        return this;
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

        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readByte(offset);
        } catch (BufferOverflowException e) {

            return 0;
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
        long offset = readCheckOffset(position, 4);
        return bytesStore.readVolatileInt(offset);
    }

    protected long writeOffsetPositionMoved(int adding) {
        long offset = writeCheckOffset(position, adding);
        position += adding;
        assert position <= limit();
        return offset;
    }

    protected long readOffsetPositionMoved(int adding) {
        long offset = readCheckOffset(position, adding);
        position += adding;
        assert position <= limit();
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
        long write = min(remaining(), bytes.remaining());
        long offset = bytes.position();
        bytes.skip(write);
        return write(bytes, offset, write);
    }


    @Override
    public Bytes<Underlying> write(Bytes bytes, long offset, long length) {
        long write = min(remaining(), length);
        long targetOffset = accessPositionOffset();
        skip(write);
        Access.copy(bytes.access(), bytes.accessHandle(), bytes.accessOffset(offset),
                access(), accessHandle(), targetOffset, write);
        return this;
    }


    @Override
    public Bytes<Underlying> skip(long bytesToSkip) {
        position += bytesToSkip;
        assert position <= limit();
        readOffsetPositionMoved(0);
        return this;
    }

    @Override
    public Bytes<Underlying> flip() {
        limit = position;
        position = start();
        assert limit >= start();
        return this;
    }

    @Override
    public String toString() {
        return BytesUtil.toString(this);
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
    public Bytes<Underlying> write(long offsetInRDO, Bytes bytes, long offset, long length) {
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

    private <S, T> void read(Accessor.Full<S, T> accessor, S target, long off, long len) {
        long sourceOffset = accessPositionOffset();
        long size = accessor.size(len);
        skip(size);
        Access.copy(access(), accessHandle(), sourceOffset,
                accessor.access(target), accessor.handle(target), accessor.offset(target, off),
                size);
    }

    @Override
    public void read(byte[] bytes) {
        read(byteArrayAccessor(), bytes, 0, bytes.length);
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
        if (offset < start())
            throw new BufferUnderflowException();
        long limit0 = limit();
        if (offset + adding > limit0) {
          assert false : "can't read bytes past the limit : limit=" + limit0 + ",offset=" + offset +
                    ",adding=" + adding;
            throw new BufferUnderflowException();
        }
        return offset;
    }

    protected long writeCheckOffset(long offset, long adding) {
        if (offset < start())
            throw new BufferUnderflowException();
        if (offset + adding > limit()) {
            assert offset + adding <= limit() : "cant add bytes past the limit : limit=" + limit +
                  ",offset=" +
                offset +
                 ",adding=" + adding;
            throw new BufferOverflowException();
        }
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
        return Access.compare(access(), accessHandle(), accessPositionOffset(),
                b2.access(), b2.accessHandle(), b2.accessPositionOffset(), remaining);
    }


    @Override
    public Underlying underlyingObject() {
        return bytesStore.underlyingObject();
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

    public Bytes<Underlying> zeroOut(long start, long end) {
        bytesStore.zeroOut(start, end);
        return this;
    }






    @Override
    public long accessOffset(long randomOffset) {
        return bytesStore.accessOffset(randomOffset);
    }

    @Override
    public Access<Underlying> access() {
        return bytesStore.access();
    }

    @Override
    public Underlying accessHandle() {
        return bytesStore.accessHandle();
    }
}


