package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

public class BytesStoreBytes<Underlying> extends AbstractBytes<Underlying> implements Byteable<Underlying> {
    public BytesStoreBytes(@NotNull BytesStore bytesStore) {
        super(bytesStore);
    }

    public void bytesStore(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
        BytesStore oldBS = this.bytesStore;
        this.bytesStore = bytesStore;
        oldBS.release();
        clear();
    }

    @Override
    public void bytesStore(BytesStore<Bytes<Underlying>, Underlying> byteStore, long offset, long length) {
        bytesStore(byteStore);
        limit(offset + length);
        position(offset);
    }

    @Override
    public BytesStore<Bytes<Underlying>, Underlying> bytesStore() {
        return bytesStore;
    }

    @Override
    public long offset() {
        return position();
    }

    @Override
    public long maxSize() {
        return remaining();
    }

    @Override
    public boolean isElastic() {
        return false;
    }

    @Override
    public long realCapacity() {
        return bytesStore.realCapacity();
    }

}
