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

import net.openhft.chronicle.core.ReferenceCounted;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
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
    @NotNull
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
    @NotNull
    static PointerBytesStore wrap(long address, long length) {
        @NotNull PointerBytesStore pbs = nativePointer();
        pbs.set(address, length);
        return pbs;
    }

    /**
     * @return an empty, fixed sized Bytes
     */
    static BytesStore empty() {
        return NoBytesStore.noBytesStore();
    }

    /**
     * @return whether it uses direct memory or not.
     */
    @Override
    boolean isDirectMemory();

    /**
     * @return a copy of this BytesStore.
     */
    BytesStore<B, Underlying> copy() throws IllegalArgumentException;

    /**
     * @return a Bytes to wrap this ByteStore from the start() to the realCapacity().
     */
    @Override
    @NotNull
    default Bytes<Underlying> bytesForRead() throws IllegalStateException {
        return bytesForWrite()
                .readLimit(writeLimit());
    }

    /**
     * @return a Bytes for writing to this BytesStore
     */
    @Override
    @NotNull
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
    @Override
    default long realCapacity() {
        return capacity();
    }

    /**
     * @return The maximum limit you can set.
     */
    @Override
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
     * Copy the data to another BytesStore  as long as there is space available in the destination store.
     *
     * @param store to copy to
     * @return how many bytes were copied
     */
    default long copyTo(@NotNull BytesStore store) throws IllegalStateException {
        long readPos = readPosition();
        long writePos = store.writePosition();
        long copy = min(readRemaining(), store.capacity());
        long i = 0;
        for (; i < copy - 7; i += 8)
            store.writeLong(writePos + i, readLong(readPos + i));
        for (; i < copy; i++)
            store.writeByte(writePos + i, readByte(readPos + i));
        return copy;
    }

    default void copyTo(@NotNull OutputStream out) throws IOException {
        BytesInternal.copy(this, out);
    }

    /**
     * Fill the BytesStore with zeros
     *
     * @param start first byte inclusive
     * @param end   last byte exclusive.
     * @return this.
     */
    @Override
    @NotNull
    default B zeroOut(long start, long end) throws IllegalArgumentException {
        if (end <= start)
            return (B) this;
        if (start < start())
            throw new IllegalArgumentException(start + " < " + start());
        if (end > capacity())
            throw new IllegalArgumentException(end + " > " + capacity());
        long i = start;
        for (; i < end - 7; i += 8L)
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
        return toDebugString(512);
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
    @Nullable
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
            throws BufferUnderflowException {
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
    default long longCheckSum() {
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
    default boolean endsWith(char c) {
        return readRemaining() > 0 && readUnsignedByte(readLimit() - 1) == c;
    }

    /**
     * Does the BytesStore start with a character?
     *
     * @param c to look for
     * @return true if its the last character.
     */
    default boolean startsWith(char c) {
        return readRemaining() > 0 && readUnsignedByte(readPosition()) == c;
    }

    /**
     * Compare the contents of the BytesStores.
     *
     * @param bytesStore to compare with
     * @return true if they contain the same data.
     */
    default boolean contentEquals(@Nullable BytesStore bytesStore) {
        return BytesInternal.contentEqual(this, bytesStore);
    }

    default boolean startsWith(@Nullable BytesStore bytesStore) {
        return BytesInternal.startsWith(this, bytesStore);
    }

    @NotNull
    default String to8bitString() throws IllegalArgumentException {
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

    /**
     * Clear and set the flag for present.
     *
     * @param isPresent if there is data, or false if not.
     */
    default void isPresent(boolean isPresent) {
        if (!isPresent)
            throw new IllegalArgumentException("isPresent=" + false + " not supported");
    }

    /**
     * @return if there is data, or false if not.
     */
    default boolean isPresent() {
        return true;
    }

    void move(long from, long to, long length);

    /**
     * Write a value which is not smaller.
     *
     * @param offset  to write to
     * @param atLeast value it is at least.
     */
    default void writeMaxLong(long offset, long atLeast) {
        for (; ; ) {
            long v = readVolatileLong(offset);
            if (v >= atLeast)
                return;
            if (compareAndSwapLong(offset, v, atLeast))
                return;
        }
    }

    default boolean isEmpty() {
        return readRemaining() == 0;
    }

    default void cipher(@NotNull Cipher cipher, @NotNull Bytes outBytes, @NotNull ByteBuffer using1, @NotNull ByteBuffer using2) {
        long readPos = outBytes.readPosition();
        try {
            long writePos = outBytes.writePosition();
            BytesStore inBytes;
            long size = readRemaining();
            if (this.isDirectMemory()) {
                inBytes = this;
            } else {
                inBytes = NativeBytesStore.nativeStore(size);
                this.copyTo(inBytes);
            }
            BytesInternal.assignBytesStoreToByteBuffer(inBytes, using1);
            int outputSize = cipher.getOutputSize(Math.toIntExact(size));
            outBytes.ensureCapacity(writePos + outputSize);
            outBytes.readPositionRemaining(writePos, outputSize);
            BytesInternal.assignBytesStoreToByteBuffer(outBytes, using2);
            int len = cipher.update(using1, using2);
            len += cipher.doFinal(using1, using2);
            assert len == using2.position();
            outBytes.writePosition(writePos + using2.position());
        } catch (@NotNull ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException(e);
        } finally {
            outBytes.readPosition(readPos);
        }
    }

    default void cipher(@NotNull Cipher cipher, @NotNull Bytes outBytes) {
        cipher(cipher, outBytes, BytesInternal.BYTE_BUFFER_TL.get(), BytesInternal.BYTE_BUFFER2_TL.get());
    }

    /**
     * @return whether this BytesStore is writable.
     */
    default boolean readWrite() {
        return true;
    }
}
