/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class TextIntArrayReferenceTest extends BytesTestCommon {

    @Test
    void testWriteAndReadArray() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 5;
        TextIntArrayReference.write(bytes, capacity);
        assertTrue(bytes.readRemaining() > 0);

        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, TextIntArrayReference.peakLength(bytes, 0));
            assertEquals(capacity, ref.getCapacity());

            for (long i = 0; i < capacity; i++) {
                ref.setValueAt(i, (int) i + 1);
                assertEquals((int) i + 1, ref.getValueAt(i));
            }
        }
        bytes.releaseLast();
    }

    @Test
    void testPeakLength() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 10;
        TextIntArrayReference.write(bytes, capacity);
        long length = TextIntArrayReference.peakLength(bytes, 0);
        assertTrue(length > 0);
        bytes.releaseLast();
    }

    @Test
    void testSetValueAt() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            ref.setValueAt(0, 123);
            assertEquals(123, ref.getValueAt(0));
        }
        bytes.releaseLast();
    }

    @Test
    void testCompareAndSetIndex1() {
        assumeFalse(Jvm.isArm());
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            int index = 1;
            ref.setValueAt(index, 200);
            boolean result = ref.compareAndSet(index, 200, 250);
            assertFalse(result);
            assertEquals(200, ref.getValueAt(index));
        }
        bytes.releaseLast();
    }

    @Test
    void testBindValueAt() {
        assertThrows(UnsupportedOperationException.class, () -> {
            try (TextIntArrayReference ref = new TextIntArrayReference()) {
                IntValue value = null; // Placeholder for actual IntValue implementation
                ref.bindValueAt(0, value);
                fail("Expected to throw UnsupportedOperationException");
            }
        });
    }

    @Test
    void testIsNotNullAfterBytesStore() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            assertFalse(ref.isNull());
        }
        bytes.releaseLast();
    }

    @Test
    void testReset() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            ref.reset();
            assertTrue(ref.isNull());
        }
        bytes.releaseLast();
    }

    @Test
    void testMaxSize() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            assertEquals(70, ref.maxSize());
        }
        bytes.releaseLast();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void getSetValues() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        int length = 5 * 12 + 70;
        Bytes<?> bytes = Bytes.allocateDirect(length);
        TextIntArrayReference.write(bytes, 5);

        try (@NotNull TextIntArrayReference array = new TextIntArrayReference()) {
            array.bytesStore(bytes, 0, length);

            assertEquals(5, array.getCapacity());
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals(i + 1, array.getValueAt(i));

            @NotNull final String expected = "{ locked: false, capacity: 5         , used: 0000000000, values: [ 0000000001, 0000000002, 0000000003, 0000000004, 0000000005 ] }\n";
//            System.out.println(expected.length());
            assertEquals(expected, bytes.toString());
            bytes.releaseLast();
        }
    }
}
