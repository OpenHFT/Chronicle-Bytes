/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class TextIntArrayReferenceTest extends BytesTestCommon {

    @Test
    public void testWriteAndReadArray() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 5;
        TextIntArrayReference.write(bytes, capacity);
        Assert.assertTrue(bytes.readRemaining() > 0);

        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, TextIntArrayReference.peakLength(bytes, 0));
            Assert.assertEquals(capacity, ref.getCapacity());

            for (long i = 0; i < capacity; i++) {
                ref.setValueAt(i, (int) i + 1);
                Assert.assertEquals((int) i + 1, ref.getValueAt(i));
            }
        }
        bytes.releaseLast();
    }

    @Test
    public void testPeakLength() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        long capacity = 10;
        TextIntArrayReference.write(bytes, capacity);
        long length = TextIntArrayReference.peakLength(bytes, 0);
        Assert.assertTrue(length > 0);
        bytes.releaseLast();
    }

    @Test
    public void testSetValueAt() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            ref.setValueAt(0, 123);
            Assert.assertEquals(123, ref.getValueAt(0));
        }
        bytes.releaseLast();
    }

    @Test
    public void testCompareAndSetIndex1() {
        assumeFalse(Jvm.isArm());
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            int index = 1;
            ref.setValueAt(index, 200);
            boolean result = ref.compareAndSet(index, 200, 250);
            Assert.assertFalse(result);
            Assert.assertEquals(200, ref.getValueAt(index));
        }
        bytes.releaseLast();
    }

    @Test
    public void testCompareAndSetIndex8() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            int index = 8;
            ref.setValueAt(index, 200);
            boolean result = ref.compareAndSet(index, 200, 250);
            Assert.assertFalse(result);
            Assert.assertEquals(200, ref.getValueAt(index));
        }
        bytes.releaseLast();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBindValueAt() {
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            IntValue value = null; // Placeholder for actual IntValue implementation
            ref.bindValueAt(0, value);
            fail("Expected to throw UnsupportedOperationException");
        }
    }

    @Test
    public void testIsNotNullAfterBytesStore() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            Assert.assertFalse(ref.isNull());
        }
        bytes.releaseLast();
    }

    @Test
    public void testReset() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            ref.reset();
            Assert.assertTrue(ref.isNull());
        }
        bytes.releaseLast();
    }

    @Test
    public void testMaxSize() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference ref = new TextIntArrayReference()) {
            ref.bytesStore(bytes, 0, 70); // Example length, adjust based on actual implementation
            Assert.assertEquals(70, ref.maxSize());
        }
        bytes.releaseLast();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void getSetValues() {
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
            assertEquals(expected,
                    bytes.toString());
            bytes.releaseLast();
        }
    }
}
