/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinaryIntReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryIntReference ref = new BinaryIntReference()) {
            ref.bytesStore(nbs, 16, 4);
            assertEquals(0, ref.getValue());
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue());
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString());
            assertFalse(ref.compareAndSwapValue(0, 1));
            assertTrue(ref.compareAndSwapValue(-1, 2));
            assertEquals(4, ref.maxSize());
            assertEquals(16, ref.offset());
            assertEquals(nbs, ref.bytesStore());
            assertEquals(0L, nbs.readLong(0));
            assertEquals(0L, nbs.readLong(8));
            assertEquals(2, nbs.readInt(16));
            assertEquals(0L, nbs.readLong(20));

            ref.setValue(10);
            assertEquals(10L, nbs.readInt(16));
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileInt(16));
        }
        nbs.releaseLast();
    }
}
