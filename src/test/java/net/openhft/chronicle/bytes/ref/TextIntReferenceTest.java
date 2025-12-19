/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TextIntReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        nbs.zeroOut(0, 64);
        try (@NotNull TextIntReference ref = new TextIntReference()) {
            ref.bytesStore(nbs, 16, ref.maxSize());
            assertEquals(0, ref.getValue(), "getValue should return 0 for newly initialised reference");
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue(), "getVolatileValue should return 1 after addAtomicValue(1)");
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString(), "toString should display current value -1 after addValue(-2)");
            assertFalse(ref.compareAndSwapValue(0, 1), "compareAndSwapValue should fail when expected value 0 does not match current -1");
            assertTrue(ref.compareAndSwapValue(-1, 2), "compareAndSwapValue should succeed when expected value -1 matches current value");
            assertEquals(46, ref.maxSize(), "maxSize should be 46 bytes for TextIntReference (text-formatted atomic structure)");
            assertEquals(16, ref.offset(), "offset should be 16 where reference was bound in BytesStore");
            assertEquals(nbs, ref.bytesStore(), "bytesStore should return same NativeBytesStore instance that reference is bound to");
            assertEquals(0L, nbs.readLong(0), "readLong should return 0 at offset 0 where no data was written");
            assertEquals(0L, nbs.readLong(8), "readLong should return 0 at offset 8 where no data was written");
            Bytes<Void> bytes = nbs.bytesForRead();
            bytes.readPosition(16);
            assertEquals("!!atomic {  locked: false, value: 0000000002 }", bytes.parseUtf8(StopCharTesters.CONTROL_STOP), "parseUtf8 should return text-formatted atomic structure with value 2 after compareAndSwap");
            bytes.releaseLast();
        }
        nbs.releaseLast();
    }
}
