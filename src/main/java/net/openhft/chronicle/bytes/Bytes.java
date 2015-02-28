package net.openhft.chronicle.bytes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

public interface Bytes<Underlying> extends BytesStore<Bytes<Underlying>, Underlying>,
        StreamingDataInput<Bytes<Underlying>>, StreamingDataOutput<Bytes<Underlying>>,
        ByteStringParser<Bytes<Underlying>>, ByteStringAppender<Bytes<Underlying>>,
        CharSequence {

    long position();

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
}
