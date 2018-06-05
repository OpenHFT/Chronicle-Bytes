package net.openhft.chronicle.bytes.jitter;

import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;

public class MemoryMessager {

    public static final int NOT_READY = Integer.MIN_VALUE;
    private final MappedBytes bytes;
    private long address;

    public MemoryMessager(MappedBytes bytes) {
        this.bytes = bytes;
        address = bytes.addressForRead(bytes.readPosition());
    }

    public void writeMessage(int length, long count) {
        long pos = bytes.writePosition();
        boolean works = bytes.compareAndSwapInt(pos, 0x0, NOT_READY);

        if (!works) throw new AssertionError();
        bytes.writeSkip(4);
        bytes.writeLong(count);
        length -= 8;
        int i = 0;
        for (; i < length - 7; i += 8)
            bytes.writeLong(i);
        for (; i < length; i++)
            bytes.writeByte((byte) 0);
        boolean works2 = bytes.compareAndSwapInt(pos, NOT_READY, (int) (bytes.writePosition() - pos - 4));
        if (!works2) throw new AssertionError();
    }

    public int length() {
        int length = UnsafeMemory.UNSAFE.getIntVolatile(null, address);
        Jvm.safepoint();
        return length;
    }

    public long consumeBytes() {
        int length = length();
        if (length == 0x0 || length == NOT_READY)
            throw new AssertionError("length: "+length);

        bytes.readSkip(4);
        long ret = bytes.readLong();
        length -= 8;
        int i = 0;
        for (; i < length - 7; i += 8)
            bytes.readLong();
        for (; i < length; i++)
            bytes.readByte();
        address = bytes.addressForRead(bytes.readPosition(), 4);
        return ret;
    }
}
