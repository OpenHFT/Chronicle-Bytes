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

import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Fast unchecked version of AbstractBytes
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UncheckedNativeBytes<Underlying>
        extends AbstractReferenceCounted
        implements Bytes<Underlying> {
    protected final long capacity;
    @NotNull
    private final Bytes<Underlying> underlyingBytes;
    @NotNull
    protected NativeBytesStore<Underlying> bytesStore;
    protected long readPosition;
    protected long writePosition;
    protected long writeLimit;
    private int lastDecimalPlaces = 0;
    private boolean lastNumberHadDigits = false;

    public UncheckedNativeBytes(@NotNull Bytes<Underlying> underlyingBytes)
            throws IllegalStateException {
        this.underlyingBytes = underlyingBytes;
        underlyingBytes.reserve(this);
        this.bytesStore = (NativeBytesStore<Underlying>) underlyingBytes.bytesStore();
        assert bytesStore.start() == 0;
        writePosition = underlyingBytes.writePosition();
        writeLimit = underlyingBytes.writeLimit();
        readPosition = underlyingBytes.readPosition();
        capacity = bytesStore.capacity();
    }

    @Override
    public void ensureCapacity(long desiredCapacity)
            throws IllegalArgumentException, IllegalStateException {
        if (desiredCapacity > realCapacity()) {
            underlyingBytes.ensureCapacity(desiredCapacity);
            bytesStore = (NativeBytesStore<Underlying>) underlyingBytes.bytesStore();
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
    public Bytes<Underlying> unchecked(boolean unchecked) {
        return this;
    }

    @Override
    public void move(long from, long to, long length)
            throws IllegalStateException, BufferUnderflowException {
        bytesStore.move(from - start(), to - start(), length);
    }

    @NotNull
    @Override
    public Bytes<Underlying> compact() {
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
    public Bytes<Underlying> readPosition(long position) {
        readPosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readLimit(long limit) {
        writePosition = limit;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writePosition(long position) {
        writePosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readSkip(long bytesToSkip) {
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
    public Bytes<Underlying> writeSkip(long bytesToSkip) {
        writePosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLimit(long limit) {
        writeLimit = limit;
        return this;
    }

    @NotNull
    @Override
    public BytesStore<Bytes<Underlying>, Underlying> copy() {
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
        return readPosition -= substracting;
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull RandomDataInput bytes, long offset, long length)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
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
    public Bytes<Underlying> clear() {
        readPosition = writePosition = start();
        writeLimit = capacity();
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> clearAndPad(long length)
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
    public Underlying underlyingObject() {
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
        return bytesStore.memory.readByte(bytesStore.address + offset) & 0xFF;
    }

    @Override
    public int uncheckedReadUnsignedByte() {
        return readUnsignedByte();
    }

    @Override
    public byte readByte() {
        long offset = readOffsetPositionMoved(1);
        return bytesStore.memory.readByte(bytesStore.address + offset);
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
    public Bytes<Underlying> writeByte(long offset, byte i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeShort(long offset, short i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLong(long offset, long i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        bytesStore.writeLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedLong(long offset, long i)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeFloat(long offset, float d)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeDouble(long offset, double d)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeVolatileByte(long offset, byte i8)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 1);
        bytesStore.writeVolatileByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeVolatileShort(long offset, short i16)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 2);
        bytesStore.writeVolatileShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeVolatileInt(long offset, int i32)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 4);
        bytesStore.writeVolatileInt(offset, i32);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeVolatileLong(long offset, long i64)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offset, 8);
        bytesStore.writeVolatileLong(offset, i64);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Underlying> write(long offsetInRDO, byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    public void write(long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
    }

    @Override
    @NotNull
    public Bytes<Underlying> write(long writeOffset,
                                   @NotNull RandomDataInput bytes, long readOffset, long length)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        writeCheckOffset(writeOffset, length);
        bytesStore.write(writeOffset, bytes, readOffset, length);
        return this;
    }

    void writeCheckOffset(long offset, long adding)
            throws BufferOverflowException {
//        assert writeCheckOffset0(offset, adding);
    }

    @Override
    public byte readByte(long offset) {
        return bytesStore.readByte(offset);
    }

    @Override
    public int readUnsignedByte(long offset) {
        return bytesStore.memory.readByte(bytesStore.address + offset) & 0xFF;
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
    public Bytes<Underlying> writeByte(byte i8) {
        long offset = writeOffsetPositionMoved(1);
        bytesStore.memory.writeByte(bytesStore.address + offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteByte(byte i8) {
        long offset = prewriteOffsetPositionMoved(1);
        bytesStore.memory.writeByte(bytesStore.address + offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeShort(short i16)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteShort(short i16)
            throws IllegalStateException {
        long offset = prewriteOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeInt(int i)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeIntAdv(int i, int advance)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(4, advance);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteInt(int i)
            throws IllegalStateException {
        long offset = prewriteOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLong(long i64)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLongAdv(long i64, int advance)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(8, advance);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteLong(long i64)
            throws IllegalStateException {
        long offset = prewriteOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeFloat(float f)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeFloat(offset, f);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeDouble(double d)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeDoubleAndInt(double d, int i)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(12);
        bytesStore.writeDouble(offset, d);
        bytesStore.writeInt(offset + 8, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException, ArrayIndexOutOfBoundsException {
        if (length + offset > bytes.length)
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + bytes.length + ", " +
                    "length=" + length + ", offset=" + offset);
        if (length > writeRemaining())
            throw new BufferOverflowException();
        long offsetInRDO = writeOffsetPositionMoved(length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewrite(@NotNull byte[] bytes)
            throws IllegalStateException, BufferOverflowException {
        long offsetInRDO = prewriteOffsetPositionMoved(bytes.length);
        bytesStore.write(offsetInRDO, bytes);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewrite(@NotNull BytesStore bytes)
            throws IllegalStateException, BufferOverflowException {
        long offsetInRDO = prewriteOffsetPositionMoved(bytes.length());
        bytesStore.write(offsetInRDO, bytes);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeSome(@NotNull ByteBuffer buffer)
            throws IllegalStateException {
        bytesStore.write(writePosition, buffer, buffer.position(), buffer.limit());
        writePosition += buffer.remaining();
        assert writePosition <= writeLimit();
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedInt(int i)
            throws IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedLong(long i)
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
        return BytesStoreHash.hash32(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bytes)) return false;
        @NotNull Bytes b2 = (Bytes) obj;
        long remaining = readRemaining();
        try {
            return b2.readRemaining() == remaining && equalsBytes(b2, remaining);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public boolean equalsBytes(@NotNull Bytes b2, long remaining)
            throws IllegalStateException {
        long i = 0;
        try {
            for (; i < remaining - 7; i += 8)
                if (readLong(readPosition() + i) != b2.readLong(b2.readPosition() + i))
                    return false;
            for (; i < remaining; i++)
                if (readByte(readPosition() + i) != b2.readByte(b2.readPosition() + i))
                    return false;

        } catch (BufferUnderflowException e) {
            throw Jvm.rethrow(e);
        }
        return true;
    }

    @NotNull
    @Override
    public String toString() {
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
        @Nullable NativeBytesStore bytesStore = (NativeBytesStore) bytesStore();
        return bytesStore.byteCheckSum(readPosition(), readLimit());
    }

    @Override
    @NotNull
    public Bytes<Underlying> append8bit(@NotNull CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs);
        }
        int length = cs.length();
        long offset = writeOffsetPositionMoved(length);
        long address = bytesStore.addressForWrite(offset);
        @Nullable Memory memory = bytesStore.memory;
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
    public Bytes<Underlying> appendUtf8(char[] chars, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        ensureCapacity(writePosition + length * 3L);
        @NotNull NativeBytesStore nbs = this.bytesStore;
        long position = nbs.appendUtf8(writePosition(), chars, offset, length);
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
    public Bytes<Underlying> write(@NotNull RandomDataInput bytes)
            throws IllegalStateException {
        assert bytes != this : "you should not write to yourself !";

        try {
            return write(bytes, bytes.readPosition(), Math.min(writeRemaining(), bytes.readRemaining()));
        } catch (BufferOverflowException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long write8bit(long position, BytesStore bs) {
        return bytesStore.write8bit(position, bs);
    }

    @Override
    public long write8bit(long position, String s, int start, int length) {
        return bytesStore.write8bit(position, s, start, length);
    }

    @Override
    public Bytes<Underlying> write8bit(@Nullable BytesStore bs) throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
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
    public @NotNull Bytes<Underlying> write8bit(@NotNull String s, int start, int length) {
        long toWriteLength = UnsafeMemory.INSTANCE.stopBitLength(length) + length;
        long position = writeOffsetPositionMoved(toWriteLength, 0);
        bytesStore.write8bit(position, s, start, length);
        writePosition += toWriteLength;
        return this;
    }
}
