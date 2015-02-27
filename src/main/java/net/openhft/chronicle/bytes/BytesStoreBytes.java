package net.openhft.chronicle.bytes;

public class BytesStoreBytes<Underlying> extends AbstractBytes<Underlying> {
    public BytesStoreBytes(BytesStore bytesStore) {
        super(bytesStore);
    }

    public void setBytesStore(BytesStore bytesStore) {
        BytesStore oldBS = this.bytesStore;
        this.bytesStore = bytesStore;
        oldBS.release();
        clear();
    }

    @Override
    public boolean isElastic() {
        return false;
    }
}
