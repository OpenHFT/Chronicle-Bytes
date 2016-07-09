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

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
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
public interface Bytes<Underlying> extends
        BytesStore<Bytes<Underlying>, Underlying>,
        BytesIn<Underlying>,
        BytesOut<Underlying> {

    long MAX_CAPACITY = Long.MAX_VALUE; // 8 EiB - 1
    int DEFAULT_BYTE_BUFFER_CAPACITY = 256;

    /**
     * @return an elastic wrapper for a direct ByteBuffer which will be resized as required.
     */
    static Bytes<ByteBuffer> elasticByteBuffer() {
        return elasticByteBuffer(DEFAULT_BYTE_BUFFER_CAPACITY);
    }

    static Bytes<ByteBuffer> elasticByteBuffer(int initialCapacity, int maxSize) {
        NativeBytesStore<ByteBuffer> bs = NativeBytesStore.elasticByteBuffer(initialCapacity, maxSize);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release();
        }
    }

    /**
     * Returns an elastic wrapper for a direct ByteBuffer which will be resized as required, with
     * the given initial capacity.
     */
    static Bytes<ByteBuffer> elasticByteBuffer(int initialCapacity) {
        return elasticByteBuffer(initialCapacity, Integer.MAX_VALUE & ~(OS.pageSize() - 1));
    }

    /**
     * @param byteBuffer to read
     * @return a Bytes which wrap a ByteBuffer and is ready for reading.
     */
    static Bytes<ByteBuffer> wrapForRead(ByteBuffer byteBuffer) {
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            Bytes<ByteBuffer> bbb = bs.bytesForRead();
            bbb.readLimit(byteBuffer.limit());
            bbb.readPosition(byteBuffer.position());
            return bbb;
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
            Bytes<ByteBuffer> bbb = bs.bytesForWrite();
            bbb.writePosition(byteBuffer.position());
            bbb.writeLimit(byteBuffer.limit());
            return bbb;
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
        return toString(buffer, Integer.MAX_VALUE - 4);
    }

    /**
     * Creates a string from the {@code position} to the {@code limit}, The buffer is not modified
     * by this call
     *
     * @param buffer the buffer to use
     * @param maxLen of the result returned
     * @return a string contain the text from the {@code position}  to the  {@code limit}
     */
    static String toString(@NotNull final Bytes<?> buffer, long maxLen) throws
            BufferUnderflowException {
        if (buffer.readRemaining() == 0)
            return "";

        final long length = Math.min(maxLen + 1, buffer.readRemaining());

        final StringBuilder builder = new StringBuilder();
        try {
            buffer.readWithLength(length, b -> {
                while (buffer.readRemaining() > 0) {
                    if (builder.length() >= maxLen) {
                        builder.append("...");
                        break;
                    }
                    builder.append((char) buffer.readByte());
                }
            });
        } catch (Exception e) {
            builder.append(' ').append(e);
        }
        return builder.toString();
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
            throws BufferUnderflowException {
        final long pos = buffer.readPosition();
        final long limit = buffer.readLimit();
        buffer.readPositionRemaining(position, len);

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
     * copies the contents of bytes into a direct byte buffer
     *
     * @param bytes the bytes to wrap
     * @return a direct byte buffer contain the {@code bytes}
     */
    static Bytes allocateDirect(@NotNull byte[] bytes) throws IllegalArgumentException {
        Bytes<Void> result = allocateDirect(bytes.length);
        try {
            result.write(bytes);
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
        return result;
    }

    static Bytes fromHexString(String s) {
        return BytesInternal.fromHexString(s);
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
                start() == 0 && bytesStore().isDirectMemory() ?
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
    default String toHexString() {
        return toHexString(1024);
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param maxLength limit the number of bytes to be dumped.
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString(long maxLength) {
        return toHexString(readPosition(), maxLength);
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param maxLength limit the number of bytes to be dumped.
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString(long offset, long maxLength) {
        long maxLength2 = Math.min(maxLength, readLimit() - offset);
        String ret = BytesInternal.toHexString(this, offset, maxLength2);
        return maxLength2 < readLimit() - offset ? ret + "... truncated" : ret;
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
     */
    default void ensureCapacity(long size) throws IllegalArgumentException {
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

    /**
     * Compact these Bytes by moving the readPosition to the start.
     *
     * @return this
     */
    Bytes<Underlying> compact();

    /**
     * copy bytes from one ByteStore to another
     *
     * @param store to copy to
     * @return the number of bytes copied.
     */
    @Override
    default long copyTo(@NotNull BytesStore store) {
        return BytesStore.super.copyTo(store);
    }

    @Override
    default void copyTo(OutputStream out) throws IOException {
        BytesStore.super.copyTo(out);
    }

    @Override
    default boolean sharedMemory() {
        return bytesStore().sharedMemory();
    }
}