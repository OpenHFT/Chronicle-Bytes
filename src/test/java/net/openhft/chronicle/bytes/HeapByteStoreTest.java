/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.HeapBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class HeapByteStoreTest extends BytesTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull HeapBytesStore hbs = HeapBytesStore.wrap("Hello".getBytes());
        @NotNull HeapBytesStore hbs2 = HeapBytesStore.wrap("Hello".getBytes());
        @NotNull HeapBytesStore hbs3 = HeapBytesStore.wrap("He!!o".getBytes());
        @NotNull HeapBytesStore hbs4 = HeapBytesStore.wrap("Hi".getBytes());
        assertEquals(hbs, hbs2);
        assertEquals(hbs2, hbs);
        assertNotEquals(hbs, hbs3);
        assertNotEquals(hbs3, hbs);
        assertNotEquals(hbs, hbs4);
        assertNotEquals(hbs4, hbs);
    }

    @Test
    public void testElasticBytesEnsuringCapacity() {
        Bytes<?> bytes = Bytes.elasticHeapByteBuffer();
        long initialCapacity = bytes.realCapacity();
        bytes.clearAndPad(bytes.realCapacity() + 128);
        // ensure this succeeds even though we are above the real capacity - this should trigger resize
        bytes.prewriteInt(1);
        assertTrue(bytes.realCapacity()> initialCapacity);
    }
}
