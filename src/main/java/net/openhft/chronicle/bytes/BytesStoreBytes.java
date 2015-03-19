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

    public void setByteStore(BytesStore byteStore, long position, long length) {
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
