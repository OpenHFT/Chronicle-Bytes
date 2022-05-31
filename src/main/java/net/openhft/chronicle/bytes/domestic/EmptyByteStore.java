package net.openhft.chronicle.bytes.domestic;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.internal.SingletonEmptyByteStore;

public interface EmptyByteStore extends BytesStore<EmptyByteStore, Void> {

     @SuppressWarnings("unchecked")
    static <T, B extends BytesStore<B, T>> BytesStore<B, T> acquire() {
        return (BytesStore<B, T>) SingletonEmptyByteStore.INSTANCE;
    }

}