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

import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BinaryTwoLongReferenceUnitTest {

    @Test
    public void testGetValue2AndSetValue2() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        binaryTwoLongReference.bytesStore(bytesStore, 0, 16);

        binaryTwoLongReference.setValue2(123456789L);
        assertEquals(123456789L, binaryTwoLongReference.getValue2());
    }

    @Test
    public void testGetVolatileValue2AndSetVolatileValue2() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        binaryTwoLongReference.bytesStore(bytesStore, 0, 16);

        binaryTwoLongReference.setVolatileValue2(123456789L);
        assertEquals(123456789L, binaryTwoLongReference.getVolatileValue2());
    }

    @Test
    public void testSetOrderedValue2() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        binaryTwoLongReference.bytesStore(bytesStore, 0, 16);

        binaryTwoLongReference.setOrderedValue2(987654321L);
        assertEquals(987654321L, binaryTwoLongReference.getValue2());
    }

    @Test
    public void testAddValue2() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        binaryTwoLongReference.bytesStore(bytesStore, 0, 16);

        binaryTwoLongReference.setValue2(100L);
        assertEquals(150L, binaryTwoLongReference.addValue2(50L));
    }

    @Test
    public void testAddAtomicValue2() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        binaryTwoLongReference.bytesStore(bytesStore, 0, 16);

        binaryTwoLongReference.setValue2(100L);
        assertEquals(150L, binaryTwoLongReference.addAtomicValue2(50L));
    }

    @Test
    public void testCompareAndSwapValue2() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        binaryTwoLongReference.bytesStore(bytesStore, 0, 16);

        binaryTwoLongReference.setValue2(100L);
        assertTrue(binaryTwoLongReference.compareAndSwapValue2(100L, 200L));
        assertEquals(200L, binaryTwoLongReference.getValue2());
    }

    @Test
    public void testMaxSize() {
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        assertEquals(16, binaryTwoLongReference.maxSize());
    }

    @Test
    public void testToStringWithValueSet() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        binaryTwoLongReference.bytesStore(bytesStore, 0, 16);

        binaryTwoLongReference.setValue(500L);
        binaryTwoLongReference.setValue2(600L);
        assertEquals("value: 500, value2: 600", binaryTwoLongReference.toString());
    }

    @Test
    public void testToStringWhenBytesIsNull() {
        BinaryTwoLongReference binaryTwoLongReference = new BinaryTwoLongReference();
        assertEquals("bytes is null", binaryTwoLongReference.toString());
    }
}
