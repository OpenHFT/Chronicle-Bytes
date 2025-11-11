/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NativeBytesStoreOpsTest extends BytesTestCommon {

    @Test
    public void readWriteAndVolatileOrderedOps() {
        BytesStore<?, ?> store = BytesStore.nativeStore(32);
        try {
            long off = 0;
            store.writeLong(off, 0x0102030405060708L);
            assertEquals(0x0102030405060708L, store.readLong(off));
            store.writeInt(off + 8, 0x11223344);
            assertEquals(0x11223344, store.readInt(off + 8));

            store.writeVolatileLong(off, 9L);
            assertEquals(9L, store.readVolatileLong(off));
            store.writeOrderedLong(off, 10L);
            assertEquals(10L, store.readLong(off));
            assertEquals(15L, store.addAndGetLong(off, 5L));
        } finally {
            store.releaseLast();
        }
    }
}

