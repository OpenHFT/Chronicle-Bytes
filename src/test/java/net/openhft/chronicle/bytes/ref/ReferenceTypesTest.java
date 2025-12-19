/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

            assertEquals(42, ref.getValue(), "getValue should return 42 after initial write");

            ref.setValue(1234);
            assertEquals(1234, ref.getValue(), "getValue should return 1234 after setValue");

            assertTrue(ref.compareAndSwapValue(1234, 5678), "compareAndSwapValue should succeed when current value matches expected 1234");
            assertEquals(5678, ref.getValue(), "getValue should return 5678 after successful CAS operation");

            assertEquals(5680, ref.addValue(2), "addValue should return new value 5680 after adding 2 to current value 5678");
            assertEquals(5680, ref.getVolatileValue(), "getVolatileValue should return 5680 after addValue operation");
            ref.setOrderedValue(99);
            assertEquals(99, ref.getValue(), "getValue should return 99 after setOrderedValue");
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
            assertEquals(7L, ref.getValue(), "getValue should return 7 after setValue");
            assertEquals(7L, ref.getVolatileValue(), "getVolatileValue should return 7 after setValue");
            ref.setVolatileValue(8L);
            assertEquals(8L, ref.getValue(), "getValue should return 8 after setVolatileValue");
            ref.setOrderedValue(9L);
            assertEquals(9L, ref.getValue(), "getValue should return 9 after setOrderedValue");
            assertEquals(19L, ref.addValue(10L), "addValue should return new value 19 after adding 10 to current value 9");
            assertEquals(29L, ref.addAtomicValue(10L), "addAtomicValue should return new value 29 after atomically adding 10 to current value 19");
            assertTrue(ref.toString().contains("value:"), "string representation should contain 'value:' prefix in BinaryLongReference toString output");
            ref.close();
        } finally {
            b.releaseLast();
        }
    }
}
