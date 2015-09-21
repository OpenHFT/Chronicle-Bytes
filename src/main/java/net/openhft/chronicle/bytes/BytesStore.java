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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static java.lang.Math.min;

/**
 * A immutable reference to some bytes with fixed extents.
 * This can be shared safely across thread provided the data referenced is accessed in a thread safe manner.
 * Only offset access within the capacity is possible.
 */
public interface BytesStore<B extends BytesStore<B, Underlying>, Underlying>
        extends RandomDataInput, RandomDataOutput<B>, ReferenceCounted, CharSequence {

    /**
     * @deprecated Use from(CharSequence) instead.
     */
    @Deprecated
    static BytesStore wrap(@NotNull CharSequence cs) {
        return from(cs);
    }

    /**
     * This method builds a BytesStore using the bytes in a CharSequence. This chars are encoded using ISO_8859_1
     *
     * @param cs to convert
     * @return BytesStore
     */
    static BytesStore from(@NotNull CharSequence cs) {
        if (cs instanceof BytesStore)
            return ((BytesStore) cs).copy();
        return wrap(cs.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Wraps a byte[].  This means there is one copy in memory.
     *
     * @param bytes to wrap
     * @return BytesStore
     */
    static <B extends BytesStore<B, byte[]>> B wrap(@NotNull byte[] bytes) {
        return (B) (BytesStore) HeapBytesStore.wrap(ByteBuffer.wrap(bytes));
    }

    /**
     * Wraps a ByteBuffer which can be either on heap or off heap.
     *
     * @param bb to wrap
     * @return BytesStore
     */
    static <B extends BytesStore<B, ByteBuffer>> B wrap(@NotNull ByteBuffer bb) {
        return (B) (bb.isDirect()
                ? NativeBytesStore.wrap(bb)
                : HeapBytesStore.wrap(bb));
    }

    /**
     * @return a PointerBytesStore which can be set to any address
     */
    @NotNull
    static PointerBytesStore nativePointer() {
        return new PointerBytesStore();
    }

    /**
     * Return the address and length as a BytesStore
     *
     * @param address for the start
     * @param length  of data
     * @return as a BytesStore
     */
    static PointerBytesStore wrap(long address, long length) {
        PointerBytesStore pbs = nativePointer();
        pbs.set(address, length);
        return pbs;
    }

    /**
     * @return a copy of this BytesStore.
     */
    BytesStore<B, Underlying> copy() throws IllegalArgumentException;

    /**
     * @return a Bytes to wrap this ByteStore from the start() to the realCapacity().
     */
    default Bytes<Underlying> bytesForRead() throws IllegalStateException {
        return bytesForWrite()
                .readLimit(writeLimit());
    }

    /**
     * @return a Bytes for writing to this BytesStore
     */
    default Bytes<Underlying> bytesForWrite() throws IllegalStateException {
        return new VanillaBytes<>(this, writePosition(), writeLimit());
    }

    /**
     * The Bytes are clear if start() == readPosition() &amp;&amp; writeLimit() == capacity()
     *
     * @return is the Bytes clear?
     */
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

    /**
     * @return the underlying object being wrapped, if there is one, or null if not.
     */
    @Nullable
    Underlying underlyingObject();

    /**
     * Use this test to determine if an offset is considered safe.
     */
    default boolean inside(long offset) {
        return start() <= offset && offset < safeLimit();
    }

    /**
     * @return how many bytes can be safely read, i.e. what is the real capacity of the underlying data.
     */
    default long safeLimit() {
        return capacity();
    }

    /**
     * Copy the data to another BytesStore
     *
     * @param store to copy to
     */
    default void copyTo(@NotNull BytesStore store) throws IllegalStateException, IORuntimeException {
        long copy = min(capacity(), store.capacity());
        int i = 0;
        for (; i < copy - 7; i++)
            store.writeLong(i, readLong(i));
        for (; i < copy; i++)
            store.writeByte(i, readByte(i));
    }

    /**
     * Fill the BytesStore with zeros
     *
     * @param start first byte inclusive
     * @param end   last byte exclusive.
     * @return this.
     */
    default B zeroOut(long start, long end) throws IllegalArgumentException, IORuntimeException {
        if (end <= start)
            return (B) this;
        if (start < start())
            throw new IllegalArgumentException(start + " < " + start());
        if (end > capacity())
            throw new IllegalArgumentException(end + " > " + capacity());
        long i = start;
        for (; i < end - 7; i++)
            writeLong(i, 0L);
        for (; i < end; i++)
            writeByte(i, 0);
        return (B) this;
    }

    /**
     * @return length in bytes to read or Integer.MAX_VALUE if longer.
     */
    @Override
    default int length() {
        return (int) Math.min(Integer.MAX_VALUE, readRemaining());
    }

    /**
     * Assume ISO-8859-1 encoding, subclasses can override this.
     */
    @Override
    default char charAt(int index) throws IndexOutOfBoundsException {
        try {
            return (char) readUnsignedByte(readPosition() + index);
        } catch (BufferUnderflowException e) {
            throw new IndexOutOfBoundsException((readPosition() + index) + " >= " + readLimit());
        } catch (IORuntimeException e) {
            throw new IndexOutOfBoundsException(e.toString());
        }
    }

    /**
     * Not supported.
     */
    @NotNull
    @Override
    default CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * @return This BytesStore as a DebugString.
     */
    @NotNull
    default String toDebugString() {
        return BytesInternal.toDebugString(this, Integer.MAX_VALUE);
    }

    /**
     * @return the underlying BytesStore
     */
    default BytesStore bytesStore() {
        return this;
    }

    /**
     * Check if a portion of a BytesStore matches this one.
     *
     * @param bytesStore to match against
     * @param length     to match.
     * @return true if the bytes and length matched.
     */
    default boolean equalBytes(@NotNull BytesStore bytesStore, long length)
            throws BufferUnderflowException, IORuntimeException {
        return length == 8
                ? readLong(readPosition()) == bytesStore.readLong(bytesStore.readPosition())
                : BytesInternal.equalBytesAny(this, bytesStore, length);
    }

    /**
     * Return the bytes sum of the readable bytes.
     *
     * @return unsigned byte sum.
     */
    default int byteCheckSum() throws IORuntimeException {
        byte b = 0;
        for (long i = readPosition(); i < readLimit(); i++)
            b += readByte(i);
        return b & 0xFF;
    }

    /**
     * Return the long sum of the readable bytes.
     *
     * @return signed long sum.
     */
    default long longCheckSum() throws IORuntimeException {
        long sum = 0;
        long i;
        for (i = readPosition(); i < readLimit() - 7; i += 8)
            sum += readLong(i);
        if (i < readLimit())
            sum += readIncompleteLong(i);
        return sum;
    }

    /**
     * Does the BytesStore end with a character?
     *
     * @param c to look for
     * @return true if its the last character.
     */
    default boolean endsWith(char c) throws IORuntimeException {
        return readRemaining() > 0 && readUnsignedByte(readLimit() - 1) == c;
    }

    /**
     * Does the BytesStore start with a character?
     *
     * @param c to look for
     * @return true if its the last character.
     */
    default boolean startsWith(char c) throws IORuntimeException {
        return readRemaining() > 0 && readUnsignedByte(readPosition()) == c;
    }

    /**
     * Compare the contents of the BytesStores.
     *
     * @param bytesStore to compare with
     * @return true if they contain the same data.
     */
    default boolean contentEquals(@Nullable BytesStore bytesStore) throws IORuntimeException {
        return BytesInternal.contentEqual(this, bytesStore);
    }

    default String to8bitString() throws IORuntimeException, IllegalArgumentException {
        return BytesInternal.to8bitString(this);
    }
}
