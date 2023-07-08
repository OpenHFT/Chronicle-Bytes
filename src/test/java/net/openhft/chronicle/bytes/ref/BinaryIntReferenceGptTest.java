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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryIntReferenceGptTest extends BytesTestCommon {

    private BinaryIntReference binaryIntReference;
    private Bytes bytes;

    @BeforeEach
    public void setup() {
        binaryIntReference = new BinaryIntReference();
        bytes = Bytes.allocateDirect(4);
        binaryIntReference.bytesStore(bytes, 0, 4);
    }

    @AfterEach
    public void tearDown() {
        bytes.releaseLast();
    }
    @Test
    public void testGetValue() throws BufferUnderflowException, IllegalStateException {
        bytes.writeInt(0, 12345);
        assertEquals(12345, binaryIntReference.getValue());
    }

    @Test
    public void testSetValue() throws BufferOverflowException, IllegalStateException {
        binaryIntReference.setValue(67890);
        assertEquals(67890, bytes.readInt(0));
    }

    @Test
    public void testGetVolatileValue() throws BufferUnderflowException, IllegalStateException {
        bytes.writeVolatileInt(0, 22222);
        assertEquals(22222, binaryIntReference.getVolatileValue());
    }

    @Test
    public void testSetOrderedValue() throws BufferOverflowException, IllegalStateException {
        binaryIntReference.setOrderedValue(33333);
        assertEquals(33333, bytes.readVolatileInt(0));
    }

    @Test
    public void testAddValue() throws BufferUnderflowException, IllegalStateException {
        bytes.writeInt(0, 100);
        assertEquals(150, binaryIntReference.addValue(50));
        assertEquals(150, bytes.readInt(0));
    }

    @Test
    public void testAddAtomicValue() throws BufferUnderflowException, IllegalStateException {
        bytes.writeInt(0, 200);
        assertEquals(300, binaryIntReference.addAtomicValue(100));
        assertEquals(300, bytes.readInt(0));
    }

    @Test
    public void testCompareAndSwapValue() throws BufferOverflowException, IllegalStateException {
        bytes.writeInt(0, 1000);
        assertTrue(binaryIntReference.compareAndSwapValue(1000, 2000));
        assertEquals(2000, bytes.readInt(0));
        assertFalse(binaryIntReference.compareAndSwapValue(1000, 3000));
    }

    @Test
    public void testThrowsExceptionWhenClosed() {
        binaryIntReference.close();
        assertThrows(IllegalStateException.class, () -> binaryIntReference.setValue(123));
    }
}
