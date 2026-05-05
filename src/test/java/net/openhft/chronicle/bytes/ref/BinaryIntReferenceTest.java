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

/**
 * Tests BinaryIntReference native store updates because correct atomic
 * operations are essential for lock-free counters in shared memory
 * structures.
 */
@SuppressWarnings({"MMOverusedWord", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("Binary int reference behaviour for native store updates")
class BinaryIntReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("native bytes store operations update a single int")
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryIntReference ref = new BinaryIntReference()) {
            ref.bytesStore(nbs, 16, 4);
            assertEquals(0, ref.getValue(),
                    "Initial int reference read starts at zero");
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue(),
                    "Atomic increment updates volatile int read to one");
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString(),
                    "String format reflects negative one after add");
            assertFalse(ref.compareAndSwapValue(0, 1),
                    "Compare and swap rejects mismatched expected int");
            assertTrue(ref.compareAndSwapValue(-1, 2),
                    "Compare and swap accepts negative one as current value");
            assertEquals(4, ref.maxSize(),
                    "Reference size reports four bytes for int");
            assertEquals(16, ref.offset(),
                    "Reference offset targets byte sixteen within store");
            assertEquals(nbs, ref.bytesStore(),
                    "Reference retains assigned native store instance");
            assertEquals(0L, nbs.readLong(0),
                    "Native store base remains zero at slot zero");
            assertEquals(0L, nbs.readLong(8),
                    "Native store second slot remains zero");
            assertEquals(2, nbs.readInt(16),
                    "Native store reflects updated int at offset sixteen");
            assertEquals(0L, nbs.readLong(20),
                    "Native store tail long remains zero after int write");

            ref.setValue(10);
            assertEquals(10, nbs.readInt(16),
                    "Direct set writes ten to int backing store");
            ref.setOrderedValue(20);
            // Allow memory visibility propagation
            Thread.yield();
            assertEquals(20, nbs.readVolatileInt(16),
                    "Ordered int write visible through volatile read");
        }
        nbs.releaseLast();
    }
}
