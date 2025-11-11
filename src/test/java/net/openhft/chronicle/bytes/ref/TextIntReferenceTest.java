/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextIntReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        nbs.zeroOut(0, 64);
        try (@NotNull TextIntReference ref = new TextIntReference()) {
            ref.bytesStore(nbs, 16, ref.maxSize());
            assertEquals(0, ref.getValue());
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue());
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString());
            assertFalse(ref.compareAndSwapValue(0, 1));
            assertTrue(ref.compareAndSwapValue(-1, 2));
            assertEquals(46, ref.maxSize());
            assertEquals(16, ref.offset());
            assertEquals(nbs, ref.bytesStore());
            assertEquals(0L, nbs.readLong(0));
            assertEquals(0L, nbs.readLong(8));
            Bytes<Void> bytes = nbs.bytesForRead();
            bytes.readPosition(16);
            assertEquals("!!atomic {  locked: false, value: 0000000002 }", bytes.parseUtf8(StopCharTesters.CONTROL_STOP));
            bytes.releaseLast();
        }
        nbs.releaseLast();
    }
}
