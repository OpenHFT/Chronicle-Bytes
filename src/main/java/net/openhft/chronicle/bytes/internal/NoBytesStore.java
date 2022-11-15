/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.ReferenceChangeListener;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * This is a ByteStore which uses no space but could be resized to be larger (by replacing it with a ByteStore with space)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class NoBytesStore implements BytesStore {
    public static final BytesStore NO_BYTES_STORE = new NoBytesStore();

    public static final long NO_PAGE;

    @NotNull
    public static final Bytes<?> NO_BYTES;

    static {
        try {
            NO_PAGE = OS.memory().allocate(OS.pageSize());
            NO_BYTES = new NativeBytes<>(noBytesStore());
            IOTools.unmonitor(NO_BYTES);

        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    private NoBytesStore() {
    }

    @NotNull
    public static <T, B extends BytesStore<B, T>> BytesStore<B, T> noBytesStore() {
        return (BytesStore<B, T>) NO_BYTES_STORE;
    }

    @Override
    public void reserve(ReferenceOwner owner)
            throws IllegalStateException {
        // Do nothing
    }

    @Override
    public void release(ReferenceOwner owner)
            throws IllegalStateException {
        // Do nothing
    }

    @Override
    public void releaseLast(ReferenceOwner id)
            throws IllegalStateException {
        // Do nothing
    }

    @Override
    public int refCount() {
        return 1;
    }

    @Override
    public void addReferenceChangeListener(ReferenceChangeListener referenceChangeListener) {
        // Do nothing
    }

    @Override
    public void removeReferenceChangeListener(ReferenceChangeListener referenceChangeListener) {
        // Do nothing
    }

    @Override
    public boolean tryReserve(ReferenceOwner owner) {
        return false;
    }

    @Override
    public boolean reservedBy(ReferenceOwner owner) {
        return true;
    }

    @NotNull
    @Override
    public RandomDataOutput writeByte(@NonNegative long offset, byte i8) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeShort(@NonNegative long offset, short i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeInt(@NonNegative long offset, int i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeOrderedInt(@NonNegative long offset, int i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeLong(@NonNegative long offset, long i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeOrderedLong(@NonNegative long offset, long i) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeFloat(@NonNegative long offset, float d) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeDouble(@NonNegative long offset, double d) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileByte(@NonNegative long offset, byte i8) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileShort(@NonNegative long offset, short i16) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileInt(@NonNegative long offset, int i32) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput writeVolatileLong(@NonNegative long offset, long i64) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput write(@NonNegative final long offsetInRDO,
                                  final byte[] byteArray,
                                  @NonNegative final int offset,
                                  @NonNegative final int length) {
        requireNonNegative(offsetInRDO);
        requireNonNull(byteArray);
        requireNonNegative(offset);
        requireNonNegative(length);
        if (length != 0)
            throw new UnsupportedOperationException();
        return this;
    }

    @Override
    public void write(@NonNegative long offsetInRDO, @NotNull ByteBuffer bytes, @NonNegative int offset, @NonNegative int length) {
        requireNonNull(bytes);
        if (length != 0)
            throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public RandomDataOutput write(@NonNegative long writeOffset, @NotNull RandomDataInput bytes, @NonNegative long readOffset, @NonNegative long length) {
        requireNonNegative(writeOffset);
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        requireNonNegative(readOffset);
        requireNonNegative(length);
        if (length != 0)
            throw new UnsupportedOperationException();
        return this;
    }

    @Override
    public byte readByte(@NonNegative long offset) {
        throw throwBUE(offset);
    }

    private static BufferUnderflowException throwBUE(long offset) {
        requireNonNegative(offset);
        return new BufferUnderflowException();
    }

    @Override
    public int peekUnsignedByte(@NonNegative long offset) {
        return -1;
    }

    @Override
    public short readShort(@NonNegative long offset) {
        throw throwBUE(offset);
    }

    @Override
    public int readInt(@NonNegative long offset) {
        throw throwBUE(offset);
    }

    @Override
    public long readLong(@NonNegative long offset) {
        throw throwBUE(offset);
    }

    @Override
    public float readFloat(@NonNegative long offset) {
        throw throwBUE(offset);
    }

    @Override
    public double readDouble(@NonNegative long offset) {
        throw throwBUE(offset);
    }

    @Override
    public byte readVolatileByte(@NonNegative long offset)
            throws BufferUnderflowException {
        throw throwBUE(offset);
    }

    @Override
    public short readVolatileShort(@NonNegative long offset)
            throws BufferUnderflowException {
        throw throwBUE(offset);
    }

    @Override
    public int readVolatileInt(@NonNegative long offset)
            throws BufferUnderflowException {
        throw throwBUE(offset);
    }

    @Override
    public long readVolatileLong(@NonNegative long offset)
            throws BufferUnderflowException {
        throw throwBUE(offset);
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
    public @NotNull Bytes<?> bytesForRead() throws IllegalStateException {
        return VanillaBytes.wrap(this);
    }

    @Override
    public @NonNegative long capacity() {
        return 0;
    }

    @Override
    public Void underlyingObject() {
        return null;
    }

    @Override
    public boolean inside(@NonNegative long offset) {
        return false;
    }

    @Override
    public boolean inside(@NonNegative long offset, @NonNegative long buffer) {
        return false;
    }

    @Override
    public long copyTo(@NotNull BytesStore store) {
        requireNonNull(store);
        // nothing to copy.
        return 0L;
    }

    @Override
    public void nativeWrite(long address, @NonNegative long position, @NonNegative long size) {
        requireNonNegative((size | position));
        if ((size | position) > 0) throw new BufferOverflowException();
    }

    @Override
    public long write8bit(@NonNegative long position, @NotNull BytesStore bs) {
        requireNonNull(bs);
        requireNonNegative(position);
        throw new BufferOverflowException();
    }

    @Override
    public long write8bit(@NonNegative long position, @NotNull String s, @NonNegative int start, @NonNegative int length) {
        requireNonNull(s);
        requireNonNegative((long) (start | length));
        throw new BufferOverflowException();
    }

    @Override
    public void nativeRead(@NonNegative long position, long address, @NonNegative long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapInt(@NonNegative long offset, int expected, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void testAndSetInt(@NonNegative long offset, int expected, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapLong(@NonNegative long offset, long expected, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equalBytes(@NotNull BytesStore bytesStore, long length) {
        requireNonNull(bytesStore);
        requireNonNegative(length);
        return length == 0;
    }

    @Override
    public void move(@NonNegative long from, @NonNegative long to, @NonNegative long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long addressForRead(@NonNegative long offset)
            throws BufferUnderflowException {
        requireNonNegative(offset);
        throw new BufferOverflowException();
    }

    @Override
    public long addressForWrite(@NonNegative long offset)
            throws BufferOverflowException {
        requireNonNegative(offset);
        throw new BufferOverflowException();
    }

    @Override
    public long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException {
        return addressForWrite(writePosition());
    }

    @NotNull
    @Override
    public Bytes<?> bytesForWrite() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }

    @Override
    public boolean isImmutableEmptyByteStore() {
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BytesStore && ((BytesStore) obj).length() == 0;
    }

    @Override
    public String toString() {
        return "";
    }
}
