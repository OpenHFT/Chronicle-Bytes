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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.ReferenceCounter;
import org.jetbrains.annotations.Nullable;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

public class NativeBytesStore<Underlying>
        implements BytesStore<NativeBytesStore<Underlying>, Underlying> {
    private static final long MEMORY_MAPPED_SIZE = 128 << 10;
    private static final Memory MEMORY = OS.memory();
    @Nullable
    private final Cleaner cleaner;
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);
    private final boolean elastic;
    private final Underlying underlyingObject;
    protected long address;
    long maximumLimit;

    private NativeBytesStore(ByteBuffer bb, boolean elastic) {
        this.elastic = elastic;
        underlyingObject = (Underlying) bb;
        this.address = ((DirectBuffer) bb).address();
        this.maximumLimit = bb.capacity();
        cleaner = ((DirectBuffer) bb).cleaner();
    }

    NativeBytesStore(
            long address, long maximumLimit, Runnable deallocator, boolean elastic) {
        this.address = address;
        this.maximumLimit = maximumLimit;
        cleaner = deallocator == null ? null : Cleaner.create(this, deallocator);
        underlyingObject = null;
        this.elastic = elastic;
    }

    public static NativeBytesStore<ByteBuffer> wrap(ByteBuffer bb) {
        return new NativeBytesStore<>(bb, false);
    }

    /**
     * this is an elastic native store
     *
     * @param capacity of the buffer.
     */
    public static NativeBytesStore<Void> nativeStore(long capacity) {
        return of(capacity, true, true);
    }

    private static NativeBytesStore<Void> of(long capacity, boolean zeroOut, boolean elastic) {
        long address = MEMORY.allocate(capacity);
        if (zeroOut || capacity < MEMORY_MAPPED_SIZE) {
            MEMORY.setMemory(address, capacity, (byte) 0);
            MEMORY.storeFence();
        }
        Deallocator deallocator = new Deallocator(address);
        return new NativeBytesStore<>(address, capacity, deallocator, elastic);
    }

    public static NativeBytesStore<Void> nativeStoreWithFixedCapacity(long capacity) {
        return of(capacity, true, false);
    }

    public static NativeBytesStore<Void> lazyNativeBytesStoreWithFixedCapacity(long capacity) {
        return of(capacity, false, false);
    }

    public static NativeBytesStore<ByteBuffer> elasticByteBuffer() {
        return elasticByteBuffer(OS.pageSize());
    }

    public static NativeBytesStore<ByteBuffer> elasticByteBuffer(int size) {
        return new NativeBytesStore<>(ByteBuffer.allocateDirect(size), true);
    }

    @Override
    public BytesStore<NativeBytesStore<Underlying>, Underlying> copy() {
        if (underlyingObject == null) {
            NativeBytesStore<Void> copy = of(realCapacity(), false, true);
            OS.memory().copyMemory(address, copy.address, capacity());
            return (BytesStore) copy;

        } else if (underlyingObject instanceof ByteBuffer) {
            ByteBuffer bb = ByteBuffer.allocateDirect(Maths.toInt32(capacity()));
            bb.put((ByteBuffer) underlyingObject);
            bb.clear();
            return (BytesStore) wrap(bb);

        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Bytes<Underlying> bytes() {
        return elastic ? new NativeBytes<>(this) : new VanillaBytes<>(this);
    }

    @Override
    public Bytes bytes(UnderflowMode underflowMode) {
        return underflowMode == UnderflowMode.BOUNDED
                ? new NativeBytes<>(this)
                : BytesStore.super.bytes(underflowMode);
    }

    @Override
    public long realCapacity() {
        return maximumLimit;
    }

    @Override
    public long capacity() {
        return maximumLimit;
    }

    @Override
    public Underlying underlyingObject() {
        return underlyingObject;
    }

    @Override
    public NativeBytesStore<Underlying> zeroOut(long start, long end) {
        if (start < writePosition() || end > writeLimit())
            throw new IllegalArgumentException("position: " + writePosition() + ", start: " + start + ", end: " + end + ", limit: " + writeLimit());
        if (start >= end)
            return this;

        MEMORY.setMemory(address + translate(start), end - start, (byte) 0);
        return this;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        return MEMORY.compareAndSwapInt(address + translate(offset), expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        return MEMORY.compareAndSwapLong(address + translate(offset), expected, value);
    }

    private long translate(long offset) {
        long offset2 = offset - start();
        if (offset2 < 0 || offset2 > capacity())
            throw new IllegalArgumentException("Offset out of bounds " + offset2 + " cap: " + capacity());
        return offset2;
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
    public byte readByte(long offset) {
        return MEMORY.readByte(address + translate(offset));
    }

    @Override
    public short readShort(long offset) {
        return MEMORY.readShort(address + translate(offset));
    }

    @Override
    public int readInt(long offset) {
        return MEMORY.readInt(address + translate(offset));
    }

    @Override
    public long readLong(long offset) {
        return MEMORY.readLong(address + translate(offset));
    }

    @Override
    public float readFloat(long offset) {
        return MEMORY.readFloat(address + translate(offset));
    }

    @Override
    public double readDouble(long offset) {
        return MEMORY.readDouble(address + translate(offset));
    }

    @Override
    public int readVolatileInt(long offset) {
        return MEMORY.readVolatileInt(address + translate(offset));
    }

    @Override
    public long readVolatileLong(long offset) {
        return MEMORY.readVolatileLong(address + translate(offset));
    }

    @Override
    public NativeBytesStore<Underlying> writeByte(long offset, byte i8) {
        MEMORY.writeByte(address + translate(offset), i8);
        return this;
    }

    @Override
    public NativeBytesStore<Underlying> writeShort(long offset, short i16) {
        MEMORY.writeShort(address + translate(offset), i16);
        return this;
    }

    @Override
    public NativeBytesStore<Underlying> writeInt(long offset, int i32) {
        MEMORY.writeInt(address + translate(offset), i32);
        return this;
    }

    @Override
    public NativeBytesStore<Underlying> writeOrderedInt(long offset, int i) {
        MEMORY.writeOrderedInt(address + translate(offset), i);
        return this;
    }

    @Override
    public NativeBytesStore<Underlying> writeLong(long offset, long i64) {
        MEMORY.writeLong(address + translate(offset), i64);
        return this;
    }

    @Override
    public NativeBytesStore<Underlying> writeOrderedLong(long offset, long i) {
        MEMORY.writeOrderedLong(address + translate(offset), i);
        return this;
    }

    @Override
    public NativeBytesStore<Underlying> writeFloat(long offset, float f) {
        MEMORY.writeFloat(address + translate(offset), f);
        return this;
    }

    @Override
    public NativeBytesStore<Underlying> writeDouble(long offset, double d) {
        MEMORY.writeDouble(address + translate(offset), d);
        return this;
    }

    @Override
    public NativeBytesStore<Underlying> write(
            long offsetInRDO, byte[] bytes, int offset, int length) {
        MEMORY.copyMemory(bytes, offset, address + translate(offsetInRDO), length);
        return this;
    }

    @Override
    public void write(
            long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        if (bytes.isDirect()) {
            MEMORY.copyMemory(((DirectBuffer) bytes).address(),
                    address + translate(offsetInRDO), length);

        } else {
            MEMORY.copyMemory(bytes.array(), offset, address + translate(offsetInRDO), length);
        }
    }

    @Override
    public NativeBytesStore<Underlying> write(
            long offsetInRDO, Bytes bytes, long offset, long length) {
        long i = 0;
        for (; i < length - 7; i += 8) {
            writeLong(offsetInRDO + i, bytes.readLong(offset + i));
        }
        for (; i < length; i++) {
            writeByte(offsetInRDO + i, bytes.readByte(offset + i));
        }
        return this;
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public long accessOffset(long randomOffset) {
        return address + translate(randomOffset);
    }

    private void performRelease() {
        if (cleaner != null)
            cleaner.clean();
    }

    @Override
    public String toString() {
        return BytesUtil.toDebugString(this, 1024);
    }

    @Override
    public void nativeRead(long position, long address, long size) {
        // TODO add bounds checking.
        OS.memory().copyMemory(address() + position, address, size);
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        // TODO add bounds checking.
        OS.memory().copyMemory(address, address() + position, size);
    }

    void write8bit(long position, char[] chars, int offset, int length) {
        long addr = address + translate(position);
        Memory memory = NativeBytesStore.MEMORY;
        for (int i = 0; i < length; i++)
            memory.writeByte(addr + i, (byte) chars[offset + i]);
    }

    void read8bit(long position, char[] chars, int length) {
        long addr = address + translate(position);
        Memory memory = NativeBytesStore.MEMORY;
        for (int i = 0; i < length; i++)
            chars[i] = (char) (memory.readByte(addr + i) & 0xFF);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BytesStore && BytesUtil.contentEqual(this, (BytesStore) obj);
    }

    static class Deallocator implements Runnable {
        private volatile long address;

        Deallocator(long address) {
            assert address != 0;
            this.address = address;
        }

        @Override
        public void run() {
            if (address == 0)
                return;
            address = 0;
            MEMORY.freeMemory(address);
        }
    }
}
