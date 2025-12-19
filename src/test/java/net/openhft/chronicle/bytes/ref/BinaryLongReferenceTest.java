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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class BinaryLongReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryLongReference ref = new BinaryLongReference()) {
            ref.bytesStore(nbs, 16, 8);
            assertEquals(0, ref.getValue(), "getValue should return 0 for newly initialised reference");
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue(), "getVolatileValue should return 1 after addAtomicValue(1)");
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString(), "toString should display current value -1 after addValue(-2)");
            assertFalse(ref.compareAndSwapValue(0, 1), "compareAndSwapValue should fail when expected value 0 does not match current -1");
            assertTrue(ref.compareAndSwapValue(-1, 2), "compareAndSwapValue should succeed when expected value -1 matches current value");
            assertEquals(8, ref.maxSize(), "maxSize should be 8 bytes for BinaryLongReference (one 8-byte long)");
            assertEquals(16, ref.offset(), "offset should be 16 where reference was bound in BytesStore");
            assertEquals(nbs, ref.bytesStore(), "bytesStore should return same BytesStore instance that reference is bound to");
            assertEquals(0L, nbs.readLong(0), "readLong should return 0 at offset 0 where no data was written");
            assertEquals(0L, nbs.readLong(8), "readLong should return 0 at offset 8 where no data was written");
            assertEquals(2L, nbs.readLong(16), "readLong should return 2 at offset 16 where reference is bound");
            assertEquals(0L, nbs.readLong(24), "readLong should return 0 at offset 24 where no data was written");

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16), "readLong should return 10 at offset 16 after setValue(10)");
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileLong(16), "readVolatileLong should return 20 at offset 16 after setOrderedValue(20)");
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
                assertEquals(1234, blr.getValue(), "getValue should return 1234 after setValue on mapped bytes at offset 8192");
            } finally {
                bytes.release(referenceOwner);
            }
        }
    }
}
