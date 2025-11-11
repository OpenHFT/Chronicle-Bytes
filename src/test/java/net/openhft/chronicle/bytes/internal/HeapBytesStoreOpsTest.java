/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HeapBytesStoreOpsTest extends BytesTestCommon {

    @Test
    public void heapStorePrimitiveOps() {
        Bytes<?> heap = Bytes.allocateElasticOnHeap(32);
        try {
            BytesStore<?, ?> store = heap.bytesStore();
            long off = heap.start();
            store.writeInt(off, 0x11223344);
            assertEquals(0x11223344, store.readInt(off));
            store.writeOrderedInt(off, 0x55667788);
            assertEquals(0x55667788, store.readInt(off));
        } finally {
            heap.releaseLast();
        }
    }
}

