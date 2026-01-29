/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests BytesStore default methods for boundary checks and comparisons because correct
 * behaviour of inside, zeroOut, byteCheckSum, and equalBytes is required for data safety.
 */
@SuppressWarnings("deprecation")
@DisplayName("BytesStore default methods cover boundary and comparison scenarios")
public class BytesStoreAdditionalTest extends BytesTestCommon {

    @Test
    @DisplayName("inside reports offsets within the safe limits")
    public void insideReportsSafeOffsets() {
        BytesStore<?, ?> store = BytesStore.wrap(new byte[8]);
        try {
            long start = store.start();
            long safeLimit = store.safeLimit();
            assertTrue(store.inside(start),
                    "Start offset should be inside the store");
            assertTrue(store.inside(safeLimit - 1),
                    "Offset just before the safe limit should be inside the store");
            assertFalse(store.inside(safeLimit),
                    "Offset at the safe limit should be outside the store");
            assertTrue(store.inside(start, safeLimit - start),
                    "Range covering the store should be inside");
            assertFalse(store.inside(safeLimit, 1),
                    "Range starting at the safe limit should be outside");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("zeroOut clamps ranges and clears bytes")
    public void zeroOutClampsAndClears() {
        BytesStore<?, ?> store = BytesStore.wrap("abcd".getBytes(ISO_8859_1));
        try {
            store.zeroOut(0, store.capacity() + 4);
            assertEquals(0,
                    store.readByte(0),
                    "zeroOut should clear the first byte");
            assertEquals(0,
                    store.readByte(3),
                    "zeroOut should clear the last byte within capacity");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("byte checksum sums the readable bytes")
    public void byteCheckSumCountsBytes() {
        BytesStore<?, ?> store = BytesStore.wrap(new byte[] {1, 2, 3});
        try {
            assertEquals(6,
                    store.byteCheckSum(),
                    "byteCheckSum should sum the bytes in the store");
            assertEquals(3,
                    store.byteCheckSum(0, 2),
                    "byteCheckSum over a range should sum the selected bytes");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("startsWith and endsWith evaluate byte boundaries")
    public void startsWithAndEndsWith() {
        BytesStore<?, ?> store = BytesStore.wrap("abc".getBytes(ISO_8859_1));
        try {
            assertTrue(store.startsWith('a'),
                    "store '" + store + "' should start with 'a'");
            assertTrue(store.endsWith('c'),
                    "store '" + store + "' should end with 'c'");
            assertFalse(store.startsWith('b'),
                    "store '" + store + "' should not start with 'b'");
            assertFalse(store.endsWith('b'),
                    "store '" + store + "' should not end with 'b'");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("equalBytes compares the eight-byte fast path")
    public void equalBytesUsesFastPath() {
        BytesStore<?, ?> first = BytesStore.wrap(new byte[8]);
        BytesStore<?, ?> second = BytesStore.wrap(new byte[8]);
        try {
            first.writeLong(0, 0x0102030405060708L);
            second.writeLong(0, 0x0102030405060708L);
            assertTrue(first.equalBytes(second, 8),
                    "equalBytes should report true for equal eight-byte values");
            second.writeLong(0, 0x0102030405060709L);
            assertFalse(first.equalBytes(second, 8),
                    "equalBytes should report false for differing eight-byte values");
        } finally {
            first.releaseLast();
            second.releaseLast();
        }
    }

    @Test
    @DisplayName("addAndGet and writeMax helpers update values")
    public void addAndGetAndWriteMax() {
        BytesStore<?, ?> store = BytesStore.wrap(new byte[32]);
        try {
            store.writeByte(0, (byte) 250);
            assertEquals(4,
                    store.addAndGetUnsignedByteNotAtomic(0, 10),
                    "Unsigned byte add should wrap modulo 256");

            store.writeShort(2, (short) 1);
            assertEquals(2,
                    store.addAndGetShortNotAtomic(2, (short) 1),
                    "Short add should return the updated value");
            assertEquals(2,
                    store.readShort(2),
                    "Short add should update the stored value");

            store.writeInt(4, 1);
            assertEquals(3,
                    store.addAndGetIntNotAtomic(4, 2),
                    "Int add should return the updated value");

            store.writeFloat(8, 1.5f);
            assertEquals(2.0f,
                    store.addAndGetFloatNotAtomic(8, 0.5f),
                    0.0f,
                    "Float add should return the updated value");

            store.writeDouble(16, 1.25d);
            assertEquals(2.25d,
                    store.addAndGetDoubleNotAtomic(16, 1.0d),
                    0.0d,
                    "Double add should return the updated value");

            store.writeLong(24, 1L);
            store.writeMaxLong(24, 0L);
            store.writeMaxLong(24, 5L);
            assertEquals(5L,
                    store.readLong(24),
                    "writeMaxLong should retain the maximum value");

            store.writeInt(28, 1);
            store.writeMaxInt(28, 5);
            assertEquals(5,
                    store.readInt(28),
                    "writeMaxInt should retain the maximum value");
        } finally {
            store.releaseLast();
        }
    }
}
