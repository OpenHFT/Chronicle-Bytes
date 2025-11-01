/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class UncheckedLongReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull UncheckedLongReference ref = new UncheckedLongReference()) {
            ref.bytesStore(nbs, 16, 8);
            assertEquals(0, ref.getValue());
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue());
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString());
            assertFalse(ref.compareAndSwapValue(0, 1));
            assertTrue(ref.compareAndSwapValue(-1, 2));
            assertEquals(8, ref.maxSize());
            assertEquals(nbs.addressForRead(16), ref.offset());
            assertEquals(nbs, ref.bytesStore());
            assertEquals(0L, nbs.readLong(0));
            assertEquals(0L, nbs.readLong(8));
            assertEquals(2L, nbs.readLong(16));
            assertEquals(0L, nbs.readLong(24));

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16));
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readLong(16));
        }
        nbs.releaseLast();
    }
}
