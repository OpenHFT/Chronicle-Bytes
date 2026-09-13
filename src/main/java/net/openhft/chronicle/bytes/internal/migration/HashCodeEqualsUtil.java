/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal.migration;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods used when migrating hashing and equality behaviours for
 * {@link BytesStore} implementations. The helper guards the underlying store
 * with a temporary {@link ReferenceOwner} so hashing remains safe even if
 * another thread releases the store while the hash is being computed.
 */
public final class HashCodeEqualsUtil {

    private HashCodeEqualsUtil() {
    }

    /**
     * Computes a 32-bit hash code for the supplied {@link BytesStore}. The store
     * is temporarily reserved to avoid accessing memory that might be released
     * concurrently, and the reservation is always cleaned up.
     *
     * @param bytes the bytes store to hash
     * @return hash code produced by {@link BytesStoreHash#hash32(BytesStore)}
     */
    public static int hashCode(final @NotNull BytesStore<?, ?> bytes) {
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
