package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

/**
 * Created by peter.lawrey on 19/04/15.
 */
public class SubZeroedBytes<Underlying> extends ZeroedBytes<Underlying> {
    private long start;
    private long capacity;

    public SubZeroedBytes(@NotNull BytesStore bytesStore, UnderflowMode underflowMode, long start, long limit) {
        super(bytesStore, underflowMode);
        this.start = start;
        this.capacity = limit - start;
        clear();
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long realCapacity() {
        return capacity;
    }
}
