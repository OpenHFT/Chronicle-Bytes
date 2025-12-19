/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryTwoLongReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryTwoLongReference ref = new BinaryTwoLongReference()) {
            ref.bytesStore(nbs, 16, 16);
            assertEquals(0, ref.getValue(), "getValue should return 0 for newly initialized reference");
            assertEquals(0, ref.getValue2(), "getValue2 should return 0 for newly initialized reference");
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue(), "getVolatileValue should return 1 after addAtomicValue(1) with memory barrier");
            assertEquals(0, ref.getVolatileValue2(), "getVolatileValue2 should return 0 with memory barrier before modification");
            ref.addAtomicValue2(-1);
            assertEquals(1, ref.getVolatileValue(), "getVolatileValue should return 1 with memory barrier after value2 modification");
            assertEquals(-1, ref.getVolatileValue2(), "getVolatileValue2 should return -1 after addAtomicValue2(-1) with memory barrier");

            ref.addValue(-2);
            assertEquals("value: -1, value2: -1", ref.toString(), "toString should display both values after addValue(-2) from initial 1");
            assertFalse(ref.compareAndSwapValue(0, 1), "compareAndSwapValue should return false when expected value 0 does not match current value -1");
            assertTrue(ref.compareAndSwapValue(-1, 2), "compareAndSwapValue should return true when expected value -1 matches current value");
            assertEquals(16, ref.maxSize(), "maxSize should be 16 bytes for BinaryTwoLongReference (two 8-byte longs)");
            assertEquals(16, ref.offset(), "offset should be 16 where reference was bound in BytesStore");
            assertEquals(nbs, ref.bytesStore(), "bytesStore should return same BytesStore instance that reference is bound to");
            assertEquals(0L, nbs.readLong(0), "readLong should return 0 at offset 0 (unused region before reference)");
            assertEquals(0L, nbs.readLong(8), "readLong should return 0 at offset 8 (unused region before reference)");
            assertEquals(2L, nbs.readLong(16), "readLong should return 2 at offset 16 (first value after compareAndSwap)");
            assertEquals(-1L, nbs.readLong(24), "readLong should return -1 at offset 24 (second value after addAtomicValue2)");

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16), "readLong should return 10 at offset 16 after setValue(10)");
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileLong(16), "readVolatileLong should return 20 at offset 16 after setOrderedValue(20) with memory barrier");
        }
        nbs.releaseLast();
    }
}
