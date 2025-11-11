/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReferenceTypesTest extends BytesTestCommon {

    @Test
    public void textIntReferenceRoundTrip() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(128);
        TextIntReference ref = null;
        try {
            // Prepare backing store with template and bind reference
            TextIntReference.write(b, 42);
            ref = new TextIntReference();
            BytesStore<?, ?> store = b.bytesStore();
            ref.bytesStore(store, b.start(), ref.maxSize());

            assertEquals(42, ref.getValue());

            ref.setValue(1234);
            assertEquals(1234, ref.getValue());

            assertTrue(ref.compareAndSwapValue(1234, 5678));
            assertEquals(5678, ref.getValue());

            assertEquals(5680, ref.addValue(2));
            assertEquals(5680, ref.getVolatileValue());
            ref.setOrderedValue(99);
            assertEquals(99, ref.getValue());
        } finally {
            // ensure reference is closed for leak checks
            if (ref != null) ref.close();
            b.releaseLast();
        }
    }

    @Test
    public void binaryLongReferenceOps() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(16);
        try {
            BinaryLongReference ref = new BinaryLongReference();
            ref.bytesStore(b.bytesStore(), b.start(), ref.maxSize());
            ref.setValue(7L);
            assertEquals(7L, ref.getValue());
            assertEquals(7L, ref.getVolatileValue());
            ref.setVolatileValue(8L);
            assertEquals(8L, ref.getValue());
            ref.setOrderedValue(9L);
            assertEquals(9L, ref.getValue());
            assertEquals(19L, ref.addValue(10L));
            assertEquals(29L, ref.addAtomicValue(10L));
            assertTrue(ref.toString().contains("value:"));
            ref.close();
        } finally {
            b.releaseLast();
        }
    }
}
