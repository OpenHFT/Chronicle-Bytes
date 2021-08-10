/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.cleaner.CleanerServiceLocator;
import net.openhft.chronicle.core.cleaner.spi.ByteBufferCleanerService;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.SimpleCleaner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static net.openhft.chronicle.bytes.Bytes.MAX_CAPACITY;
import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

@SuppressWarnings({"restriction", "rawtypes", "unchecked"})
@Deprecated(/* to be moved to internal in x.22*/)
public class NativeBytesStore<Underlying>
        extends AbstractBytesStore<NativeBytesStore<Underlying>, Underlying> {
    private static final long MEMORY_MAPPED_SIZE = 128 << 10;
    private static final Field BB_ADDRESS, BB_CAPACITY, BB_ATT;
    private static final ByteBufferCleanerService CLEANER_SERVICE = CleanerServiceLocator.cleanerService();
    //    static MappedBytes last;

    static {
        Class directBB = ByteBuffer.allocateDirect(0).getClass();
        BB_ADDRESS = Jvm.getField(directBB, "address");
        BB_CAPACITY = Jvm.getField(directBB, "capacity");
        BB_ATT = Jvm.getField(directBB, "att");
    }

    private final Finalizer finalizer;
    protected long address;
    // on release, set this to null.
    protected Memory memory = OS.memory();
    protected long limit, maximumLimit;
    @Nullable
    private SimpleCleaner cleaner;
    private boolean elastic;
    @Nullable
    private Underlying underlyingObject;

    private NativeBytesStore() {
        finalizer = null;
    }

    private NativeBytesStore(@NotNull ByteBuffer bb, boolean elastic) {
        this(bb, elastic, Bytes.MAX_HEAP_CAPACITY);
    }

    public NativeBytesStore(@NotNull ByteBuffer bb, boolean elastic, int maximumLimit) {
        this();
        init(bb, elastic);
        this.maximumLimit = elastic ? maximumLimit : Math.min(limit, maximumLimit);
    }

    public NativeBytesStore(
            long address, long limit) {
        this(address, limit, null, false);
    }

    public NativeBytesStore(
            long address, long limit, @Nullable Runnable deallocator, boolean elastic) {
        this(address, limit, deallocator, elastic, false);
    }

    protected NativeBytesStore(
            long address, long limit, @Nullable Runnable deallocator, boolean elastic, boolean monitored) {
        super(monitored);
        setAddress(address);
        this.limit = limit;
        this.maximumLimit = elastic ? MAX_CAPACITY : limit;
        this.cleaner = deallocator == null ? null : new SimpleCleaner(deallocator);
        underlyingObject = null;
        this.elastic = elastic;
        if (cleaner == null)
            finalizer = null;
        else
            finalizer = new Finalizer();
    }

    /**
     * @param bb ByteBuffer
     * @return BytesStore
     * @deprecated use {@link BytesStore#wrap(ByteBuffer)}
     */
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
     * @deprecated use {@link BytesStore#nativeStore(long)}
     */
    @NotNull
    public static NativeBytesStore<Void> nativeStore(long capacity)
            throws IllegalArgumentException {
        return of(capacity, true, true);
    }

    @NotNull
    private static NativeBytesStore<Void> of(long capacity, boolean zeroOut, boolean elastic)
            throws IllegalArgumentException {
        if (capacity <= 0)
            return new NativeBytesStore<>(NoBytesStore.NO_PAGE, 0, null, elastic);
        Memory memory = OS.memory();
        long address = memory.allocate(capacity);
        if (zeroOut || capacity < MEMORY_MAPPED_SIZE) {
            memory.setMemory(address, capacity, (byte) 0);
            memory.storeFence();
        }
        @NotNull Deallocator deallocator = new Deallocator(address, capacity);
        return new NativeBytesStore<>(address, capacity, deallocator, elastic);
    }

    /**
     * @deprecated use {@link BytesStore#nativeStoreWithFixedCapacity(long)}
     */
    @NotNull
    public static NativeBytesStore<Void> nativeStoreWithFixedCapacity(long capacity)
            throws IllegalArgumentException {
        return of(capacity, true, false);
    }

    /**
     * @deprecated use {@link BytesStore#lazyNativeBytesStoreWithFixedCapacity(long)}
     */
    @NotNull
    public static NativeBytesStore<Void> lazyNativeBytesStoreWithFixedCapacity(long capacity)
            throws IllegalArgumentException {
        return of(capacity, false, false);
    }

    @NotNull
    public static NativeBytesStore<ByteBuffer> elasticByteBuffer() {
        return elasticByteBuffer(OS.pageSize(), MAX_CAPACITY);
    }

    @NotNull
    public static NativeBytesStore<ByteBuffer> elasticByteBuffer(int size, long maxSize) {
        return new NativeBytesStore<>(ByteBuffer.allocateDirect(size), true, Math.toIntExact(maxSize));
    }

    @NotNull
    public static NativeBytesStore from(@NotNull String text) {
        return from(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    @NotNull
    public static NativeBytesStore from(@NotNull byte[] bytes) {
        try {
            @NotNull NativeBytesStore nbs = nativeStoreWithFixedCapacity(bytes.length);
            Bytes<byte[]> bytes2 = Bytes.wrapForRead(bytes);
            bytes2.copyTo(nbs);
            bytes2.releaseLast();
            return nbs;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean isDirectMemory() {
        return true;
    }

    @Override
    public boolean canReadDirect(long length) {
        return limit >= length;
    }

    public void init(@NotNull ByteBuffer bb, boolean elastic) {
        this.elastic = elastic;
        underlyingObject = (Underlying) bb;
        setAddress(Jvm.address(bb));
        this.limit = bb.capacity();
    }

    public void uninit() {
        underlyingObject = null;
        address = 0;
        limit = 0;
        maximumLimit = 0;
        cleaner = null;
    }

    @Override
    public void move(long from, long to, long length)
            throws BufferUnderflowException, IllegalStateException {
        if (from < 0 || to < 0) throw new BufferUnderflowException();
        long address = this.address;
        if (address == 0) throwException(null);
        memoryCopyMemory(address + from, address + to, length);
    }

    private void memoryCopyMemory(long fromAddress, long toAddress, long length)
            throws IllegalStateException {
        try {
            memory.copyMemory(fromAddress, toAddress, length);
        } catch (NullPointerException ifReleased) {
            throwException(ifReleased);
        }
    }

    private void throwException(Throwable ifReleased)
            throws IllegalStateException {
        throwExceptionIfReleased();
        throw new IllegalStateException(ifReleased);
    }

    @NotNull
    @Override
    public BytesStore<NativeBytesStore<Underlying>, Underlying> copy()
            throws IllegalStateException {
        try {
            if (underlyingObject == null) {
                @NotNull NativeBytesStore<Void> copy = of(realCapacity(), false, true);
                memoryCopyMemory(address, copy.address, capacity());
                return (BytesStore) copy;

            } else if (underlyingObject instanceof ByteBuffer) {
                ByteBuffer bb = ByteBuffer.allocateDirect(Maths.toInt32(capacity()));
                bb.put((ByteBuffer) underlyingObject);
                bb.clear();
                return (BytesStore) wrap(bb);

            } else {
                throw new UnsupportedOperationException();
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    @Override
    public VanillaBytes<Underlying> bytesForWrite()
            throws IllegalStateException {
        try {
            return elastic
                    ? NativeBytes.wrapWithNativeBytes(this, this.capacity())
                    : new NativeBytes<>(this);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long realCapacity() {
        return limit;
    }

    @Override
    public long capacity() {
        return maximumLimit;
    }

    @Nullable
    @Override
    public Underlying underlyingObject() {
        return underlyingObject;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> zeroOut(long start, long end) {
        if (end <= start)
            return this;
        if (start < start())
            start = start();
        if (end > capacity())
            end = capacity();


        // don't dirty cache lines unnecessarily
        long address = this.address + translate(start);
        long size = end - start;
        // align the start
        while ((address & 0x7) != 0 && size > 0) {
            if (memory.readByte(address, 0) != 0)
                memory.writeByte(address, (byte) 0);
            address++;
            size--;
        }
        long i = 0;
        for (; i < size - 7; i += 8)
            if (memory.readLong(address + i, 0) != 0)
                memory.writeLong(address + i, 0);

        for (; i < size; i++)
            if (memory.readByte(address + i, 0) != 0)
                memory.writeByte(address + i, (byte) 0);
        return this;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value)
            throws IllegalStateException {
        return memory.compareAndSwapInt(address + translate(offset), expected, value);
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value)
            throws IllegalStateException {
        memory.testAndSetInt(address + translate(offset), offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws IllegalStateException {
        return memory.compareAndSwapLong(address + translate(offset), expected, value);
    }

    @Override
    public long addAndGetLong(long offset, long adding)
            throws BufferUnderflowException {
        return memory.addLong(address + translate(offset), adding);
    }

    @Override
    public int addAndGetInt(long offset, int adding)
            throws BufferUnderflowException {
        return memory.addInt(address + translate(offset), adding);
    }

    protected long translate(long offset) {
        return offset;
    }

    @Override
    public byte readByte(long offset) {
        return memory.readByte(address + translate(offset));
    }

    public int readUnsignedByte(long offset)
            throws BufferUnderflowException {
        return readByte(offset) & 0xFF;
    }

    @Override
    public short readShort(long offset) {
        return memory.readShort(address + translate(offset));
    }

    @Override
    public int readInt(long offset) {
        return memory.readInt(address + translate(offset));
    }

    @Override
    public long readLong(long offset) {
        long address = this.address;
        assert address != 0;
        return memory.readLong(address + translate(offset));
    }

    @Override
    public float readFloat(long offset) {
        return memory.readFloat(address + translate(offset));
    }

    @Override
    public double readDouble(long offset) {
        return memory.readDouble(address + translate(offset));
    }

    @Override
    public byte readVolatileByte(long offset) {
        return memory.readVolatileByte(address + translate(offset));
    }

    @Override
    public short readVolatileShort(long offset) {
        return memory.readVolatileShort(address + translate(offset));
    }

    @Override
    public int readVolatileInt(long offset) {
        return memory.readVolatileInt(address + translate(offset));
    }

    @Override
    public long readVolatileLong(long offset) {
        return memory.readVolatileLong(address + translate(offset));
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeByte(long offset, byte i8)
            throws IllegalStateException {
        memory.writeByte(address + translate(offset), i8);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeShort(long offset, short i16)
            throws IllegalStateException {
        memory.writeShort(address + translate(offset), i16);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeInt(long offset, int i32)
            throws IllegalStateException {
        try {
            memory.writeInt(address + translate(offset), i32);
            return this;
        } catch (NullPointerException e) {
            throwExceptionIfReleased();
            throw e;
        }
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeOrderedInt(long offset, int i)
            throws IllegalStateException {
        memory.writeOrderedInt(address + translate(offset), i);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeLong(long offset, long i64)
            throws IllegalStateException {
        memory.writeLong(address + translate(offset), i64);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeOrderedLong(long offset, long i)
            throws IllegalStateException {
        memory.writeOrderedLong(address + translate(offset), i);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeFloat(long offset, float f)
            throws IllegalStateException {
        memory.writeFloat(address + translate(offset), f);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeDouble(long offset, double d)
            throws IllegalStateException {
        memory.writeDouble(address + translate(offset), d);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeVolatileByte(long offset, byte i8)
            throws IllegalStateException {
        memory.writeVolatileByte(address + translate(offset), i8);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeVolatileShort(long offset, short i16)
            throws IllegalStateException {
        memory.writeVolatileShort(address + translate(offset), i16);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeVolatileInt(long offset, int i32)
            throws IllegalStateException {
        memory.writeVolatileInt(address + translate(offset), i32);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> writeVolatileLong(long offset, long i64)
            throws IllegalStateException {
        memory.writeVolatileLong(address + translate(offset), i64);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> write(
            long offsetInRDO, byte[] bytes, int offset, int length)
            throws IllegalStateException {
        memory.copyMemory(bytes, offset, address + translate(offsetInRDO), length);
        return this;
    }

    @Override
    public void write(
            long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length)
            throws IllegalStateException {
        if (bytes.isDirect()) {
            memoryCopyMemory(Jvm.address(bytes) + offset, address + translate(offsetInRDO), length);

        } else {
            memory.copyMemory(bytes.array(), offset, address + translate(offsetInRDO), length);
        }
    }

    @NotNull
    @Override
    public NativeBytesStore<Underlying> write(
            long writeOffset, @NotNull RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        if (bytes.isDirectMemory()) {
            memoryCopyMemory(bytes.addressForRead(readOffset), addressForWrite(writeOffset), length);
        } else {
            write0(writeOffset, bytes, readOffset, length);
        }
        return this;
    }

    public void write0(long offsetInRDO, @NotNull RandomDataInput bytes, long offset, long length)
            throws BufferUnderflowException, IllegalStateException {
        long i = 0;
        for (; i < length - 7; i += 8) {
            writeLong(offsetInRDO + i, bytes.readLong(offset + i));
        }
        for (; i < length; i++) {
            writeByte(offsetInRDO + i, bytes.readByte(offset + i));
        }
    }

    @Override
    public long addressForRead(long offset)
            throws BufferUnderflowException {
        if (offset < start() || offset > realCapacity())
            throw new BufferUnderflowException();
        return address + translate(offset);
    }

    @Override
    public long addressForWrite(long offset)
            throws BufferOverflowException {
        if (offset < start() || offset > realCapacity())
            throw new BufferOverflowException();
        return address + translate(offset);
    }

    @Override
    public long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException {
        return addressForWrite(start());
    }

    @Override
    protected void performRelease() {
        memory = null;
        if (cleaner != null) {
            cleaner.clean();

        } else if (underlyingObject instanceof ByteBuffer) {
            ByteBuffer underlyingObject = (ByteBuffer) this.underlyingObject;
            if (underlyingObject.isDirect())
                CLEANER_SERVICE.clean(underlyingObject);
        }
    }

    @NotNull
    @Override
    public String toString() {
        return BytesInternal.toString(this);
    }

    @Override
    public void nativeRead(long position, long address, long size)
            throws BufferUnderflowException, IllegalStateException {
        // TODO add bounds checking.
        memoryCopyMemory(addressForRead(position), address, size);
    }

    @Override
    public void nativeWrite(long address, long position, long size)
            throws BufferOverflowException, IllegalStateException {
        // TODO add bounds checking.
        memoryCopyMemory(address, addressForWrite(position), size);
    }

    void write8bit(long position, char[] chars, int offset, int length)
            throws IllegalStateException {
        long addr = address + translate(position);
        @Nullable Memory memory = this.memory;
        for (int i = 0; i < length; i++)
            memory.writeByte(addr + i, (byte) chars[offset + i]);
    }

    @Override
    public long write8bit(long position, BytesStore bs) {
        long addressForWrite = addressForWrite(position);
        final long length = bs.readRemaining();

        addressForWrite = BytesUtil.writeStopBit(addressForWrite, length);

        if (bs.isDirectMemory()) {
            copy8bit(bs.addressForRead(bs.readPosition()), addressForWrite, length);
        } else {
            final Object o = bs.underlyingObject();
            if (o instanceof byte[])
                copy8bit((byte[]) o, bs.readPosition(), addressForWrite, length);
            else
                BytesUtil.copy8bit(bs, addressForWrite, length);
        }
        return addressForWrite + length;
    }

    @Override
    public long write8bit(long position, String s, int start, int length) {
        position = BytesUtil.writeStopBit(this, position, length);
        MEMORY.copy8bit(s, start, length, addressForWrite(position));
        return position + length;
    }

    private void copy8bit(byte[] arr, long readPosition, long addressForWrite, long readRemaining) {
        int readOffset = Math.toIntExact(readPosition);
        int length = Math.toIntExact(readRemaining);
        MEMORY.copyMemory(arr, readOffset, addressForWrite, length);
    }

    private void copy8bit(long addressForRead, long addressForWrite, long length) {
        OS.memory().copyMemory(addressForRead, addressForWrite, length);
    }

    void read8bit(long position, char[] chars, int length) {
        long addr = address + translate(position);
        Memory memory = this.memory;
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
        try {
            return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore) obj);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public void setAddress(long address) {
        if ((address & ~0x3FFF) == 0)
            throw new AssertionError("Invalid addressForRead " + Long.toHexString(address));
        this.address = address;
    }

    @Deprecated(/* to be removed in x.22 */)
    public long appendUTF(long pos, char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
        return appendUtf8(pos, chars, offset, length);
    }

    public long appendUtf8(long pos, char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
        if (pos + length > realCapacity())
            throw new BufferOverflowException();

        long address = this.address + translate(0);
        @Nullable Memory memory = this.memory;
        if (memory == null) throw new NullPointerException();
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
                UnsafeMemory.unsafePutInt(address + pos, value);
                pos += 4;
            }
            for (; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                UnsafeMemory.unsafePutByte(address + pos++, (byte) c);
            }

            return pos;
        }
        return appendUtf8a(pos, chars, offset, length, i);
    }

    private long appendUtf8a(long pos, char[] chars, int offset, int length, int i)
            throws IllegalStateException {
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
    public long copyTo(@NotNull BytesStore store)
            throws IllegalStateException {
        if (store.isDirectMemory())
            return copyToDirect(store);
        else
            return super.copyTo(store);
    }

    public long copyToDirect(@NotNull BytesStore store)
            throws IllegalStateException {
        long toCopy = Math.min(limit, store.safeLimit());
        if (toCopy > 0) {
            try {
                long addr = address;
                long addr2 = store.addressForWrite(0);
                memoryCopyMemory(addr, addr2, toCopy);
            } catch (BufferOverflowException e) {
                throw new AssertionError(e);
            }
        }
        return toCopy;
    }

    @NotNull
    @Override
    public ByteBuffer toTemporaryDirectByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(0);
        try {
            BB_ADDRESS.setLong(bb, address);
            BB_CAPACITY.setInt(bb, Maths.toUInt31(readRemaining()));
            BB_ATT.set(bb, this);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        bb.clear();
        return bb;
    }

    @Override
    public int byteCheckSum()
            throws IORuntimeException {
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
        final long address = this.address + translate(offsetInRDI);
        for (i = 0; i < len - 7; i += 8)
            UnsafeMemory.unsafePutLong(bytes, i, memory.readLong(address + i));
        if (i < len - 3) {
            UnsafeMemory.unsafePutInt(bytes, i, memory.readInt(address + i));
            i += 4;
        }
        for (; i < len; i++)
            UnsafeMemory.unsafePutByte(bytes, i, memory.readByte(address + i));
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
        int ret = translate >= limit ? -1 :
                memory.readByte(address2) & 0xFF;
//        last.writeVolatileByte(88, (byte) 0xFF);
//        last.writeLong(24, Thread.currentThread().getId());
        return ret;
    }

    @Override
    public int fastHash(long offset, int length)
            throws BufferUnderflowException, IllegalStateException {
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
        return limit;
    }

    @Override
    public boolean isEqual(long start, long length, String s) {
        if (s == null || s.length() != length)
            return false;
        return MEMORY.isEqual(addressForRead(start), s, (int) length);
    }

    static class Deallocator implements Runnable {

        private final long size;
        private volatile long address;

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

    private class Finalizer {
        @Override
        protected void finalize()
                throws Throwable {
            super.finalize();
            warnAndReleaseIfNotReleased();
        }
    }
}
