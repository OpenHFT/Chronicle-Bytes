package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash;

/**
 * Created by peter on 07/05/16.
 */
public abstract class AbstractBytesStore<B extends BytesStore<B, Underlying>, Underlying> implements BytesStore<B, Underlying> {
    @Override
    public int peekUnsignedByte(long offset) {
        return offset >= readLimit() ? -1 : readUnsignedByte(offset);
    }

    @Override
    public int hashCode() {
        long h = VanillaBytesStoreHash.INSTANCE.applyAsLong(this);
        h ^= h >> 32;
        return (int) h;
    }
}
