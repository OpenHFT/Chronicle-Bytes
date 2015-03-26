package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.ReferenceCounted;

public class MappedBytesStore extends NativeBytesStore {
    private final long start;
    private final long safeLimit;

    protected MappedBytesStore(ReferenceCounted owner, long start, long address, long capacity, long safeCapacity) {
        super(address, start + capacity, new OS.Unmapper(address, capacity, owner), false);
        this.start = start;
        this.safeLimit = start + safeCapacity;
    }

    @Override
    public Bytes<Void> bytes() {
        return new BytesStoreBytes<>(this);
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long safeLimit() {
        return safeLimit;
    }
}
