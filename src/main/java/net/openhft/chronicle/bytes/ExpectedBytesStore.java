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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public class ExpectedBytesStore<B extends BytesStore<B, Underlying>, Underlying> implements BytesStore<B, Underlying> {
    private static final int NOT_READY = 1 << 31;
    private final BytesStore<B, Underlying> underlyingBytesStore;

    ExpectedBytesStore(BytesStore<B, Underlying> underlyingBytesStore) {
        this.underlyingBytesStore = underlyingBytesStore;
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
            if ((i & NOT_READY) == 0)
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
        long ia = underlyingBytesStore.readLong(offset);
        if (ia != i)
            throw new AssertionError("Expected: " + ia + "\nActual: " + i);
        return (B) this;
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
    public B write(long offsetInRDO, byte[] bytes, int offset, int length) throws AssertionError {
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
}
