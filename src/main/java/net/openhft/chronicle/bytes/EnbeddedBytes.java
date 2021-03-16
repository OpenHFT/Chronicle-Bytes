package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

public class EnbeddedBytes<Underlying> extends VanillaBytes<Underlying> {
    private EnbeddedBytes(@NotNull BytesStore bytesStore, long writePosition, long writeLimit) throws IllegalStateException, IllegalArgumentException {
        super(bytesStore, writePosition, writeLimit);
    }

    public static <U> EnbeddedBytes<U> wrap(HeapBytesStore bytesStore) {
        long wp = bytesStore.start();
        int length = bytesStore.readUnsignedByte(wp - 1);
        return new EnbeddedBytes<>(bytesStore, wp, wp + length);
    }

    @Override
    protected void uncheckedWritePosition(long writePosition) {
        super.uncheckedWritePosition(writePosition);
        bytesStore.writeUnsignedByte(bytesStore.start() - 1, (int) writePosition);
    }
}
