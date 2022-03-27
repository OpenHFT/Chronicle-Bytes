package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.UnsafeMemory;
import org.jetbrains.annotations.NotNull;

public enum BinaryLengthLength {
    LENGTH_8BIT {
        @Override
        public int code() {
            return 0x80;
        }

        @Override
        public long initialise(@NotNull final BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            long pos = bytes.writePosition();
            bytes.writeByte((byte) 0);
            return pos;
        }

        @Override
        public void writeLength(@NotNull Bytes<?> bytes, long positionReturnedFromInitialise, long end) {
            long length = (end - positionReturnedFromInitialise - 1) & MASK;
            if (length >= 1 << 8)
                throw invalidLength(length);
            bytes.writeByte(positionReturnedFromInitialise, (byte) length);
            UnsafeMemory.MEMORY.storeFence();
        }
    },
    LENGTH_16BIT {
        @Override
        public int code() {
            return 0x81;
        }

        @Override
        public long initialise(@NotNull final BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            final long pos = bytes.writePosition();
            bytes.writeShort((short) 0);
            return pos;
        }

        @Override
        public void writeLength(@NotNull Bytes<?> bytes, long positionReturnedFromInitialise, long end) {
            final long length = (end - positionReturnedFromInitialise - 2) & MASK;
            if (length >= 1 << 16)
                throw invalidLength(length);
            bytes.writeShort(positionReturnedFromInitialise, (short) length);
            UnsafeMemory.MEMORY.storeFence();
        }
    },
    LENGTH_32BIT {
        @Override
        public int code() {
            return 0x82;
        }

        @Override
        public long initialise(@NotNull BytesOut<?> bytes) {
            bytes.writeUnsignedByte(code());
            final long pos = bytes.writePosition();
            bytes.writeInt(0);
            return pos;
        }

        @Override
        public void writeLength(@NotNull Bytes<?> bytes, long positionReturnedFromInitialise, long end) {
            final long length = (end - positionReturnedFromInitialise - 4) & MASK;
            if (length >= 1L << 31)
                throw invalidLength(length);
            bytes.writeOrderedInt(positionReturnedFromInitialise, (int) length);
        }
    };

    static final long MASK = 0xFFFFFFFFL;

    IllegalStateException invalidLength(final long length) {
        return new IllegalStateException("length: " + length);
    }

    public abstract int code();

    public abstract long initialise(@NotNull BytesOut<?> bytes);

    public abstract void writeLength(@NotNull Bytes<?> bytes,
                                     long positionReturnedFromInitialise,
                                     long end);
}
