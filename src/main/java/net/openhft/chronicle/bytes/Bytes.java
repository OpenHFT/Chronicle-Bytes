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

import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Bytes is a pointer to a region of memory within a BytesStore. It can be for a fixed region of
 * memory or an "elastic" buffer which can be resized, but not for a fixed region. <p></p> This is a
 * BytesStore which is mutable and not thread safe. It has a write position and read position which
 * must follow these constraints <p></p> start() &lt;= readPosition() &lt;= writePosition() &lt;=
 * writeLimit() &lt;= capacity() <p></p> Also readLimit() == writePosition() and readPosition()
 * &lt;= safeLimit(); <p></p>
 */
public interface Bytes<Underlying> extends BytesStore<Bytes<Underlying>, Underlying>,
        StreamingDataInput<Bytes<Underlying>>,
        StreamingDataOutput<Bytes<Underlying>>,
        ByteStringParser<Bytes<Underlying>>,
        ByteStringAppender<Bytes<Underlying>>,
        BytesPrepender<Bytes<Underlying>> {

    long MAX_CAPACITY = Long.MAX_VALUE; // 8 EiB - 1

    /**
     * @return an elastic wrapper for a direct ByteBuffer which will be resized as required.
     */
    static Bytes<ByteBuffer> elasticByteBuffer() {
        NativeBytesStore<ByteBuffer> bs = NativeBytesStore.elasticByteBuffer();
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release();
        }
    }

    /**
     * @param byteBuffer to read
     * @return a Bytes which wrap a ByteBuffer and is ready for reading.
     */
    static Bytes<ByteBuffer> wrapForRead(ByteBuffer byteBuffer) {
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            return bs.bytesForRead();
        } finally {
            bs.release();
        }
    }

    /**
     * @param byteBuffer to read
     * @return a Bytes which wrap a ByteBuffer and is ready for writing.
     */
    static Bytes<ByteBuffer> wrapForWrite(ByteBuffer byteBuffer) {
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release();
        }
    }

    /**
     * A Bytes suitable for writing to for testing purposes. It checks the writes made are the
     * expected ones. An AssertionError is thrown if unexpected data is written, an
     * UnsupportedOperationException is thrown if a read is attempted.
     *
     * @param text expected
     * @return the expected buffer as Bytes
     */
    @NotNull
    static Bytes<byte[]> expect(@NotNull String text) {
        return expect(wrapForRead(text.getBytes(StandardCharsets.ISO_8859_1)));
    }

    /**
     * A Bytes suitable for writing to for testing purposes. It checks the writes made are the
     * expected ones. An AssertionError is thrown if unexpected data is written, an
     * UnsupportedOperationException is thrown if a read is attempted.
     *
     * @param bytesStore expected
     * @return the expected buffer as Bytes
     */
    @NotNull
    static <B extends BytesStore<B, Underlying>, Underlying> Bytes<Underlying> expect(BytesStore<B, Underlying> bytesStore) {
        return new VanillaBytes<>(new ExpectedBytesStore<>(bytesStore));
    }

    /**
     * Wrap the byte[] ready for reading
     *
     * @param byteArray to wrap
     * @return the Bytes ready for reading.
     */
    static Bytes<byte[]> wrapForRead(byte[] byteArray) {
        HeapBytesStore<byte[]> bs = BytesStore.wrap(byteArray);
        try {
            return bs.bytesForRead();
        } finally {
            bs.release();
        }
    }

    /**
     * Wrap the byte[] ready for writing
     *
     * @param byteArray to wrap
     * @return the Bytes ready for writing.
     */
    static Bytes<byte[]> wrapForWrite(byte[] byteArray) {
        BytesStore bs = (BytesStore) BytesStore.wrap(byteArray);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release();
        }
    }

    /**
     * Convert text to bytes using ISO-8859-1 encoding and return a Bytes ready for reading.
     *
     * @param text to convert
     * @return Bytes ready for reading.
     */
    static Bytes<byte[]> from(@NotNull CharSequence text) throws IllegalArgumentException, IllegalStateException {
        if (text instanceof BytesStore)
            return ((BytesStore) text).copy().bytesForRead();
        return wrapForRead(text.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    @Deprecated
    static Bytes<byte[]> wrapForRead(@NotNull CharSequence text) throws IllegalArgumentException, IllegalStateException {
        return from(text);
    }

    /**
     * Allocate a fixed size buffer read for writing.
     *
     * @param capacity minimum to allocate
     * @return a new Bytes ready for writing.
     */
    static VanillaBytes<Void> allocateDirect(long capacity) throws IllegalArgumentException {
        NativeBytesStore<Void> bs = NativeBytesStore.nativeStoreWithFixedCapacity(capacity);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release();
        }
    }

    /**
     * Allocate an elastic buffer with initially no size.
     *
     * @return Bytes for writing.
     */
    static NativeBytes<Void> allocateElasticDirect() {
        return NativeBytes.nativeBytes();
    }

    /**
     * Allocate an elastic buffer with initially no size.
     *
     * @return Bytes for writing.
     */
    static NativeBytes<Void> allocateElasticDirect(long initialCapacity) throws IllegalArgumentException {
        return NativeBytes.nativeBytes(initialCapacity);
    }

    /**
     * Creates a string from the {@code position} to the {@code limit}, The buffer is not modified
     * by this call
     *
     * @param buffer the buffer to use
     * @return a string contain the text from the {@code position}  to the  {@code limit}
     */
    static String toString(@NotNull final Bytes<?> buffer) throws BufferUnderflowException {
        if (buffer.readRemaining() == 0)
            return "";
        return buffer.parseWithLength(buffer.readRemaining(), b -> {
            final StringBuilder builder = new StringBuilder();
            try {
                while (buffer.readRemaining() > 0) {
                    builder.append((char) buffer.readByte());
                }
            } catch (IORuntimeException e) {
                builder.append(' ').append(e);
            }

            // remove the last comma
            return builder.toString();
        });
    }

    /**
     * The buffer is not modified by this call
     *
     * @param buffer   the buffer to use
     * @param position the position to create the string from
     * @param len      the number of characters to show in the string
     * @return a string contain the text from offset {@code position}
     */
    static String toString(@NotNull final Bytes buffer, long position, long len)
            throws BufferUnderflowException, IORuntimeException {
        final long pos = buffer.readPosition();
        final long limit = buffer.readLimit();
        buffer.readPosition(position);
        buffer.readLimit(position + len);

        try {

            final StringBuilder builder = new StringBuilder();
            while (buffer.readRemaining() > 0) {
                builder.append((char) buffer.readByte());
            }

            // remove the last comma
            return builder.toString();
        } finally {
            buffer.readLimit(limit);
            buffer.readPosition(pos);
        }
    }

    /**
     * @return an empty, fixed sized Bytes
     */
    static BytesStore empty() {
        return NoBytesStore.noBytesStore();
    }

    /**
     * copies the contents of bytes into a direct byte buffer
     *
     * @param bytes the bytes to wrap
     * @return a direct byte buffer contain the {@code bytes}
     */
    static Bytes allocateDirect(@NotNull byte[] bytes) throws IllegalArgumentException {
        VanillaBytes<Void> result = allocateDirect(bytes.length);
        try {
            result.write(bytes);
        } catch (BufferOverflowException | IORuntimeException e) {
            throw new AssertionError(e);
        }
        return result;
    }

    /**
     * Return a Bytes which is optionally unchecked.  This allows bounds checks to be turned off.
     *
     * @param unchecked if true, minimal bounds checks will be performed.
     * @return Bytes without bounds checking.
     * @throws IllegalStateException if the underlying BytesStore has been released
     */
    default Bytes<Underlying> unchecked(boolean unchecked) throws IllegalStateException {
        return unchecked ?
                start() == 0 && bytesStore() instanceof NativeBytesStore ?
                        new UncheckedNativeBytes<>(this) :
                        new UncheckedBytes<>(this) :
                this;
    }

    /**
     * @return the size which can be safely read.  If this isElastic() it can be lower than the
     * point it can safely write.
     */
    default long safeLimit() {
        return bytesStore().safeLimit();
    }

    /**
     * @return is the readPosition at the start and the writeLimit at the end.
     */
    default boolean isClear() {
        return start() == readPosition() && writeLimit() == capacity();
    }

    /**
     * @return if isElastic, this can be much lower than the virtual capacity().
     */
    default long realCapacity() {
        return BytesStore.super.realCapacity();
    }

    /**
     * @return a copy of this Bytes from position() to limit().
     */
    BytesStore<Bytes<Underlying>, Underlying> copy();

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString() throws IORuntimeException {
        return BytesInternal.toHexString(this);
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param maxLength limit the number of bytes to be dumped.
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString(long maxLength)
            throws IORuntimeException {
        if (readRemaining() < maxLength) return toHexString();
        return BytesInternal.toHexString(this, readPosition(), maxLength) + ".... truncated";
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param maxLength limit the number of bytes to be dumped.
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString(long offset, long maxLength)
            throws IORuntimeException {
        long maxLength2 = Math.min(maxLength, readLimit() - offset);
        String ret = BytesInternal.toHexString(this, offset, maxLength2);
        return maxLength2 < maxLength ? ret + "... truncated" : ret;
    }

    /**
     * @return can the Bytes resize when more data is written than it's realCapacity()
     */
    boolean isElastic();

    /**
     * grow the buffer if the buffer is elastic, if the buffer is not elastic and there is not
     * enough capacity then this method will throws {@link java.nio.BufferOverflowException}
     *
     * @param size the capacity that you required
     * @throws IllegalArgumentException if the buffer is not elastic and there is not enough space
     * @throws IORuntimeException       if an error occured trying to resize the underlying buffer.
     */
    default void ensureCapacity(long size)
            throws IllegalArgumentException, IORuntimeException {
        if (size > capacity())
            throw new IllegalArgumentException(isElastic() ? "todo" : "not elastic");
    }

    /**
     * Creates a slice of the current Bytes based on its position() and limit().  As a sub-section
     * of a Bytes it cannot be elastic.
     *
     * @return a slice of the existing Bytes where the start is moved to the position and the
     * current limit determines the capacity.
     * @throws IllegalStateException if the underlying BytesStore has been released
     */
    @Override
    default Bytes<Underlying> bytesForRead() throws IllegalStateException {
        return isClear() ? BytesStore.super.bytesForRead() : new SubBytes<>(this, readPosition(), readLimit() + start());
    }

    /**
     * @return the ByteStore this Bytes wraps.
     */
    BytesStore bytesStore();

    default boolean isEqual(String s) {
        return StringUtils.isEqual(this, s);
    }

}