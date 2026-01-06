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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("Binary int array reference marshalling and access")
public class BinaryIntArrayReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("direct array values round trip through reference")
    public void getSetValues() {
        final int length = 128 * 4 + 2 * 8;
        final Bytes<?> bytes = Bytes.allocateDirect(length);
        try {
            BinaryIntArrayReference.write(bytes, 128);

            try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
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
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for marshalling test");
        assumeFalse(NativeBytes.areNewGuarded(),
                "Native bytes guards must be disabled for marshalling test");
        final Bytes<?> bytes = Bytes.allocateElasticDirect(256);
        try {
            final IntArrays la = new IntArrays(4, 8);
            la.writeMarshallable(bytes);

            final String expected =
                    "00000000 04 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000020 08 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000030 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000040 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual,
                    "Marshalled hex dump matches expected output snapshot");

            //System.out.println(bytes.toHexString());

            final IntArrays la2 = new IntArrays(0, 0);
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
    @DisplayName("write rejects capacities above the maximum")
    public void writeRejectsCapacityAboveMax() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            long capacity = BinaryIntArrayReference.MAX_CAPACITY + 1;
            assertThrows(IllegalArgumentException.class,
                    () -> BinaryIntArrayReference.write(bytes, capacity),
                    "Capacity above MAX_CAPACITY should be rejected");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("forceAllToNotCompleteState moves collected references to not-complete state")
    public void forceAllToNotCompleteStateUpdatesReferences() {
        BinaryIntArrayReference.startCollecting();
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        BinaryIntArrayReference array = new BinaryIntArrayReference(2);
        try {
            BinaryIntArrayReference.write(bytes, 2);
            array.bytesStore(bytes, 0, array.maxSize());
            array.setValueAt(0, 1);
            array.compareAndSet(0, 1, BinaryIntReference.INT_NOT_COMPLETE);

            BinaryIntArrayReference.forceAllToNotCompleteState();
            assertEquals(BinaryIntReference.INT_NOT_COMPLETE,
                    array.getValueAt(0),
                    "Collected references should be moved to the not-complete state");
        } finally {
            array.close();
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("forceAllToNotCompleteState ignores missing collectors without throwing")
    public void forceAllToNotCompleteStateIgnoresMissingCollectors() {
        assertDoesNotThrow(BinaryIntArrayReference::forceAllToNotCompleteState,
                "forceAllToNotCompleteState should ignore missing collectors");
    }

    @Test
    @DisplayName("lazyWrite leaves existing value bytes untouched")
    public void lazyWriteLeavesExistingBytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            for (int i = 16; i < 24; i++) {
                bytes.writeByte(i, (byte) 0x7F);
            }
            BinaryIntArrayReference.lazyWrite(bytes, 2);
            assertEquals((byte) 0x7F,
                    bytes.readByte(16),
                    "lazyWrite should not zero out the values area");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("sizeInBytes rejects capacities above the maximum")
    public void sizeInBytesRejectsOverflow() {
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            long overflow = BinaryIntArrayReference.MAX_CAPACITY + 1;
            assertThrows(IllegalArgumentException.class,
                    () -> array.sizeInBytes(overflow),
                    "sizeInBytes should reject values above the maximum capacity");
        }
    }

    @Test
    @DisplayName("bindValueAt rejects non-binary int references")
    public void bindValueAtRejectsUnsupportedType() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            try (TextIntReference invalid = new TextIntReference()) {
                assertNotNull(invalid,
                        "Test must supply a non-binary int reference");
                assertThrows(IllegalArgumentException.class,
                        () -> array.bindValueAt(0, invalid),
                        "bindValueAt should reject non-binary int references");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("bindValueAt forwards to binary references with configured length")
    public void bindValueAtForwardsBinaryReference() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            try (BinaryIntReference value = new BinaryIntReference()) {
                assertThrows(IllegalArgumentException.class,
                        () -> array.bindValueAt(1, value),
                        "Binary int references enforce the expected element length");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("bytesStore rejects lengths that do not match peak length")
    public void bytesStoreRejectsLengthMismatch() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
            assertThrows(IllegalArgumentException.class,
                    () -> array.bytesStore(bytes, 0, length - 1),
                    "bytesStore should reject lengths that differ from peak length");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("setMaxUsed updates the stored used count")
    public void setMaxUsedUpdatesUsedCount() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.setMaxUsed(2);
            assertEquals(2,
                    array.getUsed(),
                    "setMaxUsed should update the used count");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("toString reports when no bytes store is set")
    public void toStringReportsWhenUnset() {
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            String summary = array.toString();
            assertTrue(summary.contains("not set"),
                    "summary '" + summary + "' should contain 'not set' when bytes store is missing");
        }
    }

    @Test
    @DisplayName("toString appends ellipsis when used is below capacity")
    public void toStringAppendsEllipsisForPartialUse() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 4);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.setValueAt(0, 11);
            array.setValueAt(1, 22);
            array.setMaxUsed(2);
            String summary = array.toString();
            assertTrue(summary.contains("..."),
                    "summary '" + summary + "' should contain '...' when capacity is not fully used");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeMarshallable includes comments when hex dump output is used")
    public void writeMarshallableIncludesHexDumpComments() {
        try (BinaryIntArrayReference array = new BinaryIntArrayReference(2)) {
            HexDumpBytes bytes = new HexDumpBytes();
            try {
                array.writeMarshallable(bytes);
                assertTrue(bytes.toHexString().contains("BinaryIntArrayReference"),
                        "Hex dump output should include the binary int array description");
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("bytesStore accepts hex dump bytes output")
    public void bytesStoreAcceptsHexDumpBytes() {
        HexDumpBytes bytes = new HexDumpBytes();
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.setValueAt(0, 123);
            assertEquals(123,
                    bytes.readInt(16),
                    "Hex dump backing stores should accept array value updates");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("reset clears the reference state and marks it null")
    public void resetClearsReference() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
            array.bytesStore(bytes, 0, length);
            array.reset();
            assertTrue(array.isNull(),
                    "Reference should report null after reset");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("capacity and getCapacity work without a backing store")
    public void capacityWorksWithoutBackingStore() {
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            array.capacity(3);
            assertEquals(3,
                    array.getCapacity(),
                    "getCapacity should return the configured size when unbound");
            assertTrue(array.isNull(),
                    "Array reference should remain null without a backing store");
        }
    }

    @Test
    @DisplayName("writeMarshallable emits data for unbound arrays without comments")
    public void writeMarshallableForUnboundArray() {
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference(2)) {
            array.writeMarshallable(output);
            output.readPosition(0);
            assertEquals(2L,
                    output.readLong(),
                    "Unbound array should emit its configured capacity");
            assertEquals(0L,
                    output.readLong(),
                    "Unbound array should emit zero used count");
        } finally {
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("peakLength rejects capacity above the maximum")
    public void peakLengthRejectsCapacityAboveMax() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.writeLong(BinaryIntArrayReference.MAX_CAPACITY + 1);
            bytes.writeLong(0L);
            assertThrows(IllegalArgumentException.class,
                    () -> BinaryIntArrayReference.peakLength(bytes, 0),
                    "peakLength should reject capacities above the maximum");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readMarshallable rejects negative capacity values from input")
    public void readMarshallableRejectsNegativeCapacity() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            bytes.writeLong(-1L);
            bytes.writeLong(0L);
            bytes.readPosition(0);
            assertThrows(net.openhft.chronicle.core.io.IORuntimeException.class,
                    () -> array.readMarshallable(bytes),
                    "Negative capacity should be rejected");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readMarshallable rejects used values above capacity")
    public void readMarshallableRejectsUsedAboveCapacity() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            bytes.writeLong(1L);
            bytes.writeLong(2L);
            bytes.writeSkip(4);
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
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
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
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
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
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
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
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
            BinaryIntArrayReference.write(bytes, 2);
            long length = BinaryIntArrayReference.peakLength(bytes, 0);
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

    private static final class IntArrays implements BytesMarshallable {
        BinaryIntArrayReference first = new BinaryIntArrayReference();
        BinaryIntArrayReference second = new BinaryIntArrayReference();

        IntArrays(int firstLength, int secondLength) {
            first.capacity(firstLength);
            second.capacity(secondLength);
        }

        void closeAll() {
            first.close();
            second.close();
        }
    }
}
