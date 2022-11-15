/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.*;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.cleaner.CleanerServiceLocator;
import net.openhft.chronicle.core.cleaner.spi.ByteBufferCleanerService;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Longs;
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
import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

@SuppressWarnings({"restriction", "rawtypes", "unchecked"})
public class NativeBytesStore<U>
        extends AbstractBytesStore<NativeBytesStore<U>, U> {
    private static final long MEMORY_MAPPED_SIZE = 128 << 10;
    private static final Field BB_ADDRESS;
    private static final Field BB_CAPACITY;
    private static final Field BB_ATT;
    private static final ByteBufferCleanerService CLEANER_SERVICE = CleanerServiceLocator.cleanerService();

    static {
        Class directBB = ByteBuffer.allocateDirect(0).getClass();
        BB_ADDRESS = Jvm.getField(directBB, "address");
        BB_CAPACITY = Jvm.getField(directBB, "capacity");
        BB_ATT = Jvm.getField(directBB, "att");
    }

    // Even though not referenced, this field needs to stay
    // TODO: Rework using a non-finalizer solution
    private final Finalizer finalizer;
    public long address;
    // on release, set this to null.
    public Memory memory = OS.memory();
    public long maximumLimit;
    protected long limit;
    @Nullable
    private SimpleCleaner cleaner;
    private boolean elastic;
    @Nullable
    private U underlyingObject;

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
            long address, @NonNegative long limit, @Nullable Runnable deallocator, boolean elastic) {
        this(address, limit, deallocator, elastic, false);
    }

    protected NativeBytesStore(
            long address, @NonNegative long limit, @Nullable Runnable deallocator, boolean elastic, boolean monitored) {
        super(monitored);
        setAddress(address);
        this.limit = limit;
        this.maximumLimit = elastic ? MAX_CAPACITY : limit;
        this.cleaner = deallocator == null ? null : new SimpleCleaner(deallocator);
        underlyingObject = null;
        this.elastic = elastic;
        if (cleaner == null) {
            finalizer = null;
        } else {
            finalizer = new Finalizer();
        }
    }

    /**
     * @param bb ByteBuffer
     * @return BytesStore
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
     */
    @NotNull
    public static NativeBytesStore<Void> nativeStore(@NonNegative long capacity)
            throws IllegalArgumentException {
        return of(capacity, true, true);
    }

    @NotNull
    private static NativeBytesStore<Void> of(@NonNegative long capacity, boolean zeroOut, boolean elastic)
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

    @NotNull
    public static NativeBytesStore<Void> nativeStoreWithFixedCapacity(@NonNegative long capacity)
            throws IllegalArgumentException {
        return of(capacity, true, false);
    }

    @NotNull
    public static NativeBytesStore<Void> lazyNativeBytesStoreWithFixedCapacity(@NonNegative long capacity)
            throws IllegalArgumentException {
        return of(capacity, false, false);
    }

    @NotNull
    public static NativeBytesStore<ByteBuffer> elasticByteBuffer() {
        return elasticByteBuffer(OS.pageSize(), MAX_CAPACITY);
    }

    @NotNull
    public static NativeBytesStore<ByteBuffer> elasticByteBuffer(@NonNegative int size, @NonNegative long maxSize) {
        return new NativeBytesStore<>(ByteBuffer.allocateDirect(size), true, Math.toIntExact(maxSize));
    }

    @NotNull
    public static NativeBytesStore from(@NotNull String text) {
        return from(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    @NotNull
    public static NativeBytesStore from(byte[] bytes) {
        try {
            @NotNull NativeBytesStore nbs = nativeStoreWithFixedCapacity(bytes.length);
            nbs.write(0, bytes);
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
    public boolean canReadDirect(@NonNegative long length) {
        return limit >= length;
    }

    // Used in Chronicle Map
    public void init(@NotNull ByteBuffer bb, boolean elastic) {
        this.elastic = elastic;
        underlyingObject = (U) bb;
        setAddress(Jvm.address(bb));
        this.limit = bb.capacity();
    }

    // Used in Chronicle Map
    public void uninit() {
        underlyingObject = null;
        address = 0;
        limit = 0;
        maximumLimit = 0;
        cleaner = null;
    }

    @Override
    public void move(@NonNegative long from, @NonNegative long to, @NonNegative long length)
            throws BufferUnderflowException, IllegalStateException {
        if (from < 0 || to < 0) throw new IllegalArgumentException();
        long addr = this.address;
        if (addr == 0) throwException(null);
        memoryCopyMemory(addr + from, addr + to, length);
    }

    private void memoryCopyMemory(long fromAddress, long toAddress, @NonNegative long length)
            throws IllegalStateException {
        if (length < 0) throw new IllegalArgumentException();
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
    public BytesStore<NativeBytesStore<U>, U> copy()
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
    public VanillaBytes<U> bytesForWrite()
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
    public @NonNegative long realCapacity() {
        return limit;
    }

    @Override
    public @NonNegative long capacity() {
        return maximumLimit;
    }

    @Nullable
    @Override
    public U underlyingObject() {
        return underlyingObject;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> zeroOut(@NonNegative long start, @NonNegative long end) {
        if (end <= start)
            return this;
        if (start < start())
            start = start();
        if (end > capacity())
            end = capacity();

        // don't dirty cache lines unnecessarily
        long addr = this.address + translate(start);
        long size = end - start;
        // align the start
        while ((addr & 0x7) != 0 && size > 0) {
            memory.writeByte(addr, (byte) 0);
            addr++;
            size--;
        }
        long i = 0;
        for (; i < size - 7; i += 8)
            if (memory.readLong(addr + i) != 0)
                memory.writeLong(addr + i, 0);

        for (; i < size; i++)
            memory.writeByte(addr + i, (byte) 0);
        return this;
    }

    @Override
    public boolean compareAndSwapInt(@NonNegative long offset, int expected, int value)
            throws IllegalStateException {
        return memory.compareAndSwapInt(address + translate(offset), expected, value);
    }

    @Override
    public void testAndSetInt(@NonNegative long offset, int expected, int value)
            throws IllegalStateException {
        memory.testAndSetInt(address + translate(offset), offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(@NonNegative long offset, long expected, long value)
            throws IllegalStateException {
        return memory.compareAndSwapLong(address + translate(offset), expected, value);
    }

    @Override
    public long addAndGetLong(@NonNegative long offset, long adding)
            throws BufferUnderflowException {
        return memory.addLong(address + translate(offset), adding);
    }

    @Override
    public int addAndGetInt(@NonNegative long offset, int adding)
            throws BufferUnderflowException {
        return memory.addInt(address + translate(offset), adding);
    }

    public long translate(@NonNegative long offset) {
        return offset;
    }

    @Override
    public byte readByte(@NonNegative long offset) {
        return memory.readByte(address + translate(offset));
    }

    @Override
    public int readUnsignedByte(@NonNegative long offset)
            throws BufferUnderflowException {
        return readByte(offset) & 0xFF;
    }

    @Override
    public short readShort(@NonNegative long offset) {
        return memory.readShort(address + translate(offset));
    }

    @Override
    public int readInt(@NonNegative long offset) {
        return memory.readInt(address + translate(offset));
    }

    @Override
    public long readLong(@NonNegative long offset) {
        long addr = this.address;
        assert addr != 0;
        return memory.readLong(addr + translate(offset));
    }

    @Override
    public float readFloat(@NonNegative long offset) {
        return memory.readFloat(address + translate(offset));
    }

    @Override
    public double readDouble(@NonNegative long offset) {
        return memory.readDouble(address + translate(offset));
    }

    @Override
    public byte readVolatileByte(@NonNegative long offset) {
        return memory.readVolatileByte(address + translate(offset));
    }

    @Override
    public short readVolatileShort(@NonNegative long offset) {
        return memory.readVolatileShort(address + translate(offset));
    }

    @Override
    public int readVolatileInt(@NonNegative long offset) {
        return memory.readVolatileInt(address + translate(offset));
    }

    @Override
    public long readVolatileLong(@NonNegative long offset) {
        return memory.readVolatileLong(address + translate(offset));
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeByte(@NonNegative long offset, byte i8)
            throws IllegalStateException {
        memory.writeByte(address + translate(offset), i8);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeShort(@NonNegative long offset, short i16)
            throws IllegalStateException {
        memory.writeShort(address + translate(offset), i16);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeInt(@NonNegative long offset, int i32)
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
    public NativeBytesStore<U> writeOrderedInt(@NonNegative long offset, int i)
            throws IllegalStateException {
        memory.writeOrderedInt(address + translate(offset), i);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeLong(@NonNegative long offset, long i64)
            throws IllegalStateException {
        memory.writeLong(address + translate(offset), i64);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeOrderedLong(@NonNegative long offset, long i)
            throws IllegalStateException {
        memory.writeOrderedLong(address + translate(offset), i);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeFloat(@NonNegative long offset, float f)
            throws IllegalStateException {
        memory.writeFloat(address + translate(offset), f);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeDouble(@NonNegative long offset, double d)
            throws IllegalStateException {
        memory.writeDouble(address + translate(offset), d);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeVolatileByte(@NonNegative long offset, byte i8)
            throws IllegalStateException {
        memory.writeVolatileByte(address + translate(offset), i8);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeVolatileShort(@NonNegative long offset, short i16)
            throws IllegalStateException {
        memory.writeVolatileShort(address + translate(offset), i16);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeVolatileInt(@NonNegative long offset, int i32)
            throws IllegalStateException {
        memory.writeVolatileInt(address + translate(offset), i32);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> writeVolatileLong(@NonNegative long offset, long i64)
            throws IllegalStateException {
        memory.writeVolatileLong(address + translate(offset), i64);
        return this;
    }

    @NotNull
    @Override
    public NativeBytesStore<U> write(@NonNegative final long offsetInRDO,
                                     final byte[] byteArray,
                                     @NonNegative final int offset,
                                     @NonNegative final int length) throws IllegalStateException {
        requireNonNegative(offsetInRDO);
        requireNonNull(byteArray);
        Longs.requireNonNegative(offset);
        Longs.requireNonNegative(length);
        memory.copyMemory(byteArray, offset, address + translate(offsetInRDO), length);
        return this;
    }

    @Override
    public void write(
            @NonNegative long offsetInRDO, @NotNull ByteBuffer bytes, @NonNegative int offset, @NonNegative int length)
            throws IllegalStateException {
        if (bytes.isDirect()) {
            memoryCopyMemory(Jvm.address(bytes) + offset, address + translate(offsetInRDO), length);

        } else {
            memory.copyMemory(bytes.array(), offset, address + translate(offsetInRDO), length);
        }
    }

    @NotNull
    @Override
    public NativeBytesStore<U> write(
            @NonNegative long writeOffset, @NotNull RandomDataInput bytes, @NonNegative long readOffset, @NonNegative long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        requireNonNegative(writeOffset);
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        requireNonNegative(readOffset);
        requireNonNegative(length);
        throwExceptionIfReleased();
        if (bytes.isDirectMemory()) {
            memoryCopyMemory(bytes.addressForRead(readOffset), addressForWrite(writeOffset), length);
        } else {
            write0(writeOffset, bytes, readOffset, length);
        }
        return this;
    }

    public void write0(@NonNegative long offsetInRDO, @NotNull RandomDataInput bytes, @NonNegative long offset, @NonNegative long length)
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
    public long addressForRead(@NonNegative long offset)
            throws BufferUnderflowException {
        if (offset < start() || offset > realCapacity()) {
            if (offset < 0) throw new IllegalArgumentException();
            throw new BufferUnderflowException();
        }
        return address + translate(offset);
    }

    @Override
    public long addressForWrite(@NonNegative long offset)
            throws BufferOverflowException {
        if (offset < start() || offset > realCapacity()) {
            if (offset < 0) throw new IllegalArgumentException();
            throw new BufferOverflowException();
        }
        return address + translate(offset);
    }

    @Override
    public long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException {
        return addressForWrite(start());
    }

    @Override
    protected void backgroundPerformRelease() {
        // eagerly clear
        memory = null;
        super.backgroundPerformRelease();
    }

    @Override
    protected void performRelease() {
        memory = null;
        if (cleaner != null) {
            cleaner.clean();

        } else if (underlyingObject instanceof ByteBuffer) {
            ByteBuffer underlying = (ByteBuffer) this.underlyingObject;
            if (underlying.isDirect())
                CLEANER_SERVICE.clean(underlying);
        }
    }

    @NotNull
    @Override
    public String toString() {
        return BytesInternal.toString(this);
    }

    @Override
    public void nativeRead(@NonNegative long position, long address, @NonNegative long size)
            throws BufferUnderflowException, IllegalStateException {
        memoryCopyMemory(addressForRead(position), address, size);
    }

    @Override
    public void nativeWrite(long address, @NonNegative long position, @NonNegative long size)
            throws BufferOverflowException, IllegalStateException {
        memoryCopyMemory(address, addressForWrite(position), size);
    }

    void write8bit(@NonNegative long position, char[] chars, @NonNegative int offset, @NonNegative int length)
            throws IllegalStateException {
        long addr = address + translate(position);
        @Nullable Memory mem = this.memory;
        for (int i = 0; i < length; i++)
            mem.writeByte(addr + i, (byte) chars[offset + i]);
    }

    @Override
    public long write8bit(@NonNegative long position, @NotNull BytesStore bs) {
        requireNonNegative(position);
        final long length = bs.readRemaining();
        long addressForWrite = addressForWrite(position);

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
    public long write8bit(@NonNegative long position, @NotNull String s, @NonNegative int start, @NonNegative int length) {
        requireNonNegative(position);
        requireNonNull(s);
        requireNonNegative(start);
        requireNonNegative(length);
        position = BytesUtil.writeStopBit(this, position, length);
        MEMORY.copy8bit(s, start, length, addressForWrite(position));
        return position + length;
    }

    private void copy8bit(byte[] arr, @NonNegative long readPosition, long addressForWrite, @NonNegative long readRemaining) {
        requireNonNull(arr);
        int readOffset = Math.toIntExact(readPosition);
        int length = Math.toIntExact(readRemaining);
        MEMORY.copyMemory(arr, readOffset, addressForWrite, length);
    }

    private void copy8bit(long addressForRead, long addressForWrite, @NonNegative long length) {
        OS.memory().copyMemory(addressForRead, addressForWrite, length);
    }

    public void read8bit(@NonNegative long position, char[] chars, @NonNegative int length) {
        long addr = address + translate(position);
        Memory mem = this.memory;
        for (int i = 0; i < length; i++)
            chars[i] = (char) (mem.readByte(addr + i) & 0xFF);
    }

    @Override
    public long readIncompleteLong(@NonNegative long offset) {
        int remaining = (int) Math.min(8, readRemaining() - offset);
        long l = 0;
        for (int i = 0; i < remaining; i++) {
            byte b = memory.readByte(address + offset + i);
            l |= (long) (b & 0xFF) << (i * 8);
        }
        return l;
    }

    public void setAddress(long address) {
        if ((address & ~0x3FFF) == 0)
            throw new AssertionError("Invalid addressForRead " + Long.toHexString(address));
        this.address = address;
    }

    public long appendUtf8(@NonNegative long pos, char[] chars, @NonNegative int offset, @NonNegative int length)
            throws BufferOverflowException, IllegalStateException {
        requireNonNull(chars);
        if (pos + length > realCapacity())
            throw new BufferOverflowException();

        long addr = this.address + translate(0);
        @Nullable Memory mem = this.memory;
        if (mem == null) throw new NullPointerException();
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
                UnsafeMemory.unsafePutInt(addr + pos, value);
                pos += 4;
            }
            for (; i < length; i++) {
                char c = chars[offset + i];
                if (c > 0x007F)
                    break ascii;
                UnsafeMemory.unsafePutByte(addr + pos++, (byte) c);
            }

            return pos;
        }
        return appendUtf8a(pos, chars, offset, length, i);
    }

    private long appendUtf8a(@NonNegative long pos, char[] chars, @NonNegative int offset, @NonNegative int length, int i)
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

    @Override
    public int byteCheckSum(@NonNegative long position, @NonNegative long limit) {
        @Nullable Memory mem = this.memory;
        assert mem != null;
        int b = 0;
        long ptr = address + position;
        long end = address + limit;
        for (; ptr < end - 7; ptr += 8) {
            b += mem.readByte(ptr)
                    + mem.readByte(ptr + 1)
                    + mem.readByte(ptr + 2)
                    + mem.readByte(ptr + 3)
                    + mem.readByte(ptr + 4)
                    + mem.readByte(ptr + 5)
                    + mem.readByte(ptr + 6)
                    + mem.readByte(ptr + 7);
        }
        for (; ptr < end; ptr++) {
            b += mem.readByte(ptr);
        }
        return b & 0xFF;
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }

    @Override
    public long read(@NonNegative long offsetInRDI, byte[] bytes, @NonNegative int offset, @NonNegative int length) {
        requireNonNull(bytes);
        int len = Maths.toUInt31(Math.min(length, requireNonNegative(readLimit() - offsetInRDI)));

        memory.readBytes(this.address + translate(offsetInRDI), bytes, offset, len);
        return len;
    }

    @Override
    public int peekUnsignedByte(@NonNegative long offset) {
        final long addr = this.address;
        @Nullable final Memory mem = this.memory;
        final long translate = translate(offset);
        final long address2 = addr + translate;
        return translate < start() || limit <= translate
                ? -1
                : mem.readByte(address2) & 0xFF;
    }

    @Override
    public int fastHash(@NonNegative long offset, @NonNegative int length)
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
    public boolean isEqual(@NonNegative long start, @NonNegative long length, String s) {
        if (s == null || s.length() != length)
            return false;
        return MEMORY.isEqual(addressForRead(start), s, (int) length);
    }

    // Explicitly overrides because this class adds properties which triggers static analyzing warnings unless
    // this method is overridden
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // Explicitly overrides because this class adds properties which triggers static analyzing warnings unless
    // this method is overridden
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    static final class Deallocator implements Runnable {

        private final long size;
        private volatile long address;

        Deallocator(long address, @NonNegative long size) {
            assert address != 0;
            this.address = address;
            this.size = size;
        }

        @Override
        public void run() {
            if (address == 0)
                return;
            long addressToFree = address;
            address = 0;
            OS.memory().freeMemory(addressToFree, size);
        }
    }

    private final class Finalizer {
        @Override
        protected void finalize()
                throws Throwable {
            super.finalize();
            warnAndReleaseIfNotReleased();
        }
    }
}
