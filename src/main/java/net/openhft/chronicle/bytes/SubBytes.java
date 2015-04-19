package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

/**
 * Created by peter.lawrey on 19/04/15.
 */
public class SubBytes<Underlying> extends VanillaBytes<Underlying> {
    private long start;
    private long capacity;

    public SubBytes(@NotNull BytesStore bytesStore, long start, long limit) {
        super(bytesStore);
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
