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

import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public enum NoBytesStore implements BytesStore {
    NO_BYTES_STORE;

    public static final long NO_PAGE = OS.memory().allocate(OS.pageSize());
    public static final Bytes NO_BYTES = new VanillaBytes(noBytesStore());

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
        return 1L;
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
    public RandomDataOutput write(long offsetInRDO, byte[] bytes, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput write(long offsetInRDO, RandomDataInput bytes, long offset, long length) {
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
    public void copyTo(BytesStore store) {
        // nothing to copy.
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
    public boolean equalBytes(BytesStore b, long remaining) {
        return remaining == 0;
    }

    @Override
    public long address(long offset) throws UnsupportedOperationException {
        if (offset != 0)
            throw new IllegalArgumentException("offset: " + offset);
        return NO_PAGE;
    }

    @NotNull
    @Override
    public Bytes bytesForWrite() {
        throw new UnsupportedOperationException("todo");
    }


}
