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

import net.openhft.chronicle.core.ReferenceCounted;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * BytesStore to wrap memory mapped data.
 */
public class ReadOnlyMappedBytesStore extends MappedBytesStore {

    public ReadOnlyMappedBytesStore(ReferenceCounted owner, long start, long address, long capacity, long safeCapacity)
            throws IllegalStateException {
        super(owner, start, address, capacity, safeCapacity);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> zeroOut(long start, long end) throws IllegalArgumentException {
        checkReadOnly();
        return super.zeroOut(start, end);
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        checkReadOnly();
        return super.compareAndSwapInt(offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        checkReadOnly();
        return super.compareAndSwapLong(offset, expected, value);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeByte(long offset, byte i8) {
        checkReadOnly();
        return super.writeByte(offset, i8);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeShort(long offset, short i16) {
        checkReadOnly();
        return super.writeShort(offset, i16);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeInt(long offset, int i32) {
        checkReadOnly();
        return super.writeInt(offset, i32);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeLong(long offset, long i64) {
        checkReadOnly();
        return super.writeLong(offset, i64);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeOrderedLong(long offset, long i) {
        checkReadOnly();
        return super.writeOrderedLong(offset, i);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeFloat(long offset, float f) {
        checkReadOnly();
        return super.writeFloat(offset, f);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeDouble(long offset, double d) {
        checkReadOnly();
        return super.writeDouble(offset, d);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeVolatileByte(long offset, byte i8) {
        checkReadOnly();
        return super.writeVolatileByte(offset, i8);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeVolatileShort(long offset, short i16) {
        checkReadOnly();
        return super.writeVolatileShort(offset, i16);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeVolatileInt(long offset, int i32) {
        checkReadOnly();
        return super.writeVolatileInt(offset, i32);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeVolatileLong(long offset, long i64) {
        checkReadOnly();
        return super.writeVolatileLong(offset, i64);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> write(long offsetInRDO, byte[] bytes, int offset, int length) {
        checkReadOnly();
        return super.write(offsetInRDO, bytes, offset, length);
    }

    @Override
    public void write(long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length) {
        checkReadOnly();
        super.write(offsetInRDO, bytes, offset, length);
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> write(long offsetInRDO, @NotNull RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException {
        checkReadOnly();
        return super.write(offsetInRDO, bytes, offset, length);
    }

    @Override
    public void write0(long offsetInRDO, @NotNull RandomDataInput bytes, long offset, long length) {
        checkReadOnly();
        super.write0(offsetInRDO, bytes, offset, length);
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        checkReadOnly();
        super.nativeWrite(address, position, size);
    }

    @Override
    void write8bit(long position, char[] chars, int offset, int length) {
        checkReadOnly();
        super.write8bit(position, chars, offset, length);
    }

    @Override
    public long appendUTF(long pos, char[] chars, int offset, int length) {
        checkReadOnly();
        return super.appendUTF(pos, chars, offset, length);
    }

    @Override
    public long appendUtf8(long pos, char[] chars, int offset, int length) {
        checkReadOnly();
        return super.appendUtf8(pos, chars, offset, length);
    }

    private void checkReadOnly() {
        throw new IllegalStateException("Read Only");
    }

    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite() throws IllegalStateException {
        checkReadOnly();
        return super.bytesForWrite();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeOrderedInt(long offset, int i) {
        checkReadOnly();
        return super.writeOrderedInt(offset, i);
    }
}
