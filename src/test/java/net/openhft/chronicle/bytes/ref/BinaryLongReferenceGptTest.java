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
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BinaryLongReferenceGptTest {

    @Test
    public void testGetValueAndSetValue() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        binaryLongReference.bytesStore(bytesStore, 0, 8);

        binaryLongReference.setValue(123456789L);
        assertEquals(123456789L, binaryLongReference.getValue());
    }

    @Test
    public void testGetVolatileValueAndSetVolatileValue() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        binaryLongReference.bytesStore(bytesStore, 0, 8);

        binaryLongReference.setVolatileValue(123456789L);
        assertEquals(123456789L, binaryLongReference.getVolatileValue());
    }

    @Test
    public void testSetOrderedValue() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        binaryLongReference.bytesStore(bytesStore, 0, 8);

        binaryLongReference.setOrderedValue(987654321L);
        assertEquals(987654321L, binaryLongReference.getValue());
    }

    @Test
    public void testAddValue() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        binaryLongReference.bytesStore(bytesStore, 0, 8);

        binaryLongReference.setValue(100L);
        assertEquals(150L, binaryLongReference.addValue(50L));
    }

    @Test
    public void testAddAtomicValue() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        binaryLongReference.bytesStore(bytesStore, 0, 8);

        binaryLongReference.setValue(100L);
        assertEquals(150L, binaryLongReference.addAtomicValue(50L));
    }

    @Test
    public void testCompareAndSwapValue() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        binaryLongReference.bytesStore(bytesStore, 0, 8);

        binaryLongReference.setValue(100L);
        assertTrue(binaryLongReference.compareAndSwapValue(100L, 200L));
        assertEquals(200L, binaryLongReference.getValue());
    }

    @Test
    public void testMaxSize() {
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        assertEquals(8, binaryLongReference.maxSize());
    }

    @Test
    public void testBytesStoreThrowsExceptionForInvalidLength() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryLongReference binaryLongReference = new BinaryLongReference();

        assertThrows(IllegalArgumentException.class, () ->
                binaryLongReference.bytesStore(bytesStore, 0, 10));
    }

    @Test
    public void testBytesStoreWithHexDumpBytes() {
        BytesStore bytesStore = new HexDumpBytes();
        BinaryLongReference binaryLongReference = new BinaryLongReference();

        // This should not throw an exception
        binaryLongReference.bytesStore(bytesStore, 0, 8);
    }

    @Test
    public void testToString() {
        BytesStore bytesStore = BytesStore.nativeStoreWithFixedCapacity(16);
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        binaryLongReference.bytesStore(bytesStore, 0, 8);

        binaryLongReference.setValue(500L);
        assertEquals("value: 500", binaryLongReference.toString());
    }

    @Test
    public void testToStringWhenBytesIsNull() {
        BinaryLongReference binaryLongReference = new BinaryLongReference();
        assertEquals("bytes is null", binaryLongReference.toString());
    }

}
