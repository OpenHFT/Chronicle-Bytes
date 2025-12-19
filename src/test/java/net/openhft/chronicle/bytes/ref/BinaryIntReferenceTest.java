/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryIntReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        BytesStore<?, Void> nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryIntReference ref = new BinaryIntReference()) {
            ref.bytesStore(nbs, 16, 4);
            assertEquals(0, ref.getValue(), "getValue should return 0 for newly initialised reference");
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue(), "getVolatileValue should return 1 after addAtomicValue(1)");
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString(), "toString should display current value -1 after addValue(-2)");
            assertFalse(ref.compareAndSwapValue(0, 1), "compareAndSwapValue should fail when expected value 0 does not match current -1");
            assertTrue(ref.compareAndSwapValue(-1, 2), "compareAndSwapValue should succeed when expected value -1 matches current value");
            assertEquals(4, ref.maxSize(), "maxSize should be 4 bytes for BinaryIntReference (one 4-byte int)");
            assertEquals(16, ref.offset(), "offset should be 16 where reference was bound in BytesStore");
            assertEquals(nbs, ref.bytesStore(), "bytesStore should return same BytesStore instance that reference is bound to");
            assertEquals(0L, nbs.readLong(0), "readLong should return 0 at offset 0 where no data was written");
            assertEquals(0L, nbs.readLong(8), "readLong should return 0 at offset 8 where no data was written");
            assertEquals(2, nbs.readInt(16), "readInt should return 2 at offset 16 where reference is bound");
            assertEquals(0L, nbs.readLong(20), "readLong should return 0 at offset 20 where no data was written");

            ref.setValue(10);
            assertEquals(10L, nbs.readInt(16), "readInt should return 10 at offset 16 after setValue(10)");
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileInt(16), "readVolatileInt should return 20 at offset 16 after setOrderedValue(20)");
        }
        nbs.releaseLast();
    }
}
