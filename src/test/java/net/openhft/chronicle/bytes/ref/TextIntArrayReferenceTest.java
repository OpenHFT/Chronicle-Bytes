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

@SuppressWarnings("deprecation")
public class TextIntArrayReferenceTest extends BytesTestCommon {

    public static final int LENGTH = 70;

    @Test
    public void testWriteAndReadArray() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 5;
        TextIntArrayReference.write(bytes, capacity);
        assertTrue(bytes.readRemaining() > 0, "write() should leave readable content");

        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, TextIntArrayReference.peakLength(bytes, 0));
            assertEquals(capacity, ref.getCapacity(), "TextIntArrayReference should preserve capacity 5 specified during write");

            for (long i = 0; i < capacity; i++) {
                ref.setValueAt(i, (int) i + 1);
                assertEquals((int) i + 1, ref.getValueAt(i), "getValueAt should return " + ((int) i + 1) + " after setValueAt at index " + i);
            }
        }
        bytes.releaseLast();
    }

    @Test
    public void testPeakLength() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 10;
        TextIntArrayReference.write(bytes, capacity);
        long length = TextIntArrayReference.peakLength(bytes, 0);
        assertTrue(length > 0, "peakLength should be positive");
        bytes.releaseLast();
    }

    @Test
    public void testSetValueAt() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            ref.setValueAt(0, 123);
            assertEquals(123, ref.getValueAt(0), "getValueAt should return 123 after setValueAt at index 0");
        }
        bytes.releaseLast();
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testCompareAndSetIndex1() {
        assumeFalse(Jvm.isArm());
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            int index = 1;
            ref.setValueAt(index, 200);
            boolean result = ref.compareAndSet(index, 200, 250);
            assertFalse(result, "TextIntArrayReference compareAndSet should return false as text format does not support atomic operations");
            assertEquals(200, ref.getValueAt(index), "TextIntArrayReference value should remain 200 after failed compareAndSet at index " + index);
        }
        bytes.releaseLast();
    }

    @Test
    public void testBindValueAt() {
        assertThrows(UnsupportedOperationException.class, () -> {
            try (TextIntArrayReference ref = new TextIntArrayReference();
                 BinaryIntReference value = new BinaryIntReference()) {
                ref.bindValueAt(0, value);
                fail("Expected to throw UnsupportedOperationException");
            }
        });
    }

    @Test
    public void testIsNotNullAfterBytesStore() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            assertFalse(ref.isNull(), "reference should not be null after bytesStore");
        }
        bytes.releaseLast();
    }

    @Test
    public void testReset() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            ref.reset();
            assertTrue(ref.isNull(), "reference should be null after reset");
        }
        bytes.releaseLast();
    }

    @Test
    public void testMaxSize() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            assertEquals(LENGTH, ref.maxSize(), "maxSize should match allocated LENGTH");
        }
        bytes.releaseLast();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void getSetValues() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        int length = 5 * 12 + LENGTH;
        Bytes<?> bytes = Bytes.allocateDirect(length);
        TextIntArrayReference.write(bytes, 5);

        try (@NotNull TextIntArrayReference array = new TextIntArrayReference()) {
            array.bytesStore(bytes, 0, length);

            assertEquals(5, array.getCapacity(), "TextIntArrayReference should preserve capacity 5 from initial write");
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals(i + 1, array.getValueAt(i), "getValueAt should return " + (i + 1) + " after sequential initialization at index " + i);

            @NotNull final String expected = "{ locked: false, capacity: 5         , used: 0000000000, values: [ 0000000001, 0000000002, 0000000003, 0000000004, 0000000005 ] }\n";
//            System.out.println(expected.length());
            assertEquals(expected, bytes.toString(),
                    "text serialisation should produce canonical format with locked flag, capacity, used bitmap, and zero-padded values");
            bytes.releaseLast();
        }
    }
}
