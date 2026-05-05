/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests TextIntArrayReference write and read operations because correct
 * text-formatted arrays are essential for human-readable data inspection
 * in diagnostic tools.
 */
@DisplayName("Text int array reference write read and capacity checks")
@SuppressWarnings({"deprecation", "PMD.JUnit5TestShouldBePackagePrivate"})
class TextIntArrayReferenceTest extends BytesTestCommon {

    @Test
    @DisplayName("write and read array preserves capacity and stored values")
    public void testWriteAndReadArray() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for write/read array test");

        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 5;
        TextIntArrayReference.write(bytes, capacity);
        assertTrue(bytes.readRemaining() > 0,
                "Written array should leave remaining bytes in the buffer");

        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, TextIntArrayReference.peakLength(bytes, 0));
            assertEquals(capacity,
                    ref.getCapacity(),
                    "Reference should report the configured capacity");

            for (long i = 0; i < capacity; i++) {
                ref.setValueAt(i, (int) i + 1);
                assertEquals((int) i + 1,
                        ref.getValueAt(i),
                        "Value at index " + i + " should match the stored value");
            }
        }
        bytes.releaseLast();
    }

    @Test
    @DisplayName("peak length returns a positive size after writing")
    public void testPeakLength() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for peak length test");

        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 10;
        TextIntArrayReference.write(bytes, capacity);
        long length = TextIntArrayReference.peakLength(bytes, 0);
        assertTrue(length > 0,
                "Peak length should be positive after writing, but was " + length);
        bytes.releaseLast();
    }

    @Test
    @DisplayName("setValueAt stores value at requested index")
    public void testSetValueAt() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            ref.setValueAt(0, 123);
            assertEquals(123,
                    ref.getValueAt(0),
                    "Value at index zero should match the stored value");
        }
        bytes.releaseLast();
    }

    @Test
    @DisplayName("compareAndSet on index one keeps value when expected mismatches")
    public void testCompareAndSetIndex1() {
        assumeFalse(Jvm.isArm(), "Atomic compareAndSet is not supported on ARM");
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            int index = 1;
            ref.setValueAt(index, 200);
            boolean result = ref.compareAndSet(index, 200, 250);
            assertFalse(result,
                    "compareAndSet should report false when value is unchanged");
            assertEquals(200,
                    ref.getValueAt(index),
                    "Index one should retain the original value after compareAndSet");
        }
        bytes.releaseLast();
    }

    @Test
    @DisplayName("bindValueAt rejects unsupported value binding requests")
    public void testBindValueAt() {
        try (TextIntArrayReference ref = new TextIntArrayReference();
             IntValue value = new BinaryIntReference()) {
            assertThrows(UnsupportedOperationException.class,
                    () -> ref.bindValueAt(0, value),
                    "bindValueAt should throw UnsupportedOperationException");
        }
    }

    @Test
    @DisplayName("bytesStore initialises the reference to a usable state")
    public void testIsNotNullAfterBytesStore() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            assertFalse(ref.isNull(),
                    "Reference should be non-null after bytesStore initialisation");
        }
        bytes.releaseLast();
    }

    @Test
    @DisplayName("reset clears the reference state and marks it null")
    public void testReset() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            ref.reset();
            assertTrue(ref.isNull(),
                    "Reference should report null after reset");
        }
        bytes.releaseLast();
    }

    @Test
    @DisplayName("maxSize returns the configured length set during bytesStore binding")
    public void testMaxSize() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            assertEquals(70,
                    ref.maxSize(),
                    "maxSize should match the configured length");
        }
        bytes.releaseLast();
    }

    @SuppressWarnings({"rawtypes", "MMAnnotationTestOrder"})
    @Test
    @DisplayName("text int array formats values and reports capacity")
    public void getSetValues() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for text array format test");

        int length = 5 * 12 + 70;
        Bytes<?> bytes = Bytes.allocateDirect(length);
        TextIntArrayReference.write(bytes, 5);

        try (@NotNull TextIntArrayReference array = new TextIntArrayReference()) {
            array.bytesStore(bytes, 0, length);

            assertEquals(5,
                    array.getCapacity(),
                    "Array reference should report the configured capacity");
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals(i + 1,
                        array.getValueAt(i),
                        "Array value at index " + i + " should match the stored value");

            @NotNull final String expected = "{ locked: false, capacity: 5         , used: 0000000000, values: [ 0000000001, 0000000002, 0000000003, 0000000004, 0000000005 ] }\n";
            assertEquals(expected,
                    bytes.toString(),
                    "Formatted array output should match the expected text");
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("setMaxUsed updates used value when lower")
    public void setMaxUsedUpdatesWhenLower() {
        try (TextIntArrayReference array = new TextIntArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextIntArrayReference.write(bytes, 3);
                long length = TextIntArrayReference.peakLength(bytes, 0);
                array.bytesStore(bytes, 0, length);
                array.setMaxUsed(2);
                assertEquals(2,
                        array.getUsed(),
                        "setMaxUsed should update used value when it is lower");
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("setMaxUsed preserves higher used count when lower value supplied")
    public void setMaxUsedPreservesHigherValue() {
        try (TextIntArrayReference array = new TextIntArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextIntArrayReference.write(bytes, 3);
                long length = TextIntArrayReference.peakLength(bytes, 0);
                array.bytesStore(bytes, 0, length);
                array.setMaxUsed(2);
                array.setMaxUsed(1);
                assertEquals(2,
                        array.getUsed(),
                        "setMaxUsed should keep the higher used value");
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("bytesStore rejects mismatched lengths for text header")
    public void bytesStoreRejectsMismatchedLength() {
        Bytes<?> bytes = Bytes.allocateElastic(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            TextIntArrayReference.write(bytes, 2);
            long length = TextIntArrayReference.peakLength(bytes, 0);
            long badLength = length - 1;
            assertThrows(IllegalArgumentException.class,
                    () -> ref.bytesStore(bytes, 0, badLength),
                    "bytesStore should reject lengths that do not match the text header");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSet updates array slot when expected value matches")
    public void compareAndSetUpdatesWhenExpectedMatches() {
        assumeFalse(Jvm.isArm(),
                "Atomic compareAndSet is not supported on ARM for TextIntArrayReference");
        Bytes<?> bytes = Bytes.allocateElastic(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            TextIntArrayReference.write(bytes, 2);
            long length = TextIntArrayReference.peakLength(bytes, 0);
            ref.bytesStore(bytes, 0, length);
            ref.setValueAt(0, 10);
            assertTrue(ref.compareAndSet(0, 10, 20),
                    "compareAndSet should report true when expected matches the current value");
            assertEquals(20,
                    ref.getValueAt(0),
                    "compareAndSet should update the stored value when expected matches");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("toString reports bytes equals null before initialisation binding")
    public void toStringReportsNullState() {
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            String value = ref.toString();
            assertTrue(value.contains("bytes=null"),
                    "toString should report bytes=null when reference is uninitialised");
        }
    }
}
