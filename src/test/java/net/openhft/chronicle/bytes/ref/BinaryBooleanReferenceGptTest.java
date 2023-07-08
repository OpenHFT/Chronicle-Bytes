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
import org.junit.Assert;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public class BinaryBooleanReferenceGptTest {

    @Test
    public void testGetValueAndSetValue() throws BufferUnderflowException {
        BytesStore<?, Void> bytes = BytesStore.nativeStore(1);
        try (BinaryBooleanReference binaryBooleanReference = new BinaryBooleanReference()) {
            binaryBooleanReference.bytesStore(bytes, 0, 1);

            // Test set and get value
            binaryBooleanReference.setValue(true);
            Assert.assertTrue(binaryBooleanReference.getValue());

            binaryBooleanReference.setValue(false);
            Assert.assertFalse(binaryBooleanReference.getValue());
        }
        bytes.releaseLast();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSize() throws BufferOverflowException {
        BytesStore<?, Void> bytes = BytesStore.nativeStore(1);
        try (BinaryBooleanReference binaryBooleanReference = new BinaryBooleanReference()) {
            binaryBooleanReference.bytesStore(bytes, 0, 2); // length != maxSize()
        } finally {
            bytes.releaseLast();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidValue() throws BufferUnderflowException {
        BytesStore<?, Void> bytes = BytesStore.nativeStore(1);
        try (BinaryBooleanReference binaryBooleanReference = new BinaryBooleanReference()) {
            binaryBooleanReference.bytesStore(bytes, 0, 1);

            bytes.writeByte(0, (byte) 0xFF); // write invalid value

            binaryBooleanReference.getValue(); // should throw exception
        } finally {
            bytes.releaseLast();
        }
    }
}
