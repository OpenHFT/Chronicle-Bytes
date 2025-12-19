/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class BytesMarshallerTest {

    static class SampleClass {
        @SuppressWarnings("WeakerAccess")
        @UsedViaReflection
        public String[] stringArray;
    }

    private BytesMarshaller<SampleObject> marshaller;
    private Bytes<?> bytes;

    private BytesMarshaller.ObjectArrayFieldAccess fieldAccess;
    private BytesOut<?> bytesOut;
    private BytesIn<?> bytesIn;
    private SampleClass testObject;

    static class SampleObject implements ReadBytesMarshallable, WriteBytesMarshallable {
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
        marshaller = new BytesMarshaller<>(SampleObject.class);
        bytes = Bytes.allocateDirect(64);
        Field field = SampleClass.class.getField("stringArray");
        fieldAccess = new BytesMarshaller.ObjectArrayFieldAccess(field);
        bytesOut = mock(BytesOut.class);
        bytesIn = mock(BytesIn.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        // Initialize your test object
        testObject = new SampleClass();
        // Assuming SampleClass has a field named "stringArray" you want to test
        Field field = SampleClass.class.getDeclaredField("stringArray");
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
        assertNull(testObject.stringArray, "reading stop-bit -1 should deserialize to null array");
    }

    @Test
    void setValueWithEmptyArray() throws IllegalAccessException {
        // Simulate reading 0 for empty array
        when(bytesIn.readStopBit()).thenReturn(0L);
        fieldAccess.setValue(testObject, bytesIn);
        assertEquals(0, testObject.stringArray.length, "reading stop-bit 0 should deserialize to empty array");
    }

    @Test
    void setValueWithNonEmptyArray() throws IllegalAccessException {
        // Simulate reading 2 for array size, then read strings
        when(bytesIn.readStopBit()).thenReturn(2L);
        when(bytesIn.readRemaining()).thenReturn(12L);
        when(bytesIn.readObject(String.class)).thenReturn("hello", "world");
        fieldAccess.setValue(testObject, bytesIn);
        assertArrayEquals(new String[]{"hello", "world"}, testObject.stringArray, "reading stop-bit 2 should deserialize to array with 2 elements");
    }

    @Test
    void testWriteAndReadMarshallable() {
        SampleObject original = new SampleObject();
        original.intValue = 42;
        original.stringValue = "Hello";
        original.doubleValue = 3.14;

        marshaller.writeMarshallable(original, bytes);
        bytes.readPosition(0); // Reset the read position to the start

        SampleObject result = new SampleObject();
        marshaller.readMarshallable(result, bytes);

        assertEquals(original.intValue, result.intValue, "marshalling should preserve int value");
        assertEquals(original.stringValue, result.stringValue, "marshalling should preserve string value");
        assertEquals(original.doubleValue, result.doubleValue, 0.001, "marshalling should preserve double value");
    }
}
