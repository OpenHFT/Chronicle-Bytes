/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers ordered, volatile and plain read/write operations on
 * {@link BytesStore} to ensure low-level accessors behave consistently for
 * native stores.
 */
@DisplayName("native bytes store primitive operation coverage")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class NativeBytesStoreOpsTest extends BytesTestCommon {

    @Test
    @DisplayName("native store volatile and ordered operations")
    public void readWriteAndVolatileOrderedOps() {
        BytesStore<?, ?> store = BytesStore.nativeStore(32);
        try {
            long off = 0;
            store.writeLong(off, 0x0102030405060708L);
            assertEquals(0x0102030405060708L, store.readLong(off),
                    "Read long matches written value at base offset");
            store.writeInt(off + 8, 0x11223344);
            assertEquals(0x11223344, store.readInt(off + 8),
                    "Read int matches written value at offset eight");

            store.writeVolatileLong(off, 9L);
            assertEquals(9L, store.readVolatileLong(off),
                    "Volatile read returns latest written value");
            store.writeOrderedLong(off, 10L);
            assertEquals(10L, store.readLong(off),
                    "Ordered write long is visible to read");
            assertEquals(15L, store.addAndGetLong(off, 5L),
                    "Add and get returns updated long value");
        } finally {
            store.releaseLast();
        }
    }
}
