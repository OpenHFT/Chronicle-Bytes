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
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class BytesMarshallerTest {

    static class TestClass {
        public String[] stringArray;
    }

    private BytesMarshaller<TestObject> marshaller;
    private Bytes<?> bytes;

    private BytesMarshaller.ObjectArrayFieldAccess fieldAccess;
    private BytesOut<?> bytesOut;
    private BytesIn<?> bytesIn;
    private TestClass testObject;
    private Field field;

    static class TestObject implements ReadBytesMarshallable, WriteBytesMarshallable {
        int intValue;
        String stringValue;
        double doubleValue;

        // Assume getters and setters

        @Override
        public void readMarshallable(BytesIn<?> bytes) {
            this.intValue = bytes.readInt();
            this.stringValue = bytes.readUtf8();
            this.doubleValue = bytes.readDouble();
        }

        @Override
        public void writeMarshallable(BytesOut<?> bytes) {
            bytes.writeInt(intValue);
            bytes.writeUtf8(stringValue);
            bytes.writeDouble(doubleValue);
        }
    }

    @BeforeEach
    void setup() throws NoSuchFieldException {
        marshaller = new BytesMarshaller<>(TestObject.class);
        bytes = Bytes.allocateDirect(64);
        Field field = TestClass.class.getField("stringArray");
        fieldAccess = new BytesMarshaller.ObjectArrayFieldAccess(field);
        bytesOut = mock(BytesOut.class);
        bytesIn = mock(BytesIn.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        // Initialize your test object
        testObject = new TestClass();
        // Assuming TestClass has a field named "stringArray" you want to test
        field = TestClass.class.getDeclaredField("stringArray");
        field.setAccessible(true);
        // Initialize the ObjectArrayFieldAccess with the field
        fieldAccess = new BytesMarshaller.ObjectArrayFieldAccess(field);
    }

    @Test
    void getValueWithEmptyArray() throws IllegalAccessException {
        // Empty array
        testObject.stringArray = new String[0];
        fieldAccess.getValue(testObject, bytesOut);
        verify(bytesOut, times(1)).writeStopBit(0);
    }

    @Test
    void getValueWithNonEmptyArray() throws IllegalAccessException {
        // Non-empty array
        testObject.stringArray = new String[]{"hello", "world"};
        fieldAccess.getValue(testObject, bytesOut);
        verify(bytesOut, times(1)).writeStopBit(2);
    }

    @Test
    void setValueWithNullArray() throws IllegalAccessException {
        // Simulate reading -1 for null array
        when(bytesIn.readStopBit()).thenReturn(-1L);
        fieldAccess.setValue(testObject, bytesIn);
        assert testObject.stringArray == null;
    }

    @Test
    void setValueWithEmptyArray() throws IllegalAccessException {
        // Simulate reading 0 for empty array
        when(bytesIn.readStopBit()).thenReturn(0L);
        fieldAccess.setValue(testObject, bytesIn);
        assert testObject.stringArray.length == 0;
    }

    @Test
    void setValueWithNonEmptyArray() throws IllegalAccessException {
        // Simulate reading 2 for array size, then read strings
        when(bytesIn.readStopBit()).thenReturn(2L);
        when(bytesIn.readObject(String.class)).thenReturn("hello", "world");
        fieldAccess.setValue(testObject, bytesIn);
        assert Arrays.equals(testObject.stringArray, new String[]{"hello", "world"});
    }

    @Test
    void testWriteAndReadMarshallable() {
        TestObject original = new TestObject();
        original.intValue = 42;
        original.stringValue = "Hello";
        original.doubleValue = 3.14;

        marshaller.writeMarshallable(original, bytes);
        bytes.readPosition(0); // Reset the read position to the start

        TestObject result = new TestObject();
        marshaller.readMarshallable(result, bytes);

        assertEquals(original.intValue, result.intValue);
        assertEquals(original.stringValue, result.stringValue);
        assertEquals(original.doubleValue, result.doubleValue, 0.001);
    }
}
