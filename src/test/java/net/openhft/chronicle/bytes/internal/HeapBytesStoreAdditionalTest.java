/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for HeapBytesStore additional branches, because copy, move, and
 * atomic operations require explicit boundary validation to avoid memory corruption.
 */
@DisplayName("Heap bytes store additional branch coverage")
public class HeapBytesStoreAdditionalTest {

    @Test
    @DisplayName("HeapBytesStore wrap rejects null byte array input")
    public void wrapRejectsNullArray() {
        assertThrows(NullPointerException.class,
                () -> HeapBytesStore.wrap((byte[]) null),
                "HeapBytesStore.wrap should reject null arrays");
    }

    @Test
    @DisplayName("copy returns a no-bytes store for zero capacity")
    public void copyReturnsNoBytesStoreForZeroCapacity() {
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(new byte[0]);
        try {
            Object copy = store.copy();
            assertTrue(copy instanceof NoBytesStore,
                    "Zero-capacity copy should return the no-bytes store");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("copy throws for non-zero capacity stores")
    public void copyThrowsForNonZeroCapacity() {
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(new byte[4]);
        try {
            assertThrows(UnsupportedOperationException.class,
                    store::copy,
                    "Non-zero capacity copy should be unsupported");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("move copies bytes within the backing array")
    public void moveCopiesBytes() {
        byte[] data = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.move(0, 4, 4);
            assertArrayEquals(new byte[] {1, 2, 3, 4, 1, 2, 3, 4},
                    data,
                    "Move should copy the requested range within the array");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("move rejects invalid offsets and lengths")
    public void moveRejectsInvalidInputs() {
        byte[] data = new byte[] {1, 2, 3, 4};
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> store.move(-1, 0, 1),
                    "Move should reject negative source offsets");
            assertThrows(IllegalArgumentException.class,
                    () -> store.move(0, -1, 1),
                    "Move should reject negative destination offsets");
            assertThrows(IllegalArgumentException.class,
                    () -> store.move(0, 0, -1),
                    "Move should reject negative lengths");
            assertThrows(IllegalArgumentException.class,
                    () -> store.move(0, 0, (long) Integer.MAX_VALUE + 1),
                    "Move should reject lengths that do not fit in an int");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSwapInt updates stored value when expected matches")
    public void compareAndSwapIntUpdatesValue() {
        byte[] data = new byte[8];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeInt(0, 7);
            assertTrue(store.compareAndSwapInt(0, 7, 9),
                    "compareAndSwapInt should succeed when expected matches");
            assertEquals(9,
                    store.readInt(0),
                    "compareAndSwapInt should update the stored value");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addressForRead and addressForWrite reject out-of-range offsets")
    public void addressForReadWriteRejectsOutOfRange() {
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(new byte[4]);
        try {
            assertThrows(BufferUnderflowException.class,
                    () -> store.addressForRead(-1),
                    "addressForRead should reject offsets before the start");
            assertThrows(BufferOverflowException.class,
                    () -> store.addressForRead(4),
                    "addressForRead should reject offsets beyond capacity");
            assertThrows(BufferUnderflowException.class,
                    () -> store.addressForWrite(-1),
                    "addressForWrite should reject offsets before the start");
            assertThrows(BufferOverflowException.class,
                    () -> store.addressForWrite(4),
                    "addressForWrite should reject offsets beyond capacity");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeRead and nativeWrite reject invalid sizes")
    public void nativeReadWriteRejectInvalidSizes() {
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(new byte[8]);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> store.nativeRead(0, 0, -1),
                    "nativeRead should reject negative sizes");
            assertThrows(UnsupportedOperationException.class,
                    () -> store.nativeRead(0, 0, 1),
                    "nativeRead should reject non-zero sizes");
            assertThrows(IllegalArgumentException.class,
                    () -> store.nativeWrite(0, 0, -1),
                    "nativeWrite should reject negative sizes");
            assertThrows(UnsupportedOperationException.class,
                    () -> store.nativeWrite(0, 0, 1),
                    "nativeWrite should reject non-zero sizes");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("write copies bytes from direct and heap buffers")
    public void writeCopiesFromByteBuffers() {
        byte[] data = new byte[6];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            ByteBuffer direct = ByteBuffer.allocateDirect(3);
            direct.put(new byte[] {1, 2, 3});
            store.write(0, direct, 0, 3);

            ByteBuffer heap = ByteBuffer.wrap(new byte[] {4, 5, 6});
            store.write(3, heap, 0, 3);
            assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6},
                    data,
                    "write should copy bytes from direct and heap ByteBuffers");
        } finally {
            store.releaseLast();
        }
    }
}
