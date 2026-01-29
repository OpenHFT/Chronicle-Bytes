/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests BytesMarshaller read and write operations for primitive and array fields because
 * correct round-trip serialisation is required to avoid data loss during persistence.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("BytesMarshaller - read and write operations for primitive and array fields")
class BytesMarshallerTest {

    static class TestClass {
        @SuppressWarnings("WeakerAccess")
        @UsedViaReflection
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
    @DisplayName("object array access writes zero length for empty arrays")
    void getValueWithEmptyArray() throws IllegalAccessException {
        // Empty array
        testObject.stringArray = new String[0];
        fieldAccess.getValue(testObject, bytesOut);
        verify(bytesOut, times(1)).writeStopBit(0);
    }

    @Test
    @DisplayName("object array access writes length for non-empty arrays")
    void getValueWithNonEmptyArray() throws IllegalAccessException {
        // Non-empty array
        testObject.stringArray = new String[]{"hello", "world"};
        fieldAccess.getValue(testObject, bytesOut);
        verify(bytesOut, times(1)).writeStopBit(2);
    }

    @Test
    @DisplayName("null array value is restored when stop bit is -1")
    void setValueWithNullArray() throws IllegalAccessException {
        // Simulate reading -1 for null array
        when(bytesIn.readStopBit()).thenReturn(-1L);
        fieldAccess.setValue(testObject, bytesIn);
        assertNull(testObject.stringArray,
                "Reading -1 should restore a null array value");
    }

    @Test
    @DisplayName("empty array value is restored when stop bit is zero")
    void setValueWithEmptyArray() throws IllegalAccessException {
        // Simulate reading 0 for empty array
        when(bytesIn.readStopBit()).thenReturn(0L);
        fieldAccess.setValue(testObject, bytesIn);
        assertEquals(0,
                testObject.stringArray.length,
                "Reading zero should restore an empty array");
    }

    @Test
    @DisplayName("array values are restored when stop bit is positive")
    void setValueWithNonEmptyArray() throws IllegalAccessException {
        // Simulate reading 2 for array size, then read strings
        when(bytesIn.readStopBit()).thenReturn(2L);
        when(bytesIn.readRemaining()).thenReturn(12L);
        when(bytesIn.readObject(String.class)).thenReturn("hello", "world");
        fieldAccess.setValue(testObject, bytesIn);
        assertArrayEquals(new String[]{"hello", "world"},
                testObject.stringArray,
                "Reading array values should restore the expected strings");
    }

    @Test
    @DisplayName("marshaller round trip restores all primitive fields")
    void testWriteAndReadMarshallable() {
        TestObject original = new TestObject();
        original.intValue = 42;
        original.stringValue = "Hello";
        original.doubleValue = 3.14;

        try {
            marshaller.writeMarshallable(original, bytes);
            bytes.readPosition(0); // Reset the read position to the start

            TestObject result = new TestObject();
            marshaller.readMarshallable(result, bytes);

            assertEquals(original.intValue,
                    result.intValue,
                    "Round trip should preserve the int value");
            assertEquals(original.stringValue,
                    result.stringValue,
                    "Round trip should preserve the string value");
            assertEquals(original.doubleValue,
                    result.doubleValue,
                    0.001,
                    "Round trip should preserve the double value");
        } finally {
            bytes.releaseLast();
        }
    }
}
