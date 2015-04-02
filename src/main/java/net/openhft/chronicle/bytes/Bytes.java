/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

public interface Bytes<Underlying> extends BytesStore<Bytes<Underlying>, Underlying>,
        StreamingDataInput<Bytes<Underlying>, Access<Underlying>, Underlying>,
        StreamingDataOutput<Bytes<Underlying>, Access<Underlying>, Underlying>,
        ByteStringParser<Bytes<Underlying>, Access<Underlying>, Underlying>,
        ByteStringAppender<Bytes<Underlying>, Access<Underlying>, Underlying>,
        CharSequence {

    long position();

    /**
     * Sets this buffer's mark at its position.
     *
     * @return This buffer
     * @deprecated Don't use mark() or reset(), use a lambda method like withLength
     */
    @Deprecated
    Bytes mark();

    /**
     * Resets this buffer's position to the previously-marked position.
     *
     * Invoking this method neither changes nor discards the mark's value.
     *
     * @return This buffer
     * @throws InvalidMarkException If the mark has not been set
     * @deprecated Don't use mark() or reset(), use a lambda method like withLength
     */
    @Deprecated
    Bytes reset() throws InvalidMarkException;

    Bytes<Underlying> position(long position);

    long limit();

    Bytes<Underlying> limit(long limit);

    static Bytes<ByteBuffer> elasticByteBuffer() {
        return NativeBytesStore.elasticByteBuffer().bytes();

    }

    static Bytes<ByteBuffer> wrap(ByteBuffer byteBuffer) {
        return BytesStore.wrap(byteBuffer).bytes();
    }

    static Bytes<byte[]> wrap(byte[] byteArray) {
        return BytesStore.<byte[]>wrap(byteArray).bytes();
    }

    @Override
    default int length() {
        return (int) Math.min(remaining(), Integer.MAX_VALUE);
    }

    @Override
    default char charAt(int offset) {
        return (char) readUnsignedByte(position() + offset);
    }

    @Override
    default String subSequence(int start, int end) {
        throw new UnsupportedOperationException();
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
     * @throws java.nio.BufferOverflowException if the buffer is not elastic and there is not enough
     *                                          space
     */
    default void ensureCapacity(long size) {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param buffer the buffer you wish to toString()
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    public static String toHex(@NotNull final Bytes buffer) {
        return toHex(buffer, buffer.position(), buffer.remaining());
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param buffer the buffer you wish to toString()
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    public static String toHex(@NotNull final Bytes buffer, long offset, long len) {

        if (len == 0)
            return "";

        long position = buffer.position();
        long limit = buffer.limit();

        try {

            buffer.limit(offset + len);
            buffer.position(offset);

            final StringBuilder builder = new StringBuilder("[");

            while (buffer.remaining() > 0) {

                long pos = buffer.position();
                byte b = buffer.readByte();
                char c = (char) b;
                builder.append(c + "(" + String.format("%02X ", b).trim() + ")[" + pos + "]");
                builder.append(",");
            }

            // remove the last comma
            builder.deleteCharAt(builder.length() - 1);
            builder.append("]");
            return builder.toString();
        } finally {
            buffer.limit(limit);
            buffer.position(position);

        }
    }

    /**
     * Creates a string from the {@code position} to the  {@code limit}, The buffer is not modified
     * by this call
     *
     * @param buffer the buffer to use
     * @return a string contain the text from the {@code position}  to the  {@code limit}
     */
    public static String toDebugString(@NotNull final Bytes buffer) {

        if (buffer.remaining() == 0)
            return "";

        long position = buffer.position();
        long limit = buffer.limit();

        try {

            final StringBuilder builder = new StringBuilder();
            while (buffer.remaining() > 0) {
                builder.append((char) buffer.readByte());
            }

            // remove the last comma
            return builder.toString();
        } finally {
            buffer.limit(limit);
            buffer.position(position);
        }
    }

    /**
     * Creates a string from the {@code position} to the  {@code limit}, The buffer is not modified
     * by this call
     *
     * @param buffer the buffer to use
     * @return a string contain the text from the {@code position}  to the  {@code limit}
     */
    public static String toDebugString(@NotNull final ByteBuffer buffer) {

        if (buffer.remaining() == 0)
            return "";

        int position = buffer.position();
        int limit = buffer.limit();

        try {

            final StringBuilder builder = new StringBuilder();
            while (buffer.remaining() > 0) {
                builder.append((char) buffer.get());
            }

            // remove the last comma
            return builder.toString();
        } finally {
            buffer.limit(limit);
            buffer.position(position);
        }
    }

    /**
     * The buffer is not modified by this call
     *
     * @param buffer   the buffer to use
     * @param position the position to create the string from
     * @param len      the number of characters to show in the string
     * @return a string contain the text from offset {@code position}
     */
    public static String toDebugString(@NotNull final Bytes buffer, long position, long len) {

        final long pos = buffer.position();
        final long limit = buffer.readLimit();
        buffer.position(position);
        buffer.limit(position + len);

        try {

            final StringBuilder builder = new StringBuilder();
            while (buffer.remaining() > 0) {
                builder.append((char) buffer.readByte());
            }

            // remove the last comma
            return builder.toString();
        } finally {
            buffer.limit(limit);
            buffer.position(pos);
        }
    }

    // this "needless" override is needed for better erasure while accessing raw Bytes/BytesStore
    @Override
    Access<Underlying> access();
}