/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinaryTwoLongReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryTwoLongReference ref = new BinaryTwoLongReference()) {
            ref.bytesStore(nbs, 16, 16);
            assertEquals(0, ref.getValue());
            assertEquals(0, ref.getValue2());
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue());
            assertEquals(0, ref.getVolatileValue2());
            ref.addAtomicValue2(-1);
            assertEquals(1, ref.getVolatileValue());
            assertEquals(-1, ref.getVolatileValue2());

            ref.addValue(-2);
            assertEquals("value: -1, value2: -1", ref.toString());
            assertFalse(ref.compareAndSwapValue(0, 1));
            assertTrue(ref.compareAndSwapValue(-1, 2));
            assertEquals(16, ref.maxSize());
            assertEquals(16, ref.offset());
            assertEquals(nbs, ref.bytesStore());
            assertEquals(0L, nbs.readLong(0));
            assertEquals(0L, nbs.readLong(8));
            assertEquals(2L, nbs.readLong(16));
            assertEquals(-1L, nbs.readLong(24));

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16));
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileLong(16));
        }
        nbs.releaseLast();
    }
}
