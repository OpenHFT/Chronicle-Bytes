/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash;
import net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.internal.HeapBytesStore;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
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
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static java.lang.Math.min;
import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * An immutable reference to some bytes with fixed extents. This can be shared safely across thread
 * provided the data referenced is accessed in a thread safe manner. Only offset access within the
 * capacity is possible.
 *
 * @param <B> BytesStore type
 * @param <U> Underlying type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesStore<B extends BytesStore<B, U>, U>
        extends RandomDataInput, RandomDataOutput<B>, ReferenceCounted, CharSequence {

    @SuppressWarnings("deprecation")
    @Override
    default boolean compareAndSwapFloat(@NonNegative long offset, float expected, float value)
            throws BufferOverflowException, IllegalStateException {
        return compareAndSwapInt(offset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(value));
    }

    @SuppressWarnings("deprecation")
    @Override
    default boolean compareAndSwapDouble(@NonNegative long offset, double expected, double value)
            throws BufferOverflowException, IllegalStateException {
        return compareAndSwapLong(offset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value));
    }

    @SuppressWarnings("deprecation")
    @Override
    default int addAndGetInt(@NonNegative long offset, int adding)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.addAndGetInt(this, offset, adding);
    }

    @SuppressWarnings("deprecation")
    @Override
    default long addAndGetLong(@NonNegative long offset, long adding)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.addAndGetLong(this, offset, adding);
    }

    @SuppressWarnings("deprecation")
    @Override
    default float addAndGetFloat(@NonNegative long offset, float adding)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.addAndGetFloat(this, offset, adding);
    }

    @SuppressWarnings("deprecation")
    @Override
    default double addAndGetDouble(@NonNegative long offset, double adding)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.addAndGetDouble(this, offset, adding);
    }

    /**
     * Returns a BytesStore using the bytes in a specified CharSequence. These chars are encoded
     * using ISO_8859_1
     *
     * @param cs a CharSequence to convert
     * @return a BytesStore which contains the bytes in cs
     */
    static BytesStore from(@NotNull CharSequence cs)
            throws IllegalStateException {
        if (cs.length() == 0)
            return empty();
        if (cs instanceof BytesStore)
            return from((BytesStore) cs);
        return from(cs.toString());
    }

    /**
     * Returns a BytesStore using the bytes in another specified BytesStore.
     *
     * @param cs a BytesStore
     * @return the resulting BytesStore
     * @throws IllegalStateException
     */
    static BytesStore from(@NotNull BytesStore cs)
            throws IllegalStateException {
        return cs.copy();
    }

    /**
     * Returns a BytesStore using the bytes in a String. The characters in the String are encoded
     * using ISO_8859_1
     *
     * @param cs a String
     * @return BytesStore
     */
    static BytesStore from(@NotNull String cs) {
        return cs.length() == 0 ? empty() : BytesStore.wrap(cs.getBytes(StandardCharsets.ISO_8859_1));
    }

    static <T> BytesStore<?, T> forFields(Object o, String groupName, int padding) {
        return HeapBytesStore.forFields(o, groupName, padding);
    }

    /**
     * Wraps a byte[]. This means there is one copy in memory.
     *
     * @param bytes to wrap
     * @return BytesStore
     */
    static BytesStore<?, byte[]> wrap(byte[] bytes) {
        return HeapBytesStore.wrap(bytes);
    }

    /**
     * Wraps a ByteBuffer which can be either on heap or off heap.
     * <p>
     * When resulting BytesStore instance is closed, direct {@code byteBuffer} will be deallocated and should
     * no longer be used.
     *
     * @param bb the ByteBuffer to wrap
     * @return BytesStore
     */
    @NotNull
    static BytesStore<?, ByteBuffer> wrap(@NotNull ByteBuffer bb) {
        return bb.isDirect()
                ? NativeBytesStore.wrap(bb)
                : HeapBytesStore.wrap(bb);
    }

    /**
     * This is an elastic native store.
     *
     * @param capacity of the buffer
     */
    static BytesStore<?, Void> nativeStore(@NonNegative long capacity) {
        return NativeBytesStore.nativeStore(capacity);
    }

    static BytesStore<?, Void> nativeStoreWithFixedCapacity(@NonNegative long capacity) {
        return NativeBytesStore.nativeStoreWithFixedCapacity(capacity);
    }

    static BytesStore<?, Void> lazyNativeBytesStoreWithFixedCapacity(@NonNegative long capacity) {
        return NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(capacity);
    }

    static BytesStore<?, ByteBuffer> elasticByteBuffer(@NonNegative int size, @NonNegative long maxSize) {
        return NativeBytesStore.elasticByteBuffer(size, maxSize);
    }

    /**
     * Creates and returns a new BytesStore that resides in native memory whereby the contents and
     * size of the native memory is determined by the provided {@code bytes} array.
     *
     * @param bytes the content to initialize the new ByteStore
     * @return a new BytesStore that resides in native memory whereby the contents and
     * size of the native memory is determined by the provided {@code bytes} array
     * @throws AssertionError if the method fails for any reason
     */
    static BytesStore<?, Void> nativeStoreFrom(byte[] bytes) {
        Objects.requireNonNull(bytes);
        return NativeBytesStore.from(bytes);
    }

    /**
     * @return a PointerBytesStore which can be set to any addressForRead
     */
    @NotNull
    static PointerBytesStore nativePointer() {
        return new PointerBytesStore();
    }

    /**
     * Returns a PointerBytesStore that wraps bytes from a starting address in memory.
     *
     * @param address the start address of PointerBytesStore
     * @param length  the length of PointerBytesStore
     * @return a PointerBytesStore
     */
    @NotNull
    static PointerBytesStore wrap(long address, @NonNegative long length) {
        @NotNull PointerBytesStore pbs = nativePointer();
        pbs.set(address, length);
        return pbs;
    }

    /**
     * @return an empty, fixed-sized immutable BytesStore.
     */
    static BytesStore empty() {
        byte[] noBytes = null;
        return HeapBytesStore.wrap(noBytes);
    }

    /**
     * @return whether it uses direct memory or not.
     */
    @Override
    boolean isDirectMemory();

    /**
     * @return a copy of this BytesStore.
     */
    BytesStore<B, U> copy()
            throws IllegalStateException;

    /**
     * Returns a Bytes that wraps this ByteStore from the {@code start} to the {@code realCapacity}.
     * <p>
     * The returned Bytes is not elastic and can be both read and written using cursors.
     *
     * @return a Bytes that wraps this ByteStore
     * @throws IllegalStateException if this Bytes has been released
     */
    @Override
    @NotNull
    default Bytes<U> bytesForRead()
            throws IllegalStateException {
        try {
            Bytes<U> ret = bytesForWrite();
            ret.readLimit(writeLimit());
            ret.writeLimit(realCapacity());
            ret.readPosition(start());
            return ret;
        } catch (BufferUnderflowException | BufferOverflowException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a Bytes that wraps this ByteStore from the {@code start} to the {@code realCapacity}.
     * <p>
     * The returned Bytes is not elastic and can be both read and written using cursors.
     *
     * @return a Bytes that wraps this BytesStore
     * @throws IllegalStateException if this BytesStore has been released
     */
    @Override
    @NotNull
    default Bytes<U> bytesForWrite()
            throws IllegalStateException {
        try {
            return new VanillaBytes<>(this, writePosition(), writeLimit());
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns if the {@code readPosition} is at the {@code start} and
     * the {@code writeLimit} is at the {@code end}.
     * <p>
     * I.e {@code start() == readPosition() && writeLimit() == capacity()}
     *
     * @return if the {@code readPosition} is at the {@code start} and
     * the {@code writeLimit} is at the {@code end}
     */
    default boolean isClear() {
        return true;
    }

    /**
     * @return the actual capacity available before resizing
     */
    @Override
    default @NonNegative long realCapacity() {
        return capacity();
    }

    /**
     * @return The maximum limit you can set.
     */
    @Override
    @NonNegative
    long capacity();

    /**
     * @return the underlying object being wrapped, if there is one, or null if not.
     */
    @Nullable
    U underlyingObject();

    /**
     * Returns if a specified offset is inside this BytesStore limits.
     * <p>
     * Use this test to determine if an offset is considered safe for reading from. Note that it checks we are
     * inside the BytesStore limits *without* including the overlap
     *
     * @param offset the specified offset to check
     * @return <code>true</code> if offset is safe
     */
    default boolean inside(@NonNegative long offset) {
        return start() <= offset && offset < safeLimit();
    }

    /**
     * Returns if a number of bytes starting from an offset are inside this ByteStore limits.
     * If you are going to write n bytes starting at offset, you need to call this method with buffer=n-1,
     * as the 1st byte is written at offset, and the last at offset+n-1
     * <p>
     * Use this test to determine if an offset is considered safe to write to. Note that it checks we are
     * inside the BytesStore limits *including* the overlap
     *
     * @param offset the starting index to check
     * @param buffer the number of bytes after the offset to check
     * @return <code>true</code> if the bytes between the offset and offset+buffer are inside the BytesStore
     */
    default boolean inside(@NonNegative long offset, @NonNegative long buffer) {
        return start() <= offset && offset + buffer < safeLimit();
    }

    /**
     * @return how many bytes can be safely read, i.e. what is the real capacity of the underlying data.
     */
    default long safeLimit() {
        return capacity();
    }

    /**
     * Returns the number of bytes that were copied from this BytesStore to a destination BytesStore.
     * <p>
     * Copies the data to another BytesStore as long as space is available in the destination BytesStore.
     *
     * @param store the BytesStore to copy to
     * @return how many bytes were copied
     * @throws IllegalStateException if this BytesStore has been released
     */
    default long copyTo(@NotNull BytesStore store)
            throws IllegalStateException {
        requireNonNull(store);
        throwExceptionIfReleased(this);
        throwExceptionIfReleased(store);
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

    /**
     * Copies the bytes in the BytesStore to an OutputStream object.
     *
     * @param out the specified OutputStream that this BytesStore is copied to
     * @throws IllegalStateException if this Bytes has been released
     * @see java.io.OutputStream
     */
    default void copyTo(@NotNull OutputStream out)
            throws IOException, IllegalStateException {
        BytesInternal.copy(this, out);
    }

    /**
     * Fills the BytesStore with zeros.
     *
     * @param start first byte inclusive
     * @param end   last byte exclusive
     * @return this
     */
    @Override
    @NotNull
    default B zeroOut(@NonNegative long start, @NonNegative long end)
            throws IllegalStateException {
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
        } catch (BufferOverflowException | IllegalArgumentException | ArithmeticException e) {
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
    @NonNegative
    default int length() {
        return (int) Math.min(Integer.MAX_VALUE, readRemaining());
    }

    /**
     * Assume ISO-8859-1 encoding, subclasses can override this.
     */
    @Override
    default char charAt(@NonNegative int index)
            throws IndexOutOfBoundsException {
        try {
            return (char) readUnsignedByte(readPosition() + index);

        } catch (BufferUnderflowException e) {
            throw new IndexOutOfBoundsException((readPosition() + index) + " >= " + readLimit());
        } catch (IllegalStateException e) {
            throw Jvm.rethrow(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <br>
     * This method constructs a new {@link BytesStore}, memory storage type (heap or native) is preserved.
     */
    @NotNull
    @Override
    default CharSequence subSequence(@NonNegative int start, @NonNegative int end) {
        if (start < 0 || end > length() || end < start)
            throw new IndexOutOfBoundsException("start " + start + ", end " + end + ", length " + length());

        return subBytes(readPosition() + start, (long) end - start);
    }

    /**
     * By default the maximum length of data shown is 256 characters. Use {@link #toDebugString(long)} if you want more.
     *
     * @return this BytesStore as a DebugString
     */
    @NotNull
    default String toDebugString()
            throws IllegalStateException {
        try {
            return toDebugString(512);
        } catch (ArithmeticException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @param maxLength the maximum length of the output
     * @return this BytesStore as a DebugString.
     */
    @NotNull
    default String toDebugString(@NonNegative long maxLength)
            throws IllegalStateException, ArithmeticException {
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
     * Returns if a portion of a specified BytesStore matches this BytesStore.
     *
     * @param bytesStore the BytesStore to match against
     * @param length     the length to match
     * @return <code>true</code> if the bytes up to min(length, this.length(), bytesStore.length()) matched.
     */
    default boolean equalBytes(@NotNull BytesStore bytesStore, @NonNegative long length)
            throws BufferUnderflowException, IllegalStateException {
        return length == 8 && bytesStore.length() >= 8
                ? readLong(readPosition()) == bytesStore.readLong(bytesStore.readPosition())
                : BytesInternal.equalBytesAny(this, bytesStore, length);
    }

    /**
     * Returns the bytes sum of the readable bytes in this BytesStore.
     *
     * @return unsigned bytes sum
     * @throws IllegalStateException if the BytesStore has been released
     */
    default int byteCheckSum()
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        try {
            return byteCheckSum(readPosition(), readLimit());
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns the bytes sum between the specified indexes; start (inclusive) and end (exclusive).
     *
     * @param start the index of the first byte to sum
     * @param end   the index of the last byte to sum
     * @return unsigned bytes sum
     * @throws BufferUnderflowException if the specified indexes are outside the limits of the BytesStore
     * @throws IllegalStateException    if the BytesStore has been released
     */
    default int byteCheckSum(@NonNegative long start, @NonNegative long end)
            throws BufferUnderflowException, IllegalStateException {
        int sum = 0;
        for (long i = start; i < end; i++) {
            sum += readByte(i);
        }
        return sum & 0xFF;
    }

    /**
     * Returns if the BytesStore ends with a specified character.
     *
     * @param c the character to look for
     * @return <code>true</code> if the specified character is the same as the last character of this BytesStore
     */
    default boolean endsWith(char c)
            throws IllegalStateException {
        try {
            return readRemaining() > 0 && readUnsignedByte(readLimit() - 1) == c;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns if the BytesStore starts with a specified character.
     *
     * @param c the character to look for
     * @return <code>true</code> if the specified character is the same as the first character of this BytesStore
     */
    default boolean startsWith(char c)
            throws IllegalStateException {
        try {
            return readRemaining() > 0 && readUnsignedByte(readPosition()) == c;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns if the content of this BytesStore is the same as the content of a specified BytesStore.
     *
     * @param bytesStore the BytesStore to compare with
     * @return <code>true</code> if this BytesStore and the input BytesStore contain the same data
     * @throws IllegalStateException if this BytesStore has been released
     */
    default boolean contentEquals(@Nullable BytesStore bytesStore)
            throws IllegalStateException {
        return BytesInternal.contentEqual(this, bytesStore);
    }

    /**
     * Returns if the content of this BytesStore starts with bytes equal to the content of a specified BytesStore.
     *
     * @param bytesStore the BytesStore to compare with
     * @return <code>true</code> if the content of this BytesStore starts with bytesStore
     * @throws IllegalStateException if this BytesStore has been released
     */
    default boolean startsWith(@Nullable BytesStore bytesStore)
            throws IllegalStateException {
        return bytesStore != null && BytesInternal.startsWith(this, bytesStore);
    }

    /**
     * Returns the content of this BytesStore in 8bitString format.
     *
     * @return a String from the content of this BytesStore
     */
    @NotNull
    default String to8bitString() {
        return BytesInternal.to8bitString(this);
    }

    /**
     * Perform a <i>not</i> atomic add and get operation for an unsigned byte value. This method
     * <i>does not</i> check for unsigned byte overflow.
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     */
    default int addAndGetUnsignedByteNotAtomic(@NonNegative long offset, int adding)
            throws BufferUnderflowException, IllegalStateException {
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
    default short addAndGetShortNotAtomic(@NonNegative long offset, short adding)
            throws BufferUnderflowException, IllegalStateException {
        try {
            short r = (short) (readShort(offset) + adding);
            writeByte(offset, r);
            return r;
        } catch (BufferOverflowException | IllegalArgumentException | ArithmeticException e) {
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
    default int addAndGetIntNotAtomic(@NonNegative long offset, int adding)
            throws BufferUnderflowException, IllegalStateException {
        try {
            int r = readInt(offset) + adding;
            writeInt(offset, r);
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
    default double addAndGetDoubleNotAtomic(@NonNegative long offset, double adding)
            throws BufferUnderflowException, IllegalStateException {
        try {
            double r = readDouble(offset) + adding;
            writeDouble(offset, r);
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
    default float addAndGetFloatNotAtomic(@NonNegative long offset, float adding)
            throws BufferUnderflowException, IllegalStateException {
        try {
            float r = readFloat(offset) + adding;
            writeFloat(offset, r);
            return r;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    void move(@NonNegative long from, @NonNegative long to, @NonNegative long length)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException;

    /**
     * Writes a long value at a specified offset if the value is not smaller than the current value at that offset.
     *
     * @param offset  the offset to write to
     * @param atLeast the long value that is to be written at offset if it is not less than the current value at offset
     */
    default void writeMaxLong(@NonNegative long offset, long atLeast)
            throws BufferUnderflowException, IllegalStateException {
        try {
            for (; ; ) {
                long v = readVolatileLong(offset);
                if (v >= atLeast)
                    return;
                if (compareAndSwapLong(offset, v, atLeast))
                    return;
                Jvm.nanoPause();
            }
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Writes an int value at a specified offset if the value is not smaller than the current value at that offset.
     *
     * @param offset  the offset to write to
     * @param atLeast the int value that is to be written at offset if it is not less than the current value at offset
     */
    default void writeMaxInt(@NonNegative long offset, int atLeast)
            throws BufferUnderflowException, IllegalStateException {
        try {
            for (; ; ) {
                int v = readVolatileInt(offset);
                if (v >= atLeast)
                    return;
                if (compareAndSwapInt(offset, v, atLeast))
                    return;
                Jvm.nanoPause();
            }
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @return <code>true</code> if the number of readable bytes of this BytesStore is zero.
     */
    default boolean isEmpty() {
        return readRemaining() == 0;
    }

    default void cipher(@NotNull Cipher cipher, @NotNull Bytes<?> outBytes, @NotNull ByteBuffer using1, @NotNull ByteBuffer using2)
            throws IllegalStateException {
        final long readPos = outBytes.readPosition();
        try {
            long writePos = outBytes.writePosition();
            BytesStore inBytes;
            long size = readRemaining();
            if (this.isDirectMemory()) {
                inBytes = this;
            } else {
                inBytes = BytesStore.nativeStore(size);
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
            // This would never fail as readPos is final and was valid from the beginning.
            outBytes.readPosition(readPos);
        }
    }

    default void cipher(@NotNull Cipher cipher, @NotNull Bytes<?> outBytes)
            throws IllegalStateException {
        cipher(cipher, outBytes, BytesInternal.BYTE_BUFFER_TL.get(), BytesInternal.BYTE_BUFFER2_TL.get());
    }

    /**
     * Returns if this ByteStore can be both read from and written to.
     * <p>
     * This is in contrast to a ByteStore than can only be read.
     *
     * @return if this ByteStore can be both read from and written to
     */
    default boolean readWrite() {
        return true;
    }

    default long hash(long length) {
        return bytesStore() instanceof NativeBytesStore
                ? OptimisedBytesStoreHash.INSTANCE.applyAsLong(this, length)
                : VanillaBytesStoreHash.INSTANCE.applyAsLong(this, length);
    }

    /**
     * Returns if a specified portion of this BytesStore is equal to a specified String.
     * The portion is specified with its offset and length.
     *
     * @param start  the portion offset
     * @param length the number of bytes from this BytesStore that should be compared to s
     * @param s      the String to compare to
     * @return <code>true</code> if the specified portion of this BytesStore is equal to s
     */
    default boolean isEqual(@NonNegative long start, @NonNegative long length, String s) {
        if (s == null || s.length() != length)
            return false;
        int length2 = (int) length;
        for (int i = 0; i < length2; i++)
            if (s.charAt(i) != readUnsignedByte(start + i))
                return false;
        return true;
    }

    // Can be removed once RandomDataInput:compareAndSwapInt is removed
    // To be removed in x.25
    @SuppressWarnings("deprecation")
    @Override
    default boolean compareAndSwapInt(long offset, int expected, int value) throws BufferOverflowException, IllegalStateException {
        return ((RandomDataOutput<B>) this).compareAndSwapInt(offset, expected, value);
    }

    // Can be removed once RandomDataInput:compareAndSwapLong is removed
    // To be removed in x.25
    @SuppressWarnings("deprecation")
    @Override
    default boolean compareAndSwapLong(long offset, long expected, long value) throws BufferOverflowException, IllegalStateException {
        return ((RandomDataOutput<B>) this).compareAndSwapLong(offset, expected, value);
    }

    /**
     * Returns if this ByteStore is an immutable empty ByteStore or if it is backed
     * by an immutable empty ByteStore.
     *
     * @return if immutable empty or backed by such
     */
    @Deprecated(/* to be removed in x.25 */)
    default boolean isImmutableEmptyByteStore() {
        return capacity() == 0;
    }

}
