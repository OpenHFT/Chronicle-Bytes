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
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.UnsafeText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

@SuppressWarnings("rawtypes")
public abstract class AbstractBytes<Underlying>
        extends AbstractReferenceCounted
        implements Bytes<Underlying> {
    private static final boolean BYTES_BOUNDS_UNCHECKED = Jvm.getBoolean("bytes.bounds.unchecked", false);
    // used for debugging
    @UsedViaReflection
    private final String name;
    @NotNull
    protected BytesStore<Bytes<Underlying>, Underlying> bytesStore;
    protected long readPosition;
    protected long writePosition;
    protected long writeLimit;
    protected boolean isPresent;
    private int lastDecimalPlaces = 0;
    private boolean lenient = false;
    private boolean lastNumberHadDigits = false;

    AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore, long writePosition, long writeLimit)
            throws IllegalStateException {
        this(bytesStore, writePosition, writeLimit, "");
    }

    AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore, long writePosition, long writeLimit, String name)
            throws IllegalStateException {
        super(bytesStore.isDirectMemory());
        this.bytesStore(bytesStore);
        bytesStore.reserve(this);
        readPosition = bytesStore.readPosition();
        this.uncheckedWritePosition(writePosition);
        this.writeLimit = writeLimit;
        // used for debugging
        this.name = name;
    }

    @Override
    public boolean isDirectMemory() {
        return bytesStore.isDirectMemory();
    }

    @Override
    public boolean canReadDirect(long length) {
        long remaining = writePosition() - readPosition;
        return bytesStore.isDirectMemory() && remaining >= length;
    }

    @Override
    public void move(long from, long to, long length)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        long start = start();
        bytesStore.move(from - start, to - start, length);
    }

    @NotNull
    @Override
    public Bytes<Underlying> compact()
            throws IllegalStateException {
        long start = start();
        long readRemaining = readRemaining();
        try {
            if ((readRemaining > 0) && (start < readPosition)) {
                bytesStore.move(readPosition, start, readRemaining);
                readPosition = start;
                uncheckedWritePosition(readPosition + readRemaining);
            }
            return this;
        } catch (BufferUnderflowException | ArithmeticException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @NotNull
    public Bytes<Underlying> clear()
            throws IllegalStateException {
        long start = start();
        readPosition = start;
        uncheckedWritePosition(start);
        writeLimit = capacity();
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> clearAndPad(long length)
            throws BufferOverflowException {
        if ((start() + length) > capacity()) {
            throw newBOERange(start(), length, "clearAndPad failed. Start: %d + length: %d > capacity: %d", capacity());
        }
        long l = start() + length;
        readPosition = l;
        uncheckedWritePosition(l);
        writeLimit = capacity();
        return this;
    }

    @Override
    public long readLimit() {
        return writePosition();
    }

    @Override
    public long writeLimit() {
        return writeLimit;
    }

    @Override
    public long realCapacity() {
        return bytesStore.capacity();
    }

    @Override
    public boolean canWriteDirect(long count) {
        return isDirectMemory() &&
                Math.min(writeLimit, bytesStore.realCapacity())
                        >= count + writePosition();
    }

    @Override
    public long capacity() {
        return bytesStore.capacity();
    }

    @Nullable
    @Override
    public Underlying underlyingObject() {
        return bytesStore.underlyingObject();
    }

    @Override
    public long start() {
        return bytesStore.start();
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

    public @NotNull AbstractBytes append(double d)
            throws BufferOverflowException, IllegalStateException {
        boolean fits = canWriteDirect(380);
        double ad = Math.abs(d);
        if (ad < 1e-18) {
            append(Double.toString(d));
            return this;
        }
        if (!fits) {
            fits = 1e-6 <= ad && ad < 1e20 && canWriteDirect(24);
        }
        if (fits) {
            long address = addressForWrite(writePosition());
            long address2 = UnsafeText.appendDouble(address, d);
            writeSkip(address2 - address);
            return this;
        }
        BytesInternal.append(this, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readPosition(long position)
            throws BufferUnderflowException, IllegalStateException {
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
    public Bytes<Underlying> readLimit(long limit)
            throws BufferUnderflowException {
        if (limit < start())
            throw limitLessThanStart(limit);

        if (limit > writeLimit())
            throw limitGreaterThanWriteLimit(limit);

        uncheckedWritePosition(limit);
        return this;
    }

    private DecoratedBufferUnderflowException limitGreaterThanWriteLimit(long limit) {
        return new DecoratedBufferUnderflowException(String.format("readLimit failed. Limit: %d > writeLimit: %d", limit, writeLimit()));
    }

    private DecoratedBufferUnderflowException limitLessThanStart(long limit) {
        return new DecoratedBufferUnderflowException(String.format("readLimit failed. Limit: %d < start: %d", limit, start()));
    }

    @NotNull
    @Override
    public Bytes<Underlying> writePosition(long position)
            throws BufferOverflowException {
        if (position > writeLimit())
            throw writePositionTooLarge(position);

        if (position < start())
            throw writePositionTooSmall(position);

        if (position < readPosition())
            this.readPosition = position;

        uncheckedWritePosition(position);
        return this;
    }

    @NotNull
    private DecoratedBufferOverflowException writePositionTooSmall(long position) {
        return new DecoratedBufferOverflowException(String.format("writePosition failed. Position: %d < start: %d", position, start()));
    }

    private DecoratedBufferOverflowException writePositionTooLarge(long position) {
        return new DecoratedBufferOverflowException(
                String.format("writePosition failed. Position: %d > writeLimit: %d", position, writeLimit()));
    }

    @NotNull
    @Override
    public Bytes<Underlying> readSkip(long bytesToSkip)
            throws BufferUnderflowException, IllegalStateException {
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
    public Bytes<Underlying> writeSkip(long bytesToSkip)
            throws BufferOverflowException, IllegalStateException {
        final long writePosition = writePosition();
        writeCheckOffset(writePosition, bytesToSkip);
        uncheckedWritePosition(writePosition + bytesToSkip);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLimit(long limit)
            throws BufferOverflowException {
        if (limit < start()) {
            throw writeLimitTooSmall(limit);
        }
        long capacity = capacity();
        if (limit > capacity) {
            throw writeLimitTooBig(limit, capacity);
        }
        this.writeLimit = limit;
        return this;
    }

    @NotNull
    private DecoratedBufferOverflowException writeLimitTooBig(long limit, long capacity) {
        return new DecoratedBufferOverflowException(String.format("writeLimit failed. Limit: %d > capacity: %d", limit, capacity));
    }

    @NotNull
    private DecoratedBufferOverflowException writeLimitTooSmall(long limit) {
        return new DecoratedBufferOverflowException(String.format("writeLimit failed. Limit: %d < start: %d", limit, start()));
    }

    @Override
    protected void performRelease() {
        try {
            this.bytesStore.release(this);
        } catch (IllegalStateException e) {
            Jvm.warn().on(getClass(), e);
        }
    }

    @Override
    public int readUnsignedByte()
            throws IllegalStateException {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readUnsignedByte(offset);

        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    public int readUnsignedByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readByte(offset) & 0xFF;
    }

    @Override
    public int uncheckedReadUnsignedByte() {
        try {
            int unsignedByte = bytesStore.readUnsignedByte(readPosition);
            readPosition++;
            return unsignedByte;
        } catch (BufferUnderflowException | IllegalStateException e) {
            return -1;
        }
    }

    @Override
    public byte readByte()
            throws IllegalStateException {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readByte(offset);

        } catch (BufferUnderflowException e) {
            return 0;
        }
    }

    @Override
    public int peekUnsignedByte()
            throws IllegalStateException {
        try {
            return readPosition >= writePosition() ? -1 : bytesStore.readUnsignedByte(readPosition);
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    public short readShort()
            throws BufferUnderflowException, IllegalStateException {
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
    public int readInt()
            throws BufferUnderflowException, IllegalStateException {
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
    public byte readVolatileByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 1, true);
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 2, true);
        return bytesStore.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readVolatileLong(offset);
    }

    @Override
    public long readLong()
            throws BufferUnderflowException, IllegalStateException {
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
    public float readFloat()
            throws BufferUnderflowException, IllegalStateException {
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
    public double readDouble()
            throws BufferUnderflowException, IllegalStateException {
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
    public int readVolatileInt()
            throws BufferUnderflowException, IllegalStateException {
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
    public long readVolatileLong()
            throws BufferUnderflowException, IllegalStateException {
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

    protected long readOffsetPositionMoved(long adding)
            throws BufferUnderflowException, IllegalStateException {
        long offset = readPosition;
        readCheckOffset(readPosition, Math.toIntExact(adding), false);
        readPosition += adding;
        assert readPosition <= readLimit();
        return offset;
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
    public Bytes<Underlying> write(@NotNull RandomDataInput bytes)
            throws IllegalStateException, BufferOverflowException {
        assert bytes != this : "you should not write to yourself !";

        try {
            return write(bytes, bytes.readPosition(), Math.min(writeLimit() - writePosition(), bytes.readRemaining()));
        } catch (BufferUnderflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @NotNull
    public Bytes<Underlying> write(long offsetInRDO, byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
        long remaining = length;
        while (remaining > 0) {
            int copy = (int) Math.min(remaining, safeCopySize()); // copy 64 KB at a time.
            writeCheckOffset(offsetInRDO, copy);
            bytesStore.write(offsetInRDO, bytes, offset, copy);
            offsetInRDO += copy;
            offset += copy;
            remaining -= copy;
        }
        return this;
    }

    @Override
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
    }

    @Override
    @NotNull
    public Bytes<Underlying> write(long writeOffset, RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {

        long remaining = length;
        while (remaining > 0) {
            int copy = (int) Math.min(remaining, safeCopySize()); // copy 64 KB at a time.
            writeCheckOffset(writeOffset, copy);
            bytesStore.write(writeOffset, bytes, readOffset, copy);
            writeOffset += copy;
            readOffset += copy;
            remaining -= copy;
        }
        return this;
    }

    @Override
    public @NotNull Bytes<Underlying> write8bit(@NotNull String s, int start, int length) throws BufferOverflowException, IndexOutOfBoundsException, ArithmeticException, IllegalStateException, BufferUnderflowException {
        final long toWriteLength = UnsafeMemory.INSTANCE.stopBitLength(length) + (long) length;
        final long position = writeOffsetPositionMoved(toWriteLength, 0);
        bytesStore.write8bit(position, s, start, length);
        uncheckedWritePosition(writePosition() + toWriteLength);
        return this;
    }

    public @NotNull Bytes<Underlying> write8bit(@Nullable BytesStore bs) throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
        if (bs == null) {
            BytesInternal.writeStopBitNeg1(this);
            return this;
        }
        long readRemaining = bs.readRemaining();
        long toWriteLength = UnsafeMemory.INSTANCE.stopBitLength(readRemaining) + readRemaining;
        long position = writeOffsetPositionMoved(toWriteLength, 0);
        bytesStore.write8bit(position, bs);
        uncheckedWritePosition(writePosition() + toWriteLength);
        return this;
    }

    @Override
    public long write8bit(long position, BytesStore bs) {
        return bytesStore.write8bit(position, bs);
    }

    @Override
    public long write8bit(long position, String s, int start, int length) {
        return bytesStore.write8bit(position, s, start, length);
    }

    protected void writeCheckOffset(long offset, long adding)
            throws BufferOverflowException, IllegalStateException {
        if (BYTES_BOUNDS_UNCHECKED)
            return;
        writeCheckOffset0(offset, adding);
    }

    private void writeCheckOffset0(long offset, long adding)
            throws DecoratedBufferOverflowException {
        if (offset < start()) {
            throw newBOELower(offset);
        }
        if ((offset + adding) > writeLimit()) {
            throw newBOERange(offset, adding, "writeCheckOffset failed. Offset: %d + adding %d> writeLimit: %d", writeLimit());
        }
    }

    @NotNull
    private DecoratedBufferOverflowException newBOERange(long offset, long adding, String msg, long limit) {
        return new DecoratedBufferOverflowException(
                String.format(msg, offset, adding, limit));
    }

    @NotNull
    private DecoratedBufferOverflowException newBOELower(long offset) {
        return new DecoratedBufferOverflowException(String.format("writeCheckOffset failed. Offset: %d < start: %d", offset, start()));
    }

    @Override
    public byte readByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 1, true);
        return bytesStore.readByte(offset);
    }

    @Override
    public int peekUnsignedByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return offset >= readLimit() ? -1 : bytesStore.peekUnsignedByte(offset);
    }

    @Override
    public short readShort(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 2, true);
        return bytesStore.readShort(offset);
    }

    @Override
    public int readInt(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readInt(offset);
    }

    @Override
    public long readLong(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readLong(offset);
    }

    @Override
    public float readFloat(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readFloat(offset);
    }

    @Override
    public double readDouble(long offset)
            throws BufferUnderflowException, IllegalStateException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readDouble(offset);
    }

    protected void readCheckOffset(long offset, long adding, boolean given)
            throws BufferUnderflowException, IllegalStateException {
        if (BYTES_BOUNDS_UNCHECKED)
            return;
        readCheckOffset0(offset, adding, given);
    }

    private void readCheckOffset0(long offset, long adding, boolean given)
            throws DecoratedBufferUnderflowException {
        if (offset < start()) {
            throw newBOEReadLower(offset);
        }
        long limit0 = given ? writeLimit() : readLimit();
        if ((offset + adding) > limit0) {
            throw newBOEReadUpper(offset, adding, given);
        }
    }

    @NotNull
    private DecoratedBufferUnderflowException newBOEReadUpper(long offset, long adding, boolean given) {
        long limit2 = given ? writeLimit() : readLimit();
        return new DecoratedBufferUnderflowException(String
                .format("readCheckOffset0 failed. Offset: %d + adding: %d > limit: %d (given: %s)", offset, adding, limit2, given));
    }

    @NotNull
    private DecoratedBufferUnderflowException newBOEReadLower(long offset) {
        return new DecoratedBufferUnderflowException(String.format("readCheckOffset0 failed. Offset: %d < start: %d", offset, start()));
    }

    void prewriteCheckOffset(long offset, long subtracting)
            throws BufferOverflowException, IllegalStateException {
        if (BYTES_BOUNDS_UNCHECKED)
            return;
        prewriteCheckOffset0(offset, subtracting);
    }

    private void prewriteCheckOffset0(long offset, long subtracting)
            throws BufferOverflowException {
        if ((offset - subtracting) < start()) {
            throw newBOERange(offset, subtracting, "prewriteCheckOffset0 failed. Offset: %d - subtracting: %d < start: %d", start());
        }
        long limit0 = readLimit();
        if (offset > limit0) {
            // assert false : "can't read bytes past the limit : limit=" + limit0 + ",offset=" +
            // offset +
            // ",adding=" + adding;
            throw new DecoratedBufferOverflowException(
                    String.format("prewriteCheckOffset0 failed. Offset: %d > readLimit: %d", offset, limit0));
        }
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeByte(byte i8)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(1, 1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewrite(@NotNull byte[] bytes)
            throws BufferOverflowException, IllegalStateException {
        long offset = prewriteOffsetPositionMoved(bytes.length);
        bytesStore.write(offset, bytes);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewrite(@NotNull BytesStore bytes)
            throws BufferOverflowException, IllegalStateException {
        long offset = prewriteOffsetPositionMoved(bytes.readRemaining());
        bytesStore.write(offset, bytes);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteByte(byte i8)
            throws BufferOverflowException, IllegalStateException {
        long offset = prewriteOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteInt(int i)
            throws BufferOverflowException, IllegalStateException {
        long offset = prewriteOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteShort(short i)
            throws BufferOverflowException, IllegalStateException {
        long offset = prewriteOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> prewriteLong(long l)
            throws BufferOverflowException, IllegalStateException {
        long offset = prewriteOffsetPositionMoved(8);
        bytesStore.writeLong(offset, l);
        return this;
    }

    protected final long writeOffsetPositionMoved(long adding)
            throws BufferOverflowException, IllegalStateException {
        return writeOffsetPositionMoved(adding, adding);
    }

    protected long writeOffsetPositionMoved(long adding, long advance)
            throws BufferOverflowException, IllegalStateException {
        long oldPosition = writePosition();
        writeCheckOffset(oldPosition, adding);
        uncheckedWritePosition(writePosition() + advance);
        return oldPosition;
    }

    protected void uncheckedWritePosition(long writePosition) {
        this.writePosition = writePosition;
    }

    protected long prewriteOffsetPositionMoved(long subtracting)
            throws BufferOverflowException, IllegalStateException {
        prewriteCheckOffset(readPosition, subtracting);
        return readPosition -= subtracting;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeShort(short i16)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeInt(int i)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeIntAdv(int i, int advance)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(4, advance);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLong(long i64)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLongAdv(long i64, int advance)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(8, advance);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeFloat(float f)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeFloat(offset, f);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeDouble(double d)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeDoubleAndInt(double d, int i)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(12);
        bytesStore.writeDouble(offset, d);
        bytesStore.writeInt(offset + 8, i);
        return this;
    }

    @Override
    public int read(@NotNull byte[] bytes, int off, int len) throws BufferUnderflowException, IllegalStateException {
        long remaining = readRemaining();
        if (remaining <= 0)
            return -1;
        final int totalToCopy = (int) Math.min(len, remaining);
        int remainingToCopy = totalToCopy;
        int currentOffset = off;
        while (remainingToCopy > 0) {
            int currentBatchSize = Math.min(remainingToCopy, safeCopySize());
            long offsetInRDO = readOffsetPositionMoved(currentBatchSize);
            bytesStore.read(offsetInRDO, bytes, currentOffset, currentBatchSize);
            currentOffset += currentBatchSize;
            remainingToCopy -= currentBatchSize;
        }
        return totalToCopy;
    }

    @Override
    public long read(long offsetInRDI, byte[] bytes, int offset, int length) throws IllegalStateException {
        return bytesStore.read(offsetInRDI, bytes, offset, length);
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException {

        if ((length + offset) > bytes.length) {
            throw new DecoratedBufferOverflowException("bytes.length=" + bytes.length + ", " + "length=" + length + ", offset=" + offset);
        }
        if (length > writeRemaining()) {
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));
        }
        ensureCapacity(writePosition() + length);
        int remaining = length;
        while (remaining > 0) {
            int copy = Math.min(remaining, safeCopySize()); // copy 64 KB at a time.
            long offsetInRDO = writeOffsetPositionMoved(copy);
            bytesStore.write(offsetInRDO, bytes, offset, copy);
            offset += copy;
            remaining -= copy;
        }
        return this;
    }

    protected int safeCopySize() {
        return 64 << 10;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeSome(@NotNull ByteBuffer buffer)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
        int length = (int) Math.min(buffer.remaining(), writeRemaining());
        try {
            ensureCapacity(writePosition() + length);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
        bytesStore.write(writePosition(), buffer, buffer.position(), length);
        uncheckedWritePosition(writePosition() + length);
        buffer.position(buffer.position() + length);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedInt(int i)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedLong(long i)
            throws BufferOverflowException, IllegalStateException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @Override
    public long addressForRead(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return bytesStore.addressForRead(offset);
    }

    @Override
    public long addressForWrite(long offset)
            throws BufferOverflowException, IllegalStateException {
        return bytesStore.addressForWrite(offset);
    }

    @Override
    public long addressForWritePosition()
            throws BufferOverflowException, IllegalStateException {
        return bytesStore.addressForWrite(writePosition());
    }

    @Override
    public int hashCode() {
        return BytesStoreHash.hash32(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BytesStore)) {
            return false;
        }
        @NotNull BytesStore bs = (BytesStore) obj;
        long remaining = readRemaining();
        try {
            return (bs.readRemaining() == remaining) &&
                    BytesInternal.contentEqual(this, bs);
        } catch (IllegalStateException e) {
            return false;
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
    public void nativeRead(long position, long address, long size)
            throws BufferUnderflowException, IllegalStateException {
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    public void nativeWrite(long address, long position, long size)
            throws BufferOverflowException, IllegalStateException {
        bytesStore.nativeWrite(address, position, size);
    }

    @NotNull
    @Override
    public BytesStore bytesStore() {
        return bytesStore;
    }

    protected void bytesStore(BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
        this.bytesStore = bytesStore;
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

    @Override
    public BytesStore<Bytes<Underlying>, Underlying> copy() throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isElastic() {
        return false;
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
    public int byteCheckSum()
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        return byteCheckSum(readPosition(), readLimit());
    }

    @Override
    public int byteCheckSum(long start, long end)
            throws BufferUnderflowException, IllegalStateException {
        if (end < Integer.MAX_VALUE && isDirectMemory())
            return byteCheckSum((int) start, (int) end);
        return Bytes.super.byteCheckSum(start, end);
    }

    public int byteCheckSum(int start, int end)
            throws BufferUnderflowException, IllegalStateException {
        int sum = 0;
        for (int i = start; i < end; i++) {
            sum += readByte(i);
        }
        return sum & 0xFF;
    }

    static class ReportUnoptimised {
        static {
            Jvm.reportUnoptimised();
        }

        static void reportOnce() {
            // Do nothing
        }
    }
}
