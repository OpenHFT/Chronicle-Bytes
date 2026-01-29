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

/**
 * Tests TextIntReference value updates and text formatting because correct
 * locking and alignment are essential to avoid concurrent modification
 * issues in text-based stores.
 */
@DisplayName("Text int reference update locking and alignment")
public class TextIntReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("text int reference updates value and formats output")
    public void test() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        nbs.zeroOut(0, 64);
        try (@NotNull TextIntReference ref = new TextIntReference()) {
            ref.bytesStore(nbs, 16, ref.maxSize());
            assertEquals(0,
                    ref.getValue(),
                    "Initial reference value should be zero");
            ref.addAtomicValue(1);
            assertEquals(1,
                    ref.getVolatileValue(),
                    "Atomic increment should update volatile value");
            ref.addValue(-2);
            assertEquals("value: -1",
                    ref.toString(),
                    "Reference string should reflect the current value");
            assertFalse(ref.compareAndSwapValue(0, 1),
                    "compareAndSwap should fail for unexpected value");
            assertTrue(ref.compareAndSwapValue(-1, 2),
                    "compareAndSwap should succeed for expected value");
            assertEquals(46,
                    ref.maxSize(),
                    "Text int reference should expose the expected max size");
            assertEquals(16,
                    ref.offset(),
                    "Reference offset should match the configured position");
            assertEquals(nbs,
                    ref.bytesStore(),
                    "Reference should expose the backing bytes store");
            assertEquals(0L,
                    nbs.readLong(0),
                    "Prefix slot should remain zero at offset zero");
            assertEquals(0L,
                    nbs.readLong(8),
                    "Prefix slot should remain zero at offset eight");
            Bytes<Void> bytes = nbs.bytesForRead();
            bytes.readPosition(16);
            assertEquals("!!atomic {  locked: false, value: 0000000002 }",
                    bytes.parseUtf8(StopCharTesters.CONTROL_STOP),
                    "Text formatting should reflect the stored value");
            bytes.releaseLast();
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("text int reference rejects mismatched lengths")
    public void bytesStoreRejectsLengthMismatch() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        try (@NotNull TextIntReference ref = new TextIntReference()) {
            assertThrows(IllegalArgumentException.class,
                    () -> ref.bytesStore(nbs, 0, ref.maxSize() - 1),
                    "bytesStore should reject lengths that differ from the template size");
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("text int reference aligns offsets and initialises template")
    public void bytesStoreAlignsAndInitialises() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(128);
        nbs.zeroOut(0, 128);
        try (@NotNull TextIntReference ref = new TextIntReference()) {
            ref.bytesStore(nbs, 1, ref.maxSize());
            assertEquals(8,
                    ref.offset(),
                    "Offset should be aligned to the next 8-byte boundary");
            for (int i = 1; i < 8; i++) {
                assertEquals(' ',
                        nbs.readByte(i),
                        "Alignment should pad bytes with spaces at index " + i);
            }
            Bytes<Void> bytes = nbs.bytesForRead();
            bytes.readPosition(ref.offset());
            assertEquals("!!atomic {  locked: false, value: 0000000000 }",
                    bytes.parseUtf8(StopCharTesters.CONTROL_STOP),
                    "Template should be written for uninitialised references");
            bytes.releaseLast();
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("text int reference rejects invalid lock state")
    public void getValueRejectsInvalidLock() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        nbs.zeroOut(0, 64);
        try (@NotNull TextIntReference ref = new TextIntReference()) {
            ref.bytesStore(nbs, 0, ref.maxSize());
            nbs.writeInt(20, 0x12345678);
            assertThrows(IllegalStateException.class,
                    ref::getValue,
                    "Invalid lock bytes should be rejected");
        }
        nbs.releaseLast();
    }
}
