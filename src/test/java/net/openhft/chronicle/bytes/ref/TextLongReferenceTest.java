/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextLongReferenceTest extends BytesTestCommon {

    @Test
    public void testSetValue() {
        @NotNull NativeBytesStore<Void> bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        bytesStore.zeroOut(0, 64);
        try (@NotNull final TextLongReference value = new TextLongReference()) {
            value.bytesStore(bytesStore, 0, value.maxSize());
            bytesStore.writeByte(value.maxSize(), 0);
            int expected = 10;
            value.setValue(expected);

            long l = bytesStore.parseLong(TextLongReference.VALUE);

            Assert.assertEquals(expected, value.getValue());
            Assert.assertEquals(expected, l);

            assertFalse(value.compareAndSwapValue(0, 1));
            assertTrue(value.compareAndSwapValue(10, 2));
            assertEquals(56, value.maxSize());
            assertEquals(0, value.offset());

            Bytes<Void> bytes = bytesStore.bytesForRead();
            bytes.readPosition(0);
            assertEquals("!!atomic {  locked: false, value: 00000000000000000002 }", bytes.parseUtf8(StopCharTesters.CONTROL_STOP));
            bytes.releaseLast();
        }
        bytesStore.releaseLast();
    }
}
