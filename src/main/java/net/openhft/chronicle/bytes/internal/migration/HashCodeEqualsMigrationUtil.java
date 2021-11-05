package net.openhft.chronicle.bytes.internal.migration;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.StackTrace;

public final class HashCodeEqualsMigrationUtil {

    private HashCodeEqualsMigrationUtil() {
    }

    public static boolean equals(final Bytes<?> bytes,
                                 final Object other,
                                 final boolean contentDependentHashcodeAndEquals) {
        if (contentDependentHashcodeAndEquals) {
            Warner.warnOnce();
            if (!(other instanceof BytesStore)) {
                return false;
            }
            final BytesStore<?, ?> bs = (BytesStore<?, ?>) other;
            bytes.reserve(bytes);
            try {
                final long remaining = bytes.readRemaining();
                try {
                    return (bs.readRemaining() == remaining) &&
                            BytesInternal.contentEqual(bytes, bs);
                } catch (IllegalStateException e) {
                    return false;
                }
            } finally {
                bytes.release(bytes);
            }
        } else {
            // Intended future behaviour that mimics Object::equals
            return (bytes == other);
        }
    }

    public static int hashCode(final Bytes<?> bytes,
                               boolean contentDependentHashcodeAndEquals) {
        if (contentDependentHashcodeAndEquals) {
            Warner.warnOnce();
            // Reserving prevents illegal access to this Bytes object if released by another thread
            bytes.reserve(bytes);
            try {
                return BytesStoreHash.hash32(bytes);
            } finally {
                bytes.release(bytes);
            }
        } else {
            // Intended future behaviour that mimics Object::hashCode
            return System.identityHashCode(bytes);
        }
    }

    private static final class Warner {

        private static final String MSG =
                "********** " +
                        "The behaviour of Bytes::hashCode/Bytes::equals will/has change(d) " +
                        "to Object::hashCode/Object::equals in a future/recent version. " +
                        "Make sure to update your application if it depends on the old behaviour where " +
                        "the contents of the Bytes object affected the outcome of these methods.";

        static {
            Jvm.warn().on(Warner.class, MSG, new StackTrace());
        }

        private Warner() {
        }

        public static void warnOnce() {
            // Everything is made in the static block above as the
            // class is first used
        }
    }
}
