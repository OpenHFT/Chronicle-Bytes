package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.util.function.Consumer;

public interface Bytes<Underlying> extends BytesStore<Bytes<Underlying>, Underlying>,
        StreamingDataInput<Bytes<Underlying>>, StreamingDataOutput<Bytes<Underlying>>,
        ByteStringParser<Bytes<Underlying>>, ByteStringAppender<Bytes<Underlying>>,
        CharSequence {

    long position();

    long mark = -1;

    /**
     * Sets this buffer's mark at its position.
     *
     * @return This buffer
     */
    Bytes mark();

    /**
     * Resets this buffer's position to the previously-marked position.
     *
     * Invoking this method neither changes nor discards the mark's value.
     *
     * @return This buffer
     * @throws InvalidMarkException If the mark has not been set
     */
    Bytes reset() throws InvalidMarkException;

    Bytes<Underlying> position(long position);

    long limit();

    Bytes<Underlying> limit(long limit);

    static Bytes<ByteBuffer> elasticByteBuffer() {
        return NativeStore.elasticByteBuffer().bytes();

    }

    static Bytes<ByteBuffer> wrap(ByteBuffer byteBuffer) {
        return BytesStore.wrap(byteBuffer).bytes();
    }

    static Bytes<byte[]> wrap(byte[] byteArray) {
        return BytesStore.wrap(byteArray).bytes();
    }

    default Bytes<Underlying> writeLength8(Consumer<Bytes<Underlying>> writer) {
        long position = position();
        writeUnsignedByte(0);

        writer.accept(this);
        long length = position() - position;
        if (length >= 1 << 8)
            throw new IllegalStateException("Cannot have an 8-bit length of " + length);
        writeUnsignedByte(position, (short) length);
        storeFence();

        return this;
    }


    default Bytes<Underlying> readLength8(Consumer<Bytes<Underlying>> reader) {
        loadFence();
        int length = readUnsignedByte() - 1;
        if (length < 0)
            throw new IllegalStateException("Unset length");
        return withLength(length, reader);
    }

    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
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
        if (size > capacity())
            // for the elastic buffer this will grow the buffer
            writeByte(size, 0);
    }

/*
    Bytes writeLength16(Consumer<Bytes> writer);

    Bytes readLength16(Consumer<Bytes> writer);

    Bytes writeLength32(Consumer<Bytes> writer);

    Bytes readLength32(Consumer<Bytes> writer);
*/

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param buffer the buffer you wish to toString()
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    public static String toHex(@NotNull final Bytes buffer) {

        if (buffer.remaining() == 0)
            return "";

        long position = buffer.position();
        long limit = buffer.limit();

        try {

            final StringBuilder builder = new StringBuilder("[");

            while (buffer.remaining() > 0) {
                byte b = buffer.readByte();
                char c = (char) b;
                builder.append(c + "(" + String.format("%02X ", b).trim() + ")");
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
        final long limit = buffer.limit();
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
}