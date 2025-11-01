/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

/**
 * Detects and optionally retains the newest timestamp for each host id so duplicates can be filtered.
 */
public interface DistributedUniqueTimeDeduplicator {

    /**
     * Compares {@code timestampHostId} with the last timestamp held for its host id.
     *
     * @param timestampHostId value embedding time and host id
     * @return -1 if older, 0 if equal or no previous value, +1 if newer
     */
    int compareByHostId(long timestampHostId);

    /**
     * As {@link #compareByHostId(long)} but also retains {@code timestampHostId} if it is newer.
     */
    int compareAndRetainNewer(long timestampHostId);
}
