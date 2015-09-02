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

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wrapper for Heap ByteBuffers and arrays.
 */
@SuppressWarnings("sunapi")
public class HeapBytesStore<Underlying>
        implements BytesStore<HeapBytesStore<Underlying>, Underlying> {
    private static final Memory MEMORY = OS.memory();
    @NotNull
    private final Object realUnderlyingObject;
    private final int dataOffset;
    private final int capacity;
    private final AtomicLong refCount = new AtomicLong(1);
    @NotNull
    private final Underlying underlyingObject;

    private HeapBytesStore(@NotNull ByteBuffer byteBuffer) {
        //noinspection unchecked
        this.underlyingObject = (Underlying) byteBuffer;
        this.realUnderlyingObject = byteBuffer.array();
        this.dataOffset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
        this.capacity = byteBuffer.capacity();
    }

    @NotNull
    static HeapBytesStore<ByteBuffer> wrap(@NotNull ByteBuffer bb) {
        return new HeapBytesStore<>(bb);
    }

    @NotNull
    @Override
    public String toString() {
        try {
            return BytesInternal.toString(this);
        } catch (IllegalStateException | IORuntimeException e) {
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

    private void checkOffset(long offset, int size) throws BufferUnderflowException {
        if (offset < start() || offset + size > capacity) {
            throw new BufferUnderflowException();
        }
    }

    private void writeCheckOffset(long offset, int size) throws BufferOverflowException {
        if (offset < start() || offset + size > capacity) {
            throw new BufferOverflowException();
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeByte(long offset, byte b) throws BufferOverflowException {
        writeCheckOffset(offset, 1);
        MEMORY.writeByte(realUnderlyingObject, dataOffset + offset, b);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeShort(long offset, short i16) throws BufferOverflowException {
        writeCheckOffset(offset, 2);
        MEMORY.writeShort(realUnderlyingObject, dataOffset + offset, i16);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore writeInt(long offset, int i32) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        MEMORY.writeInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeOrderedInt(long offset, int i32) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        MEMORY.writeOrderedInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeLong(long offset, long i64) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        MEMORY.writeLong(realUnderlyingObject, dataOffset + offset, i64);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeOrderedLong(long offset, long i) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        MEMORY.writeOrderedLong(realUnderlyingObject, dataOffset + offset, i);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeFloat(long offset, float f) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        MEMORY.writeFloat(realUnderlyingObject, dataOffset + offset, f);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeDouble(long offset, double d) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        MEMORY.writeDouble(realUnderlyingObject, dataOffset + offset, d);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> write(
            long offsetInRDO, byte[] bytes, int offset, int length) throws BufferOverflowException {
        writeCheckOffset(offset, length);
        MEMORY.copyMemory(
                bytes, offset, realUnderlyingObject, this.dataOffset + offsetInRDO, length);
        return this;
    }

    @Override
    public void write(
            long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length) throws BufferOverflowException {
        writeCheckOffset(offset, length);
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
    public HeapBytesStore<Underlying> write(long offsetInRDO,
                                            RandomDataInput bytes, long offset, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long address(long offset) throws UnsupportedOperationException {
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

    @Override
    public boolean equals(Object obj) {
        try {
            return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore) obj);
        } catch (IORuntimeException e) {
            throw new AssertionError(e);
        }
    }
}
