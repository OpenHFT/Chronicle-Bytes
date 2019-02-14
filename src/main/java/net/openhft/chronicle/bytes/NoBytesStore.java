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

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

@SuppressWarnings({"rawtypes", "unchecked"})
public enum NoBytesStore implements BytesStore {
    NO_BYTES_STORE;

    public static final long NO_PAGE;

    @NotNull
    public static final Bytes NO_BYTES;

    static {
        try {
            NO_PAGE = OS.memory().allocate(OS.pageSize());
            NO_BYTES = new VanillaBytes(noBytesStore());

        } catch (@NotNull IllegalArgumentException | IllegalStateException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    public static <T, B extends BytesStore<B, T>> BytesStore<B, T> noBytesStore() {
        return NO_BYTES_STORE;
    }

    @Override
    public void reserve() throws IllegalStateException {
    }

    @Override
    public void release() throws IllegalStateException {
    }

    @Override
    public long refCount() {
        return 0L;
    }

    @Override
    public boolean tryReserve() {
        return false;
    }

    @NotNull
    @Override
    public RandomDataOutput writeByte(long offset, byte i8) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeShort(long offset, short i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeInt(long offset, int i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeOrderedInt(long offset, int i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeLong(long offset, long i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeOrderedLong(long offset, long i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeFloat(long offset, float d) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeDouble(long offset, double d) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileByte(long offset, byte i8) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileShort(long offset, short i16) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileInt(long offset, int i32) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileLong(long offset, long i64) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput write(long offsetInRDO, byte[] bytes, int offset, int length) {
        if (length != 0)
            throw new UnsupportedOperationException();
        return this;
    }

    @Override
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        if (length != 0)
            throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput write(long writeOffset, RandomDataInput bytes, long readOffset, long length) {
        if (length != 0)
            throw new UnsupportedOperationException();
        return this;
    }

    @Override
    public byte readByte(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int peekUnsignedByte(long offset) {
        return -1;
    }

    @Override
    public short readShort(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readInt(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long readLong(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float readFloat(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double readDouble(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte readVolatileByte(long offset) throws BufferUnderflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public short readVolatileShort(long offset) throws BufferUnderflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readVolatileInt(long offset) throws BufferUnderflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long readVolatileLong(long offset) throws BufferUnderflowException {
        throw new BufferUnderflowException();
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
    public boolean inside(long offset, int buffer) {
        return false;
    }

    @Override
    public long copyTo(BytesStore store) {
        // nothing to copy.
        return 0L;
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void nativeRead(long position, long address, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equalBytes(BytesStore bytesStore, long length) {
        return length == 0;
    }

    @Override
    public void move(long from, long to, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long addressForRead(long offset) throws BufferUnderflowException {
        if (offset != 0)
            throw new BufferUnderflowException();
        return NO_PAGE;
    }

    @Override
    public long addressForWrite(long offset) throws BufferOverflowException {
        if (offset != 0)
            throw new BufferOverflowException();
        return NO_PAGE;
    }

    @NotNull
    @Override
    public Bytes bytesForWrite() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }
}
