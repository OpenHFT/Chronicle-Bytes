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

import net.openhft.chronicle.core.ReferenceCounted;

import java.nio.ByteBuffer;

import static java.lang.Math.min;

/**
 * A reference to some bytes with fixed extents.
 * Only offset access within the capacity is possible.
 */
public interface BytesStore<B extends BytesStore<B, Underlying>, Underlying>
        extends RandomDataInput, RandomDataOutput<B>, ReferenceCounted {
    static BytesStore wrap(byte[] bytes) {
        return HeapBytesStore.wrap(ByteBuffer.wrap(bytes));
    }

    static BytesStore wrap(ByteBuffer bb) {
        return bb.isDirect()
                ? NativeBytesStore.wrap(bb)
                : HeapBytesStore.wrap(bb);
    }

    static PointerBytesStore nativePointer() {
        return new PointerBytesStore();
    }

    /**
     * @return a copy of this BytesStore.
     */
    BytesStore<B, Underlying> copy();

    /**
     * @return a Bytes to wrap this ByteStore from the start() to the realCapacity().
     */

    default Bytes<Underlying> bytes() {
        return bytes(UnderflowMode.PADDED);
    }

    default Bytes bytes(UnderflowMode underflowMode) {
        switch (underflowMode) {
            case BOUNDED:
                return new VanillaBytes(this).readLimit(writeLimit());
            case ZERO_EXTEND:
            case PADDED:
                return new ZeroedBytes(this, underflowMode).readLimit(writeLimit());
            default:
                throw new UnsupportedOperationException("Unknown known mode " + underflowMode);
        }
    }

    default boolean isClear() {
        return true;
    }

    /**
     * @return the actual capacity available before resizing.
     */
    default long realCapacity() {
        return capacity();
    }

    /**
     * @return The maximum limit you can set.
     */
    long capacity();

    Underlying underlyingObject();

    /**
     * Use this test to determine if an offset is considered safe.
     */
    default boolean inStore(long offset) {
        return start() <= offset && offset < safeLimit();
    }

    default long safeLimit() {
        return capacity();
    }

    default void copyTo(BytesStore store) {
        long copy = min(capacity(), store.capacity());
        int i = 0;
        for (; i < copy - 7; i++)
            store.writeLong(i, readLong(i));
        for (; i < copy; i++)
            store.writeByte(i, readByte(i));
    }

    default B zeroOut(long start, long end) {
        if (start < start() || end > capacity() || end > start)
            throw new IllegalArgumentException();
        long i = start;
        for (; i < end - 7; i++)
            writeLong(i, 0L);
        for (; i < end; i++)
            writeByte(i, 0);
        return (B) this;
    }

    boolean compareAndSwapInt(long offset, int expected, int value);

    boolean compareAndSwapLong(long offset, long expected, long value);

    default int addAndGetInt(long offset, int adding) {
        return BytesUtil.getAndAddInt(this, offset, adding) + adding;
    }

    default int getAndAddInt(long offset, int adding) {
        return BytesUtil.getAndAddInt(this, offset, adding);
    }

    default long addAndGetLong(long offset, long adding) {
        return BytesUtil.getAndAddLong(this, offset, adding) + adding;
    }
}
