package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HeapBytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @param <U> Underlying type (e.g. ByteBuffer or byte[])
 */
public class EnbeddedBytes<U> extends VanillaBytes<U> {
    private EnbeddedBytes(@NotNull BytesStore<?, ?> bytesStore,
                          long writePosition,
                          long writeLimit) throws IllegalStateException, IllegalArgumentException {
        super(bytesStore, writePosition, writeLimit);
    }

    public static <U> EnbeddedBytes<U> wrap(HeapBytesStore<?> bytesStore) {
        final long wp = bytesStore.start();
        final int length = bytesStore.readUnsignedByte(wp - 1);
        return new EnbeddedBytes<>(bytesStore, wp, wp + length);
    }

    @Override
    protected void uncheckedWritePosition(long writePosition) {
        super.uncheckedWritePosition(writePosition);
        bytesStore.writeUnsignedByte(bytesStore.start() - 1, (int) writePosition);
    }

    @Override
    public long writePosition() {
        return bytesStore.readUnsignedByte(bytesStore.start() - 1);
    }
}
