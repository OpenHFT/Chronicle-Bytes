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

import net.openhft.chronicle.core.ReferenceCounter;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public abstract class AbstractBytes<Underlying> implements Bytes<Underlying> {
    @Nullable
    protected BytesStore<Bytes<Underlying>, Underlying> bytesStore;
    private final ReferenceCounter refCount = ReferenceCounter.onReleased(this::performRelease);
    protected long readPosition;
    protected long writePosition;
    protected long writeLimit;
    protected boolean isPresent;
    private int lastDecimalPlaces = 0;

    AbstractBytes(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore, long writePosition, long writeLimit)
            throws IllegalStateException {
        this.bytesStore = bytesStore;
        bytesStore.reserve();
        readPosition = bytesStore.readPosition();
        this.writePosition = writePosition;
        this.writeLimit = writeLimit;
    }


    @Override
    public void move(long from, long to, long length) {
        long start = start();
        bytesStore.move(from - start, to - start, length);
    }

    @Override
    public Bytes<Underlying> compact() {
        long start = start();
        long readRemaining = readRemaining();
        if (readRemaining > 0 && start < readPosition) {
            bytesStore.move(readPosition, start, readRemaining);
            readPosition = start;
            writePosition = readPosition + readRemaining;
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

    @NotNull
    public Bytes<Underlying> clear() {
        readPosition = writePosition = start();
        writeLimit = capacity();
        return this;
    }

    @Override
    public Bytes<Underlying> clearAndPad(long length) throws BufferOverflowException {
        if (start() + length > capacity())
            throw new BufferOverflowException();
        readPosition = writePosition = start() + length;
        writeLimit = capacity();
        return this;
    }

    @Override
    @ForceInline
    public long readLimit() {
        return writePosition;
    }

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
    public boolean compareAndSwapInt(long offset, int expected, int value)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 4);
        return bytesStore.compareAndSwapInt(offset, expected, value);
    }

    @Override
    @ForceInline
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 8);
        return bytesStore.compareAndSwapLong(offset, expected, value);
    }

    @Override
    @ForceInline
    public Bytes<Underlying> readPosition(long position) throws BufferUnderflowException {
        if (position < start())
            throw new BufferUnderflowException();
        if (position > readLimit())
            throw new BufferUnderflowException();
        this.readPosition = position;
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> readLimit(long limit) throws BufferUnderflowException {
        if (limit < start()) throw new BufferUnderflowException();
        if (limit > writeLimit())
            throw new BufferUnderflowException();
        this.writePosition = limit;
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writePosition(long position) throws BufferOverflowException {
        if (position > writeLimit())
            throw new BufferOverflowException();
        if (position < start())
            throw new BufferUnderflowException();
        if (position < readPosition())
            this.readPosition = position;
        this.writePosition = position;
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> readSkip(long bytesToSkip)
            throws BufferUnderflowException, IORuntimeException {
        readOffsetPositionMoved(bytesToSkip);
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeSkip(long bytesToSkip)
            throws BufferOverflowException, IORuntimeException {
        writeCheckOffset(writePosition, bytesToSkip);
        writePosition += bytesToSkip;
        return this;
    }

    @Override
    @ForceInline
    public Bytes<Underlying> writeLimit(long limit) throws BufferOverflowException {
        if (limit < start())
            throw new BufferOverflowException();
        long capacity = capacity();
        if (limit > capacity) {
            assert false : "cant set limit=" + limit + " > " + "capacity=" + capacity;
            throw new BufferOverflowException();
        }
        this.writeLimit = limit;
        return this;
    }

    void performRelease() {
        try {
            this.bytesStore.release();
        } finally {
            this.bytesStore = NoBytesStore.noBytesStore();
        }
    }

    @Override
    public int readUnsignedByte() throws IORuntimeException {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readUnsignedByte(offset);

        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    @ForceInline
    public byte readByte() throws IORuntimeException {
        try {
            long offset = readOffsetPositionMoved(1);
            return bytesStore.readByte(offset);

        } catch (BufferUnderflowException e) {
            return 0;
        }
    }

    @Override
    @ForceInline
    public int peekUnsignedByte() throws IORuntimeException {
        try {
            return readRemaining() > 0 ? bytesStore.readUnsignedByte(readPosition) : -1;

        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    @Override
    @ForceInline
    public short readShort() throws BufferUnderflowException, IORuntimeException {
        long offset = readOffsetPositionMoved(2);
        return bytesStore.readShort(offset);
    }

    @Override
    @ForceInline
    public int readInt() throws BufferUnderflowException, IORuntimeException {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readInt(offset);
    }

    @Override
    @ForceInline
    public long readLong() throws BufferUnderflowException, IORuntimeException {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readLong(offset);
    }

    @Override
    @ForceInline
    public float readFloat() throws BufferUnderflowException, IORuntimeException {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readFloat(offset);
    }

    @Override
    @ForceInline
    public double readDouble() throws BufferUnderflowException, IORuntimeException {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readDouble(offset);
    }

    @Override
    @ForceInline
    public int readVolatileInt() throws BufferUnderflowException, IORuntimeException {
        long offset = readOffsetPositionMoved(4);
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    @ForceInline
    public long readVolatileLong() throws BufferUnderflowException, IORuntimeException {
        long offset = readOffsetPositionMoved(8);
        return bytesStore.readVolatileLong(offset);
    }

    protected long readOffsetPositionMoved(long adding)
            throws BufferUnderflowException, IORuntimeException {
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
    public Bytes<Underlying> writeByte(long offset, byte i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 1);
        bytesStore.writeByte(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeShort(long offset, short i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeInt(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedInt(long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        if (offset < 0) throw new IllegalArgumentException();
        writeCheckOffset(offset, 4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeLong(long offset, long i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 8);
        bytesStore.writeLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedLong(long offset, long i)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeFloat(long offset, float d)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 4);
        bytesStore.writeFloat(offset, d);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeDouble(long offset, double d)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @Override
    public Bytes<Underlying> writeVolatileByte(long offset, byte i8)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 1);
        bytesStore.writeVolatileByte(offset, i8);
        return this;
    }

    @Override
    public Bytes<Underlying> writeVolatileShort(long offset, short i16)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 2);
        bytesStore.writeVolatileShort(offset, i16);
        return this;
    }

    @Override
    public Bytes<Underlying> writeVolatileInt(long offset, int i32)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 4);
        bytesStore.writeVolatileInt(offset, i32);
        return this;
    }

    @Override
    public Bytes<Underlying> writeVolatileLong(long offset, long i64)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offset, 8);
        bytesStore.writeVolatileLong(offset, i64);
        return this;
    }

    @NotNull
    @ForceInline
    public Bytes<Underlying> write(long offsetInRDO, byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @ForceInline
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
    }

    @NotNull
    @ForceInline
    public Bytes<Underlying> write(long offsetInRDO,
                                   RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalArgumentException, IORuntimeException {
        writeCheckOffset(offsetInRDO, length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @ForceInline
    void writeCheckOffset(long offset, long adding)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        assert writeCheckOffset0(offset, adding);
    }

    private boolean writeCheckOffset0(long offset, long adding) throws BufferOverflowException {
        if (offset < start())
            throw new BufferOverflowException();
        if (offset + adding > writeLimit()) {
            assert offset + adding <= writeLimit() : "cant add bytes past the limit : limit=" + writeLimit() +
                    ",offset=" +
                    offset +
                    ",adding=" + adding;
            throw new BufferOverflowException();
        }
        return true;
    }

    @Override
    @ForceInline
    public byte readByte(long offset) throws BufferUnderflowException, IORuntimeException {
        readCheckOffset(offset, 1, true);
        return bytesStore.readByte(offset);
    }

    @Override
    @ForceInline
    public short readShort(long offset) throws BufferUnderflowException, IORuntimeException {
        readCheckOffset(offset, 2, true);
        return bytesStore.readShort(offset);
    }

    @Override
    @ForceInline
    public int readInt(long offset) throws BufferUnderflowException, IORuntimeException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readInt(offset);
    }

    @Override
    @ForceInline
    public long readLong(long offset) throws BufferUnderflowException, IORuntimeException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readLong(offset);
    }

    @Override
    @ForceInline
    public float readFloat(long offset) throws BufferUnderflowException, IORuntimeException {
        readCheckOffset(offset, 4, true);
        return bytesStore.readFloat(offset);
    }

    @Override
    @ForceInline
    public double readDouble(long offset) throws BufferUnderflowException, IORuntimeException {
        readCheckOffset(offset, 8, true);
        return bytesStore.readDouble(offset);
    }

    @ForceInline
    void readCheckOffset(long offset, long adding, boolean given)
            throws BufferUnderflowException, IORuntimeException {
        assert readCheckOffset0(offset, adding, given);
    }

    @ForceInline
    private boolean readCheckOffset0(long offset, long adding, boolean given) throws BufferUnderflowException {
        if (offset < start())
            throw new BufferUnderflowException();
        long limit0 = given ? writeLimit() : readLimit();
        if (offset + adding > limit0) {
//          assert false : "can't read bytes past the limit : limit=" + limit0 + ",offset=" +
            //                  offset +
            //                ",adding=" + adding;
            throw new BufferUnderflowException();
        }
        return true;
    }

    @ForceInline
    void prewriteCheckOffset(long offset, long subtracting)
            throws BufferUnderflowException, IORuntimeException {
        assert prewriteCheckOffset0(offset, subtracting);
    }

    @ForceInline
    private boolean prewriteCheckOffset0(long offset, long subtracting) throws BufferOverflowException {
        if (offset - subtracting < start())
            throw new BufferOverflowException();
        long limit0 = readLimit();
        if (offset > limit0) {
//          assert false : "can't read bytes past the limit : limit=" + limit0 + ",offset=" +
            //                  offset +
            //                ",adding=" + adding;
            throw new BufferOverflowException();
        }
        return true;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeByte(byte i8)
            throws BufferOverflowException, IORuntimeException {
        long offset = writeOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @Override
    public Bytes<Underlying> prewrite(byte[] bytes) {
        long offset = prewriteOffsetPositionMoved(bytes.length);
        bytesStore.write(offset, bytes);
        return this;
    }

    @Override
    public Bytes<Underlying> prewrite(BytesStore bytes) {
        long offset = prewriteOffsetPositionMoved(bytes.readRemaining());
        bytesStore.write(offset, bytes);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> prewriteByte(byte i8)
            throws BufferOverflowException, IORuntimeException {
        long offset = prewriteOffsetPositionMoved(1);
        bytesStore.writeByte(offset, i8);
        return this;
    }

    @Override
    public Bytes<Underlying> prewriteInt(int i) {
        long offset = prewriteOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> prewriteShort(short i) {
        long offset = prewriteOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i);
        return this;
    }

    @Override
    public Bytes<Underlying> prewriteLong(long l) {
        long offset = prewriteOffsetPositionMoved(8);
        bytesStore.writeLong(offset, l);
        return this;
    }

    protected long writeOffsetPositionMoved(long adding)
            throws BufferOverflowException, IORuntimeException {
        long oldPosition = writePosition;
            writeCheckOffset(writePosition, adding);
        writePosition += adding;
        return oldPosition;
    }

    protected long prewriteOffsetPositionMoved(long subtracting)
            throws BufferOverflowException, IORuntimeException {
            prewriteCheckOffset(readPosition, subtracting);
        return readPosition -= subtracting;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeShort(short i16)
            throws BufferOverflowException, IORuntimeException {
        long offset = writeOffsetPositionMoved(2);
        bytesStore.writeShort(offset, i16);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeInt(int i) throws BufferOverflowException, IORuntimeException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeLong(long i64)
            throws BufferOverflowException, IORuntimeException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeLong(offset, i64);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeFloat(float f)
            throws BufferOverflowException, IORuntimeException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeFloat(offset, f);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeDouble(double d)
            throws BufferOverflowException, IORuntimeException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeDouble(offset, d);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> write(byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        if (bytes.length > writeRemaining())
            throw new BufferOverflowException();
        long offsetInRDO = writeOffsetPositionMoved(length);
        bytesStore.write(offsetInRDO, bytes, offset, length);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeSome(@NotNull ByteBuffer buffer)
            throws BufferOverflowException, IORuntimeException {
        int length = (int) Math.min(buffer.remaining(), writeRemaining());
        bytesStore.write(writePosition, buffer, buffer.position(), length);
        writePosition += length;
        buffer.position(buffer.position() + length);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedInt(int i)
            throws BufferOverflowException, IORuntimeException {
        long offset = writeOffsetPositionMoved(4);
        bytesStore.writeOrderedInt(offset, i);
        return this;
    }

    @NotNull
    @Override
    @ForceInline
    public Bytes<Underlying> writeOrderedLong(long i)
            throws BufferOverflowException, IORuntimeException {
        long offset = writeOffsetPositionMoved(8);
        bytesStore.writeOrderedLong(offset, i);
        return this;
    }

    @Override
    public long address(long offset) throws BufferOverflowException, BufferUnderflowException {
        return bytesStore.address(offset);
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bytes)) return false;
        Bytes b2 = (Bytes) obj;
        long remaining = readRemaining();
        try {
            return b2.readRemaining() == remaining && equalsBytes(b2, remaining);
        } catch (IORuntimeException e) {
            throw new AssertionError(e);
        }
    }

    public boolean equalsBytes(@NotNull Bytes b2, long remaining) throws IORuntimeException {
        long i = 0;
        for (; i < remaining - 7; i++)
            if (readLong(readPosition() + i) != b2.readLong(b2.readPosition() + i))
                return false;
        for (; i < remaining; i++)
            if (readByte(readPosition() + i) != b2.readByte(b2.readPosition() + i))
                return false;
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
    @ForceInline
    public void nativeRead(long address, long size)
            throws BufferUnderflowException, IORuntimeException {
        long position = readPosition();
        readSkip(size);
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    @ForceInline
    public void nativeWrite(long address, long size)
            throws BufferOverflowException, IORuntimeException {
        long position = writePosition();
        writeSkip(size);
        bytesStore.nativeWrite(address, position, size);
    }

    @Override
    @ForceInline
    public void nativeRead(long position, long address, long size) {
        bytesStore.nativeRead(position, address, size);
    }

    @Override
    @ForceInline
    public void nativeWrite(long address, long position, long size) {
        bytesStore.nativeWrite(address, position, size);
    }

    @Nullable
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
}

