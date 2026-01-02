/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.HeapBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HeapBytesStore equality and capacity behaviour for heap stores")
public class HeapByteStoreTest extends BytesTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    @DisplayName("heap bytes store equality matches content")
    public void testEquals() {
        @NotNull HeapBytesStore hbs = HeapBytesStore.wrap("Hello".getBytes(StandardCharsets.ISO_8859_1));
        @NotNull HeapBytesStore hbs2 = HeapBytesStore.wrap("Hello".getBytes(StandardCharsets.ISO_8859_1));
        @NotNull HeapBytesStore hbs3 = HeapBytesStore.wrap("He!!o".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(hbs, hbs2,
                "Matching stores are equal");
        assertEquals(hbs2, hbs,
                "Equality is symmetric for matching stores");
        assertNotEquals(hbs, hbs3,
                "Different content stores are not equal");
        assertNotEquals(hbs3, hbs,
                "Inequality is symmetric for different stores");
        @NotNull HeapBytesStore hbs4 = HeapBytesStore.wrap("Hi".getBytes(StandardCharsets.ISO_8859_1));
        assertNotEquals(hbs, hbs4,
                "Different length stores are not equal");
        assertNotEquals(hbs4, hbs,
                "Inequality holds for different length stores");
    }

    @Test
    @DisplayName("elastic bytes expands when exceeding capacity")
    public void testElasticBytesEnsuringCapacity() {
        Bytes<?> bytes = Bytes.elasticHeapByteBuffer();
        long initialCapacity = bytes.realCapacity();
        bytes.clearAndPad(bytes.realCapacity() + 128);
        // ensure this succeeds even though we are above the real capacity - this should trigger resize
        bytes.prewriteInt(1);
        assertTrue(bytes.realCapacity() > initialCapacity,
                "Elastic bytes capacity grows beyond initial value");
    }
}
