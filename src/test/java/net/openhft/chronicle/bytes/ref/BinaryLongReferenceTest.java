/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class BinaryLongReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryLongReference ref = new BinaryLongReference()) {
            ref.bytesStore(nbs, 16, 8);
            assertEquals(0, ref.getValue());
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue());
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString());
            assertFalse(ref.compareAndSwapValue(0, 1));
            assertTrue(ref.compareAndSwapValue(-1, 2));
            assertEquals(8, ref.maxSize());
            assertEquals(16, ref.offset());
            assertEquals(nbs, ref.bytesStore());
            assertEquals(0L, nbs.readLong(0));
            assertEquals(0L, nbs.readLong(8));
            assertEquals(2L, nbs.readLong(16));
            assertEquals(0L, nbs.readLong(24));

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16));
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileLong(16));
        }
        nbs.releaseLast();
    }

    @Test
    public void testCanAssignByteStoreWithExistingOffsetNotInRange() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final File tempFile = IOTools.createTempFile("testCanAssignByteStoreWithExistingOffsetNotInRange");
        final ReferenceOwner referenceOwner = ReferenceOwner.temporary("test");
        try (final MappedFile mappedFile = MappedFile.mappedFile(tempFile, 4096)) {
            MappedBytesStore bytes = mappedFile.acquireByteStore(referenceOwner, 8192);
            try (final BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(bytes, 8192, 8);
                blr.setValue(1234);
                assertEquals(1234, blr.getValue());
            } finally {
                bytes.release(referenceOwner);
            }
        }
    }
}
