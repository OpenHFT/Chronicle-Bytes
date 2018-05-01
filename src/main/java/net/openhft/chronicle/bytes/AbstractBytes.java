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

import net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.ReferenceCounter;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public abstract class AbstractBytes<Underlying> implements Bytes<Underlying> {
    // used for debugging
    @UsedViaReflection
    private final String name;
    @NotNull
    protected BytesStore<Bytes<Underlying>, Underlying> bytesStore;
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);
    protected long readPosition;
    protected long writePosition;
    protected long writeLimit;
    protected boolean isPresent;
    private int lastDecimalPlaces = 0;
    private volatile Thread threadPositionSetBy;
    private boolean lenient = false;

    AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore, long writePosition, long writeLimit)
            throws IllegalStateException {
        this(bytesStore, writePosition, writeLimit, "");
    }

    AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore, long writePosition, long writeLimit, String name)
            throws IllegalStateException {
        this.bytesStore = bytesStore;
        bytesStore.reserve();
        readPosition = bytesStore.readPosition();
        this.uncheckedWritePosition(writePosition);
        this.writeLimit = writeLimit;
        // used for debugging
        this.name = name;

        assert !bytesStore.isDirectMemory() || BytesUtil.register(this);
    }

    @Override
    public boolean isDirectMemory() {
        return bytesStore.isDirectMemory();
    }

    @Override
    public void move(long from, long to, long length) throws BufferUnderflowException {
        long start = start();
        bytesStore.move(from - start, to - start, length);
    }

    @NotNull
    @Override
    public Bytes<Underlying> compact() {
        try {
            long start = start();
            long readRemaining = readRemaining();
            if ((readRemaining > 0) && (start < readPosition)) {
                bytesStore.move(readPosition, start, readRemaining);
                readPosition = start;
                uncheckedWritePosition(readPosition + readRemaining);
            }
            return this;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void isPresent(boolean isPresent) {
        clear();
        this.isPresent = isPresent;
    }

    @Override
    public boolean isPresent() {
        return isPresent;
    }

    @Override
    @NotNull
    public Bytes<Underlying> clear() {
        long start = start();
        readPosition = start;
        uncheckedWritePosition(start);
        writeLimit = capacity();
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> clearAndPad(long length) throws BufferOverflowException {
        if ((start() + length) > capacity()) {
            throw new DecoratedBufferOverflowException(
                    String.format("clearAndPad failed. Start: %d + length: %d > capacity: %d", start(), length, capacity()));
        }
        long l = start() + length;
        readPosition = l;
        uncheckedWritePosition(l);
        writeLimit = capacity();
        return this;
    }

    @Override
    @ForceInline
    public long readLimit() {
        return writePosition;
    }

    @Override
    @ForceInline
    public long writeLimit() {
        return writeLimit;
    }

    @Override
    @ForceInline
    public long realCapacity() {
        return bytesStore.capacity();
    }

    @Override
    @ForceInline
    public long capacity() {
        return bytesStore.capacity();
    }

    @Nullable
    @Override
    public Underlying underlyingObject() {
        return bytesStore.underlyingObject();
    }

    @Override
    @ForceInline
    public long start() {
        return bytesStore.start();
    }

    @Override
    @ForceInline
    public long readPosition() {
        return readPosition;
    }

    @Override
    @ForceInline
    public long writePosition() {
        return writePosition;
    }

    @Override
    @ForceInline
    public boolean compareAndSwapInt(long offset, int expected, int value) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        return bytesStore.compareAndSwapInt(offset, expected, value);
    }

    @Override
    @ForceInline
    public boolean compareAndSwapLong(long offset, long expected, long value) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> readPosition(long position) throws BufferUnderflowException {
        if (position < start()) {
            throw new DecoratedBufferUnderflowException(String.format("readPosition failed. Position: %d < start: %d", position, start()));
        }
        if (position > readLimit()) {
            throw new DecoratedBufferUnderflowException(
                    String.format("readPosition failed. Position: %d > readLimit: %d", position, readLimit()));
        }
        this.readPosition = position;
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> readLimit(long limit) throws BufferUnderflowException {
        if (limit < start()) {
            return limitLessThanStart(limit);
        } else if (limit > writeLimit()) {
            return limitGreaterThanWriteLimit(limit);
        }

        uncheckedWritePosition(limit);
        return this;
    }

    private Bytes<Underlying> limitGreaterThanWriteLimit(long limit) {
        throw new DecoratedBufferUnderflowException(String.format("readLimit failed. Limit: %d > writeLimit: %d", limit, writeLimit()));
    }

    private Bytes<Underlying> limitLessThanStart(long limit) {
        throw new DecoratedBufferUnderflowException(String.format("readLimit failed. Limit: %d < start: %d", limit, start()));
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writePosition(long position) throws BufferOverflowException {
        if (position > writeLimit()) {
            throw new DecoratedBufferOverflowException(
                    String.format("writePosition failed. Position: %d > writeLimit: %d", position, writeLimit()));
        }
        if (position < start()) {
            throw new DecoratedBufferOverflowException(String.format("writePosition failed. Position: %d < start: %d", position, start()));
        }
        if (position < readPosition()) {
            this.readPosition = position;
        }
        uncheckedWritePosition(position);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> readSkip(long bytesToSkip) throws BufferUnderflowException {
        if (lenient) {
            bytesToSkip = Math.min(bytesToSkip, readRemaining());
        }
        readOffsetPositionMoved(bytesToSkip);
        return this;
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
    @ForceInline
    public Bytes<Underlying> writeSkip(long bytesToSkip) throws BufferOverflowException {
        writeCheckOffset(writePosition, bytesToSkip);
        uncheckedWritePosition(writePosition + bytesToSkip);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeLimit(long limit) throws BufferOverflowException {
        if (limit < start()) {
            throw new DecoratedBufferOverflowException(String.format("writeLimit failed. Limit: %d < start: %d", limit, start()));
        }
        long capacity = capacity();
        if (limit > capacity) {
            assert false : "cant set limit=" + limit + " > " + "capacity=" + capacity;
            throw new DecoratedBufferOverflowException(String.format("writeLimit failed. Limit: %d > capacity: %d", limit, capacity));
        }
        this.writeLimit = limit;
        return this;
    }

    void performRelease() throws IllegalStateException {
        try {
            this.bytesStore.release();
        } finally {
            this.bytesStore = NoBytesStore.noBytesStore();
        }
    }

    @Override
    public int readUnsignedByte() {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readUnsignedByte(offset);

        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    public int readUnsignedByte(long offset) throws BufferUnderflowException {
        return readByte(offset) & 0xFF;
    }

    @Override
    public int uncheckedReadUnsignedByte() {
        try {
            int unsignedByte = bytesStore.readUnsignedByte(readPosition);
            readPosition++;
            return unsignedByte;
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    @ForceInline
    public byte readByte() {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readByte(offset);

        } catch (BufferUnderflowException e) {
            return 0;
        }
    }

    @Override
    @ForceInline
    public int peekUnsignedByte() {
        try {
            return readPosition >= writePosition ? -1 : bytesStore.peekUnsignedByte(readPosition);
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @ForceInline
    public short readShort() throws BufferUnderflowException {
        try {
            long offset = readOffsetPositionMoved(2);
            return bytesStore.readShort(offset);
        } catch (BufferUnderflowException e) {
            if (lenient) {
                return 0;
            }
            throw e;
        }
    }

    @Override
    @ForceInline
    public int readInt() throws BufferUnderflowException {
        try {
            long offset = readOffsetPositionMoved(4);
            return bytesStore.readInt(offset);
        } catch (BufferUnderflowException e) {
            if (lenient) {
                return 0;
            }
            throw e;
        }
    }

    @Override
    public byte readVolatileByte(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 1, true);
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 2, true);
        return bytesStore.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readVolatileLong(offset);
    }

    @Override
    @ForceInline
    public long readLong() throws BufferUnderflowException {
        try {
            long offset = readOffsetPositionMoved(8);
            return bytesStore.readLong(offset);
        } catch (BufferUnderflowException e) {
            if (lenient) {
                return 0;
            }
            throw e;
        }
    }

    @Override
    @ForceInline
    public float readFloat() throws BufferUnderflowException {
        try {
            long offset = readOffsetPositionMoved(4);
            return bytesStore.readFloat(offset);
        } catch (BufferUnderflowException e) {
            if (lenient) {
                return 0;
            }
            throw e;
        }
    }

    @Override
    @ForceInline
    public double readDouble() throws BufferUnderflowException {
        try {
            long offset = readOffsetPositionMoved(8);
            return bytesStore.readDouble(offset);
        } catch (BufferUnderflowException e) {
            if (lenient) {
                return 0;
            }
            throw e;
        }
    }

    @Override
    @ForceInline
    public int readVolatileInt() throws BufferUnderflowException {
        try {
            long offset = readOffsetPositionMoved(4);
            return bytesStore.readVolatileInt(offset);
        } catch (BufferUnderflowException e) {
            if (lenient) {
                return 0;
            }
            throw e;
        }
    }

    @Override
    @ForceInline
    public long readVolatileLong() throws BufferUnderflowException {
        try {
            long offset = readOffsetPositionMoved(8);
            return bytesStore.readVolatileLong(offset);
        } catch (BufferUnderflowException e) {
            if (lenient) {
                return 0;
            }
            throw e;
        }
    }

    protected long readOffsetPositionMoved(long adding) throws BufferUnderflowException {
        long offset = readPosition;
        readCheckOffset(readPosition, adding, false);
        readPosition += adding;
        assert readPosition <= readLimit();
        return offset;
    }

    @Override
    public void reserve() throws IllegalStateException {
        refCount.reserve();
    }

    @Override
    public void release() throws IllegalStateException {
        refCount.release();
    }

    @Override
    public long refCount() {
        return refCount.get();
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeByte(long offset, byte i) throws BufferOverflowException {
        writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeShort(long offset, short i) throws BufferOverflowException {
        writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeInt(long offset, int i) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedInt(long offset, int i) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeLong(long offset, long i) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        bytesStore.writeLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedLong(long offset, long i) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeFloat(long offset, float d) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset, d);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeDouble(long offset, double d) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeVolatileByte(long offset, byte i8) throws BufferOverflowException {
        writeCheckOffset(offset, 1);
        bytesStore.writeVolatileByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeVolatileShort(long offset, short i16) throws BufferOverflowException {
        writeCheckOffset(offset, 2);
        bytesStore.writeVolatileShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeVolatileInt(long offset, int i32) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        bytesStore.writeVolatileInt(offset, i32);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeVolatileLong(long offset, long i64) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        bytesStore.writeVolatileLong(offset, i64);
        return this;
    }

    @Override
    @NotNull
    @ForceInline
    public Bytes<Underlying> write(long offsetInRDO, byte[] bytes, int offset, int length) throws BufferOverflowException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @Override
    @ForceInline
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) throws BufferOverflowException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
    }

    @Override
    @NotNull
    @ForceInline
    public Bytes<Underlying> write(long offsetInRDO, RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @ForceInline
    void writeCheckOffset(long offset, long adding) throws BufferOverflowException {
        assert writeCheckOffset0(offset, adding);
    }

    protected boolean writeCheckOffset0(long offset, long adding) throws BufferOverflowException {
        if (offset < start()) {
            throw new DecoratedBufferOverflowException(String.format("writeCheckOffset0 failed. Offset: %d < start: %d", offset, start()));
        }
        if ((offset + adding) > writeLimit()) {
            assert (offset + adding) <= writeLimit() : "cant add bytes past the limit : limit=" + writeLimit() + ",offset=" + offset
                    + ",adding=" + adding;
            throw new DecoratedBufferOverflowException(
                    String.format("writeCheckOffset0 failed. Offset: %d + adding %d> writeLimit: %d", offset, adding, writeLimit()));
        }
        return true;
    }

    @Override
    @ForceInline
    public byte readByte(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 1, true);
        return bytesStore.readByte(offset);
    }

    @Override
    public int peekUnsignedByte(long offset) throws BufferUnderflowException {
        return offset >= readLimit() ? -1 : bytesStore.peekUnsignedByte(offset);
    }

    @Override
    @ForceInline
    public short readShort(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 2, true);
        return bytesStore.readShort(offset);
    }

    @Override
    @ForceInline
    public int readInt(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readInt(offset);
    }

    @Override
    @ForceInline
    public long readLong(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readLong(offset);
    }

    @Override
    @ForceInline
    public float readFloat(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readFloat(offset);
    }

    @Override
    @ForceInline
    public double readDouble(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readDouble(offset);
    }

    @ForceInline
    void readCheckOffset(long offset, long adding, boolean given) throws BufferUnderflowException {
        assert readCheckOffset0(offset, adding, given);
    }

    @ForceInline
    private boolean readCheckOffset0(long offset, long adding, boolean given) throws BufferUnderflowException {
        if (offset < start()) {
            throw new DecoratedBufferUnderflowException(String.format("readCheckOffset0 failed. Offset: %d < start: %d", offset, start()));
        }
        long limit0 = given ? writeLimit() : readLimit();
        if ((offset + adding) > limit0) {
            // assert false : "can't read bytes past the limit : limit=" + limit0 + ",offset=" +
            // offset +
            // ",adding=" + adding;
            throw new DecoratedBufferUnderflowException(String
                    .format("readCheckOffset0 failed. Offset: %d + adding: %d > limit: %d (given: %s)", offset, adding, limit0, given));
        }
        return true;
    }

    @ForceInline
    void prewriteCheckOffset(long offset, long subtracting) throws BufferOverflowException {
        assert prewriteCheckOffset0(offset, subtracting);
    }

    @ForceInline
    private boolean prewriteCheckOffset0(long offset, long subtracting) throws BufferOverflowException {
        if ((offset - subtracting) < start()) {
            throw new DecoratedBufferOverflowException(
                    String.format("prewriteCheckOffset0 failed. Offset: %d - subtracting: %d < start: %d", offset, subtracting, start()));
        }
        long limit0 = readLimit();
        if (offset > limit0) {
            // assert false : "can't read bytes past the limit : limit=" + limit0 + ",offset=" +
            // offset +
            // ",adding=" + adding;
            throw new DecoratedBufferOverflowException(
                    String.format("prewriteCheckOffset0 failed. Offset: %d > readLimit: %d", offset, limit0));
        }
        return true;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeByte(byte i8) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(1, 1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewrite(@NotNull byte[] bytes) throws BufferOverflowException {
        long offset = prewriteOffsetPositionMoved(bytes.length);
        bytesStore.write(offset, bytes);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewrite(@NotNull BytesStore bytes) throws BufferOverflowException {
        long offset = prewriteOffsetPositionMoved(bytes.readRemaining());
        bytesStore.write(offset, bytes);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> prewriteByte(byte i8) throws BufferOverflowException {
        long offset = prewriteOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteInt(int i) throws BufferOverflowException {
        long offset = prewriteOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteShort(short i) throws BufferOverflowException {
        long offset = prewriteOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteLong(long l) throws BufferOverflowException {
        long offset = prewriteOffsetPositionMoved(8);
        bytesStore.writeLong(offset, l);
        return this;
    }

    protected final long writeOffsetPositionMoved(long adding) throws BufferOverflowException {
        return writeOffsetPositionMoved(adding, adding);
    }

    protected long writeOffsetPositionMoved(long adding, long advance) throws BufferOverflowException {
        long oldPosition = writePosition;
        writeCheckOffset(writePosition, adding);
        uncheckedWritePosition(writePosition + advance);
        return oldPosition;
    }

    void uncheckedWritePosition(long writePosition) {
        this.writePosition = writePosition;
    }

    private boolean checkThreadPositionSetBy() {
        if (threadPositionSetBy == null) {
            threadPositionSetBy = Thread.currentThread();
        }
        return threadPositionSetBy == Thread.currentThread();
    }

    protected long prewriteOffsetPositionMoved(long subtracting) throws BufferOverflowException {
        prewriteCheckOffset(readPosition, subtracting);
        return readPosition -= subtracting;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeShort(short i16) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeInt(int i) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeIntAdv(int i, int advance) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(4, advance);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeLong(long i64) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeLongAdv(long i64, int advance) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(8, advance);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeFloat(float f) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeFloat(offset, f);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeDouble(double d) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeDoubleAndInt(double d, int i) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(12);
        bytesStore.writeDouble(offset, d);
        bytesStore.writeInt(offset + 8, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> write(@NotNull byte[] bytes, int offset, int length) throws BufferOverflowException {
        if ((length + offset) > bytes.length) {
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + bytes.length + ", " + "length=" + length + ", offset=" + offset);
        }
        if (length > writeRemaining()) {
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));
        }
        long offsetInRDO = writeOffsetPositionMoved(length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeSome(@NotNull ByteBuffer buffer) throws BufferOverflowException {
        int length = (int) Math.min(buffer.remaining(), writeRemaining());
        bytesStore.write(writePosition, buffer, buffer.position(), length);
        uncheckedWritePosition(writePosition + length);
        buffer.position(buffer.position() + length);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedInt(int i) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedLong(long i) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @Override
    public long addressForRead(long offset) throws BufferUnderflowException {
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(long offset) throws UnsupportedOperationException, BufferOverflowException {
        return bytesStore.addressForWrite(offset);
    }

    @Override
    public int hashCode() {
        long h = VanillaBytesStoreHash.INSTANCE.applyAsLong(this);
        h ^= h >> 32;
        return (int) h;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BytesStore)) {
            return false;
        }
        @NotNull BytesStore b2 = (BytesStore) obj;
        long remaining = readRemaining();
        return (b2.readRemaining() == remaining) && equalsBytes(b2, remaining);
    }

    public boolean equalsBytes(@NotNull BytesStore b2, long remaining) {
        try {
            long i = 0;
            for (; i < (remaining - 7); i += 8) {
                if (readLong(readPosition() + i) != b2.readLong(b2.readPosition() + i)) {
                    return false;
                }
            }
            for (; i < remaining; i++) {
                if (readByte(readPosition() + i) != b2.readByte(b2.readPosition() + i)) {
                    return false;
                }
            }
            return true;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    @Override
    public String toString() {
        try {
            return BytesInternal.toString(this);
        } catch (Exception e) {
            return e.toString();
        }
    }

    @Override
    @ForceInline
    public void nativeRead(long address, long size) throws BufferUnderflowException {
        long position = readPosition();
        readSkip(size);
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    @ForceInline
    public void nativeWrite(long address, long size) throws BufferOverflowException {
        long position = writePosition();
        writeSkip(size);
        bytesStore.nativeWrite(address, position, size);
    }

    @Override
    @ForceInline
    public void nativeRead(long position, long address, long size) throws BufferUnderflowException {
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    @ForceInline
    public void nativeWrite(long address, long position, long size) throws BufferOverflowException {
        bytesStore.nativeWrite(address, position, size);
    }

    @NotNull
    @Override
    public BytesStore bytesStore() {
        return bytesStore;
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
    public void lenient(boolean lenient) {
        this.lenient = lenient;
    }

    @Override
    public boolean lenient() {
        return lenient;
    }

    @Override
    public int byteCheckSum() throws IORuntimeException {
        return byteCheckSum(readPosition(), readLimit());
    }

    @Override
    public int byteCheckSum(long start, long end) {
        if (end < Integer.MAX_VALUE && isDirectMemory())
            return byteCheckSum((int) start, (int) end);
        return Bytes.super.byteCheckSum(start, end);
    }

    public int byteCheckSum(int start, int end) {
        int sum = 0;
        for (int i = start; i < end; i++) {
            sum += readByte(i);
        }
        return sum & 0xFF;
    }
}
