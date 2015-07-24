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

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public interface Bytes<Underlying> extends BytesStore<Bytes<Underlying>, Underlying>,
        StreamingDataInput<Bytes<Underlying>>,
        StreamingDataOutput<Bytes<Underlying>>,
        ByteStringParser<Bytes<Underlying>>,
        ByteStringAppender<Bytes<Underlying>> {

    long MAX_CAPACITY = Long.MAX_VALUE; // 8 EiB - 1

    static Bytes<ByteBuffer> elasticByteBuffer() {
        return NativeBytesStore.elasticByteBuffer().bytesForWrite();
    }

    static Bytes<ByteBuffer> wrapForRead(ByteBuffer byteBuffer) {
        return BytesStore.wrap(byteBuffer).bytesForRead();
    }

    static Bytes<ByteBuffer> wrapForWrite(ByteBuffer byteBuffer) {
        return BytesStore.wrap(byteBuffer).bytesForWrite();
    }

    static Bytes<byte[]> expect(String text) {
        return expect(wrapForRead(text.getBytes(StandardCharsets.ISO_8859_1)));
    }

    static <B extends BytesStore<B, Underlying>, Underlying> Bytes<Underlying> expect(BytesStore<B, Underlying> bytesStore) {
        return new VanillaBytes<>(new ExpectedBytesStore<>(bytesStore));
    }

    static Bytes<byte[]> wrapForRead(byte[] byteArray) {
        return BytesStore.<byte[]>wrap(byteArray).bytesForRead();
    }

    static Bytes<byte[]> wrapForWrite(byte[] byteArray) {
        return BytesStore.<byte[]>wrap(byteArray).bytesForWrite();
    }

    static Bytes<byte[]> from(String text) {
        return wrapForRead(text.getBytes(StandardCharsets.UTF_8));
    }

    default Bytes<Underlying> unchecked(boolean unchecked) {
        return unchecked ? new UncheckedBytes<>(this) : this;
    }

    default long safeLimit() {
        return bytesStore().safeLimit();
    }

    static VanillaBytes<Void> allocateDirect(long capacity) {
        return NativeBytesStore.nativeStoreWithFixedCapacity(capacity).bytesForWrite();
    }

    static NativeBytes<Void> allocateElasticDirect() {
        return NativeBytes.nativeBytes();
    }

    static NativeBytes<Void> allocateElasticDirect(long initialCapacity) {
        return NativeBytes.nativeBytes(initialCapacity);
    }


    /**
     * Creates a string from the {@code position} to the {@code limit}, The buffer is not modified
     * by this call
     *
     * @param buffer the buffer to use
     * @return a string contain the text from the {@code position}  to the  {@code limit}
     */
    static String toString(@NotNull final Bytes<?> buffer) {
        if (buffer.readRemaining() == 0)
            return "";
        return buffer.parseWithLength(buffer.readRemaining(), b -> {
            final StringBuilder builder = new StringBuilder();
            while (buffer.readRemaining() > 0) {
                builder.append((char) buffer.readByte());
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
    static String toString(@NotNull final Bytes buffer, long position, long len) {
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

    default boolean isClear() {
        return start() == readPosition() && writeLimit() == capacity();
    }

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
    default String toHexString() {
        return BytesUtil.toHexString(this);
    }

    default String toHexString(long maxLength) {
        if (readRemaining() < maxLength) return toHexString();
        return BytesUtil.toHexString(this, readPosition(), maxLength) + ".... truncated";
    }

    /**
     * @return can the Bytes resize when more data is written than it's realCapacity()
     */
    boolean isElastic();

    /**
     * grow the buffer if the buffer is elastic, if the buffer is not elastic and there is not enough capacity then this
     * method will throws {@link java.nio.BufferOverflowException}
     *
     * @param size the capacity that you required
     * @throws java.nio.BufferOverflowException if the buffer is not elastic and there is not enough space
     */
    default void ensureCapacity(long size) {
        if (size > capacity())
            throw new UnsupportedOperationException(isElastic() ? "todo" : "not elastic");
    }

    /**
     * Creates a slice of the current Bytes based on its position() and limit().  As a sub-section of a Bytes it cannot
     * be elastic.
     *
     * @return a slice of the existing Bytes where the start is moved to the position and the current limit determines
     * the capacity.
     */
    @Override
    default Bytes<Underlying> bytesForRead() {
        return isClear() ? BytesStore.super.bytesForRead() : new SubBytes<>(this, readPosition(), readLimit() + start());
    }
}