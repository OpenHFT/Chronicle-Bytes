package net.openhft.chronicle.bytes.internal.migration;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.io.ReferenceOwner;

public final class HashCodeEqualsUtil {

    private HashCodeEqualsUtil() {
    }

    public static int hashCode(final BytesStore<?, ?> bytes) {
        // Reserving prevents illegal access to this Bytes object if released by another thread
        final ReferenceOwner owner = ReferenceOwner.temporary("hashCode");
        bytes.reserve(owner);
        try {
            return BytesStoreHash.hash32(bytes);
        } finally {
            bytes.release(owner);
        }
    }

}
