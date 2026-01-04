/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextLongArrayReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("text long array reference stores values and formats output")
    public void getSetValues() {
        int length = 5 * 22 + 90;
        Bytes<?> bytes = Bytes.allocateElastic(length);
        TextLongArrayReference.write(bytes, 5);

        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            array.bytesStore(bytes, 0, length);

            assertEquals(5,
                    array.getCapacity(),
                    "Array reference should report the configured capacity");
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals(i + 1,
                        array.getValueAt(i),
                        "Value at index " + i + " should match the stored value");

            @NotNull final String expected = "{ locked: false, capacity: 5                   , used: 00000000000000000000, " +
                    "values: [ 00000000000000000001, 00000000000000000002, 00000000000000000003, 00000000000000000004, 00000000000000000005 ] }\n";
            assertEquals(expected,
                    bytes.toString(),
                    "Formatted array output should match the expected text");
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("setMaxUsed updates used value when lower")
    public void setMaxUsedUpdatesWhenLower() {
        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextLongArrayReference.write(bytes, 3);
                long length = TextLongArrayReference.peakLength(bytes, 0);
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
        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextLongArrayReference.write(bytes, 3);
                long length = TextLongArrayReference.peakLength(bytes, 0);
                array.bytesStore(bytes, 0, length);
                array.setUsed(2);
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
    @DisplayName("compareAndSet updates array slot when expected value matches")
    public void compareAndSetUpdatesMatchingValue() {
        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextLongArrayReference.write(bytes, 2);
                long length = TextLongArrayReference.peakLength(bytes, 0);
                array.bytesStore(bytes, 0, length);
                array.setValueAt(0, 1);
                assertTrue(array.compareAndSet(0, 1, 7),
                        "compareAndSet should update when expected matches");
                assertEquals(7,
                        array.getValueAt(0),
                        "Updated value should be visible after compareAndSet");
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("compareAndSet leaves array slot unchanged when expected differs")
    public void compareAndSetLeavesMismatchedValue() {
        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextLongArrayReference.write(bytes, 2);
                long length = TextLongArrayReference.peakLength(bytes, 0);
                array.bytesStore(bytes, 0, length);
                array.setValueAt(0, 4);
                assertFalse(array.compareAndSet(0, 2, 9),
                        "compareAndSet should return false when expected does not match");
                assertEquals(4,
                        array.getValueAt(0),
                        "Value should remain unchanged when compareAndSet fails");
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("bytesStore rejects mismatched lengths for text header")
    public void bytesStoreRejectsMismatchedLength() {
        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextLongArrayReference.write(bytes, 2);
                long length = TextLongArrayReference.peakLength(bytes, 0);
                long badLength = length - 1;
                assertFalse(badLength == length,
                        "Sanity check should confirm badLength=" + badLength + " differs from length=" + length);
                assertThrows(IllegalArgumentException.class,
                        () -> array.bytesStore(bytes, 0, badLength),
                        "bytesStore should reject lengths that do not match the header");
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("toString reports null state before initialisation")
    public void toStringReportsNullState() {
        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            String value = array.toString();
            assertTrue(value.contains("bytes=null"),
                    "toString should report bytes=null when reference is uninitialised");
        }
    }

    @Test
    @DisplayName("ordered writes are visible via volatile reads")
    public void orderedAndVolatileAccessors() {
        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextLongArrayReference.write(bytes, 2);
                long length = TextLongArrayReference.peakLength(bytes, 0);
                array.bytesStore(bytes, 0, length);
                array.setOrderedValueAt(0, 42);
                assertEquals(42,
                        array.getVolatileValueAt(0),
                        "Volatile reads should observe ordered writes");
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("reset clears the reference state and marks it null")
    public void resetClearsReference() {
        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            Bytes<?> bytes = Bytes.allocateElastic(256);
            try {
                TextLongArrayReference.write(bytes, 2);
                long length = TextLongArrayReference.peakLength(bytes, 0);
                array.bytesStore(bytes, 0, length);
                array.reset();
                assertTrue(array.isNull(),
                        "Reference should report null after reset");
            } finally {
                bytes.releaseLast();
            }
        }
    }
}
