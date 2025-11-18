/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.HeapBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class HeapByteStoreTest extends BytesTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull HeapBytesStore hbs = HeapBytesStore.wrap("Hello".getBytes(StandardCharsets.ISO_8859_1));
        @NotNull HeapBytesStore hbs2 = HeapBytesStore.wrap("Hello".getBytes(StandardCharsets.ISO_8859_1));
        @NotNull HeapBytesStore hbs3 = HeapBytesStore.wrap("He!!o".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(hbs, hbs2);
        assertEquals(hbs2, hbs);
        assertNotEquals(hbs, hbs3);
        assertNotEquals(hbs3, hbs);
        @NotNull HeapBytesStore hbs4 = HeapBytesStore.wrap("Hi".getBytes(StandardCharsets.ISO_8859_1));
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
