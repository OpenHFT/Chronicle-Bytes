/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UncheckedLongReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull UncheckedLongReference ref = new UncheckedLongReference()) {
            ref.bytesStore(nbs, 16, 8);
            assertEquals(0, ref.getValue(), "getValue should return 0 for newly initialized reference");
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue(), "getVolatileValue should return 1 after addAtomicValue(1) with memory barrier");
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString(), "toString should display 'value: -1' after addValue(-2) from initial 1");
            assertFalse(ref.compareAndSwapValue(0, 1), "compareAndSwapValue should return false when expected value 0 does not match current value -1");
            assertTrue(ref.compareAndSwapValue(-1, 2), "compareAndSwapValue should return true when expected value -1 matches current value");
            assertEquals(8, ref.maxSize(), "maxSize should be 8 bytes for UncheckedLongReference (one 8-byte long)");
            assertEquals(nbs.addressForRead(16), ref.offset(), "offset should be native memory address at position 16 for unchecked reference");
            assertEquals(nbs, ref.bytesStore(), "bytesStore should return same NativeBytesStore instance that reference is bound to");
            assertEquals(0L, nbs.readLong(0), "readLong should return 0 at offset 0 (unused region before reference)");
            assertEquals(0L, nbs.readLong(8), "readLong should return 0 at offset 8 (unused region before reference)");
            assertEquals(2L, nbs.readLong(16), "readLong should return 2 at offset 16 (reference value after compareAndSwap)");
            assertEquals(0L, nbs.readLong(24), "readLong should return 0 at offset 24 (unused region after reference)");

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16), "readLong should return 10 at offset 16 after setValue(10)");
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readLong(16), "readLong should return 20 at offset 16 after setOrderedValue(20)");
        }
        nbs.releaseLast();
    }
}
