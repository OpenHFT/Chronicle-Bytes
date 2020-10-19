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

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceCounted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static java.lang.Math.min;

/**
 * A immutable reference to some bytes with fixed extents. This can be shared safely across thread
 * provided the data referenced is accessed in a thread safe manner. Only offset access within the
 * capacity is possible.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
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
        return NativeBytesStore.from(cs.toString());
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
     * @return a PointerBytesStore which can be set to any addressForRead
     */
    @NotNull
    static PointerBytesStore nativePointer() {
        return new PointerBytesStore();
    }

    /**
     * Return the addressForRead and length as a BytesStore
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
    BytesStore<B, Underlying> copy();

    /**
     * @return a Bytes to wrap this ByteStore from the start() to the realCapacity().
     * @throws IllegalStateException if this Bytes has been released.
     */
    @Override
    @NotNull
    default Bytes<Underlying> bytesForRead() throws IllegalStateException {
        try {
            Bytes<Underlying> ret = bytesForWrite();
            ret.readLimit(writeLimit());
            ret.writeLimit(realCapacity());
            ret.readPosition(start());
            return ret;
        } catch (BufferUnderflowException e) {
            throw new IllegalStateException(e);
        }
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
     * Returns if the {@code readPosition} is at the {@code start} and
     * the {@code writeLimit} is at the {@code end}.
     * <p>
     * I.e {@code start() == readPosition() &amp;&amp; writeLimit() == capacity()}
     *
     * @return if the {@code readPosition} is at the {@code start} and
     * the {@code writeLimit} is at the {@code end}
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

    default boolean inside(long offset, long buffer) {
        return start() <= offset && offset + buffer < safeLimit();
    }

    /**
     * @return how many bytes can be safely read, i.e. what is the real capacity of the underlying data.
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
    default long copyTo(@NotNull BytesStore store) {
        long readPos = readPosition();
        long writePos = store.writePosition();
        long copy = min(readRemaining(), store.capacity());
        long i = 0;
        try {
            for (; i < copy - 7; i += 8)
                store.writeLong(writePos + i, readLong(readPos + i));
            for (; i < copy; i++)
                store.writeByte(writePos + i, readByte(readPos + i));
        } catch (BufferOverflowException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
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
    default B zeroOut(long start, long end) {
        if (end <= start)
            return (B) this;
        if (start < start())
            start = start();
        if (end > capacity())
            end = capacity();
        long i = start;
        try {
            for (; i < end - 7; i += 8L)
                writeLong(i, 0L);
            for (; i < end; i++)
                writeByte(i, 0);
        } catch (BufferOverflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
        return (B) this;
    }

    /**
     * This method is inherited from CharSequence so result should be the length of the contained
     * chars sequence although it actually returns the number of underlying bytes. These 2 numbers are only the same
     * if the encoding we are using is single char for single byte.
     *
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
     * @param maxLength the maximum len of the output
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
     * @return true      if the bytes up to min(length, this.length(), bytesStore.length()) matched.
     */
    default boolean equalBytes(@NotNull BytesStore bytesStore, long length)
            throws BufferUnderflowException {
        return length == 8 && bytesStore.length() >= 8
                ? readLong(readPosition()) == bytesStore.readLong(bytesStore.readPosition())
                : BytesInternal.equalBytesAny(this, bytesStore, length);
    }

    /**
     * Return the bytes sum of the readable bytes.
     *
     * @return unsigned byte sum.
     */
    default int byteCheckSum() throws IORuntimeException {
        return byteCheckSum(readPosition(), readLimit());
    }

    default int byteCheckSum(long start, long end) {
        int sum = 0;
        for (long i = start; i < end; i++) {
            sum += readByte(i);
        }
        return sum & 0xFF;
    }

    /**
     * Return the long sum of the readable bytes.
     *
     * @return signed long sum.
     */
    @Deprecated(/* remove in 1.13 */)
    default long longCheckSum() {
        long sum = 0;
        long i;
        try {
            for (i = readPosition(); i < readLimit() - 7; i += 8)
                sum += readLong(i);
            if (i < readLimit())
                sum += readIncompleteLong(i);
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
        return sum;
    }

    /**
     * Does the BytesStore end with a character?
     *
     * @param c to look for
     * @return true if its the last character.
     */
    default boolean endsWith(char c) {
        try {
            return readRemaining() > 0 && readUnsignedByte(readLimit() - 1) == c;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Does the BytesStore start with a character?
     *
     * @param c to look for
     * @return true if its the last character.
     */
    default boolean startsWith(char c) {
        try {
            return readRemaining() > 0 && readUnsignedByte(readPosition()) == c;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
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
    @Deprecated(/* remove in 1.13 */)
    default byte addAndGetByteNotAtomic(long offset, byte adding) throws BufferUnderflowException {
        try {
            byte r = (byte) (readByte(offset) + adding);
            writeByte(offset, r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an unsigned byte value. This method
     * <i>does not</i> check for unsigned byte overflow.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default int addAndGetUnsignedByteNotAtomic(long offset, int adding) throws BufferUnderflowException {
        try {
            int r = (readUnsignedByte(offset) + adding) & 0xFF;
            writeByte(offset, (byte) r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for a short value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default short addAndGetShortNotAtomic(long offset, short adding) throws BufferUnderflowException {
        try {
            short r = (short) (readShort(offset) + adding);
            writeByte(offset, r);
            return r;
        } catch (BufferOverflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an unsigned short value. This method
     * <i>does not</i> check for unsigned short overflow.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    @Deprecated(/* remove in 1.13 */)
    default int addAndGetUnsignedShortNotAtomic(long offset, int adding) throws BufferUnderflowException {
        try {
            int r = (readUnsignedShort(offset) + adding) & 0xFFFF;
            writeShort(offset, (short) r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an int value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default int addAndGetIntNotAtomic(long offset, int adding) throws BufferUnderflowException {
        try {
            int r = readInt(offset) + adding;
            writeInt(offset, r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an unsigned int value. This method
     * <i>does not</i> check for unsigned int overflow.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    @Deprecated(/* remove in 1.13 */)
    default long addAndGetUnsignedIntNotAtomic(long offset, int adding) throws BufferUnderflowException {
        try {
            int r = readInt(offset) + adding;
            writeInt(offset, r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for a long value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    @Deprecated(/* remove in 1.13 */)
    default long addAndGetLongNotAtomic(long offset, long adding) throws BufferUnderflowException {
        try {
            long r = readLong(offset) + adding;
            writeLong(offset, r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for a float value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default float addAndGetFloatNotAtomic(long offset, float adding) throws BufferUnderflowException {
        try {
            float r = readFloat(offset) + adding;
            writeFloat(offset, r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for a double value.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    @Deprecated(/* remove in 1.13 */)
    default double addAndGetDoubleNotAtomic(long offset, double adding) throws BufferUnderflowException {
        try {
            double r = readDouble(offset) + adding;
            writeDouble(offset, r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Clear and set the flag for present.
     *
     * @param isPresent if there is data, or false if not.
     */
    @Deprecated(/* remove in 1.13 */)
    default void isPresent(boolean isPresent) throws IllegalArgumentException {
        if (!isPresent)
            throw new IllegalArgumentException("isPresent=" + false + " not supported");
    }

    /**
     * @return if there is data, or false if not.
     */
    @Deprecated(/* remove in 1.13 */)
    default boolean isPresent() {
        return true;
    }

    void move(long from, long to, long length) throws BufferUnderflowException;

    /**
     * Write a value which is not smaller.
     *
     * @param offset  to write to
     * @param atLeast value it is at least.
     */
    default void writeMaxLong(long offset, long atLeast) throws BufferUnderflowException {
        try {
            for (; ; ) {
                long v = readVolatileLong(offset);
                if (v >= atLeast)
                    return;
                if (compareAndSwapLong(offset, v, atLeast))
                    return;
            }
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Write a value which is not smaller.
     *
     * @param offset  to write to
     * @param atLeast value it is at least.
     */
    default void writeMaxInt(long offset, int atLeast) throws BufferUnderflowException {
        try {
            for (; ; ) {
                int v = readVolatileInt(offset);
                if (v >= atLeast)
                    return;
                if (compareAndSwapInt(offset, v, atLeast))
                    return;
            }
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    default boolean isEmpty() {
        return readRemaining() == 0;
    }

    default void cipher(@NotNull Cipher cipher, @NotNull Bytes outBytes, @NotNull ByteBuffer using1, @NotNull ByteBuffer using2) throws IllegalStateException {
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

        } catch (@NotNull Exception e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                outBytes.readPosition(readPos);
            } catch (BufferUnderflowException e) {
                //noinspection ThrowFromFinallyBlock
                throw new IllegalStateException(e);
            }
        }
    }

    default void cipher(@NotNull Cipher cipher, @NotNull Bytes outBytes) throws IllegalStateException {
        cipher(cipher, outBytes, BytesInternal.BYTE_BUFFER_TL.get(), BytesInternal.BYTE_BUFFER2_TL.get());
    }

    /**
     * @return whether this BytesStore is writable.
     */
    default boolean readWrite() {
        return true;
    }
}
