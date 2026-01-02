/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.MappedBytesStore;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("Binary long reference behaviour for native store updates")
public class BinaryLongReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("native bytes store operations update a single long")
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryLongReference ref = new BinaryLongReference()) {
            ref.bytesStore(nbs, 16, 8);
            assertEquals(0, ref.getValue(),
                    "Initial long reference read starts at zero");
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue(),
                    "Atomic increment updates volatile read to one");
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString(),
                    "String format reflects negative one after add");
            assertFalse(ref.compareAndSwapValue(0, 1),
                    "Compare and swap fails for mismatched expected value");
            assertTrue(ref.compareAndSwapValue(-1, 2),
                    "Compare and swap succeeds for current minus one");
            assertEquals(8, ref.maxSize(),
                    "Reference size reports eight bytes for long");
            assertEquals(16, ref.offset(),
                    "Reference offset points at byte sixteen");
            assertEquals(nbs, ref.bytesStore(),
                    "Reference retains assigned native byte store");
            assertEquals(0L, nbs.readLong(0),
                    "Native store base remains zero at index zero");
            assertEquals(0L, nbs.readLong(8),
                    "Native store second slot remains zero");
            assertEquals(2L, nbs.readLong(16),
                    "Native store reflects updated value at offset sixteen");
            assertEquals(0L, nbs.readLong(24),
                    "Native store tail slot remains zero");

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16),
                    "Direct set writes ten to backing store");
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileLong(16),
                    "Ordered write visible through volatile read at offset");
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("mapped bytes store assigns values at a fixed offset")
    public void testCanAssignByteStoreWithExistingOffsetNotInRange() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for mapped file test");

        final File tempFile = IOTools.createTempFile("testCanAssignByteStoreWithExistingOffsetNotInRange");
        final ReferenceOwner referenceOwner = ReferenceOwner.temporary("test");
        try (final MappedFile mappedFile = MappedFile.mappedFile(tempFile, 4096)) {
            MappedBytesStore bytes = mappedFile.acquireByteStore(referenceOwner, 8192);
            try (final BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(bytes, 8192, 8);
                blr.setValue(1234);
                assertEquals(1234, blr.getValue(),
                        "Assigned value reads back from mapped store");
            } finally {
                bytes.release(referenceOwner);
            }
        }
    }
}
