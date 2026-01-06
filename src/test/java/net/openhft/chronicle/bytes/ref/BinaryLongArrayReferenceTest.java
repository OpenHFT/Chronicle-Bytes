/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static net.openhft.chronicle.bytes.ref.BinaryLongReference.LONG_NOT_COMPLETE;

@SuppressWarnings("deprecation")
@DisplayName("Binary long array reference marshalling and access")
public class BinaryLongArrayReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("direct array values round trip through reference")
    public void getSetValues() {
        final int length = 128 * 8 + 2 * 8;
        final Bytes<?> bytes = Bytes.allocateDirect(length);
        try {
            BinaryLongArrayReference.write(bytes, 128);

            try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
                array.bytesStore(bytes, 0, length);

                assertEquals(128, array.getCapacity(),
                        "Array capacity matches configured length setting");
                for (int i = 0; i < 128; i++)
                    array.setValueAt(i, i + 1);

                for (int i = 0; i < 128; i++)
                    assertEquals(i + 1, array.getValueAt(i),
                            "Array value at index " + i + " round trips through store");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("marshallable arrays write and read capacity correctly")
    public void marshallable() {
        assumeFalse(NativeBytes.areNewGuarded(),
                "Native bytes guards must be disabled for marshalling test");
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for marshalling test");

        final Bytes<?> bytes = new HexDumpBytes();
        try {
            final LongArrays la = new LongArrays(4, 8);
            la.writeMarshallable(bytes);

            final String expected =
                    "                                                # first\n" +
                            "                                                # BinaryLongArrayReference\n" +
                            "   04 00 00 00 00 00 00 00                         # capacity\n" +
                            "   00 00 00 00 00 00 00 00                         # used\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # values\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # second\n" +
                            "                                                # BinaryLongArrayReference\n" +
                            "   08 00 00 00 00 00 00 00                         # capacity\n" +
                            "   00 00 00 00 00 00 00 00                         # used\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # values\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual,
                    "Marshalled hex dump matches expected output snapshot");

            //System.out.println(bytes.toHexString());

            final LongArrays la2 = new LongArrays(0, 0);
            la2.readMarshallable(bytes);
            assertEquals(4, la2.first.getCapacity(),
                    "First array capacity reads back as four");
            assertEquals(8, la2.second.getCapacity(),
                    "Second array capacity reads back as eight");
            la.closeAll();
            la2.closeAll();
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("sizeInBytes rejects capacities above the maximum")
    public void sizeInBytesRejectsOverflow() {
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            long overflow = BinaryLongArrayReference.MAX_CAPACITY + 1;
            assertThrows(ArithmeticException.class,
                    () -> array.sizeInBytes(overflow),
                    "sizeInBytes should reject values above the maximum capacity");
        }
    }

    @Test
    @DisplayName("write rejects negative capacity values for binary long arrays")
    public void writeRejectsNegativeCapacity() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> BinaryLongArrayReference.write(bytes, -1),
                    "Negative capacity should be rejected");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("forceAllToNotCompleteState resets collected references to not-complete state")
    public void forceAllToNotCompleteStateResetsReferences() {
        assumeFalse(Jvm.isArm(), "Atomic compareAndSet is not supported on ARM");
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for reference collection test");

        BinaryLongArrayReference.startCollecting();
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            long length = array.sizeInBytes(4);
            Bytes<?> bytes = Bytes.allocateDirect(length);
            try {
                BinaryLongArrayReference.write(bytes, 4);
                array.bytesStore(bytes, 0, length);
                array.setValueAt(0, 7);
                assertTrue(array.compareAndSet(0, 7, LONG_NOT_COMPLETE),
                        "compareAndSet should add the reference when setting the not-complete marker");
                array.setValueAt(0, 11);
                BinaryLongArrayReference.forceAllToNotCompleteState();
                assertEquals(LONG_NOT_COMPLETE,
                        array.getValueAt(0),
                        "forceAllToNotCompleteState should restore the not-complete marker");
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("bytesStore rejects lengths that do not match peak length")
    public void bytesStoreRejectsLengthMismatch() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(128);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            BinaryLongArrayReference.write(bytes, 2);
            long length = BinaryLongArrayReference.peakLength(bytes, 0);
            assertThrows(IllegalArgumentException.class,
                    () -> array.bytesStore(bytes, 0, length - 1),
                    "bytesStore should reject lengths that differ from peak length");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("toString reports when no bytes store is set")
    public void toStringReportsWhenUnset() {
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            String summary = array.toString();
            assertTrue(summary.contains("not set"),
                    "summary '" + summary + "' should contain 'not set' when bytes store is missing");
        }
    }

    @Test
    @DisplayName("toString appends ellipsis when used is below capacity")
    public void toStringAppendsEllipsisForPartialUse() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(128);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            BinaryLongArrayReference.write(bytes, 4);
            long length = BinaryLongArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.setValueAt(0, 11);
            array.setValueAt(1, 22);
            array.setMaxUsed(2);
            String summary = array.toString();
            assertTrue(summary.contains("..."),
                    "summary '" + summary + "' should contain '...' when capacity exceeds used count");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("bindValueAt rejects non-binary long references")
    public void bindValueAtRejectsUnsupportedType() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            BinaryLongArrayReference.write(bytes, 2);
            long length = BinaryLongArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            try (TextLongReference invalid = new TextLongReference()) {
                assertNotNull(invalid,
                        "Test should provide a non-binary long reference");
                assertThrows(IllegalArgumentException.class,
                        () -> array.bindValueAt(0, invalid),
                        "bindValueAt should reject non-binary long references");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("peakLength applies the capacity hint when capacity is zero")
    public void peakLengthAppliesCapacityHint() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.writeLong(0L);
            bytes.writeLong(0L);
            long length = BinaryLongArrayReference.peakLength(bytes, 0, 3);
            assertEquals(3L,
                    bytes.readLong(0),
                    "Capacity hint should be written when the stored capacity is zero");
            assertEquals((3L << BinaryLongArrayReference.SHIFT) + 16,
                    length,
                    "Peak length should reflect the applied capacity hint");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSet works when collectors are disabled")
    public void compareAndSetWithoutCollectors() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            BinaryLongArrayReference.write(bytes, 2);
            long length = BinaryLongArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.setValueAt(0, 1);
            assertTrue(array.compareAndSet(0, 1, LONG_NOT_COMPLETE),
                    "compareAndSet should succeed even when no collectors are registered");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readMarshallable rejects negative capacity values from input")
    public void readMarshallableRejectsNegativeCapacity() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            bytes.writeLong(-1L);
            bytes.writeLong(0L);
            bytes.readPosition(0);
            assertThrows(net.openhft.chronicle.core.io.IORuntimeException.class,
                    () -> array.readMarshallable(bytes),
                    "readMarshallable should reject negative capacity values");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readMarshallable rejects used values above capacity")
    public void readMarshallableRejectsUsedAboveCapacity() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            bytes.writeLong(1L);
            bytes.writeLong(2L);
            bytes.writeSkip(8);
            bytes.readPosition(0);
            assertThrows(net.openhft.chronicle.core.io.IORuntimeException.class,
                    () -> array.readMarshallable(bytes),
                    "Used values above capacity should be rejected");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readMarshallable rejects capacity beyond remaining bytes")
    public void readMarshallableRejectsCapacityBeyondRemaining() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            bytes.writeLong(5L);
            bytes.writeLong(0L);
            bytes.readPosition(0);
            assertThrows(net.openhft.chronicle.core.io.IORuntimeException.class,
                    () -> array.readMarshallable(bytes),
                    "Capacity beyond remaining bytes should be rejected");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSet updates array slot when expected value matches")
    public void compareAndSetUpdatesMatchingValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(128);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            BinaryLongArrayReference.write(bytes, 2);
            long length = BinaryLongArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.setValueAt(0, 1);
            assertTrue(array.compareAndSet(0, 1, 2),
                    "compareAndSet should update when expected matches");
            assertEquals(2,
                    array.getValueAt(0),
                    "Updated value should be stored after compareAndSet");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSet leaves array slot unchanged when expected differs")
    public void compareAndSetLeavesMismatchedValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(128);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            BinaryLongArrayReference.write(bytes, 2);
            long length = BinaryLongArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.setValueAt(0, 3);
            assertFalse(array.compareAndSet(0, 1, 4),
                    "compareAndSet should return false when expected does not match");
            assertEquals(3,
                    array.getValueAt(0),
                    "Value should remain unchanged after failed compareAndSet");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeMarshallable emits backing store bytes when bound")
    public void writeMarshallableUsesBackingStore() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(128);
        Bytes<?> output = Bytes.allocateElasticOnHeap(128);
        try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            BinaryLongArrayReference.write(bytes, 2);
            long length = BinaryLongArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.setValueAt(0, 9);
            output.writePosition(0);
            array.writeMarshallable(output);
            output.readPosition(0);
            assertEquals(2L,
                    output.readLong(),
                    "Marshalled output should contain the stored capacity");
        } finally {
            bytes.releaseLast();
            output.releaseLast();
        }
    }

    private static final class LongArrays implements BytesMarshallable {
        BinaryLongArrayReference first = new BinaryLongArrayReference();
        BinaryLongArrayReference second = new BinaryLongArrayReference();

        LongArrays(int firstLength, int secondLength) {
            first.capacity(firstLength);
            second.capacity(secondLength);
        }

        void closeAll() {
            first.close();
            second.close();
        }
    }
}
