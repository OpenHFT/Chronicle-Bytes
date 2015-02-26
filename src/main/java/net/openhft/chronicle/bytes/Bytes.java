package net.openhft.chronicle.bytes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

public interface Bytes extends BytesStore<Bytes>,
        StreamingDataInput<Bytes>, StreamingDataOutput<Bytes>,
        ByteStringParser<Bytes>, ByteStringAppender<Bytes>,
        CharSequence {

    long position();

    Bytes position(long position);

    long limit();

    Bytes limit(long limit);

    static Bytes wrap(ByteBuffer byteBuffer) {
        return BytesStore.wrap(byteBuffer).bytes();
    }

    static Bytes wrap(byte[] byteArray) {
        return BytesStore.wrap(byteArray).bytes();
    }

    default Bytes writeLength8(Consumer<Bytes> writer) {
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


    default Bytes readLength8(Consumer<Bytes> reader) {
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
    
/*
    Bytes writeLength16(Consumer<Bytes> writer);

    Bytes readLength16(Consumer<Bytes> writer);

    Bytes writeLength32(Consumer<Bytes> writer);

    Bytes readLength32(Consumer<Bytes> writer);
*/
}
