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

package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.RandomDataInput;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Wrapper for Heap ByteBuffers and arrays.
 *
 * @param <U> Underlying type
 */
@SuppressWarnings("restriction")
public class HeapBytesStore<U>
        extends AbstractBytesStore<HeapBytesStore<U>, U> {
    @NotNull
    private final Object realUnderlyingObject;
    private final int dataOffset;
    private final long capacity;
    @NotNull
    private final U underlyingObject;
    private UnsafeMemory memory = UnsafeMemory.MEMORY;

    private HeapBytesStore(@NotNull ByteBuffer byteBuffer) {
        super(false);
        //noinspection unchecked
        this.underlyingObject = (U) byteBuffer;
        this.realUnderlyingObject = byteBuffer.array();
        this.dataOffset = Jvm.arrayByteBaseOffset() + byteBuffer.arrayOffset();
        this.capacity = byteBuffer.capacity();
    }

    private HeapBytesStore(@NotNull byte @NotNull [] byteArray) {
        super(false);
        //noinspection unchecked
        this.underlyingObject = (U) byteArray;
        this.realUnderlyingObject = byteArray;
        this.dataOffset = Jvm.arrayByteBaseOffset();
        this.capacity = byteArray.length;
    }

    private HeapBytesStore(Object object, long start, long length) {
        super(false);
        this.underlyingObject = (U) object;
        this.realUnderlyingObject = object;
        this.dataOffset = Math.toIntExact(start);
        this.capacity = length;
    }

    public static <T> HeapBytesStore<T> forFields(Object o, String groupName, int padding) {
        final BytesFieldInfo lookup = BytesFieldInfo.lookup(o.getClass());
        final long start = lookup.startOf(groupName);
        final long length = lookup.lengthOf(groupName);
        return new HeapBytesStore<>(o, start + padding, length - padding);
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
        throwExceptionIfReleased();
        try {
            memory.copyMemory(realUnderlyingObject, dataOffset + from, realUnderlyingObject, dataOffset + to, Maths.toUInt31(length));
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public String toString() {
        return BytesInternal.toString(this);
    }

    @NotNull
    @Override
    public BytesStore<HeapBytesStore<U>, U> copy() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    protected void performRelease() {
        memory = null;
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @NotNull
    @Override
    public U underlyingObject() {
        return underlyingObject;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        try {
            return memory.compareAndSwapInt(realUnderlyingObject, dataOffset + offset, expected, value);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value)
            throws IllegalStateException {
        try {
            memory.testAndSetInt(realUnderlyingObject, dataOffset + offset, expected, value);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        try {
            return memory.compareAndSwapLong(
                    realUnderlyingObject, dataOffset + offset, expected, value);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public byte readByte(long offset)
            throws BufferUnderflowException {
        try {
            return memory.readByte(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public short readShort(long offset)
            throws BufferUnderflowException {
        try {
            return memory.readShort(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public int readInt(long offset)
            throws BufferUnderflowException {
        try {
            return memory.readInt(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public long readLong(long offset)
            throws BufferUnderflowException {
        try {
            return memory.readLong(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public float readFloat(long offset)
            throws BufferUnderflowException {
        try {
            return memory.readFloat(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public double readDouble(long offset)
            throws BufferUnderflowException {
        try {
            return memory.readDouble(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public byte readVolatileByte(long offset)
            throws BufferUnderflowException {
        try {
            return memory.readVolatileByte(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public short readVolatileShort(long offset)
            throws BufferUnderflowException {
        try {
            return memory.readVolatileShort(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public int readVolatileInt(long offset)
            throws BufferUnderflowException {
        try {
            throwExceptionIfReleased();
            return memory.readVolatileInt(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public long readVolatileLong(long offset)
            throws BufferUnderflowException {
        try {
            throwExceptionIfReleased();
            return memory.readVolatileLong(realUnderlyingObject, dataOffset + offset);
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public long write8bit(long position, @NotNull BytesStore bs) {
        requireNonNull(bs);
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
    public long write8bit(long position, @NotNull String s, int start, int length) {
        requireNonNegative(position);
        requireNonNull(s);
        requireNonNegative(start);
        requireNonNegative(length);
        try {
            throwExceptionIfReleased();
            position = BytesInternal.writeStopBit(this, position, length);
            memory.write8bit(s, start, realUnderlyingObject, dataOffset + position, length);
            return position + length;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeByte(long offset, byte b)
            throws BufferOverflowException {
        try {
            throwExceptionIfReleased();
            memory.writeByte(realUnderlyingObject, dataOffset + offset, b);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeShort(long offset, short i16)
            throws BufferOverflowException {
        try {
            throwExceptionIfReleased();
            memory.writeShort(realUnderlyingObject, dataOffset + offset, i16);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeInt(long offset, int i32)
            throws BufferOverflowException {
        try {
            throwExceptionIfReleased();
            memory.writeInt(realUnderlyingObject, dataOffset + offset, i32);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeOrderedInt(long offset, int i32)
            throws BufferOverflowException {
        try {
            throwExceptionIfReleased();
            memory.writeOrderedInt(realUnderlyingObject, dataOffset + offset, i32);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeLong(long offset, long i64)
            throws BufferOverflowException {
        try {
            throwExceptionIfReleased();
            memory.writeLong(realUnderlyingObject, dataOffset + offset, i64);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeOrderedLong(long offset, long i)
            throws BufferOverflowException {
        try {
            throwExceptionIfReleased();
            memory.writeOrderedLong(realUnderlyingObject, dataOffset + offset, i);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeFloat(long offset, float f)
            throws BufferOverflowException {
        try {
            memory.writeFloat(realUnderlyingObject, dataOffset + offset, f);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeDouble(long offset, double d)
            throws BufferOverflowException {
        try {
            memory.writeDouble(realUnderlyingObject, dataOffset + offset, d);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeVolatileByte(long offset, byte i8)
            throws BufferOverflowException {
        try {
            memory.writeVolatileByte(realUnderlyingObject, dataOffset + offset, i8);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeVolatileShort(long offset, short i16)
            throws BufferOverflowException {
        try {
            memory.writeVolatileShort(realUnderlyingObject, dataOffset + offset, i16);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeVolatileInt(long offset, int i32)
            throws BufferOverflowException {
        try {
            memory.writeVolatileInt(realUnderlyingObject, dataOffset + offset, i32);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> writeVolatileLong(long offset, long i64)
            throws BufferOverflowException {
        try {
            memory.writeVolatileLong(realUnderlyingObject, dataOffset + offset, i64);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> write(@NonNegative final long offsetInRDO,
                                   final byte[] byteArray,
                                   @NonNegative final int offset,
                                   @NonNegative final int length) throws BufferOverflowException {
        requireNonNegative(offsetInRDO);
        requireNonNull(byteArray);
        requireNonNegative(offset);
        requireNonNegative(length);
        try {
            memory.copyMemory(
                    byteArray, offset, realUnderlyingObject, this.dataOffset + offsetInRDO, length);
            return this;
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @Override
    public void write(
            long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length)
            throws BufferOverflowException {
        try {
            assert realUnderlyingObject == null || dataOffset >= (Jvm.is64bit() ? 12 : 8);
            if (bytes.isDirect()) {
                memory.copyMemory(Jvm.address(bytes), realUnderlyingObject,
                        this.dataOffset + offsetInRDO, length);

            } else {
                memory.copyMemory(bytes.array(), offset, realUnderlyingObject,
                        this.dataOffset + offsetInRDO, length);
            }
        } catch (NullPointerException ifReleased) {
            throwExceptionIfReleased();
            throw ifReleased;
        }
    }

    @NotNull
    @Override
    public HeapBytesStore<U> write(long writeOffset,
                                   @NotNull RandomDataInput bytes, long readOffset, long length)
            throws IllegalStateException, BufferUnderflowException, BufferOverflowException {
        requireNonNegative(writeOffset);
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        requireNonNegative(readOffset);
        requireNonNegative(length);
        throwExceptionIfReleased();
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
        requireNonNull(bytes);
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
        requireNonNegative(offset);
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
        return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore) obj);
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }
}
