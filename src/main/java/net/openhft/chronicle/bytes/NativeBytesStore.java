/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.*;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.cleaner.CleanerServiceLocator;
import net.openhft.chronicle.core.cleaner.spi.ByteBufferCleanerService;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.WeakReferenceCleaner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("sunapi")
public class NativeBytesStore<Underlying>
        extends AbstractBytesStore<NativeBytesStore<Underlying>, Underlying> {
    private static final long MEMORY_MAPPED_SIZE = 128 << 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeBytesStore.class);
    private static final Field BB_ADDRESS, BB_CAPACITY;
    private static final ByteBufferCleanerService CLEANER_SERVICE = CleanerServiceLocator.cleanerService();
    //    static MappedBytes last;

    static {
        Class directBB = ByteBuffer.allocateDirect(0).getClass();
        BB_ADDRESS = Jvm.getField(directBB, "address");
        BB_CAPACITY = Jvm.getField(directBB, "capacity");
    }

    @Nullable
    private final Throwable createdHere = Jvm.isDebug() ? new Throwable("Created here") : null;
    /*    static {
            try {
                last = MappedBytes.mappedBytes(new File("last"), 8);
            } catch (FileNotFoundException e) {
                throw new AssertionError(e);
            }
        }*/
    protected long address;
    // on release, set this to null.
    protected Memory memory = OS.memory();
    protected volatile Throwable releasedHere;
    protected long maximumLimit;
    @Nullable
    private WeakReferenceCleaner cleaner;
    private boolean elastic;
    @Nullable
    private Underlying underlyingObject;
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);

    private NativeBytesStore() {
    }

    private NativeBytesStore(@NotNull ByteBuffer bb, boolean elastic) {
        init(bb, elastic);
    }

    public NativeBytesStore(
            long address, long maximumLimit) {
        this(address, maximumLimit, null, false);
    }

    public NativeBytesStore(
            long address, long maximumLimit, @Nullable Runnable deallocator, boolean elastic) {
        setAddress(address);
        this.maximumLimit = maximumLimit;
        this.cleaner = deallocator == null ? null : WeakReferenceCleaner.newCleaner(this, deallocator);
        underlyingObject = null;
        this.elastic = elastic;
    }

    @NotNull
    public static NativeBytesStore<ByteBuffer> wrap(@NotNull ByteBuffer bb) {
        return new NativeBytesStore<>(bb, false);
    }

    @NotNull
    public static <T> NativeBytesStore<T> uninitialized() {
        return new NativeBytesStore<>();
    }

    /**
     * this is an elastic native store
     *
     * @param capacity of the buffer.
     */
    @NotNull
    public static NativeBytesStore<Void> nativeStore(long capacity)
            throws IllegalArgumentException {
        return of(capacity, true, true);
    }

    @NotNull
    private static NativeBytesStore<Void> of(long capacity, boolean zeroOut, boolean elastic)
            throws IllegalArgumentException {
        Memory memory = OS.memory();
        long address = memory.allocate(capacity);
        if (zeroOut || capacity < MEMORY_MAPPED_SIZE) {
            memory.setMemory(address, capacity, (byte) 0);
            memory.storeFence();
        }
        @NotNull Deallocator deallocator = new Deallocator(address, capacity);
        return new NativeBytesStore<>(address, capacity, deallocator, elastic);
    }

    @NotNull
    public static NativeBytesStore<Void> nativeStoreWithFixedCapacity(long capacity)
            throws IllegalArgumentException {
        return of(capacity, true, false);
    }

    @NotNull
    public static NativeBytesStore<Void> lazyNativeBytesStoreWithFixedCapacity(long capacity)
            throws IllegalArgumentException {
        return of(capacity, false, false);
    }

    @NotNull
    public static NativeBytesStore<ByteBuffer> elasticByteBuffer() {
        return elasticByteBuffer(OS.pageSize(), Bytes.MAX_CAPACITY);
    }

    @NotNull
    public static NativeBytesStore<ByteBuffer> elasticByteBuffer(int size, long maxSize) {
        return new NativeBytesStore<>(ByteBuffer.allocateDirect(size), true);
    }

    @NotNull
    public static NativeBytesStore from(@NotNull String text) {
        return from(text.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    public static NativeBytesStore from(@NotNull byte[] bytes) {
        try {
            @NotNull NativeBytesStore nbs = nativeStore(bytes.length);
            Bytes.wrapForRead(bytes).copyTo(nbs);
            return nbs;
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean isDirectMemory() {
        return true;
    }

    public void init(@NotNull ByteBuffer bb, boolean elastic) {
        this.elastic = elastic;
        underlyingObject = (Underlying) bb;
        setAddress(((DirectBuffer) bb).address());
        this.maximumLimit = bb.capacity();
    }

    public void uninit() {
        underlyingObject = null;
        address = 0;
        maximumLimit = 0;
        cleaner = null;
    }

    @Override
    public void move(long from, long to, long length) throws BufferUnderflowException {
        if (from < 0 || to < 0) throw new BufferUnderflowException();
        OS.memory().copyMemory(address + from, address + to, length);
    }

    @NotNull
    @Override
    public BytesStore<NativeBytesStore<Underlying>, Underlying> copy() {
        try {
            if (underlyingObject == null) {
                @NotNull NativeBytesStore<Void> copy = of(realCapacity(), false, true);
                memory.copyMemory(address, copy.address, capacity());
                return (BytesStore) copy;

            } else if (underlyingObject instanceof ByteBuffer) {
                ByteBuffer bb = ByteBuffer.allocateDirect(Maths.toInt32(capacity()));
                bb.put((ByteBuffer) underlyingObject);
                bb.clear();
                return (BytesStore) wrap(bb);

            } else {
                throw new UnsupportedOperationException();
            }
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    @Override
    public VanillaBytes<Underlying> bytesForWrite() throws IllegalStateException {
        return elastic ? new NativeBytes<>(this) : new VanillaBytes<>(this);
    }

    @Override
    @ForceInline
    public long realCapacity() {
        return maximumLimit;
    }

    @Override
    @ForceInline
    public long capacity() {
        return maximumLimit;
    }

    @Nullable
    @Override
    @ForceInline
    public Underlying underlyingObject() {
        return underlyingObject;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> zeroOut(long start, long end) {
        if (end <= start)
            return this;
        if (start < start())
            start = start();
        if (end > capacity())
            end = capacity();

        memory.setMemory(address + translate(start), end - start, (byte) 0);
        return this;
    }

    @Override
    @ForceInline
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        return memory.compareAndSwapInt(address + translate(offset), expected, value);
    }

    @Override
    @ForceInline
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        return memory.compareAndSwapLong(address + translate(offset), expected, value);
    }

    @Override
    @ForceInline
    public long addAndGetLong(long offset, long adding) throws BufferUnderflowException {
        return memory.addLong(address + translate(offset), adding);
    }

    @Override
    @ForceInline
    public int addAndGetInt(long offset, int adding) throws BufferUnderflowException {
        return memory.addInt(address + translate(offset), adding);
    }

    protected long translate(long offset) {
        return offset;
    }

    @Override
    public void reserve() throws IllegalStateException {
        refCount.reserve();
    }

    @Override
    public void release() throws IllegalStateException {
        refCount.release();
        if (Jvm.isDebug() && refCount.get() == 0)
            releasedHere = new Error("Released here");
    }

    @Override
    public long refCount() {
        return refCount.get();
    }

    @Override
    @ForceInline
    public byte readByte(long offset) {
        if (Jvm.isDebug()) checkReleased();

        return memory.readByte(address + translate(offset));
    }

    public void checkReleased() {
        if (releasedHere != null)
            throw new InternalError("Accessing a released resource", releasedHere);
    }

    @Override
    @ForceInline
    public short readShort(long offset) {
        return memory.readShort(address + translate(offset));
    }

    @Override
    @ForceInline
    public int readInt(long offset) {
        return memory.readInt(address + translate(offset));
    }

    @Override
    @ForceInline
    public long readLong(long offset) {
        long address = this.address;
        assert address != 0;
        return memory.readLong(address + translate(offset));
    }

    @Override
    @ForceInline
    public float readFloat(long offset) {
        return memory.readFloat(address + translate(offset));
    }

    @Override
    @ForceInline
    public double readDouble(long offset) {
        return memory.readDouble(address + translate(offset));
    }

    @Override
    @ForceInline
    public byte readVolatileByte(long offset) {
        return memory.readVolatileByte(address + translate(offset));
    }

    @Override
    @ForceInline
    public short readVolatileShort(long offset) {
        return memory.readVolatileShort(address + translate(offset));
    }

    @Override
    @ForceInline
    public int readVolatileInt(long offset) {
        return memory.readVolatileInt(address + translate(offset));
    }

    @Override
    @ForceInline
    public long readVolatileLong(long offset) {
        return memory.readVolatileLong(address + translate(offset));
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeByte(long offset, byte i8) {
        memory.writeByte(address + translate(offset), i8);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeShort(long offset, short i16) {
        memory.writeShort(address + translate(offset), i16);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeInt(long offset, int i32) {
        memory.writeInt(address + translate(offset), i32);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeOrderedInt(long offset, int i) {
        memory.writeOrderedInt(address + translate(offset), i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeLong(long offset, long i64) {
        memory.writeLong(address + translate(offset), i64);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeOrderedLong(long offset, long i) {
        memory.writeOrderedLong(address + translate(offset), i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeFloat(long offset, float f) {
        memory.writeFloat(address + translate(offset), f);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeDouble(long offset, double d) {
        memory.writeDouble(address + translate(offset), d);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeVolatileByte(long offset, byte i8) {
        memory.writeVolatileByte(address + translate(offset), i8);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeVolatileShort(long offset, short i16) {
        memory.writeVolatileShort(address + translate(offset), i16);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeVolatileInt(long offset, int i32) {
        memory.writeVolatileInt(address + translate(offset), i32);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> writeVolatileLong(long offset, long i64) {
        memory.writeVolatileLong(address + translate(offset), i64);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> write(
            long offsetInRDO, byte[] bytes, int offset, int length) {
        memory.copyMemory(bytes, offset, address + translate(offsetInRDO), length);
        return this;
    }

    @Override
    @ForceInline
    public void write(
            long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length) {
        if (bytes.isDirect()) {
            memory.copyMemory(((DirectBuffer) bytes).address() + offset,
                    address + translate(offsetInRDO), length);

        } else {
            memory.copyMemory(bytes.array(), offset, address + translate(offsetInRDO), length);
        }
    }

    @NotNull
    @Override
    @ForceInline
    public NativeBytesStore<Underlying> write(
            long offsetInRDO, @NotNull RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException {
        if (bytes.isDirectMemory()) {
            memory.copyMemory(bytes.addressForRead(offset), addressForWrite(offsetInRDO), length);
        } else {
            write0(offsetInRDO, bytes, offset, length);
        }
        return this;
    }

    public void write0(long offsetInRDO, @NotNull RandomDataInput bytes, long offset, long length) throws BufferUnderflowException {
        long i = 0;
        for (; i < length - 7; i += 8) {
            writeLong(offsetInRDO + i, bytes.readLong(offset + i));
        }
        for (; i < length; i++) {
            writeByte(offsetInRDO + i, bytes.readByte(offset + i));
        }
    }

    @Override
    public long addressForRead(long offset) throws BufferUnderflowException {
        if (offset < start() || offset > realCapacity())
            throw new BufferUnderflowException();
        return address + translate(offset);
    }

    @Override
    public long addressForWrite(long offset) throws BufferOverflowException {
        if (offset < start() || offset > realCapacity())
            throw new BufferOverflowException();
        return address + translate(offset);
    }

    private void performRelease() {
        memory = null;
        if (refCount.get() > 0) {
            LOGGER.info("NativeBytesStore discarded without releasing ", createdHere);
        }
        if (releasedHere == null) {
            assert (releasedHere = new Throwable()) != null;
        }
        if (cleaner != null) {
            cleaner.scheduleForClean();
        } else if (underlyingObject instanceof ByteBuffer) {
            CLEANER_SERVICE.clean((ByteBuffer) underlyingObject);
        }
    }

    @NotNull
    @Override
    public String toString() {
        try {
            return BytesInternal.toString(this);

        } catch (IllegalStateException e) {
            return e.toString();
        }
    }

    @Override
    @ForceInline
    public void nativeRead(long position, long address, long size) throws BufferUnderflowException {
        // TODO add bounds checking.
        memory.copyMemory(addressForRead(position), address, size);
    }

    @Override
    @ForceInline
    public void nativeWrite(long address, long position, long size) throws BufferOverflowException {
        // TODO add bounds checking.
        memory.copyMemory(address, addressForWrite(position), size);
    }

    void write8bit(long position, char[] chars, int offset, int length) {
        long addr = address + translate(position);
        @Nullable Memory memory = this.memory;
        for (int i = 0; i < length; i++)
            memory.writeByte(addr + i, (byte) chars[offset + i]);
    }

    void read8bit(long position, char[] chars, int length) {
        long addr = address + translate(position);
        Memory memory = OS.memory();
        for (int i = 0; i < length; i++)
            chars[i] = (char) (memory.readByte(addr + i) & 0xFF);
    }

    @Override
    public long readIncompleteLong(long offset) {
        int remaining = (int) Math.min(8, readRemaining() - offset);
        long l = 0;
        for (int i = 0; i < remaining; i++) {
            byte b = memory.readByte(address + offset + i);
            l |= (long) (b & 0xFF) << (i * 8);
        }
        return l;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore) obj);
    }

    public void setAddress(long address) {
        if ((address & ~0x3FFF) == 0)
            throw new AssertionError("Invalid addressForRead " + Long.toHexString(address));
        this.address = address;
    }

    @Deprecated
    public long appendUTF(long pos, char[] chars, int offset, int length) throws BufferOverflowException {
        return appendUtf8(pos, chars, offset, length);
    }

    public long appendUtf8(long pos, char[] chars, int offset, int length) throws BufferOverflowException {
        if (pos + length > realCapacity())
            throw new BufferOverflowException();

        long address = this.address + translate(0);
        @Nullable Memory memory = this.memory;
        if (memory == null) throw new NullPointerException();
        Unsafe unsafe = UnsafeMemory.UNSAFE;
        int i;
        ascii:
        {
            for (i = 0; i < length - 3; i += 4) {
                final int i2 = offset + i;
                char c0 = chars[i2];
                char c1 = chars[i2 + 1];
                char c2 = chars[i2 + 2];
                char c3 = chars[i2 + 3];
                if ((c0 | c1 | c2 | c3) > 0x007F)
                    break ascii;
                final int value = (c0) | (c1 << 8) | (c2 << 16) | (c3 << 24);
                unsafe.putInt(address + pos, value);
                pos += 4;
            }
            for (; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                unsafe.putByte(address + pos++, (byte) c);
            }

            return pos;
        }
        return appendUtf8a(pos, chars, offset, length, i);
    }

    private long appendUtf8a(long pos, char[] chars, int offset, int length, int i) {
        for (; i < length; i++) {
            char c = chars[offset + i];
            if (c <= 0x007F) {
                writeByte(pos++, (byte) c);

            } else if (c <= 0x07FF) {
                writeByte(pos++, (byte) (0xC0 | ((c >> 6) & 0x1F)));
                writeByte(pos++, (byte) (0x80 | c & 0x3F));

            } else {
                writeByte(pos++, (byte) (0xE0 | ((c >> 12) & 0x0F)));
                writeByte(pos++, (byte) (0x80 | ((c >> 6) & 0x3F)));
                writeByte(pos++, (byte) (0x80 | (c & 0x3F)));
            }
        }
        return pos;
    }

    @Override
    public long copyTo(@NotNull BytesStore store) {
        if (store.isDirectMemory())
            return copyToDirect(store);
        else
            return super.copyTo(store);
    }

    public long copyToDirect(@NotNull BytesStore store) {
        long read = Math.min(readRemaining(), writeRemaining());
        if (read > 0) {
            try {
                long addr = address;
                long addr2 = store.addressForWrite(0);
                memory.copyMemory(addr, addr2, read);
            } catch (BufferOverflowException e) {
                throw new AssertionError(e);
            }
        }
        return read;
    }

    @NotNull
    @Override
    public ByteBuffer toTemporaryDirectByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(0);
        try {
            BB_ADDRESS.setLong(bb, address);
            BB_CAPACITY.setInt(bb, Maths.toUInt31(readRemaining()));

        } catch (Exception e) {
            throw new AssertionError(e);
        }
        bb.clear();
        return bb;
    }

    @Override
    public int byteCheckSum() throws IORuntimeException {
        return byteCheckSum(start(), readLimit());
    }

    public int byteCheckSum(long position, long limit) {
        @Nullable Memory memory = this.memory;
        assert memory != null;
        int b = 0;
        long ptr = address + position;
        long end = address + limit;
        for (; ptr < end - 7; ptr += 8) {
            b += memory.readByte(ptr)
                    + memory.readByte(ptr + 1)
                    + memory.readByte(ptr + 2)
                    + memory.readByte(ptr + 3)
                    + memory.readByte(ptr + 4)
                    + memory.readByte(ptr + 5)
                    + memory.readByte(ptr + 6)
                    + memory.readByte(ptr + 7);
        }
        for (; ptr < end; ptr++) {
            b += memory.readByte(ptr);
        }
        return b & 0xFF;
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }

    @Override
    public long read(long offsetInRDI, byte[] bytes, int offset, int length) {
        int len = (int) Math.min(length, readLimit() - offsetInRDI);
        int i;
        final long offset2 = Unsafe.ARRAY_BYTE_BASE_OFFSET + offset;
        final long address = this.address + translate(offsetInRDI);
        for (i = 0; i < len - 7; i += 8)
            UnsafeMemory.UNSAFE.putLong(bytes, offset2 + i, memory.readLong(address + i));
        if (i < len - 3) {
            UnsafeMemory.UNSAFE.putInt(bytes, offset2 + i, memory.readInt(address + i));
            i += 4;
        }
        for (; i < len; i++)
            UnsafeMemory.UNSAFE.putByte(bytes, offset2 + i, memory.readByte(address + i));
        return len;
    }

    @Override
    public int peekUnsignedByte(long offset) {
        final long address = this.address;
        @Nullable final Memory memory = this.memory;
        final long translate = translate(offset);
//        assert translate >= 0;
        final long address2 = address + translate;
//        last.writeLong(8, Thread.currentThread().getId());
//        last.writeLong(0, offset);
//        last.writeLong(16, translate);
//        last.writeLong(32, maximumLimit);
//        last.writeLong(48, addressForRead);
//        last.writeLong(64, address2);
//        last.writeBoolean(80, memory != null);
//        last.writeVolatileByte(88, (byte) 1);
        int ret = translate >= maximumLimit ? -1 :
                memory.readByte(address2) & 0xFF;
//        last.writeVolatileByte(88, (byte) 0xFF);
//        last.writeLong(24, Thread.currentThread().getId());
        return ret;
    }

    @Override
    public int fastHash(long offset, int length) throws BufferUnderflowException {
        long ret;
        switch (length) {
            case 0:
                return 0;
            case 1:
                ret = readByte(offset);
                break;
            case 2:
                ret = readShort(offset);
                break;
            case 4:
                ret = readInt(offset);
                break;
            case 8:
                ret = readInt(offset) * 0x6d0f27bdL + readInt(offset + 4);
                break;
            default:
                return super.fastHash(offset, length);
        }
        long hash = ret * 0x855dd4db;
        return (int) (hash ^ (hash >> 32));
    }

    @Override
    public long safeLimit() {
        return maximumLimit;
    }

    static class Deallocator implements Runnable {

        private volatile long address, size;

        Deallocator(long address, long size) {
            assert address != 0;
            this.address = address;
            this.size = size;
        }

        @Override
        public void run() {
            //     System.out.println("Release " + Long.toHexString(addressForRead));
            if (address == 0)
                return;
            long addressToFree = address;
            address = 0;
            OS.memory().freeMemory(addressToFree, size);
        }
    }

}
