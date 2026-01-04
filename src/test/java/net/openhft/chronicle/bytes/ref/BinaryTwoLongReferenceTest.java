/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Binary two long reference behaviour for paired updates")
public class BinaryTwoLongReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("native bytes store operations update two long values")
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryTwoLongReference ref = new BinaryTwoLongReference()) {
            ref.bytesStore(nbs, 16, 16);
            assertEquals(0, ref.getValue(),
                    "Primary long reference read starts at zero");
            assertEquals(0, ref.getValue2(),
                    "Secondary long reference read starts at zero");
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue(),
                    "Atomic increment updates primary volatile read to one");
            assertEquals(0, ref.getVolatileValue2(),
                    "Secondary volatile read remains zero after primary update");
            ref.addAtomicValue2(-1);
            assertEquals(1, ref.getVolatileValue(),
                    "Primary volatile read stays at one after secondary update");
            assertEquals(-1, ref.getVolatileValue2(),
                    "Secondary volatile read reflects negative one");

            ref.addValue(-2);
            assertEquals("value: -1, value2: -1", ref.toString(),
                    "String format reflects two negative values");
            assertFalse(ref.compareAndSwapValue(0, 1),
                    "Compare and swap fails for mismatched expected value");
            assertTrue(ref.compareAndSwapValue(-1, 2),
                    "Compare and swap succeeds for current minus one");
            assertEquals(16, ref.maxSize(),
                    "Reference size reports sixteen bytes for two longs");
            assertEquals(16, ref.offset(),
                    "Reference offset points at byte sixteen");
            assertEquals(nbs, ref.bytesStore(),
                    "Reference retains assigned native byte store");
            assertEquals(0L, nbs.readLong(0),
                    "Native store base remains zero at index zero");
            assertEquals(0L, nbs.readLong(8),
                    "Native store second slot remains zero");
            assertEquals(2L, nbs.readLong(16),
                    "Native store reflects updated primary value");
            assertEquals(-1L, nbs.readLong(24),
                    "Native store reflects updated secondary value");

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16),
                    "Direct set writes ten to primary backing store");
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileLong(16),
                    "Ordered primary write visible via volatile read");
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("toString reports null when reference is unbound to bytes")
    public void toStringReportsNullWhenUnbound() {
        try (@NotNull BinaryTwoLongReference ref = new BinaryTwoLongReference()) {
            assertEquals("bytes is null",
                    ref.toString(),
                    "Unbound references should describe missing bytes");
        }
    }
}
