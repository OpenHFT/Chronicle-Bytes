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

import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * BytesStore to wrap memory mapped data.
 */
public class ReadOnlyMappedBytesStore extends MappedBytesStore {

    public ReadOnlyMappedBytesStore(ReferenceOwner owner, MappedFile mappedFile, long start, long address, long capacity, long safeCapacity)
            throws IllegalStateException {
        super(owner, mappedFile, start, address, capacity, safeCapacity);
    }

    @NotNull
    @Override
    public MappedBytesStore zeroOut(long start, long end) {
        return this;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeByte(long offset, byte i8)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeShort(long offset, short i16)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeInt(long offset, int i32)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeLong(long offset, long i64)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeOrderedLong(long offset, long i)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeFloat(long offset, float f)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeDouble(long offset, double d)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileByte(long offset, byte i8)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileShort(long offset, short i16)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileInt(long offset, int i32)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeVolatileLong(long offset, long i64)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore write(long offsetInRDO, byte[] bytes, int offset, int length)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @Override
    public void write(long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore write(long writeOffset, @NotNull RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        throw checkReadOnly();
    }

    @Override
    public void write0(long offsetInRDO, @NotNull RandomDataInput bytes, long offset, long length)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @Override
    public void nativeWrite(long address, long position, long size)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @Deprecated
    @Override
    public long appendUTF(long pos, char[] chars, int offset, int length)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @Override
    public long appendUtf8(long pos, char[] chars, int offset, int length)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite()
            throws IllegalStateException {
        throw checkReadOnly();
    }

    @NotNull
    @Override
    public MappedBytesStore writeOrderedInt(long offset, int i)
            throws IllegalStateException {
        throw checkReadOnly();
    }

    private IllegalStateException checkReadOnly()
            throws IllegalStateException {
        throw new IllegalStateException("Read Only");
    }

    @Override
    public boolean readWrite() {
        return false;
    }
}
