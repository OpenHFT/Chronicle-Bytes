/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;

import java.nio.ByteBuffer;

/**
 * Created by peter.lawrey on 24/02/15.
 */
public enum NoBytesStore implements BytesStore {
    NO_BYTES_STORE;

    public static final long NO_PAGE = OS.memory().allocate(OS.pageSize());
    public static final Bytes NO_BYTES = new VanillaBytes(noBytesStore());

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



    @Override
    public RandomDataOutput writeByte(long offset, byte i8) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput writeShort(long offset, short i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput writeInt(long offset, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput writeOrderedInt(long offset, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput writeLong(long offset, long i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput writeOrderedLong(long offset, long i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput writeFloat(long offset, float d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput writeDouble(long offset, double d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput write(long offsetInRDO, byte[] bytes, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RandomDataOutput write(long offsetInRDO, Bytes bytes, long offset, long length) {
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
    public boolean inStore(long offset) {
        return false;
    }

    @Override
    public void copyTo(BytesStore store) {
        // nothing to copy.
    }

    @Override
    public Access access() {
        throw new UnsupportedOperationException();
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
    public long address() throws UnsupportedOperationException {
        return NO_PAGE;
    }

    @Override
    public Object accessHandle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long accessOffset(long randomOffset) {
        throw new UnsupportedOperationException();
    }

}
