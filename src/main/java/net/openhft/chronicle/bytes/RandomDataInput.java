package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.UnsafeMemory;

/**
 * This allows random access to the underling bytes.  This instance can be used across threads as it is stateless.
 * The thread safety of the underlying data depends on how the methods are used.
 */
public interface RandomDataInput<S extends RandomDataInput<S>> extends RandomCommon<S> {
    default boolean readBoolean(long offset) {
        return readByte(offset) != 0;
    }

    byte readByte(long offset);

    default int readUnsignedByte(long offset) {
        return readByte(offset) & 0xFF;
    }

    short readShort(long offset);

    default int readUnsignedShort(long offset) {
        return readShort(offset) & 0xFFFF;
    }

    int readInt(long offset);

    default long readUnsignedInt(long offset) {
        return readInt(offset) & 0xFFFFFFFFL;
    }

    long readLong(long offset);

    float readFloat(long offset);

    double readDouble(long offset);

    default char printable(long offset) {
        int b = readUnsignedByte(offset);
        if (b == 0)
            return '\u0660';
        else if (b < 21)
            return (char) (b + 0x2487);
        else
            return (char) b;
    }

    default int readVolatileInt(long offset) {
        UnsafeMemory.MEMORY.loadFence();
        return readInt(offset);
    }

    default long readVolatileLong(long offset) {
        UnsafeMemory.MEMORY.loadFence();
        return readLong(offset);
    }

    default long parseLong(long offset) {
        return BytesUtil.parseLong(this, offset);
    }
}
