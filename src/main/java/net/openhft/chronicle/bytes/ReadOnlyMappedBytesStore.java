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
    public NativeBytesStore<Void> zeroOut(long start, long end) {
        return this;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        throw checkReadOnly();
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeByte(long offset, byte i8) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeShort(long offset, short i16) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeInt(long offset, int i32) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeLong(long offset, long i64) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeOrderedLong(long offset, long i) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeFloat(long offset, float f) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeDouble(long offset, double d) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeVolatileByte(long offset, byte i8) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeVolatileShort(long offset, short i16) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeVolatileInt(long offset, int i32) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeVolatileLong(long offset, long i64) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> write(long offsetInRDO, byte[] bytes, int offset, int length) {
        throw checkReadOnly();
    }

    @Override
    public void write(long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> write(long writeOffset, @NotNull RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException {
        throw checkReadOnly();
    }

    @Override
    public void write0(long offsetInRDO, @NotNull RandomDataInput bytes, long offset, long length) {
        throw checkReadOnly();
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        throw checkReadOnly();
    }

    @Override
    void write8bit(long position, char[] chars, int offset, int length) {
        throw checkReadOnly();
    }

    @Override
    public long appendUTF(long pos, char[] chars, int offset, int length) {
        throw checkReadOnly();
    }

    @Override
    public long appendUtf8(long pos, char[] chars, int offset, int length) {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite() throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public NativeBytesStore<Void> writeOrderedInt(long offset, int i) {
        throw checkReadOnly();
    }

    private IllegalStateException checkReadOnly() throws IllegalStateException {
        throw new IllegalStateException("Read Only");
    }

    @Override
    public boolean readWrite() {
        return false;
    }
}
