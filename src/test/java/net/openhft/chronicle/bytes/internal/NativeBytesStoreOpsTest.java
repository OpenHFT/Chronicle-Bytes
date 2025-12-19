/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers ordered, volatile and plain read/write operations on
 * {@link BytesStore} to ensure low-level accessors behave consistently for
 * native stores.
 */
public class NativeBytesStoreOpsTest extends BytesTestCommon {

    @Test
    public void readWriteAndVolatileOrderedOps() {
        BytesStore<?, ?> store = BytesStore.nativeStore(32);
        try {
            long off = 0;
            store.writeLong(off, 0x0102030405060708L);
            assertEquals(0x0102030405060708L, store.readLong(off), "readLong should return written long value 0x0102030405060708");
            store.writeInt(off + 8, 0x11223344);
            assertEquals(0x11223344, store.readInt(off + 8), "readInt should return written int value 0x11223344 at offset 8");

            store.writeVolatileLong(off, 9L);
            assertEquals(9L, store.readVolatileLong(off), "readVolatileLong value");
            store.writeOrderedLong(off, 10L);
            assertEquals(10L, store.readLong(off), "readLong should return value 10 written via writeOrderedLong");
            assertEquals(15L, store.addAndGetLong(off, 5L), "addAndGetLong value");
        } finally {
            store.releaseLast();
        }
    }
}
