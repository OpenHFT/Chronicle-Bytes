/*
 * Copyright 2016-2020 Chronicle Software
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

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Wrapper for Heap ByteBuffers and arrays.
 */
@SuppressWarnings("restriction")
public class HeapBytesStore<Underlying>
        extends AbstractBytesStore<HeapBytesStore<Underlying>, Underlying> {
    @Nullable
    private static final Memory MEMORY = OS.memory();
    private final AtomicLong refCount = new AtomicLong(1);
    @NotNull
    private final Object realUnderlyingObject;
    private final int dataOffset;
    private final long capacity;
    @NotNull
    private final Underlying underlyingObject;

    private HeapBytesStore(@NotNull ByteBuffer byteBuffer) {
        //noinspection unchecked
        this.underlyingObject = (Underlying) byteBuffer;
        this.realUnderlyingObject = byteBuffer.array();
        this.dataOffset = Unsafe.ARRAY_BYTE_BASE_OFFSET + byteBuffer.arrayOffset();
        this.capacity = byteBuffer.capacity();
    }

    private HeapBytesStore(@NotNull byte[] byteArray) {
        //noinspection unchecked
        this.underlyingObject = (Underlying) byteArray;
        this.realUnderlyingObject = byteArray;
        this.dataOffset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
        this.capacity = byteArray.length;
    }

    // Used by Chronicle-Map.
    @NotNull
    public static HeapBytesStore<byte[]> wrap(@NotNull byte[] byteArray) {
        return new HeapBytesStore<>(byteArray);
    }

    // Used by Chronicle-Map.
    @NotNull
    public static HeapBytesStore<ByteBuffer> wrap(@NotNull ByteBuffer bb) {
        return new HeapBytesStore<>(bb);
    }

    @Override
    public boolean isDirectMemory() {
        return false;
    }

    @Override
    public void move(long from, long to, long length) {
        if (from < 0 || to < 0) throw new BufferUnderflowException();
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(realUnderlyingObject, Maths.toUInt31(from), realUnderlyingObject, Maths.toUInt31(to), Maths.toUInt31(length));
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

    @NotNull
    @Override
    public BytesStore<HeapBytesStore<Underlying>, Underlying> copy() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void reserve() {
        refCount.incrementAndGet();
    }

    @Override
    public void release() {
        long value = refCount.decrementAndGet();
        assert value >= 0 : "refCount=" + value;
    }

    @Override
    public long refCount() {
        return refCount.get();
    }

    @Override
    public boolean tryReserve() {
        for (; ; ) {
            long value = refCount.get();
            if (value <= 0)
                return false;
            if (refCount.compareAndSet(value, value + 1))
                return true;
        }
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @NotNull
    @Override
    public Underlying underlyingObject() {
        return underlyingObject;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        return MEMORY.compareAndSwapInt(realUnderlyingObject, dataOffset + offset, expected, value);
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value) {
        MEMORY.testAndSetInt(realUnderlyingObject, dataOffset + offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        return MEMORY.compareAndSwapLong(
                realUnderlyingObject, dataOffset + offset, expected, value);
    }

    @Override
    public byte readByte(long offset) throws BufferUnderflowException {
        checkOffset(offset, 1);
        return MEMORY.readByte(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public short readShort(long offset) throws BufferUnderflowException {
        checkOffset(offset, 2);
        return MEMORY.readShort(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public int readInt(long offset) throws BufferUnderflowException {
        checkOffset(offset, 4);
        return MEMORY.readInt(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public long readLong(long offset) throws BufferUnderflowException {
        checkOffset(offset, 8);
        return MEMORY.readLong(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public float readFloat(long offset) throws BufferUnderflowException {
        checkOffset(offset, 4);
        return MEMORY.readFloat(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public double readDouble(long offset) throws BufferUnderflowException {
        checkOffset(offset, 8);
        return MEMORY.readDouble(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public byte readVolatileByte(long offset) throws BufferUnderflowException {
        checkOffset(offset, 1);
        return MEMORY.readVolatileByte(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public short readVolatileShort(long offset) throws BufferUnderflowException {
        checkOffset(offset, 2);
        return MEMORY.readVolatileShort(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public int readVolatileInt(long offset) throws BufferUnderflowException {
        checkOffset(offset, 4);
        return MEMORY.readVolatileInt(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public long readVolatileLong(long offset) throws BufferUnderflowException {
        checkOffset(offset, 8);
        return MEMORY.readVolatileLong(realUnderlyingObject, dataOffset + offset);
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeByte(long offset, byte b)
            throws BufferOverflowException {
        writeCheckOffset(offset, 1);
        MEMORY.writeByte(realUnderlyingObject, dataOffset + offset, b);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeShort(long offset, short i16)
            throws BufferOverflowException {
        writeCheckOffset(offset, 2);
        MEMORY.writeShort(realUnderlyingObject, dataOffset + offset, i16);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeInt(long offset, int i32)
            throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        MEMORY.writeInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeOrderedInt(long offset, int i32)
            throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        MEMORY.writeOrderedInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeLong(long offset, long i64)
            throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        MEMORY.writeLong(realUnderlyingObject, dataOffset + offset, i64);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeOrderedLong(long offset, long i)
            throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        MEMORY.writeOrderedLong(realUnderlyingObject, dataOffset + offset, i);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeFloat(long offset, float f)
            throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        MEMORY.writeFloat(realUnderlyingObject, dataOffset + offset, f);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeDouble(long offset, double d)
            throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        MEMORY.writeDouble(realUnderlyingObject, dataOffset + offset, d);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeVolatileByte(long offset, byte i8)
            throws BufferOverflowException {
        writeCheckOffset(offset, 1);
        MEMORY.writeVolatileByte(realUnderlyingObject, dataOffset + offset, i8);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeVolatileShort(long offset, short i16)
            throws BufferOverflowException {
        writeCheckOffset(offset, 2);
        MEMORY.writeVolatileShort(realUnderlyingObject, dataOffset + offset, i16);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeVolatileInt(long offset, int i32)
            throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        MEMORY.writeVolatileInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeVolatileLong(long offset, long i64)
            throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        MEMORY.writeVolatileLong(realUnderlyingObject, dataOffset + offset, i64);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> write(
            long offsetInRDO, byte[] bytes, int offset, int length) throws BufferOverflowException {
        writeCheckOffset(offsetInRDO, length);
        MEMORY.copyMemory(
                bytes, offset, realUnderlyingObject, this.dataOffset + offsetInRDO, length);
        return this;
    }

    @Override
    public void write(
            long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length) throws BufferOverflowException {
        writeCheckOffset(offsetInRDO, length);
        assert realUnderlyingObject == null || dataOffset >= (Jvm.is64bit() ? 12 : 8);
        if (bytes.isDirect()) {
            MEMORY.copyMemory(((DirectBuffer) bytes).address(), realUnderlyingObject,
                    this.dataOffset + offsetInRDO, length);

        } else {
            MEMORY.copyMemory(bytes.array(), offset, realUnderlyingObject,
                    this.dataOffset + offsetInRDO, length);
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> write(long writeOffset,
                                            @NotNull RandomDataInput bytes, long readOffset, long length) {
        long i;
        for (i = 0; i < length - 7; i += 8) {
            long x = bytes.readLong(readOffset + i);
            writeLong(writeOffset + i, x);
        }
        for (; i < length; i++) {
            byte x = bytes.readByte(readOffset + i);
            writeByte(writeOffset + i, x);
        }
        return this;
    }

    @Override
    public long addressForRead(long offset) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long addressForWrite(long offset) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long addressForWritePosition() throws UnsupportedOperationException, BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void nativeRead(long position, long address, long size) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        throw new UnsupportedOperationException("todo");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore) obj);
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }

    private void checkOffset(long offset, int size) throws BufferUnderflowException {
        checkBounds(offset, size, DecoratedBufferUnderflowException::new);
    }

    private void writeCheckOffset(long offset, int size) throws BufferOverflowException {
        checkBounds(offset, size, DecoratedBufferOverflowException::new);
    }

    private void checkBounds(final long offset, final int size,
                             final Function<String, RuntimeException> exceptionFunction) {
        if (offset < start() || offset + size > capacity) {
            throw exceptionFunction.apply(
                    String.format("Offset: %d, start: %d, size: %d, capacity: %d",
                            offset, start(), size, capacity));
        }
    }
}
