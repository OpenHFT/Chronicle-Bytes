/*
 * Copyright (c) 2016-2022 chronicle.software
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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.RandomDataInput;
import net.openhft.chronicle.bytes.VanillaBytes;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.util.Ints;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

public final class SingletonEmptyByteStore implements EmptyByteStore {

    private static final String OP_WITH_NO_CONTENT = "This operation is not supported for an EmptyByteStore with no content.";
    public static final EmptyByteStore INSTANCE = new SingletonEmptyByteStore();
    private static final int HASH_CODE_VALUE = BytesStoreHash.hash32(INSTANCE);

    private SingletonEmptyByteStore() {
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
    public boolean tryReserve(ReferenceOwner owner) {
        return false;
    }

    @Override
    public boolean reservedBy(ReferenceOwner owner) {
        return true;
    }

    @NotNull
    @Override
    public EmptyByteStore writeByte(@NonNegative long offset,
                                    byte i8) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeShort(@NonNegative long offset,
                                     short i) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeInt(@NonNegative long offset,
                                   int i) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeOrderedInt(@NonNegative long offset,
                                          int i) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeLong(@NonNegative long offset,
                                    long i) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeOrderedLong(@NonNegative long offset,
                                           long i) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeFloat(@NonNegative long offset,
                                     float d) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeDouble(@NonNegative long offset,
                                      double d) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeVolatileByte(@NonNegative long offset,
                                            byte i8) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeVolatileShort(@NonNegative long offset,
                                             short i16) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeVolatileInt(@NonNegative long offset,
                                           int i32) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore writeVolatileLong(@NonNegative long offset,
                                            long i64) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore write(@NonNegative final long offsetInRDO,
                                final byte[] byteArray,
                                @NonNegative final int offset,
                                @NonNegative final int length) {
        requireNonNegative(offsetInRDO);
        requireNonNull(byteArray);
        Ints.requireNonNegative(offset);
        Ints.requireNonNegative(length);
        throw newUnsupportedOperationException();
    }

    @Override
    public void write(@NonNegative final long offsetInRDO,
                      @NotNull final ByteBuffer bytes,
                      @NonNegative final int offset,
                      @NonNegative final int length) {
        requireNonNegative(offsetInRDO);
        requireNonNull(bytes);
        Ints.requireNonNegative(offset);
        Ints.requireNonNegative(length);
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public EmptyByteStore write(@NonNegative final long writeOffset,
                                @NotNull final RandomDataInput bytes,
                                @NonNegative final long readOffset,
                                @NonNegative final long length) {
        requireNonNegative(writeOffset);
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        requireNonNegative(readOffset);
        requireNonNegative(length);
        throw newUnsupportedOperationException();
    }

    @Override
    public byte readByte(@NonNegative long offset) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public int peekUnsignedByte(@NonNegative long offset) {
        requireNonNegative(offset);
        return -1;
    }

    @Override
    public short readShort(@NonNegative long offset) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public int readInt(@NonNegative long offset) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public long readLong(@NonNegative long offset) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public float readFloat(@NonNegative long offset) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public double readDouble(@NonNegative long offset) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public byte readVolatileByte(@NonNegative long offset)
            throws BufferUnderflowException {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public short readVolatileShort(@NonNegative long offset)
            throws BufferUnderflowException {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public int readVolatileInt(@NonNegative long offset)
            throws BufferUnderflowException {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public long readVolatileLong(@NonNegative long offset)
            throws BufferUnderflowException {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public boolean isDirectMemory() {
        return false;
    }

    @NotNull
    @Override
    public EmptyByteStore copy() {
        return this;
    }

    @Override
    public @NotNull Bytes<Void> bytesForRead() throws IllegalStateException {
        return VanillaBytes.vanillaBytes();
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
        requireNonNegative(offset);
        return false;
    }

    @Override
    public boolean inside(@NonNegative long offset,
                          @NonNegative long buffer) {
        requireNonNegative(offset);
        requireNonNegative(buffer);
        return false;
    }

    @Override
    public long copyTo(@NotNull final BytesStore store) {
        requireNonNull(store);
        // nothing to copy.
        return 0L;
    }

    @Override
    public void nativeWrite(long address,
                            @NonNegative long position,
                            @NonNegative long size) {
        requireNonNegative(position);
        requireNonNegative(size);
        throw newUnsupportedOperationException();
    }

    @Override
    public long write8bit(@NonNegative long position,
                          @NotNull BytesStore bs) {
        requireNonNegative(position);
        requireNonNull(bs);
        throw new BufferOverflowException();
    }

    @Override
    public long write8bit(@NonNegative long position,
                          @NotNull String s,
                          @NonNegative int start,
                          @NonNegative int length) {
        requireNonNegative(position);
        requireNonNull(s);
        Ints.requireNonNegative(start);
        Ints.requireNonNegative(length);
        throw new BufferOverflowException();
    }

    @Override
    public void nativeRead(@NonNegative long position,
                           long address,
                           @NonNegative long size) {
        requireNonNegative(position);
        requireNonNegative(size);
        throw newUnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapInt(@NonNegative long offset,
                                     int expected,
                                     int value) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public void testAndSetInt(@NonNegative long offset,
                              int expected,
                              int value) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapLong(@NonNegative long offset,
                                      long expected,
                                      long value) {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public boolean equalBytes(@NotNull final BytesStore bytesStore,
                              final long length) {
        requireNonNull(bytesStore);
        requireNonNegative(length);
        return length == 0 || bytesStore.length() == 0;
    }

    @Override
    public void move(@NonNegative long from,
                     @NonNegative long to,
                     @NonNegative long length) {
        requireNonNegative(from);
        requireNonNegative(to);
        requireNonNegative(length);
        throw newUnsupportedOperationException();
    }

    @Override
    public long addressForRead(@NonNegative long offset)
            throws BufferUnderflowException {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public long addressForWrite(@NonNegative long offset)
            throws BufferOverflowException {
        requireNonNegative(offset);
        throw newUnsupportedOperationException();
    }

    @Override
    public long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException {
        throw newUnsupportedOperationException();
    }

    @NotNull
    @Override
    public Bytes<Void> bytesForWrite() {
        throw newUnsupportedOperationException();
    }

    @Override
    public boolean sharedMemory() {
        return false;
    }

    @Override
    public @NotNull EmptyByteStore zeroOut(final long start,
                                           final long end) throws IllegalStateException {
        if (start == 0 && end == 0) {
            return this;
        }
        throw new BufferOverflowException();
    }

    @Override
    public boolean isImmutableEmptyByteStore() {
        return true;
    }

    // CharSequence methods

    @NotNull
    @Override
    public String toString() {
        // Seen as a CharSequence, this is an empty String
        return "";
    }

    @NotNull
    @Override
    public IntStream chars() {
        return IntStream.empty();
    }

    @NotNull
    @Override
    public IntStream codePoints() {
        return IntStream.empty();
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public char charAt(int index) throws IndexOutOfBoundsException {
        throw newIndexOutOfBoundsException();
    }

    @Override
    public @NotNull CharSequence subSequence(int start,
                                             int end) {
        throw newIndexOutOfBoundsException();
    }

    // Object

    @Override
    public int hashCode() {
        return HASH_CODE_VALUE;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BytesStore)) {
            return false;
        }
        final BytesStore<?, ?> other = (BytesStore<?, ?>) obj;
        return other.length() == 0;
    }

    private IndexOutOfBoundsException newIndexOutOfBoundsException() {
        return newException(IndexOutOfBoundsException::new);
    }

    private UnsupportedOperationException newUnsupportedOperationException() {
        return newException(UnsupportedOperationException::new);
    }

    private <X extends Exception> X newException(@NotNull final Function<? super String, ? extends X> constructor) {
        return constructor.apply(OP_WITH_NO_CONTENT);
    }


}