/*
 * Copyright 2016-2020 Chronicle Software
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
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
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
        long remaining = writePosition - readPosition;
        return bytesStore.isDirectMemory() && remaining >= length;
    }

    @Override
    public void move(long from, long to, long length) throws BufferUnderflowException {
        long start = start();
        bytesStore.move(from - start, to - start, length);
    }

    @NotNull
    @Override
    public Bytes<Underlying> compact() {
        long start = start();
        long readRemaining = readRemaining();
        if ((readRemaining > 0) && (start < readPosition)) {
            bytesStore.move(readPosition, start, readRemaining);
            readPosition = start;
            uncheckedWritePosition(readPosition + readRemaining);
        }
        return this;
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
    public long readLimit() {
        return writePosition;
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
    public long realWriteRemaining() {
        return bytesStore.capacity() - writePosition;
    }

    @Override
    public boolean canWriteDirect(long count) {
        return isDirectMemory() &&
                Math.min(writeLimit, bytesStore.capacity())
                        >= count + writePosition;
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
    public boolean compareAndSwapInt(long offset, int expected, int value) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        return bytesStore.compareAndSwapInt(offset, expected, value);
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value) {
        writeCheckOffset(offset, 4);
        bytesStore.testAndSetInt(offset, expected, value);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    public AbstractBytes append(double d) throws BufferOverflowException {
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
            long address = addressForWrite(writePosition);
            long address2 = UnsafeText.appendDouble(address, d);
            writeSkip(address2 - address);
            return this;
        }
        BytesInternal.append(this, d);
        return this;
    }

    @NotNull
    @Override
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
    public Bytes<Underlying> readLimit(long limit) throws BufferUnderflowException {
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
    public Bytes<Underlying> writePosition(long position) throws BufferOverflowException {
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
    public Bytes<Underlying> writeSkip(long bytesToSkip) throws BufferOverflowException {
        writeCheckOffset(writePosition, bytesToSkip);
        uncheckedWritePosition(writePosition + bytesToSkip);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLimit(long limit) throws BufferOverflowException {
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
    protected void performRelease() throws IllegalStateException {
        try {
            this.bytesStore.release(this);
        } finally {
            this.bytesStore(ReleasedBytesStore.releasedBytesStore());
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
    public byte readByte() {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readByte(offset);

        } catch (BufferUnderflowException e) {
            return 0;
        }
    }

    @Override
    public int peekUnsignedByte() {
        return readPosition >= writePosition ? -1 : bytesStore.readUnsignedByte(readPosition);
    }

    @Override
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
        readCheckOffset(readPosition, Math.toIntExact(adding), false);
        readPosition += adding;
        assert readPosition <= readLimit();
        return offset;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeByte(long offset, byte i) throws BufferOverflowException {
        writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeShort(long offset, short i) throws BufferOverflowException {
        writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeInt(long offset, int i) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedInt(long offset, int i) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLong(long offset, long i) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        bytesStore.writeLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedLong(long offset, long i) throws BufferOverflowException {
        writeCheckOffset(offset, 8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeFloat(long offset, float d) throws BufferOverflowException {
        writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset, d);
        return this;
    }

    @NotNull
    @Override
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
    public Bytes<Underlying> write(@NotNull RandomDataInput bytes) {
        assert bytes != this : "you should not write to yourself !";

        try {
            return write(bytes, bytes.readPosition(), Math.min(writeLimit() - writePosition(), bytes.readRemaining()));
        } catch (BufferOverflowException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    @NotNull
    public Bytes<Underlying> write(long offsetInRDO, byte[] bytes, int offset, int length) throws BufferOverflowException {
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
    @Deprecated(/*Is this used?*/)
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) throws BufferOverflowException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);

    }

    @Override
    @NotNull
    public Bytes<Underlying> write(long writeOffset, RandomDataInput bytes, long readOffset, long length)
            throws BufferOverflowException, BufferUnderflowException {

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

    void writeCheckOffset(long offset, long adding) throws BufferOverflowException {
        assert writeCheckOffset0(offset, adding);
    }

    protected boolean writeCheckOffset0(long offset, long adding) throws BufferOverflowException {
        if (offset < start()) {
            throw new DecoratedBufferOverflowException(String.format("writeCheckOffset0 failed. Offset: %d < start: %d", offset, start()));
        }
        if ((offset + adding) > writeLimit()) {
            throw new DecoratedBufferOverflowException(
                    String.format("writeCheckOffset0 failed. Offset: %d + adding %d> writeLimit: %d", offset, adding, writeLimit()));
        }
        return true;
    }

    @Override
    public byte readByte(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 1, true);
        return bytesStore.readByte(offset);
    }

    @Override
    public int peekUnsignedByte(long offset) throws BufferUnderflowException {
        return offset >= readLimit() ? -1 : bytesStore.peekUnsignedByte(offset);
    }

    @Override
    public short readShort(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 2, true);
        return bytesStore.readShort(offset);
    }

    @Override
    public int readInt(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readInt(offset);
    }

    @Override
    public long readLong(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readLong(offset);
    }

    @Override
    public float readFloat(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readFloat(offset);
    }

    @Override
    public double readDouble(long offset) throws BufferUnderflowException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readDouble(offset);
    }

    void readCheckOffset(long offset, long adding, boolean given) throws BufferUnderflowException {
        assert readCheckOffset0(offset, adding, given);
    }

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

    void prewriteCheckOffset(long offset, long subtracting) throws BufferOverflowException {
        assert prewriteCheckOffset0(offset, subtracting);
    }

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

    protected long prewriteOffsetPositionMoved(long subtracting) throws BufferOverflowException {
        prewriteCheckOffset(readPosition, subtracting);
        return readPosition -= subtracting;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeShort(short i16) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeInt(int i) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeIntAdv(int i, int advance) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(4, advance);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLong(long i64) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLongAdv(long i64, int advance) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(8, advance);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeFloat(float f) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeFloat(offset, f);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeDouble(double d) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeDoubleAndInt(double d, int i) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(12);
        bytesStore.writeDouble(offset, d);
        bytesStore.writeInt(offset + 8, i);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull byte[] bytes, int offset, int length) throws BufferOverflowException {
        if ((length + offset) > bytes.length) {
            throw new ArrayIndexOutOfBoundsException("bytes.length=" + bytes.length + ", " + "length=" + length + ", offset=" + offset);
        }
        if (length > writeRemaining()) {
            throw new DecoratedBufferOverflowException(
                    String.format("write failed. Length: %d > writeRemaining: %d", length, writeRemaining()));
        }
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
    public Bytes<Underlying> writeSome(@NotNull ByteBuffer buffer) throws BufferOverflowException {
        int length = (int) Math.min(buffer.remaining(), writeRemaining());
        ensureCapacity(length);
        bytesStore.write(writePosition, buffer, buffer.position(), length);
        uncheckedWritePosition(writePosition + length);
        buffer.position(buffer.position() + length);
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeOrderedInt(int i) throws BufferOverflowException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
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
    public long addressForWritePosition() throws UnsupportedOperationException, BufferOverflowException {
        return bytesStore.addressForWrite(writePosition);
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
        @NotNull BytesStore b2 = (BytesStore) obj;
        long remaining = readRemaining();
        return (b2.readRemaining() == remaining) && equalsBytes(b2, remaining);
    }

    public boolean equalsBytes(@NotNull BytesStore b2, long remaining) {
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
    public void nativeRead(long address, long size) throws BufferUnderflowException {
        long position = readPosition();
        readSkip(size);
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    public void nativeWrite(long address, long size) throws BufferOverflowException {
        long position = writePosition();
        writeSkip(size);
        bytesStore.nativeWrite(address, position, size);
    }

    @Override
    public void nativeRead(long position, long address, long size) throws BufferUnderflowException {
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    public void nativeWrite(long address, long position, long size) throws BufferOverflowException {
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
