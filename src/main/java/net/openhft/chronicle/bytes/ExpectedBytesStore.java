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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * @deprecated to be removed in 1.8
 */
@Deprecated
public class ExpectedBytesStore<B extends BytesStore<B, Underlying>, Underlying> implements BytesStore<B, Underlying> {
    private static final int NOT_COMPLETE = 1 << 31;
    private final BytesStore<B, Underlying> underlyingBytesStore;

    ExpectedBytesStore(BytesStore<B, Underlying> underlyingBytesStore) {
        this.underlyingBytesStore = underlyingBytesStore;
    }

    @Override
    public boolean isDirectMemory() {
        return underlyingBytesStore.isDirectMemory();
    }

    @Override
    public int peekUnsignedByte(long offset) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public BytesStore<B, Underlying> copy() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public long capacity() {
        return underlyingBytesStore.capacity();
    }

    @Nullable
    @Override
    public Underlying underlyingObject() {
        return underlyingBytesStore.underlyingObject();
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void nativeRead(long position, long address, long size) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte readByte(long offset) {
        throw new UnsupportedOperationException();
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
    public int readVolatileInt(long offset) {
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
    public long readVolatileLong(long offset) throws BufferUnderflowException {
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

    @NotNull
    @Override
    public B writeByte(long offset, byte i8) throws AssertionError {
        byte i8a = underlyingBytesStore.readByte(offset);
        if (i8a != i8) {
            Bytes<Underlying> bytes = underlyingBytesStore.bytesForRead();
            bytes.readPosition(offset);
            throw new AssertionError(bytes.toDebugString() + "\nExpected: " + i8a + "\nActual: " + i8);
        }
        return (B) this;
    }

    @Override
    public long address(long offset) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public B writeShort(long offset, short i) throws AssertionError {
        short ia = underlyingBytesStore.readShort(offset);
        if (ia != i)
            throw new AssertionError("Expected: " + ia + "\nActual: " + i);
        return (B) this;
    }

    @NotNull
    @Override
    public B writeInt(long offset, int i) throws AssertionError {
        int ia = underlyingBytesStore.readInt(offset);
        if (ia != i)
            throw new AssertionError("Expected: " + ia + "\nActual: " + i);
        return (B) this;
    }

    @Override
    public void reserve() throws IllegalStateException {
    }

    @NotNull
    @Override
    public B writeOrderedInt(long offset, int i) throws AssertionError {
            int ia = underlyingBytesStore.readInt(offset);
            if (ia != i) {
                if ((i & NOT_COMPLETE) == 0)
                    throw new AssertionError("Expected: " + ia + " <" + Integer.toHexString(ia) + ">\nActual: " + i + " <" + Integer.toHexString(i) + ">");
            }
            return (B) this;
    }

    @Override
    public void release() throws IllegalStateException {
    }

    @NotNull
    @Override
    public B writeLong(long offset, long i) throws AssertionError {
            long ia = underlyingBytesStore.readLong(offset);
            if (ia != i)
                throw new AssertionError("Expected: " + ia + "\nActual: " + i);
            return (B) this;
    }

    @Override
    public long refCount() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public B writeOrderedLong(long offset, long i) throws AssertionError {
        return writeLong(offset, i);
    }

    @NotNull
    @Override
    public B writeFloat(long offset, float d) throws AssertionError {
            float ia = underlyingBytesStore.readFloat(offset);
            if (ia != d)
                throw new AssertionError("Expected: " + ia + "\nActual: " + d);
            return (B) this;
    }

    @NotNull
    @Override
    public B writeDouble(long offset, double d) throws AssertionError {
            double ia = underlyingBytesStore.readDouble(offset);
            if (ia != d)
                throw new AssertionError("Expected: " + ia + "\nActual: " + d);
            return (B) this;
    }

    @NotNull
    @Override
    public B writeVolatileByte(long offset, byte i8) throws BufferOverflowException {
        return writeByte(offset, i8);
    }

    @NotNull
    @Override
    public B writeVolatileShort(long offset, short i16) throws BufferOverflowException {
        return writeShort(offset, i16);
    }

    @NotNull
    @Override
    public B writeVolatileInt(long offset, int i32) throws BufferOverflowException {
        return writeInt(offset, i32);
    }

    @NotNull
    @Override
    public B writeVolatileLong(long offset, long i64) throws BufferOverflowException {
        return writeLong(offset, i64);
    }

    @NotNull
    @Override
    public B write(long offsetInRDO, byte[] bytes, int offset, int length) throws AssertionError, BufferOverflowException {
        for (int i = 0; i < length; i++)
            writeByte(offsetInRDO + i, bytes[offset + i]);
        return (B) this;
    }

    @Override
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public B write(long offsetInRDO, RandomDataInput bytes, long offset, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(long from, long to, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }
}
