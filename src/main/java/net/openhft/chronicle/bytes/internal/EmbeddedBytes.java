package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import org.jetbrains.annotations.NotNull;

public class EmbeddedBytes<Underlying> extends VanillaBytes<Underlying> {
    private EmbeddedBytes(@NotNull BytesStore<?, ?> bytesStore, long writePosition, long writeLimit) throws IllegalStateException, IllegalArgumentException {
        super(bytesStore, writePosition, writeLimit);
    }

    public static <Underlying> EmbeddedBytes<Underlying> wrap(BytesStore<?, ?> bytesStore) {
        return wrap((HeapBytesStore<?>) bytesStore);
    }

    public static <Underlying> EmbeddedBytes<Underlying> wrap(HeapBytesStore<?> bytesStore) {
        long wp = bytesStore.start();
        int length = bytesStore.readUnsignedByte(wp - 1);
        return new EmbeddedBytes<>(bytesStore, wp, wp + length);
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
