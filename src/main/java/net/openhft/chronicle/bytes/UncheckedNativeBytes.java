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

import net.openhft.chronicle.bytes.internal.*;
import net.openhft.chronicle.bytes.internal.migration.HashCodeEqualsUtil;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Fast unchecked version of AbstractBytes
 *
 * @param <U> Underlying type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UncheckedNativeBytes<U>
        extends AbstractReferenceCounted
        implements Bytes<U>, HasUncheckedRandomDataInput {
    protected final long capacity;
    private final UncheckedRandomDataInput uncheckedRandomDataInput = new UncheckedRandomDataInputHolder();
    @NotNull
    private final Bytes<U> underlyingBytes;
    @NotNull
    protected BytesStore<?, U> bytesStore;
    protected long readPosition;
    protected long writePosition;
    protected long writeLimit;
    private int lastDecimalPlaces = 0;
    private boolean lastNumberHadDigits = false;

    public UncheckedNativeBytes(@NotNull Bytes<U> underlyingBytes)
            throws IllegalStateException {
        this.underlyingBytes = underlyingBytes;
        underlyingBytes.reserve(this);
        this.bytesStore = underlyingBytes.bytesStore();
        assert bytesStore.start() == 0;
        writePosition = underlyingBytes.writePosition();
        readPosition = underlyingBytes.readPosition();
        capacity = bytesStore.realCapacity();
        writeLimit = Math.min(capacity, underlyingBytes.writeLimit());
    }

    @Override
    public void ensureCapacity(long desiredCapacity)
            throws IllegalArgumentException, IllegalStateException {
        if (desiredCapacity > realCapacity()) {
            underlyingBytes.ensureCapacity(desiredCapacity);
            bytesStore = underlyingBytes.bytesStore();
        }
    }

    @Override
    public boolean unchecked() {
        return true;
    }

    @Override
    public boolean isDirectMemory() {
        return true;
    }

    @Override
    @NotNull
    public Bytes<U> unchecked(boolean unchecked) {
        throwExceptionIfReleased();
        return this;
    }

    @Override
    public void move(long from, long to, long length)
            throws IllegalStateException, BufferUnderflowException {
        bytesStore.move(from - start(), to - start(), length);
    }

    @NotNull
    @Override
    public Bytes<U> compact() {
        try {
            long start = start();
            long readRemaining = readRemaining();
            if (readRemaining > 0 && start < readPosition) {
                bytesStore.move(readPosition, start, readRemaining);
                readPosition = start;
                writePosition = readPosition + readRemaining;
            }
            return this;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        } catch (IllegalStateException ignored) {
            return this;
        }
    }

    @NotNull
    @Override
    public Bytes<U> readPosition(long position) {
        readPosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> readLimit(long limit) {
        writePosition = limit;
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writePosition(long position) {
        writePosition = requireNonNegative(position);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> readSkip(long bytesToSkip) {
        readPosition += bytesToSkip;
        return this;
    }

    @Override
    public byte readVolatileByte(long offset)
            throws BufferUnderflowException {
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset)
            throws BufferUnderflowException {
        return bytesStore.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset)
            throws BufferUnderflowException {
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset)
            throws BufferUnderflowException {
        return bytesStore.readVolatileLong(offset);
    }

    @Override
    public void uncheckedReadSkipOne() {
        readPosition++;
    }

    @Override
    public void uncheckedReadSkipBackOne() {
        readPosition--;
    }

    @NotNull
    @Override
    public Bytes<U> writeSkip(long bytesToSkip) {
        writePosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeLimit(long limit) {
        writeLimit = requireNonNegative(limit);
        return this;
    }

    @NotNull
    @Override
    public BytesStore<Bytes<U>, U> copy() {
        throwExceptionIfReleased();
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean isElastic() {
        return false;
    }

    protected long readOffsetPositionMoved(long adding) {
        long offset = readPosition;
        readPosition += adding;
        return offset;
    }

    protected long writeOffsetPositionMoved(long adding) {
        return writeOffsetPositionMoved(adding, adding);
    }

    protected long writeOffsetPositionMoved(long adding, long advance) {
        long oldPosition = writePosition;
        long writeEnd = oldPosition + adding;
        assert writeEnd <= bytesStore.safeLimit();
        writePosition += advance;
        return oldPosition;
    }

    protected long prewriteOffsetPositionMoved(long substracting) {
        readPosition -= substracting;
        return readPosition;
    }

    @NotNull
    @Override
    public Bytes<U> write(@NotNull RandomDataInput bytes, long offset, long length)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        ReferenceCountedUtil.throwExceptionIfReleased(bytes);
        requireNonNegative(offset);
        requireNonNegative(length);
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else if (length >= 16 && bytes.isDirectMemory()) {
            rawCopy(bytes, offset, length);

        } else {
            BytesInternal.writeFully(bytes, offset, length, this);
        }
        return this;
    }

    public long rawCopy(@NotNull RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        long len = Math.min(writeRemaining(), Math.min(bytes.capacity() - offset, length));
        if (len > 0) {
            writeCheckOffset(writePosition(), len);
            this.throwExceptionIfReleased();
            OS.memory().copyMemory(bytes.addressForRead(offset), addressForWritePosition(), len);
            writeSkip(len);
        }
        return len;
    }

    @Override
    @NotNull
    public Bytes<U> clear() {
        readPosition = writePosition = start();
        writeLimit = capacity();
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> clearAndPad(long length)
            throws BufferOverflowException {
        if (start() + length > capacity())
            throw new BufferOverflowException();
        readPosition = writePosition = start() + length;
        writeLimit = capacity();
        return this;
    }

    @Override
    public long readLimit() {
        return writePosition;
    }

    @Override
    public long writeLimit() {
        return writeLimit;
    }

    @Override
    public long realCapacity() {
        return bytesStore.realCapacity();
    }

    @Override
    public long realWriteRemaining() {
        return writeRemaining();
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Nullable
    @Override
    public U underlyingObject() {
        return bytesStore.underlyingObject();
    }

    @Override
    public long readPosition() {
        return readPosition;
    }

    @Override
    public long writePosition() {
        return writePosition;
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        return bytesStore.compareAndSwapInt(offset, expected, value);
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.testAndSetInt(offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    @Override
    protected void performRelease()
            throws IllegalStateException {
        this.underlyingBytes.release(this);
        // need to wait as checks rely on this completing.
        BackgroundResourceReleaser.releasePendingResources();
    }

    @Override
    public int readUnsignedByte() {
        long offset = readOffsetPositionMoved(1);
        return bytesStore.readByte(offset) & 0xFF;
    }

    @Override
    public int uncheckedReadUnsignedByte() {
        return readUnsignedByte();
    }

    @Override
    public byte readByte() {
        long offset = readOffsetPositionMoved(1);
        return bytesStore.readByte(offset);
    }

    @Override
    public int peekUnsignedByte() {
        try {
            return readRemaining() > 0 ? bytesStore.readUnsignedByte(readPosition) : -1;

        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    public short readShort() {
        long offset = readOffsetPositionMoved(2);
        return bytesStore.readShort(offset);
    }

    @Override
    public int readInt() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readInt(offset);
    }

    @Override
    public long readLong() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readLong(offset);
    }

    @Override
    public float readFloat() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readFloat(offset);
    }

    @Override
    public double readDouble() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readDouble(offset);
    }

    @Override
    public int readVolatileInt() {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong() {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readVolatileLong(offset);
    }

    @NotNull
    @Override
    public Bytes<U> writeByte(long offset, byte i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeShort(long offset, short i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeOrderedInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeLong(long offset, long i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        bytesStore.writeLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeOrderedLong(long offset, long i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeFloat(long offset, float d)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeDouble(long offset, double d)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeVolatileByte(long offset, byte i8)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 1);
        bytesStore.writeVolatileByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeVolatileShort(long offset, short i16)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 2);
        bytesStore.writeVolatileShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeVolatileInt(long offset, int i32)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.writeVolatileInt(offset, i32);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeVolatileLong(long offset, long i64)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        bytesStore.writeVolatileLong(offset, i64);
        return this;
    }

    @Override
    @NotNull
    public Bytes<U> write(@NonNegative final long offsetInRDO,
                          final byte[] byteArray,
                          @NonNegative final int offset,
                          @NonNegative int length) throws BufferOverflowException, IllegalStateException {
        requireNonNull(byteArray);
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, byteArray, offset, length);
        return this;
    }

    @Override
    public void write(long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
        requireNonNull(bytes);
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
    }

    @Override
    @NotNull
    public Bytes<U> write(long writeOffset,
                          @NotNull RandomDataInput bytes, long readOffset, long length)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        writeCheckOffset(writeOffset, length);
        bytesStore.write(writeOffset, bytes, readOffset, length);
        return this;
    }

    void writeCheckOffset(long offset, long adding)
            throws BufferOverflowException {

    }

    @Override
    public byte readByte(long offset) {
        return bytesStore.readByte(offset);
    }

    @Override
    public int readUnsignedByte(long offset) {
        return bytesStore.readByte(offset) & 0xFF;
    }

    @Override
    public int peekUnsignedByte(long offset) {
        return offset >= writePosition ? -1 : readByte(offset);
    }

    @Override
    public short readShort(long offset) {
        return bytesStore.readShort(offset);
    }

    @Override
    public int readInt(long offset) {
        return bytesStore.readInt(offset);
    }

    @Override
    public long readLong(long offset) {
        return bytesStore.readLong(offset);
    }

    @Override
    public float readFloat(long offset) {
        return bytesStore.readFloat(offset);
    }

    @Override
    public double readDouble(long offset) {
        return bytesStore.readDouble(offset);
    }

    @NotNull
    @Override
    public Bytes<U> writeByte(byte i8) {
        long offset = writeOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> prewriteByte(byte i8) {
        long offset = prewriteOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeShort(short i16)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> prewriteShort(short i16)
            throws IllegalStateException {
        long offset = prewriteOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeInt(int i)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeIntAdv(int i, int advance)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(4, advance);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> prewriteInt(int i)
            throws IllegalStateException {
        long offset = prewriteOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeLong(long i64)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeLongAdv(long i64, int advance)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(8, advance);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> prewriteLong(long i64)
            throws IllegalStateException {
        long offset = prewriteOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeFloat(float f)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeFloat(offset, f);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeDouble(double d)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeDoubleAndInt(double d, int i)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(12);
        bytesStore.writeDouble(offset, d);
        bytesStore.writeInt(offset + 8, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> write(final byte[] byteArray,
                          @NonNegative final int offset,
                          @NonNegative final int length) throws BufferOverflowException, IllegalStateException, ArrayIndexOutOfBoundsException {
        requireNonNegative(offset);
        requireNonNegative(length);
        if (length + offset > byteArray.length)
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + byteArray.length + ", " +
                    "length=" + length + ", offset=" + offset);
        if (length > writeRemaining())
            throw new BufferOverflowException();
        long offsetInRDO = writeOffsetPositionMoved(length);
        bytesStore.write(offsetInRDO, byteArray, offset, length);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> prewrite(@NotNull byte[] bytes)
            throws IllegalStateException, BufferOverflowException {
        long offsetInRDO = prewriteOffsetPositionMoved(bytes.length);
        bytesStore.write(offsetInRDO, bytes);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> prewrite(@NotNull BytesStore bytes)
            throws IllegalStateException, BufferOverflowException {
        long offsetInRDO = prewriteOffsetPositionMoved(bytes.length());
        bytesStore.write(offsetInRDO, bytes);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeSome(@NotNull ByteBuffer buffer)
            throws IllegalStateException {
        bytesStore.write(writePosition, buffer, buffer.position(), buffer.limit());
        writePosition += buffer.remaining();
        assert writePosition <= writeLimit();
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeOrderedInt(int i)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<U> writeOrderedLong(long i)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @Override
    public long addressForRead(long offset)
            throws BufferUnderflowException {
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(long offset)
            throws BufferOverflowException {
        return bytesStore.addressForWrite(offset);
    }

    @Override
    public long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException {
        return bytesStore.addressForWrite(0);
    }

    @Override
    public int hashCode() {
        return HashCodeEqualsUtil.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BytesStore && BytesInternal.contentEqual(this, (BytesStore) obj);
    }

    @NotNull
    @Override
    public String toString() {
        throwExceptionIfReleased();
        return BytesInternal.toString(this);
    }

    @Override
    public void lenient(boolean lenient) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean lenient() {
        return false;
    }

    @Override
    public void nativeRead(long position, long address, long size)
            throws IllegalStateException, BufferUnderflowException {
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    public void nativeWrite(long address, long position, long size)
            throws IllegalStateException, BufferOverflowException {
        bytesStore.nativeWrite(address, position, size);
    }

    @Nullable
    @Override
    public BytesStore bytesStore() {
        return bytesStore;
    }

    @Override
    public int byteCheckSum()
            throws IORuntimeException {
        @Nullable NativeBytesStore nativeBytesStore = (NativeBytesStore) bytesStore();
        return nativeBytesStore.byteCheckSum(readPosition(), readLimit());
    }

    @Override
    @NotNull
    public Bytes<U> append8bit(@NotNull CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs);
        }
        int length = cs.length();
        long offset = writeOffsetPositionMoved(length);
        long address = bytesStore.addressForWrite(offset);
        @Nullable Memory memory = UnsafeMemory.MEMORY;
        assert memory != null;
        int i = 0;
        for (; i < length - 1; i += 2) {
            char c = cs.charAt(i);
            char c2 = cs.charAt(i + 1);
            memory.writeByte(address + i, (byte) c);
            memory.writeByte(address + i + 1, (byte) c2);
        }
        for (; i < length; i++) {
            char c = cs.charAt(i);
            memory.writeByte(address + i, (byte) c);
        }

        return this;
    }

    @NotNull
    @Override
    public Bytes<U> appendUtf8(char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        long actualUTF8Length = AppendableUtil.findUtf8Length(chars, offset, length);
        ensureCapacity(writePosition + actualUTF8Length);
        @NotNull BytesStore nbs = this.bytesStore;
        long position = ((NativeBytesStore) nbs).appendUtf8(writePosition(), chars, offset, length);
        writePosition(position);
        return this;
    }

    @Override
    public int lastDecimalPlaces() {
        return lastDecimalPlaces;
    }

    @Override
    public void lastDecimalPlaces(int lastDecimalPlaces) {
        this.lastDecimalPlaces = Math.max(0, lastDecimalPlaces);
    }

    @Override
    public boolean lastNumberHadDigits() {
        return lastNumberHadDigits;
    }

    @Override
    public void lastNumberHadDigits(boolean lastNumberHadDigits) {
        this.lastNumberHadDigits = lastNumberHadDigits;
    }

    @NotNull
    @Override
    public Bytes<U> write(@NotNull RandomDataInput bytes)
            throws IllegalStateException {
        assert bytes != this : "you should not write to yourself !";

        try {
            return write(bytes, bytes.readPosition(), Math.min(writeRemaining(), bytes.readRemaining()));
        } catch (BufferOverflowException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long write8bit(long position, @NotNull BytesStore bs) {
        return bytesStore.write8bit(position, bs);
    }

    @Override
    public long write8bit(long position, @NotNull String s, int start, int length) {
        return bytesStore.write8bit(position, s, start, length);
    }

    public Bytes<U> write8bit(@Nullable BytesStore bs) throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
        if (bs == null) {
            BytesInternal.writeStopBitNeg1(this);

        } else {
            final long offset = bs.readPosition();
            final long readRemaining = Math.min(writeRemaining(), bs.readLimit() - offset);
            writeStopBit(readRemaining);
            try {
                write(bs, offset, readRemaining);
            } catch (IllegalArgumentException e) {
                throw new AssertionError(e);
            }
        }
        return this;
    }

    @Override
    public @NotNull Bytes<U> write8bit(final @NotNull String text, final int start, final int length) {
        requireNonNull(text);
        final long toWriteLength = UnsafeMemory.INSTANCE.stopBitLength(length) + (long) length;
        final long position = writeOffsetPositionMoved(toWriteLength, 0);
        bytesStore.write8bit(position, text, start, length);
        writePosition += toWriteLength;
        return this;
    }

    @Override
    public @NotNull UncheckedRandomDataInput acquireUncheckedInput() {
        return uncheckedRandomDataInput;
    }

    private final class UncheckedRandomDataInputHolder implements UncheckedRandomDataInput {

        @Override
        public byte readByte(long offset) {
            return bytesStore.readByte(offset);
        }

        @Override
        public short readShort(long offset) {
            return bytesStore.readShort(offset);
        }

        @Override
        public int readInt(long offset) {
            return bytesStore.readInt(offset);
        }

        @Override
        public long readLong(long offset) {
            return bytesStore.readLong(offset);
        }
    }

}
