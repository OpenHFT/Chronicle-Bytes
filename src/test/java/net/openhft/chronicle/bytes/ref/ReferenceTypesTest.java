/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests reference type behaviours for text and binary values because
 * correct value storage is essential for both human-readable and compact
 * binary formats.
 */
@SuppressWarnings("MMOverusedWord") // value domain terminology
@DisplayName("Reference type behaviours for text and binary values")
public class ReferenceTypesTest extends BytesTestCommon {

    @Test
    @DisplayName("text int reference round trip updates values correctly")
    public void textIntReferenceRoundTrip() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(128);
        TextIntReference ref = null;
        try {
            // Prepare backing store with template and bind reference
            TextIntReference.write(b, 42);
            ref = new TextIntReference();
            BytesStore<?, ?> store = b.bytesStore();
            ref.bytesStore(store, b.start(), ref.maxSize());

            assertEquals(42,
                    ref.getValue(),
                    "Initial reference value should match the written value");

            ref.setValue(1234);
            assertEquals(1234,
                    ref.getValue(),
                    "setValue should update the reference");

            assertTrue(ref.compareAndSwapValue(1234, 5678),
                    "compareAndSwap should succeed for expected value");
            assertEquals(5678,
                    ref.getValue(),
                    "compareAndSwap should update the reference value");

            assertEquals(5680,
                    ref.addValue(2),
                    "addValue should return the updated value");
            assertEquals(5680,
                    ref.getVolatileValue(),
                    "Volatile read reflects updated sum after addValue");
            ref.setOrderedValue(99);
            assertEquals(99,
                    ref.getValue(),
                    "Ordered value should be visible via getValue");
        } finally {
            // ensure reference is closed for leak checks
            if (ref != null) ref.close();
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("binary long reference operations update values")
    public void binaryLongReferenceOps() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(16);
        try {
            BinaryLongReference ref = new BinaryLongReference();
            ref.bytesStore(b.bytesStore(), b.start(), ref.maxSize());
            ref.setValue(7L);
            assertEquals(7L,
                    ref.getValue(),
                    "setValue should update the long reference");
            assertEquals(7L,
                    ref.getVolatileValue(),
                    "Volatile read should match the updated value");
            ref.setVolatileValue(8L);
            assertEquals(8L,
                    ref.getValue(),
                    "setVolatileValue should update the reference");
            ref.setOrderedValue(9L);
            assertEquals(9L,
                    ref.getValue(),
                    "setOrderedValue should update the reference");
            assertEquals(19L,
                    ref.addValue(10L),
                    "addValue should return the updated total");
            assertEquals(29L,
                    ref.addAtomicValue(10L),
                    "addAtomicValue should return the updated total");
            assertTrue(ref.toString().contains("value:"),
                    "toString should include the value label");
            ref.close();
        } finally {
            b.releaseLast();
        }
    }
}
