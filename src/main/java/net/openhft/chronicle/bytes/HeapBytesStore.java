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

import net.openhft.chronicle.bytes.internal.BytesFieldInfo;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

/**
 * Wrapper for Heap ByteBuffers and arrays.
 */
@Deprecated(/* to be moved to internal in x.22*/)
@SuppressWarnings("restriction")
public class HeapBytesStore<Underlying>
        extends AbstractBytesStore<HeapBytesStore<Underlying>, Underlying> {
    @NotNull
    private final Object realUnderlyingObject;
    private final int dataOffset;
    private final long capacity;
    @NotNull
    private final Underlying underlyingObject;

    private HeapBytesStore(@NotNull ByteBuffer byteBuffer) {
        super(false);
        //noinspection unchecked
        this.underlyingObject = (Underlying) byteBuffer;
        this.realUnderlyingObject = byteBuffer.array();
        this.dataOffset = Jvm.arrayByteBaseOffset() + byteBuffer.arrayOffset();
        this.capacity = byteBuffer.capacity();
    }

    private HeapBytesStore(@NotNull byte @NotNull [] byteArray) {
        super(false);
        //noinspection unchecked
        this.underlyingObject = (Underlying) byteArray;
        this.realUnderlyingObject = byteArray;
        this.dataOffset = Jvm.arrayByteBaseOffset();
        this.capacity = byteArray.length;
    }

    private HeapBytesStore(Object object, long start, long length) {
        super(false);
        this.underlyingObject = (Underlying) object;
        this.realUnderlyingObject = object;
        this.dataOffset = Math.toIntExact(start);
        this.capacity = length;
    }

    public static <T> HeapBytesStore<T> forFields(Object o, String groupName, int padding) {
        final BytesFieldInfo lookup = BytesFieldInfo.lookup(o.getClass());
        final long start = lookup.startOf(groupName);
        final long length = lookup.lengthOf(groupName);
        return new HeapBytesStore<T>(o, start + padding, length - padding);
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
    public void move(long from, long to, long length)
            throws BufferUnderflowException, ArithmeticException {
        if (from < 0 || to < 0) throw new BufferUnderflowException();
        MEMORY.copyMemory(realUnderlyingObject, dataOffset + from, realUnderlyingObject, dataOffset + to, Maths.toUInt31(length));
    }

    @NotNull
    @Override
    public String toString() {
        return BytesInternal.toString(this);
    }

    @NotNull
    @Override
    public BytesStore<HeapBytesStore<Underlying>, Underlying> copy() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    protected void performRelease() {
        // nothing to do
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
    public void testAndSetInt(long offset, int expected, int value)
            throws IllegalStateException {
        MEMORY.testAndSetInt(realUnderlyingObject, dataOffset + offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        return MEMORY.compareAndSwapLong(
                realUnderlyingObject, dataOffset + offset, expected, value);
    }

    @Override
    public byte readByte(long offset)
            throws BufferUnderflowException {
        return MEMORY.readByte(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public short readShort(long offset)
            throws BufferUnderflowException {
        return MEMORY.readShort(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public int readInt(long offset)
            throws BufferUnderflowException {
        return MEMORY.readInt(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public long readLong(long offset)
            throws BufferUnderflowException {
        return MEMORY.readLong(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public float readFloat(long offset)
            throws BufferUnderflowException {
        return MEMORY.readFloat(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public double readDouble(long offset)
            throws BufferUnderflowException {
        return MEMORY.readDouble(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public byte readVolatileByte(long offset)
            throws BufferUnderflowException {
        return MEMORY.readVolatileByte(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public short readVolatileShort(long offset)
            throws BufferUnderflowException {
        return MEMORY.readVolatileShort(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public int readVolatileInt(long offset)
            throws BufferUnderflowException {
        return MEMORY.readVolatileInt(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public long readVolatileLong(long offset)
            throws BufferUnderflowException {
        return MEMORY.readVolatileLong(realUnderlyingObject, dataOffset + offset);
    }

    @Override
    public long write8bit(long position, BytesStore bs) {
        int length0 = Math.toIntExact(bs.readRemaining());
        position = BytesUtil.writeStopBit(this, position, length0);
        int i = 0;
        for (; i < length0 - 7; i += 8)
            writeLong(position + i, bs.readLong(i));
        for (; i < length0; i++)
            writeByte(position + i, bs.readByte(i));
        return position + length0;
    }

    @Override
    public long write8bit(long position, String s, int start, int length) {
        position = BytesInternal.writeStopBit(this, position, length);
        MEMORY.write8bit(s, start, realUnderlyingObject, dataOffset + position, length);
        return position + length;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeByte(long offset, byte b)
            throws BufferOverflowException {
        MEMORY.writeByte(realUnderlyingObject, dataOffset + offset, b);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeShort(long offset, short i16)
            throws BufferOverflowException {
        MEMORY.writeShort(realUnderlyingObject, dataOffset + offset, i16);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeInt(long offset, int i32)
            throws BufferOverflowException {
        MEMORY.writeInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeOrderedInt(long offset, int i32)
            throws BufferOverflowException {
        MEMORY.writeOrderedInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeLong(long offset, long i64)
            throws BufferOverflowException {
        MEMORY.writeLong(realUnderlyingObject, dataOffset + offset, i64);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeOrderedLong(long offset, long i)
            throws BufferOverflowException {
        MEMORY.writeOrderedLong(realUnderlyingObject, dataOffset + offset, i);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeFloat(long offset, float f)
            throws BufferOverflowException {
        MEMORY.writeFloat(realUnderlyingObject, dataOffset + offset, f);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeDouble(long offset, double d)
            throws BufferOverflowException {
        MEMORY.writeDouble(realUnderlyingObject, dataOffset + offset, d);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeVolatileByte(long offset, byte i8)
            throws BufferOverflowException {
        MEMORY.writeVolatileByte(realUnderlyingObject, dataOffset + offset, i8);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeVolatileShort(long offset, short i16)
            throws BufferOverflowException {
        MEMORY.writeVolatileShort(realUnderlyingObject, dataOffset + offset, i16);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeVolatileInt(long offset, int i32)
            throws BufferOverflowException {
        MEMORY.writeVolatileInt(realUnderlyingObject, dataOffset + offset, i32);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> writeVolatileLong(long offset, long i64)
            throws BufferOverflowException {
        MEMORY.writeVolatileLong(realUnderlyingObject, dataOffset + offset, i64);
        return this;
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> write(
            long offsetInRDO, byte[] bytes, int offset, int length)
            throws BufferOverflowException {
        MEMORY.copyMemory(
                bytes, offset, realUnderlyingObject, this.dataOffset + offsetInRDO, length);
        return this;
    }

    @Override
    public void write(
            long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length)
            throws BufferOverflowException {
        assert realUnderlyingObject == null || dataOffset >= (Jvm.is64bit() ? 12 : 8);
        if (bytes.isDirect()) {
            MEMORY.copyMemory(Jvm.address(bytes), realUnderlyingObject,
                    this.dataOffset + offsetInRDO, length);

        } else {
            MEMORY.copyMemory(bytes.array(), offset, realUnderlyingObject,
                    this.dataOffset + offsetInRDO, length);
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<Underlying> write(long writeOffset,
                                            @NotNull RandomDataInput bytes, long readOffset, long length)
            throws IllegalStateException, BufferUnderflowException, BufferOverflowException {
        if (length == (int) length) {
            int length0 = (int) length;

            int i;
            for (i = 0; i < length0 - 7; i += 8) {
                long x = bytes.readLong(readOffset + i);
                writeLong(writeOffset + i, x);
            }
            for (; i < length0; i++) {
                byte x = bytes.readByte(readOffset + i);
                writeByte(writeOffset + i, x);
            }
        } else {
            writeLongLength(writeOffset, bytes, readOffset, length);
        }
        return this;
    }

    private void writeLongLength(long writeOffset,
                                 @NotNull RandomDataInput bytes, long readOffset, long length)
            throws IllegalStateException, BufferUnderflowException, BufferOverflowException {
        long i;
        for (i = 0; i < length - 7; i += 8) {
            long x = bytes.readLong(readOffset + i);
            writeLong(writeOffset + i, x);
        }
        for (; i < length; i++) {
            byte x = bytes.readByte(readOffset + i);
            writeByte(writeOffset + i, x);
        }
    }

    @Override
    public long addressForRead(long offset)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long addressForWrite(long offset)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException {
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
        try {
            return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore) obj);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }
}
