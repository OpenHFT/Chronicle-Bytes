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
 * applied after a Bytes has been released and cannot be used.
 *
 * @see NoBytesStore
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public enum ReleasedBytesStore implements BytesStore {
    RELEASED_BYTES_STORE;

    @NotNull
    public static <T, B extends BytesStore<B, T>> BytesStore<B, T> releasedBytesStore() {
        return RELEASED_BYTES_STORE;
    }

    @Override
    public void reserve(ReferenceOwner id)
            throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public void release(ReferenceOwner id)
            throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public int refCount() {
        return 0;
    }

    @Override
    public boolean tryReserve(ReferenceOwner id)
            throws IllegalStateException {
        return false;
    }

    @Override
    public boolean reservedBy(ReferenceOwner owner) {
        return false;
    }

    @Override
    public void releaseLast(ReferenceOwner id)
            throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeByte(long offset, byte i8) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    private IllegalStateException newIllegalStateException() {
        return new IllegalStateException("released");
    }

    @NotNull
    @Override
    public RandomDataOutput writeShort(long offset, short i) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeInt(long offset, int i) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeOrderedInt(long offset, int i) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeLong(long offset, long i) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeOrderedLong(long offset, long i) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeFloat(long offset, float d) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeDouble(long offset, double d) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileByte(long offset, byte i8) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileShort(long offset, short i16) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileInt(long offset, int i32) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileLong(long offset, long i64) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput write(long offsetInRDO, byte[] bytes, int offset, int length) throws IllegalStateException {
        if (length != 0)
            throw newIllegalStateException();
        return this;
    }

    @Override
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public RandomDataOutput write(long writeOffset, RandomDataInput bytes, long readOffset, long length) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public byte readByte(long offset) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public int peekUnsignedByte(long offset) {
        return -1;
    }

    @Override
    public short readShort(long offset) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public int readInt(long offset) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public long readLong(long offset) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public float readFloat(long offset) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public double readDouble(long offset) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public byte readVolatileByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public short readVolatileShort(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public int readVolatileInt(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public long readVolatileLong(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public boolean isDirectMemory() {
        return false;
    }

    @NotNull
    @Override
    public BytesStore copy() {
        return this;
    }

    @Override
    public long capacity() {
        return 0;
    }

    @Override
    public Void underlyingObject() {
        return null;
    }

    @Override
    public boolean inside(long offset) {
        return false;
    }

    @Override
    public boolean inside(long offset, long buffer) {
        return false;
    }

    @Override
    public long copyTo(@NotNull BytesStore store) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public void nativeWrite(long address, long position, long size) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public void nativeRead(long position, long address, long size) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public boolean equalBytes(@NotNull BytesStore bytesStore, long length) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public void move(long from, long to, long length) throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public long addressForRead(long offset)
            throws BufferUnderflowException, IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public long addressForWrite(long offset)
            throws BufferOverflowException, IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException, IllegalStateException {
        throw newIllegalStateException();
    }

    @NotNull
    @Override
    public Bytes bytesForWrite() throws IllegalStateException {
        throw newIllegalStateException();
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }
}
