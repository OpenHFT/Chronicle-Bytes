/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextLongReferenceTest extends BytesTestCommon {

    @Test
    @DisplayName("text long reference updates value and string format")
    public void testSetValue() {
        @NotNull NativeBytesStore<Void> bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        bytesStore.zeroOut(0, 64);
        try (@NotNull final TextLongReference value = new TextLongReference()) {
            value.bytesStore(bytesStore, 0, value.maxSize());
            bytesStore.writeByte(value.maxSize(), 0);
            int expected = 10;
            value.setValue(expected);

            long l = bytesStore.parseLong(TextLongReference.VALUE);

            assertEquals(expected,
                    value.getValue(),
                    "Reference should return the last set value");
            assertEquals(expected,
                    l,
                    "Parsed long should reflect the stored value");

            assertFalse(value.compareAndSwapValue(0, 1),
                    "compareAndSwap should fail for unexpected value");
            assertTrue(value.compareAndSwapValue(10, 2),
                    "compareAndSwap should succeed for expected value");
            assertEquals(56,
                    value.maxSize(),
                    "Text long reference should expose the expected max size");
            assertEquals(0,
                    value.offset(),
                    "Reference should remain at the configured offset");

            Bytes<Void> bytes = bytesStore.bytesForRead();
            bytes.readPosition(0);
            assertEquals("!!atomic {  locked: false, value: 00000000000000000002 }",
                    bytes.parseUtf8(StopCharTesters.CONTROL_STOP),
                    "Text formatting should reflect the stored value");
            bytes.releaseLast();
        }
        bytesStore.releaseLast();
    }

    @Test
    @DisplayName("text long reference rejects mismatched lengths")
    public void bytesStoreRejectsLengthMismatch() {
        @NotNull NativeBytesStore<Void> bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        try (@NotNull TextLongReference value = new TextLongReference()) {
            assertThrows(IllegalArgumentException.class,
                    () -> value.bytesStore(bytesStore, 0, value.maxSize() - 1),
                    "bytesStore should reject lengths that differ from the template size");
        }
        bytesStore.releaseLast();
    }

    @Test
    @DisplayName("text long reference aligns offsets and initialises template")
    public void bytesStoreAlignsAndInitialises() {
        @NotNull NativeBytesStore<Void> bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(128);
        bytesStore.zeroOut(0, 128);
        try (@NotNull TextLongReference value = new TextLongReference()) {
            value.bytesStore(bytesStore, 1, value.maxSize());
            assertEquals(8,
                    value.offset(),
                    "Offset should be aligned to the next 8-byte boundary");
            for (int i = 1; i < 8; i++) {
                assertEquals(' ',
                        bytesStore.readByte(i),
                        "Alignment should pad bytes with spaces");
            }
            Bytes<Void> bytes = bytesStore.bytesForRead();
            bytes.readPosition(value.offset());
            assertTrue(bytes.parseUtf8(StopCharTesters.CONTROL_STOP).startsWith("!!atomic"),
                    "Template should be written for uninitialised references");
            bytes.releaseLast();
        }
        bytesStore.releaseLast();
    }

    @Test
    @DisplayName("text long reference addValue updates stored value")
    public void addValueUpdatesStoredValue() {
        @NotNull NativeBytesStore<Void> bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        bytesStore.zeroOut(0, 64);
        try (@NotNull TextLongReference value = new TextLongReference()) {
            value.bytesStore(bytesStore, 0, value.maxSize());
            value.setValue(3);
            assertEquals(7,
                    value.addValue(4),
                    "addValue should return the updated total");
            assertEquals(7,
                    value.getValue(),
                    "addValue should persist the updated total");
        }
        bytesStore.releaseLast();
    }

    @Test
    @DisplayName("text long reference rejects invalid lock state")
    public void getValueRejectsInvalidLock() {
        @NotNull NativeBytesStore<Void> bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        bytesStore.zeroOut(0, 64);
        try (@NotNull TextLongReference value = new TextLongReference()) {
            value.bytesStore(bytesStore, 0, value.maxSize());
            bytesStore.writeInt(20, 0x12345678);
            assertThrows(IllegalStateException.class,
                    value::getValue,
                    "Invalid lock bytes should be rejected");
        }
        bytesStore.releaseLast();
    }
}
