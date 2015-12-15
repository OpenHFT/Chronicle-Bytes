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
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static java.lang.Math.min;

/**
 * A immutable reference to some bytes with fixed extents. This can be shared safely across thread
 * provided the data referenced is accessed in a thread safe manner. Only offset access within the
 * capacity is possible.
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
     * This method builds a BytesStore using the bytes in a CharSequence. This chars are encoded
     * using ISO_8859_1
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
    static HeapBytesStore<byte[]> wrap(@NotNull byte[] bytes) {
        return HeapBytesStore.wrap(bytes);
    }

    /**
     * Wraps a ByteBuffer which can be either on heap or off heap.
     *
     * @param bb to wrap
     * @return BytesStore
     */
    static BytesStore<?, ByteBuffer> wrap(@NotNull ByteBuffer bb) {
        return bb.isDirect()
                ? NativeBytesStore.wrap(bb)
                : HeapBytesStore.wrap(bb);
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
     * @return how many bytes can be safely read, i.e. what is the real capacity of the underlying
     * data.
     */
    default long safeLimit() {
        return capacity();
    }

    /**
     * Copy the data to another BytesStore as long as there is space available in the destination store.
     *
     * @return how many bytes were copied
     * @param store to copy to
     */
    default long copyTo(@NotNull BytesStore store) throws IllegalStateException, IORuntimeException {
        final long copy = min(readRemaining(), store.capacity());
        long pos = 0L;
        for (long i =0L; i < copy - 7; i+=8L) {
            store.writeLong(i, readLong(i));
            pos+=8L;
        }
        for (long i = pos; i < copy; i++)
            store.writeByte(i, readByte(i));
        return copy;
    }


    default void copyTo(OutputStream out) throws IOException {
        BytesInternal.copy(this, out);
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
        long pos = start;
        for (long i=start; i < end - 7; i++) {
            writeLong(i, 0L);
            pos+=8L;
        }
        for (long i = pos; i < end; i++)
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
     * By default the maximum length of data shown is 256 characters. Use toDebugString(long) if you want more.
     *
     * @return This BytesStore as a DebugString.
     */
    @NotNull
    default String toDebugString() {
        return toDebugString(256);
    }


    /**
     * @param maxLength the maxiumum len of the output
     * @return This BytesStore as a DebugString.
     */
    @NotNull
    default String toDebugString(long maxLength) {
        return BytesInternal.toDebugString(this, maxLength);
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

    /**
     * Perform a <i>not</i> atomic add and get operation for a byte value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default byte addAndGetByteNotAtomic(long offset, byte adding) {
        byte r = (byte) (readByte(offset) + adding);
        writeByte(offset, r);
        return r;
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an unsigned byte value. This method
     * <i>does not</i> check for unsigned byte overflow.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default int addAndGetUnsignedByteNotAtomic(long offset, int adding) {
        int r = (readUnsignedByte(offset) + adding) & 0xFF;
        writeByte(offset, (byte) r);
        return r;
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for a short value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default short addAndGetShortNotAtomic(long offset, short adding) {
        short r = (short) (readShort(offset) + adding);
        writeByte(offset, r);
        return r;
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an unsigned short value. This method
     * <i>does not</i> check for unsigned short overflow.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default int addAndGetUnsignedShortNotAtomic(long offset, int adding) {
        int r = (readUnsignedShort(offset) + adding) & 0xFFFF;
        writeShort(offset, (short) r);
        return r;
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an int value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default int addAndGetIntNotAtomic(long offset, int adding) {
        int r = readInt(offset) + adding;
        writeInt(offset, r);
        return r;
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an unsigned int value. This method
     * <i>does not</i> check for unsigned int overflow.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default long addAndGetUnsignedIntNotAtomic(long offset, long adding) {
        long r = (readUnsignedInt(offset) + adding) & 0xFFFFFFFFL;
        writeInt(offset, (int) r);
        return r;
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for a long value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default long addAndGetLongNotAtomic(long offset, long adding) {
        long r = readLong(offset) + adding;
        writeLong(offset, r);
        return r;
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for a float value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default float addAndGetFloatNotAtomic(long offset, float adding) {
        float r = readFloat(offset) + adding;
        writeFloat(offset, r);
        return r;
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for a double value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default double addAndGetDoubleNotAtomic(long offset, double adding) {
        double r = readDouble(offset) + adding;
        writeDouble(offset, r);
        return r;
    }
}
