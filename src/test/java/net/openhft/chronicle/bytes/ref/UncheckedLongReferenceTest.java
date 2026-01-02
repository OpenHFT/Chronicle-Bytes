/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UncheckedLongReferenceTest extends BytesTestCommon {
    @Test
    @DisplayName("unchecked long reference updates backing store correctly")
    public void test() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull UncheckedLongReference ref = new UncheckedLongReference()) {
            ref.bytesStore(nbs, 16, 8);
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
            assertEquals(8,
                    ref.maxSize(),
                    "Reference should report the expected maximum size");
            assertEquals(nbs.addressForRead(16),
                    ref.offset(),
                    "Reference offset should match the configured address");
            assertEquals(nbs,
                    ref.bytesStore(),
                    "Reference should expose the backing bytes store");
            assertEquals(0L,
                    nbs.readLong(0),
                    "Prefix slot should remain zero at offset zero");
            assertEquals(0L,
                    nbs.readLong(8),
                    "Prefix slot should remain zero at offset eight");
            assertEquals(2L,
                    nbs.readLong(16),
                    "Stored value should appear at the reference offset");
            assertEquals(0L,
                    nbs.readLong(24),
                    "Trailing slot should remain zero at offset twenty-four");

            ref.setValue(10);
            assertEquals(10L,
                    nbs.readLong(16),
                    "setValue should update the backing store value");
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L,
                    nbs.readLong(16),
                    "setOrderedValue should update the backing store value");
        }
        nbs.releaseLast();
    }

    @Test
    @DisplayName("bytesStore rejects lengths that do not match a long")
    public void bytesStoreRejectsLengthMismatch() {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStoreWithFixedCapacity(32);
        try {
            try (@NotNull UncheckedLongReference ref = new UncheckedLongReference()) {
                assertThrows(IllegalArgumentException.class,
                        () -> ref.bytesStore(nbs, 0, 4),
                        "bytesStore should reject a length that is not eight bytes");
            }
        } finally {
            nbs.releaseLast();
        }
    }

    @Test
    @DisplayName("toString reports zero address before binding")
    public void toStringReportsZeroAddress() {
        try (@NotNull UncheckedLongReference ref = new UncheckedLongReference()) {
            assertEquals("addressForRead is 0",
                    ref.toString(),
                    "Uninitialised reference should report a zero address");
        }
    }
}
