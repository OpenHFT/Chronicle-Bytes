package net.openhft.chronicle.bytes.internal.migration;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.bytes.internal.BytesInternal;

public final class HashCodeEqualsUtil {

    private HashCodeEqualsUtil() {
    }

    public static boolean equals(final Bytes<?> bytes,
                                 final Object other) {

        if (!(other instanceof BytesStore)) {
            return false;
        }
        final BytesStore<?, ?> otherByteStore = (BytesStore<?, ?>) other;
        bytes.reserve(bytes);
        try {
            final long remaining = bytes.readRemaining();
            try {
                return (otherByteStore.readRemaining() == remaining) &&
                        BytesInternal.contentEqual(bytes, otherByteStore);
            } catch (IllegalStateException e) {
                return false;
            }
        } finally {
            bytes.release(bytes);
        }
    }

    public static int hashCode(final Bytes<?> bytes) {
        // Reserving prevents illegal access to this Bytes object if released by another thread
        bytes.reserve(bytes);
        try {
            return BytesStoreHash.hash32(bytes);
        } finally {
            bytes.release(bytes);
        }
    }

}
