/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for heap BytesStore primitive operations, because ordered writes
 * must maintain memory ordering guarantees for concurrent access.
 */
@DisplayName("heap bytes store primitive operation coverage")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class HeapBytesStoreOpsTest extends BytesTestCommon {

    @Test
    @DisplayName("heap store primitive read write operations")
    public void heapStorePrimitiveOps() {
        Bytes<?> heap = Bytes.allocateElasticOnHeap(32);
        try {
            BytesStore<?, ?> store = heap.bytesStore();
            long off = heap.start();
            store.writeInt(off, 0x11223344);
            assertEquals(0x11223344, store.readInt(off),
                    "Read int matches written value on heap store");
            store.writeOrderedInt(off, 0x55667788);
            assertEquals(0x55667788, store.readInt(off),
                    "Ordered write int matches read value on heap store");
        } finally {
            heap.releaseLast();
        }
    }
}
