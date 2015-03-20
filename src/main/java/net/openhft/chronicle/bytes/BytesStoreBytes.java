package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

public class BytesStoreBytes<Underlying> extends AbstractBytes<Underlying> {
    public BytesStoreBytes(@NotNull BytesStore bytesStore) {
        super(bytesStore);
    }

    public void setBytesStore(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
        BytesStore oldBS = this.bytesStore;
        this.bytesStore = bytesStore;
        oldBS.release();
        clear();
    }

    public void setByteStore(@NotNull BytesStore<Bytes<Underlying>, Underlying> byteStore,
                             long position, long length) {
        setBytesStore(byteStore);
        limit(position + length);
        position(position);
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
