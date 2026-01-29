/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HeapBytesStore branch coverage, because atomic operations
 * and boundary checks must be validated to avoid data corruption.
 */
@DisplayName("HeapBytesStore branch coverage for wrapping, atomics, and boundaries")
class HeapBytesStoreBranchTest extends BytesTestCommon {

    @Test
    @DisplayName("wrap(byte[]) should create store backed by array")
    void wrapByteArrayCreatesStore() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertEquals(16, store.capacity(), "capacity should match array length");
            assertFalse(store.isDirectMemory(),
                    "array-backed heap store should not be direct memory");
            assertSame(data, store.underlyingObject(), "underlying object should be the array");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("wrap(byte[]) should throw on null array to prevent NullPointerException later")
    void wrapNullArrayThrows() {
        assertThrows(NullPointerException.class, () -> HeapBytesStore.wrap((byte[]) null),
                "wrapping null byte array should throw NPE to prevent deferred failure");
    }

    @Test
    @DisplayName("wrap(ByteBuffer) should create store backed by heap buffer")
    void wrapByteBufferCreatesStore() {
        ByteBuffer bb = ByteBuffer.allocate(32);
        HeapBytesStore<ByteBuffer> store = HeapBytesStore.wrap(bb);
        try {
            assertEquals(32, store.capacity(), "capacity should match buffer capacity");
            assertFalse(store.isDirectMemory(),
                    "heap ByteBuffer store should not be direct memory");
            assertSame(bb, store.underlyingObject(), "underlying object should be the ByteBuffer");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("move should copy data from source to target offset within same store")
    void moveCopiesDataWithinStore() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeByte(0, (byte) 0x11);
            store.writeByte(1, (byte) 0x22);
            store.writeByte(2, (byte) 0x33);
            store.move(0, 8, 3);

            assertEquals((byte) 0x11, store.readByte(8), "byte at offset 8 should be 0x11 after move from offset 0");
            assertEquals((byte) 0x22, store.readByte(9), "byte at offset 9 should be 0x22 after move from offset 1");
            assertEquals((byte) 0x33, store.readByte(10), "byte at offset 10 should be 0x33 after move from offset 2");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("move with negative from offset should throw to prevent invalid memory access")
    void moveNegativeFromThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(IllegalArgumentException.class, () -> store.move(-1, 0, 1),
                    "move with from=-1 should throw because negative offsets are invalid");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("move with negative to offset should throw to prevent invalid memory access")
    void moveNegativeToThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(IllegalArgumentException.class, () -> store.move(0, -1, 1),
                    "move with to=-1 should throw because negative offsets are invalid");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("move with negative length should throw to prevent invalid memory access")
    void moveNegativeLengthThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(IllegalArgumentException.class, () -> store.move(0, 1, -1),
                    "move with length=-1 should throw because negative lengths are invalid");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("copy of zero capacity store should return NoBytesStore")
    void copyZeroCapacityReturnsNoBytesStore() {
        byte[] data = new byte[0];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            BytesStore<?, ?> copy = store.copy();
            assertSame(NoBytesStore.NO_BYTES_STORE, copy, "zero capacity copy should be NoBytesStore");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("copy of non-zero capacity store should throw UnsupportedOperationException")
    void copyNonZeroCapacityThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(UnsupportedOperationException.class, () -> store.copy(),
                    "copy of non-empty store is not supported");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSwapInt should atomically update stored int value")
    void compareAndSwapIntUpdatesAtomically() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeInt(0, 100);
            assertTrue(store.compareAndSwapInt(0, 100, 200),
                    "compareAndSwapInt should succeed when stored int equals 100");
            assertEquals(200, store.readInt(0),
                    "stored int should update to 200 after CAS success");

            assertFalse(store.compareAndSwapInt(0, 100, 300),
                    "compareAndSwapInt should fail when expected int is 100 but stored is 200");
            assertEquals(200, store.readInt(0),
                    "stored int should remain 200 after failed CAS");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSwapLong should atomically update stored long value")
    void compareAndSwapLongUpdatesAtomically() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeLong(0, 100L);
            assertTrue(store.compareAndSwapLong(0, 100L, 200L),
                    "compareAndSwapLong should succeed when stored long equals 100L");
            assertEquals(200L, store.readLong(0),
                    "stored long should update to 200L after CAS success");

            assertFalse(store.compareAndSwapLong(0, 100L, 300L),
                    "compareAndSwapLong should fail when expected long is 100L but stored is 200L");
            assertEquals(200L, store.readLong(0),
                    "stored long should remain 200L after failed CAS");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("testAndSetInt should update stored int when expected value matches")
    void testAndSetIntSetsValue() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeInt(0, 100);
            store.testAndSetInt(0, 100, 200);
            assertEquals(200, store.readInt(0),
                    "stored int should update to 200 after testAndSetInt");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("testAndSetInt should throw when expected value 50 does not match stored value 100")
    void testAndSetIntThrowsOnMismatch() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeInt(0, 100);
            assertThrows(IllegalStateException.class, () -> store.testAndSetInt(0, 50, 200),
                    "testAndSetInt should throw because expected=50 does not match stored=100");
        } finally {
            store.releaseLast();
        }
    }

    @ParameterizedTest
    @ValueSource(bytes = {0, 1, -1, Byte.MAX_VALUE, Byte.MIN_VALUE})
    @DisplayName("readByte and writeByte should handle all byte values")
    void readWriteByteHandlesAllValues(byte value) {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeByte(0, value);
            assertEquals(value, store.readByte(0),
                    "readByte should return " + value + " after writeByte");
        } finally {
            store.releaseLast();
        }
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 1, -1, Short.MAX_VALUE, Short.MIN_VALUE})
    @DisplayName("readShort and writeShort should handle all short values")
    void readWriteShortHandlesAllValues(short value) {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeShort(0, value);
            assertEquals(value, store.readShort(0),
                    "readShort should return " + value + " after writeShort");
        } finally {
            store.releaseLast();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("readInt and writeInt should handle all int values")
    void readWriteIntHandlesAllValues(int value) {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeInt(0, value);
            assertEquals(value, store.readInt(0),
                    "readInt should return " + value + " after writeInt");
        } finally {
            store.releaseLast();
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE})
    @DisplayName("readLong and writeLong should handle all long values")
    void readWriteLongHandlesAllValues(long value) {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeLong(0, value);
            assertEquals(value, store.readLong(0),
                    "readLong should return " + value + " after writeLong");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("readFloat and writeFloat should handle float values")
    void readWriteFloatHandlesValues() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeFloat(0, 3.14f);
            assertEquals(3.14f, store.readFloat(0), 0.0001f,
                    "readFloat should return 3.14 after writeFloat");

            store.writeFloat(4, Float.NaN);
            assertTrue(Float.isNaN(store.readFloat(4)),
                    "Float.NaN should be preserved by readFloat");

            store.writeFloat(8, Float.POSITIVE_INFINITY);
            assertEquals(Float.POSITIVE_INFINITY, store.readFloat(8),
                    "positive infinity should be preserved by readFloat");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("readDouble and writeDouble should handle double values")
    void readWriteDoubleHandlesValues() {
        byte[] data = new byte[32];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeDouble(0, 3.14159265359);
            assertEquals(3.14159265359, store.readDouble(0), 0.0000001,
                    "readDouble should return 3.14159265359 after writeDouble");

            store.writeDouble(8, Double.NaN);
            assertTrue(Double.isNaN(store.readDouble(8)),
                    "Double.NaN should be preserved by readDouble");

            store.writeDouble(16, Double.NEGATIVE_INFINITY);
            assertEquals(Double.NEGATIVE_INFINITY, store.readDouble(16),
                    "negative infinity should be preserved by readDouble");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("volatile reads and writes should be consistent")
    void volatileReadsWritesConsistent() {
        byte[] data = new byte[32];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeVolatileByte(0, (byte) 0x42);
            assertEquals((byte) 0x42, store.readVolatileByte(0), "volatile byte should match");

            store.writeVolatileShort(2, (short) 0x1234);
            assertEquals((short) 0x1234, store.readVolatileShort(2), "volatile short should match");

            store.writeVolatileInt(4, 0x12345678);
            assertEquals(0x12345678, store.readVolatileInt(4), "volatile int should match");

            store.writeVolatileLong(8, 0x123456789ABCDEF0L);
            assertEquals(0x123456789ABCDEF0L, store.readVolatileLong(8), "volatile long should match");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("writeOrderedInt should make ordered int visible at offset")
    void writeOrderedIntWritesValue() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeOrderedInt(0, 0xDEADBEEF);
            assertEquals(0xDEADBEEF, store.readInt(0), "ordered int write should be readable");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("writeOrderedLong should make ordered long visible at offset")
    void writeOrderedLongWritesValue() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            store.writeOrderedLong(0, 0xDEADBEEFCAFEBABEL);
            assertEquals(0xDEADBEEFCAFEBABEL, store.readLong(0), "ordered long write should be readable");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("read(offset, byte[], ...) should copy data to array")
    void readToByteArrayCopiesData() {
        byte[] source = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(source);
        try {
            byte[] target = new byte[4];
            long read = store.read(2, target, 0, 4);
            assertEquals(4, read, "read(offset, ...) should return 4 bytes copied from position 2");
            assertArrayEquals(new byte[]{3, 4, 5, 6}, target, "target array should contain bytes {3,4,5,6} from source offset 2");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("write(offset, byte[], ...) should copy data from array")
    void writeFromByteArrayCopiesData() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            byte[] source = new byte[]{0x10, 0x20, 0x30, 0x40};
            store.write(4, source, 0, 4);

            assertEquals((byte) 0x10, store.readByte(4), "array source byte[0] should match 0x10");
            assertEquals((byte) 0x20, store.readByte(5), "array source byte[1] should match 0x20");
            assertEquals((byte) 0x30, store.readByte(6), "array source byte[2] should match 0x30");
            assertEquals((byte) 0x40, store.readByte(7), "array source byte[3] should match 0x40");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("write(offset, ByteBuffer heap, ...) should copy from heap buffer")
    void writeFromHeapByteBufferCopiesData() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            ByteBuffer source = ByteBuffer.allocate(8);
            source.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
            store.write(0, source, 0, 8);

            assertEquals((byte) 1, store.readByte(0), "heap ByteBuffer first byte should match 1");
            assertEquals((byte) 8, store.readByte(7), "heap ByteBuffer last byte should match 8");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("write(offset, ByteBuffer direct, ...) should copy from direct buffer")
    void writeFromDirectByteBufferCopiesData() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            ByteBuffer source = ByteBuffer.allocateDirect(8);
            source.put(new byte[]{11, 12, 13, 14, 15, 16, 17, 18});
            store.write(0, source, 0, 8);

            assertEquals((byte) 11, store.readByte(0), "direct ByteBuffer first byte should match 11");
            assertEquals((byte) 18, store.readByte(7), "direct ByteBuffer last byte should match 18");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("write(offset, RandomDataInput, ...) with int length should copy")
    void writeFromRandomDataInputIntLength() {
        byte[] data = new byte[32];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            Bytes<?> source = Bytes.allocateElasticOnHeap(16);
            source.writeLong(0x1122334455667788L);
            source.writeLong(0xAABBCCDDEEFF0011L);

            store.write(0, source, 0, 16);
            assertEquals(0x1122334455667788L, store.readLong(0), "first long at offset 0 should be 0x1122334455667788L after write");
            assertEquals(0xAABBCCDDEEFF0011L, store.readLong(8), "second long at offset 8 should be 0xAABBCCDDEEFF0011L after write");
            source.releaseLast();
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("write8bit should write length-prefixed string")
    void write8bitWritesLengthPrefixedString() {
        byte[] data = new byte[32];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            long endPos = store.write8bit(0, "Hello", 0, 5);
            assertTrue(endPos > 0,
                    "end position " + endPos + " should be > 0 after write8bit string");

            // Read back using Bytes
            Bytes<?> bytes = Bytes.wrapForRead(data);
            String readBack = bytes.read8bit();
            assertEquals("Hello", readBack, "read string should match written string");
            bytes.releaseLast();
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("write8bit with BytesStore should write length-prefixed data")
    void write8bitWithBytesStoreWritesLengthPrefixedData() {
        byte[] data = new byte[32];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            Bytes<?> source = Bytes.allocateElasticOnHeap(8);
            source.write(new byte[]{0x41, 0x42, 0x43}); // "ABC"

            long endPos = store.write8bit(0, source);
            assertTrue(endPos > 0,
                    "end position " + endPos + " should be > 0 after write8bit BytesStore");

            // Verify length prefix and content
            Bytes<?> readBytes = Bytes.wrapForRead(data);
            String readBack = readBytes.read8bit();
            assertEquals("ABC", readBack, "read string should be ABC");
            readBytes.releaseLast();
            source.releaseLast();
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addressForRead should throw UnsupportedOperationException on heap store")
    void addressForReadThrowsUnsupported() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(UnsupportedOperationException.class, () -> store.addressForRead(0),
                    "heap store does not support address access");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addressForRead with offset before start should throw BufferUnderflowException")
    void addressForReadBeforeStartThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(BufferUnderflowException.class, () -> store.addressForRead(-1),
                    "addressForRead should throw underflow for negative offset");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addressForRead beyond capacity should throw BufferOverflowException")
    void addressForReadBeyondCapacityThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(BufferOverflowException.class, () -> store.addressForRead(100),
                    "offset beyond capacity should throw overflow");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addressForWrite should throw UnsupportedOperationException on heap store")
    void addressForWriteThrowsUnsupported() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(UnsupportedOperationException.class, () -> store.addressForWrite(0),
                    "heap store does not support address access");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addressForWrite with offset before start should throw BufferUnderflowException")
    void addressForWriteBeforeStartThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(BufferUnderflowException.class, () -> store.addressForWrite(-1),
                    "addressForWrite should throw underflow for negative offset");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addressForWritePosition should throw UnsupportedOperationException on heap store")
    void addressForWritePositionThrowsUnsupported() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(UnsupportedOperationException.class, () -> store.addressForWritePosition(),
                    "heap store does not support address access");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeRead with zero size should succeed")
    void nativeReadZeroSizeSucceeds() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            // Zero size should not throw
            store.nativeRead(0, 0, 0);
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeRead with position before start should throw underflow exception")
    void nativeReadBeforeStartThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(BufferUnderflowException.class, () -> store.nativeRead(-1, 0, 1),
                    "nativeRead with position=-1 should throw underflow because position is before start");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeRead exceeding readLimit should throw BufferOverflowException")
    void nativeReadExceedingLimitThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(BufferOverflowException.class, () -> store.nativeRead(0, 0, 100),
                    "nativeRead should throw when size exceeds readLimit");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeRead with negative size should throw illegal argument exception")
    void nativeReadNegativeSizeThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(IllegalArgumentException.class, () -> store.nativeRead(0, 0, -1),
                    "nativeRead with size=-1 should throw because negative sizes are invalid");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeRead with positive size should throw UnsupportedOperationException")
    void nativeReadPositiveSizeThrowsUnsupported() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(UnsupportedOperationException.class, () -> store.nativeRead(0, 0, 1),
                    "nativeRead should throw unsupported for positive size");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeWrite with zero size should succeed")
    void nativeWriteZeroSizeSucceeds() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            // Zero size should not throw
            store.nativeWrite(0, 0, 0);
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeWrite with position before start should throw underflow exception")
    void nativeWriteBeforeStartThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(BufferUnderflowException.class, () -> store.nativeWrite(0, -1, 1),
                    "nativeWrite with position=-1 should throw underflow because position is before start");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeWrite exceeding writeLimit should throw BufferOverflowException")
    void nativeWriteExceedingLimitThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(BufferOverflowException.class, () -> store.nativeWrite(0, 0, 100),
                    "nativeWrite should throw when size exceeds writeLimit");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeWrite with negative size should throw illegal argument exception")
    void nativeWriteNegativeSizeThrows() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(IllegalArgumentException.class, () -> store.nativeWrite(0, 0, -1),
                    "nativeWrite with size=-1 should throw because negative sizes are invalid");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeWrite with positive size should throw UnsupportedOperationException")
    void nativeWritePositiveSizeThrowsUnsupported() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertThrows(UnsupportedOperationException.class, () -> store.nativeWrite(0, 0, 1),
                    "nativeWrite should throw unsupported for positive size");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("sharedMemory should return false for heap store allocation")
    void sharedMemoryReturnsFalse() {
        byte[] data = new byte[16];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            assertFalse(store.sharedMemory(), "heap store is not shared memory");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("toString should return non-empty string representation for debugging")
    void toStringReturnsRepresentation() {
        byte[] data = new byte[]{0x48, 0x65, 0x6C, 0x6C, 0x6F}; // "Hello"
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            String str = store.toString();
            assertNotNull(str, "toString should return non-null string for 5-byte store containing 'Hello'");
            assertFalse(str.isEmpty(), "toString should return non-empty string for 5-byte store containing 'Hello'");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("appendAndReturnLength should format decimal number with exponent")
    void appendAndReturnLengthFormatsDecimals() {
        byte[] data = new byte[32];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            // Test positive number with positive exponent
            long len = store.appendAndReturnLength(0, false, 12345, 2, false);
            assertTrue(len > 0, "length " + len + " should be > 0 for formatted decimal");

            // Read back the formatted number
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                sb.append((char) store.readByte(i));
            }
            assertTrue(sb.toString().contains("123"),
                    "formatted number should contain digits for positive exponent");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("appendAndReturnLength should format negative numbers with sign")
    void appendAndReturnLengthHandlesNegative() {
        byte[] data = new byte[32];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            long len = store.appendAndReturnLength(0, true, 100, 0, false);
            assertTrue(len > 0, "length " + len + " should be > 0 for negative value");

            // Read back and verify negative sign
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                sb.append((char) store.readByte(i));
            }
            assertTrue(sb.toString().contains("-"),
                    "formatted number should contain minus sign for negative value");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("appendAndReturnLength should handle zero exponent when append0 is true")
    void appendAndReturnLengthHandlesZeroExponentWithAppend0() {
        byte[] data = new byte[32];
        HeapBytesStore<byte[]> store = HeapBytesStore.wrap(data);
        try {
            long len = store.appendAndReturnLength(0, false, 5, -1, true);
            assertTrue(len > 0, "length " + len + " should be > 0 for append0 path");

            // Read back the formatted result
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                sb.append((char) store.readByte(i));
            }
            // The result should contain '0' and '.' (format depends on exponent/mantissa)
            String result = sb.toString();
            assertTrue(result.contains("0") || result.contains("."),
                    "formatted number should contain digits or decimal point for append0");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("equals and hashCode should be consistent for store instances")
    void equalsAndHashCodeWork() {
        byte[] data1 = new byte[]{1, 2, 3, 4};
        byte[] data2 = new byte[]{5, 6, 7, 8};
        HeapBytesStore<byte[]> store1 = HeapBytesStore.wrap(data1);
        HeapBytesStore<byte[]> store2 = HeapBytesStore.wrap(data2);
        try {
            // Same instance should be equal to itself
            assertEquals(store1, store1, "store should equal itself");

            // Different content should not be equal
            assertNotEquals(store1, store2, "stores with different content should not be equal");

            // hashCode should be consistent
            int hash1 = store1.hashCode();
            int hash2 = store1.hashCode();
            assertEquals(hash1, hash2, "hashCode should remain consistent for same store");
        } finally {
            store1.releaseLast();
            store2.releaseLast();
        }
    }

    @Test
    @DisplayName("ByteBuffer wrapped store should use native byte order")
    void byteBufferStoreUsesNativeOrder() {
        ByteBuffer bb = ByteBuffer.allocate(8);
        HeapBytesStore<ByteBuffer> store = HeapBytesStore.wrap(bb);
        try {
            assertEquals(ByteOrder.nativeOrder(), bb.order(), "buffer should use native order");
        } finally {
            store.releaseLast();
        }
    }
}
