/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class TextLongArrayReferenceTest extends BytesTestCommon {

    public static final int LENGTH = 90;

    @Test
    public void testWriteAndReadArray() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 5;
        TextLongArrayReference.write(bytes, capacity);
        assertTrue(bytes.readRemaining() > 0, "write() should leave readable content");

        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, TextLongArrayReference.peakLength(bytes, 0));
            assertEquals(capacity, ref.getCapacity(), "TextLongArrayReference should preserve capacity 5 specified during write");

            for (long i = 0; i < capacity; i++) {
                ref.setValueAt(i, (int) i + 1);
                assertEquals((int) i + 1, ref.getValueAt(i), "getValueAt should return " + ((int) i + 1) + " after setValueAt at index " + i);
            }
        }
        bytes.releaseLast();
    }

    @Test
    public void testSetValueAt() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            ref.setValueAt(0, 123);
            assertEquals(123, ref.getValueAt(0), "array should retrieve the exact value written to first position");
        }
        bytes.releaseLast();
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testCompareAndSetIndex1() {
        assumeFalse(Jvm.isArm());
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            int index = 1;
            ref.setValueAt(index, 200);
            boolean result = ref.compareAndSet(index, 200, 250);
            assertFalse(result, "TextLongArrayReference compareAndSet should return false as text format does not support atomic operations");
            assertEquals(200, ref.getValueAt(index), "TextLongArrayReference value should remain 200 after failed compareAndSet at index " + index);
        }
        bytes.releaseLast();
    }

    @Test
    public void testBindValueAt() {
        assertThrows(UnsupportedOperationException.class, () -> {
            try (TextLongArrayReference ref = new TextLongArrayReference();
                 BinaryLongReference value = new BinaryLongReference()) {
                ref.bindValueAt(0, value);
            }
        });
    }

    @Test
    public void testIsNotNullAfterBytesStore() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            assertFalse(ref.isNull(), "reference should not be null after bytesStore");
        }
        bytes.releaseLast();
    }

    @Test
    public void testReset() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            ref.reset();
            assertTrue(ref.isNull(), "reference should be null after reset");
        }
        bytes.releaseLast();
    }

    @Test
    public void testMaxSize() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            assertEquals(LENGTH, ref.maxSize(), "maxSize should reflect the length constraint specified during bytesStore initialization");
        }
        bytes.releaseLast();
    }

    @Test
    public void getSetValues() {
        int length = 5 * 22 + LENGTH;
        Bytes<?> bytes = Bytes.allocateElastic(length);
        TextLongArrayReference.write(bytes, 5);

        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            array.bytesStore(bytes, 0, length);

            assertEquals(5, array.getCapacity(), "array should preserve capacity from the initial write operation");
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals(i + 1, array.getValueAt(i), "array should maintain sequential values written during initialization at index " + i);

            @NotNull final String expected = "{ locked: false, capacity: 5                   , used: 00000000000000000000, " +
                    "values: [ 00000000000000000001, 00000000000000000002, 00000000000000000003, 00000000000000000004, 00000000000000000005 ] }\n";
//            System.out.println(expected.length());
            assertEquals(expected, bytes.toString(),
                    "text serialization should produce the canonical human-readable format with locked flag, capacity, used bitmap, and zero-padded values");
            bytes.releaseLast();
        }
    }
}
