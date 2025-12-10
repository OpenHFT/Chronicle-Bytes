/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class TextLongArrayReferenceTest extends BytesTestCommon {

    public static final int LENGTH = 90;

    @Test
    public void testWriteAndReadArray() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 5;
        TextLongArrayReference.write(bytes, capacity);
        Assert.assertTrue("write() should leave readable content", bytes.readRemaining() > 0);

        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, TextLongArrayReference.peakLength(bytes, 0));
            Assert.assertEquals("capacity after bytesStore", capacity, ref.getCapacity());

            for (long i = 0; i < capacity; i++) {
                ref.setValueAt(i, (int) i + 1);
                Assert.assertEquals("value at index " + i, (int) i + 1, ref.getValueAt(i));
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
            Assert.assertEquals("value set at index 0", 123, ref.getValueAt(0));
        }
        bytes.releaseLast();
    }

    @Test(timeout = 1000)
    public void testCompareAndSetIndex1() {
        assumeFalse(Jvm.isArm());
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            int index = 1;
            ref.setValueAt(index, 200);
            boolean result = ref.compareAndSet(index, 200, 250);
            Assert.assertFalse("compareAndSet should fail when locked value unchanged", result);
            Assert.assertEquals("value should remain unchanged at index " + index, 200, ref.getValueAt(index));
        }
        bytes.releaseLast();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBindValueAt() {
        try (TextLongArrayReference ref = new TextLongArrayReference();
             BinaryLongReference value = new BinaryLongReference()) {
            ref.bindValueAt(0, value);
            fail("Expected to throw UnsupportedOperationException");
        }
    }

    @Test
    public void testIsNotNullAfterBytesStore() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            Assert.assertFalse("reference should not be null after bytesStore", ref.isNull());
        }
        bytes.releaseLast();
    }

    @Test
    public void testReset() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            ref.reset();
            Assert.assertTrue("reference should be null after reset", ref.isNull());
        }
        bytes.releaseLast();
    }

    @Test
    public void testMaxSize() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference ref = new TextLongArrayReference()) {
            ref.bytesStore(bytes, 0, LENGTH); // Example length, adjust based on actual implementation
            Assert.assertEquals("maxSize should match allocated LENGTH", LENGTH, ref.maxSize());
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

            assertEquals("capacity after bytesStore", 5, array.getCapacity());
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals("value at index " + i, i + 1, array.getValueAt(i));

            @NotNull final String expected = "{ locked: false, capacity: 5                   , used: 00000000000000000000, " +
                    "values: [ 00000000000000000001, 00000000000000000002, 00000000000000000003, 00000000000000000004, 00000000000000000005 ] }\n";
//            System.out.println(expected.length());
            assertEquals("text representation mismatch", expected,
                    bytes.toString());
            bytes.releaseLast();
        }
    }
}
