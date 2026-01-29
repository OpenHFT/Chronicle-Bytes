/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.VanillaBytes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NativeBytesStore, because native memory operations must be validated
 * to avoid memory corruption and ensure atomic semantics are honoured.
 */
@DisplayName("NativeBytesStore branch coverage for native memory operations")
class NativeBytesStoreBranchTest extends BytesTestCommon {

    private NativeBytesStore<?> store;

    @BeforeEach
    void setUp() {
        store = NativeBytesStore.nativeStore(256);
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.releaseLast();
        }
    }

    // ========== Factory Methods ==========

    @Test
    @DisplayName("nativeStore returns elastic native memory store")
    void shouldCreateElasticNativeStore() {
        NativeBytesStore<Void> elastic = NativeBytesStore.nativeStore(64);
        try {
            assertTrue(elastic.isDirectMemory(),
                    "native store uses direct memory");
            assertEquals(64, elastic.realCapacity(),
                    "native store real capacity is 64 bytes");
        } finally {
            elastic.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeStoreWithFixedCapacity returns fixed native store with specified size")
    void shouldCreateFixedNativeStore() {
        NativeBytesStore<Void> fixed = NativeBytesStore.nativeStoreWithFixedCapacity(128);
        try {
            assertTrue(fixed.isDirectMemory(),
                    "fixed native store should use direct memory for 128 byte allocation");
            assertEquals(128, fixed.realCapacity(),
                    "fixed native store real capacity should be exactly 128 bytes");
        } finally {
            fixed.releaseLast();
        }
    }

    @Test
    @DisplayName("lazyNativeBytesStoreWithFixedCapacity returns lazy native store with deferred allocation")
    void shouldCreateLazyStore() {
        NativeBytesStore<Void> lazy = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(64);
        try {
            assertNotNull(lazy, "lazy native store instance should be present for 64 byte capacity");
            assertEquals(64, lazy.capacity(), "lazy native store capacity should be 64 bytes after allocation");
        } finally {
            lazy.releaseLast();
        }
    }

    @Test
    @DisplayName("wrap(ByteBuffer) uses direct buffer capacity")
    void shouldWrapByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(64);
        NativeBytesStore<ByteBuffer> wrapped = NativeBytesStore.wrap(bb);
        try {
            assertEquals(64, wrapped.capacity(),
                    "wrapped store capacity matches buffer size");
            // Write via the store and read back
            wrapped.writeLong(0, 0x123456789ABCDEF0L);
            assertEquals(0x123456789ABCDEF0L, wrapped.readLong(0),
                    "wrapped store returns written long value");
        } finally {
            wrapped.releaseLast();
        }
    }

    @Test
    @DisplayName("follow(ByteBuffer) returns non-owning buffer store")
    void shouldFollowByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(64);
        bb.putLong(0x123456789ABCDEF0L);

        NativeBytesStore<ByteBuffer> followed = NativeBytesStore.follow(bb);
        try {
            assertEquals(64, followed.capacity(),
                    "followed store capacity matches buffer size");
        } finally {
            followed.releaseLast();
        }
    }

    @Test
    @DisplayName("elasticByteBuffer returns elastic buffer backed store")
    void shouldCreateElasticByteBuffer() {
        NativeBytesStore<ByteBuffer> elastic = NativeBytesStore.elasticByteBuffer();
        try {
            assertTrue(elastic.capacity() > 0,
                    "elastic buffer backed store has positive capacity");
        } finally {
            elastic.releaseLast();
        }
    }

    @Test
    @DisplayName("elasticByteBuffer with initial size 128 honours minimum capacity requirement")
    void shouldCreateElasticByteBufferWithSize() {
        NativeBytesStore<ByteBuffer> elastic = NativeBytesStore.elasticByteBuffer(128, 1024);
        try {
            // Elastic buffer may round up capacity but should be at least requested size
            assertTrue(elastic.capacity() >= 128,
                    "elastic buffer capacity=" + elastic.capacity() + " should be at least 128 bytes");
        } finally {
            elastic.releaseLast();
        }
    }

    @Test
    @DisplayName("from(String) creates native store containing string bytes")
    void shouldCreateFromString() {
        NativeBytesStore<?> fromString = NativeBytesStore.from("Hello");
        try {
            assertEquals(5, fromString.capacity(),
                    "native store from 'Hello' should have capacity 5 bytes");
            assertEquals('H', fromString.readByte(0),
                    "native store first byte should be 'H' (0x48)");
        } finally {
            fromString.releaseLast();
        }
    }

    @Test
    @DisplayName("from(byte[]) creates store from byte array data")
    void shouldCreateFromByteArray() {
        byte[] data = {1, 2, 3, 4, 5};
        NativeBytesStore<?> fromBytes = NativeBytesStore.from(data);
        try {
            assertEquals(5, fromBytes.capacity(),
                    "byte array store capacity equals array length");
            assertEquals(1, fromBytes.readByte(0),
                    "byte array store first byte equals 1");
        } finally {
            fromBytes.releaseLast();
        }
    }

    // ========== Basic Properties ==========

    @Test
    @DisplayName("native store isDirectMemory returns true for off-heap allocation")
    void shouldReturnTrueForDirectMemory() {
        assertTrue(store.isDirectMemory(),
                "isDirectMemory should return true for 256 byte native allocation");
    }

    @Test
    @DisplayName("canReadDirect returns true for length within capacity and false beyond")
    void shouldCheckCanReadDirect() {
        assertTrue(store.canReadDirect(100),
                "canReadDirect(100) should be true for 256 byte capacity store");
        assertFalse(store.canReadDirect(1000),
                "canReadDirect(1000) should be false for 256 byte capacity store");
    }

    // ========== Read Operations ==========

    @Test
    @DisplayName("readByte at offset 0 returns the byte value 42 after writeByte")
    void shouldReadByte() {
        store.writeByte(0, (byte) 42);

        assertEquals(42, store.readByte(0),
                "readByte(0) should return 42 after writeByte(0, 42)");
    }

    @Test
    @DisplayName("readShort at offset 0 returns the short value 1234 after writeShort")
    void shouldReadShort() {
        store.writeShort(0, (short) 1234);

        assertEquals(1234, store.readShort(0),
                "readShort(0) should return 1234 after writeShort(0, 1234)");
    }

    @Test
    @DisplayName("readInt at offset 0 returns the int value 0x12345678 after writeInt")
    void shouldReadInt() {
        store.writeInt(0, 0x12345678);

        assertEquals(0x12345678, store.readInt(0),
                "readInt(0) should return 0x12345678 after writeInt(0, 0x12345678)");
    }

    @Test
    @DisplayName("readLong at offset 0 returns the long value 0x123456789ABCDEF0 after writeLong")
    void shouldReadLong() {
        store.writeLong(0, 0x123456789ABCDEF0L);

        assertEquals(0x123456789ABCDEF0L, store.readLong(0),
                "readLong(0) should return 0x123456789ABCDEF0L after writeLong");
    }

    @Test
    @DisplayName("readFloat at offset 0 returns 3.14f after writeFloat within tolerance")
    void shouldReadFloat() {
        store.writeFloat(0, 3.14f);

        assertEquals(3.14f, store.readFloat(0), 0.001f,
                "readFloat(0) should return 3.14f within 0.001 tolerance after writeFloat");
    }

    @Test
    @DisplayName("readDouble at offset 0 returns 3.14159 after writeDouble within tolerance")
    void shouldReadDouble() {
        store.writeDouble(0, 3.14159);

        assertEquals(3.14159, store.readDouble(0), 0.00001,
                "readDouble(0) should return 3.14159 within 0.00001 tolerance after writeDouble");
    }

    // ========== Volatile Operations ==========

    @Test
    @DisplayName("readVolatileByte returns 77 after writeVolatileByte with memory barrier")
    void shouldReadVolatileByte() {
        store.writeVolatileByte(0, (byte) 77);

        assertEquals(77, store.readVolatileByte(0),
                "readVolatileByte(0) should return 77 after writeVolatileByte(0, 77)");
    }

    @Test
    @DisplayName("readVolatileShort returns 1234 after writeVolatileShort with memory barrier")
    void shouldReadVolatileShort() {
        store.writeVolatileShort(0, (short) 1234);

        assertEquals(1234, store.readVolatileShort(0),
                "readVolatileShort(0) should return 1234 after writeVolatileShort(0, 1234)");
    }

    @Test
    @DisplayName("readVolatileInt returns 0x12345678 after writeVolatileInt with memory barrier")
    void shouldReadVolatileInt() {
        store.writeVolatileInt(0, 0x12345678);

        assertEquals(0x12345678, store.readVolatileInt(0),
                "readVolatileInt(0) should return 0x12345678 after writeVolatileInt");
    }

    @Test
    @DisplayName("readVolatileLong returns 0x123456789ABCDEF0 after writeVolatileLong with memory barrier")
    void shouldReadVolatileLong() {
        store.writeVolatileLong(0, 0x123456789ABCDEF0L);

        assertEquals(0x123456789ABCDEF0L, store.readVolatileLong(0),
                "readVolatileLong(0) should return 0x123456789ABCDEF0L after writeVolatileLong");
    }

    // ========== Ordered Write Operations ==========

    @Test
    @DisplayName("writeOrderedInt stores value visible to readInt")
    void shouldWriteOrderedInt() {
        store.writeOrderedInt(0, 0xAABBCCDD);

        assertEquals(0xAABBCCDD, store.readInt(0),
                "readInt returns ordered value 0xAABBCCDD");
    }

    @Test
    @DisplayName("writeOrderedLong stores value visible to readLong")
    void shouldWriteOrderedLong() {
        store.writeOrderedLong(0, 0xAABBCCDDEEFF0011L);

        assertEquals(0xAABBCCDDEEFF0011L, store.readLong(0),
                "readLong returns ordered value 0xAABBCCDDEEFF0011");
    }

    // ========== Compare and Swap Operations ==========

    @Test
    @DisplayName("compareAndSwapInt updates value from 100 to 200 when expected matches stored")
    void shouldCompareAndSwapInt() {
        store.writeInt(0, 100);

        boolean result = store.compareAndSwapInt(0, 100, 200);

        assertTrue(result, "compareAndSwapInt(0, 100, 200) should return true when stored value is 100");
        assertEquals(200, store.readInt(0), "readInt(0) should return 200 after successful CAS");
    }

    @Test
    @DisplayName("compareAndSwapInt leaves value unchanged when expected 50 does not match stored 100")
    void shouldFailCompareAndSwapInt() {
        store.writeInt(0, 100);

        boolean result = store.compareAndSwapInt(0, 50, 200);

        assertFalse(result, "compareAndSwapInt(0, 50, 200) should return false when stored value is 100");
        assertEquals(100, store.readInt(0), "readInt(0) should remain 100 after failed CAS");
    }

    @Test
    @DisplayName("compareAndSwapLong updates value from 100L to 200L when expected matches stored")
    void shouldCompareAndSwapLong() {
        store.writeLong(0, 100L);

        boolean result = store.compareAndSwapLong(0, 100L, 200L);

        assertTrue(result, "compareAndSwapLong(0, 100L, 200L) should return true when stored value is 100L");
        assertEquals(200L, store.readLong(0), "readLong(0) should return 200L after successful CAS");
    }

    @Test
    @DisplayName("compareAndSwapDouble updates value from 1.0 to 2.0 when expected matches stored")
    void shouldCompareAndSwapDouble() {
        store.writeDouble(0, 1.0);

        boolean result = store.compareAndSwapDouble(0, 1.0, 2.0);

        assertTrue(result, "compareAndSwapDouble(0, 1.0, 2.0) should return true when stored value is 1.0");
        assertEquals(2.0, store.readDouble(0), 0.0001,
                "readDouble(0) should return 2.0 after successful CAS");
    }

    @Test
    @DisplayName("compareAndSwapFloat updates value from 1.0f to 2.0f when expected matches stored")
    void shouldCompareAndSwapFloat() {
        store.writeFloat(0, 1.0f);

        boolean result = store.compareAndSwapFloat(0, 1.0f, 2.0f);

        assertTrue(result, "compareAndSwapFloat(0, 1.0f, 2.0f) should return true when stored value is 1.0f");
        assertEquals(2.0f, store.readFloat(0), 0.0001f,
                "readFloat(0) should return 2.0f after successful CAS");
    }

    // ========== Move Operation ==========

    @Test
    @DisplayName("move copies bytes within native store")
    void shouldMoveData() {
        store.writeLong(0, 0x123456789ABCDEF0L);

        store.move(0, 16, 8);

        assertEquals(0x123456789ABCDEF0L, store.readLong(16),
                "move copies long value to destination offset");
    }

    @Test
    @DisplayName("move rejects from=-1 because negative source offsets are invalid")
    void shouldThrowForNegativeMoveOffsets() {
        assertThrows(IllegalArgumentException.class,
                () -> store.move(-1, 0, 8),
                "move(-1, 0, 8) should throw because from=-1 is an invalid negative offset");
    }

    // ========== bytesForWrite ==========

    @Test
    @DisplayName("bytesForWrite returns writable VanillaBytes that updates underlying native store")
    void shouldReturnBytesForWrite() {
        VanillaBytes<?> vanillaBytes = store.bytesForWrite();
        try {
            assertNotNull(vanillaBytes, "bytesForWrite should return non-null VanillaBytes for 256 byte store");
            vanillaBytes.writeLong(0x123456789ABCDEF0L);
            assertEquals(0x123456789ABCDEF0L, store.readLong(0),
                    "store.readLong(0) should return value written via bytesForWrite");
        } finally {
            vanillaBytes.releaseLast();
        }
    }

    // ========== Address Operations ==========

    @Test
    @DisplayName("addressForRead returns non-zero native address for valid offset")
    void shouldReturnAddressForRead() {
        long address = store.addressForRead(0);

        assertTrue(address != 0, "addressForRead(0) should return non-zero address=" + address + " for native store");
    }

    @Test
    @DisplayName("addressForWrite returns non-zero native address for valid offset")
    void shouldReturnAddressForWrite() {
        long address = store.addressForWrite(0);

        assertTrue(address != 0, "addressForWrite(0) should return non-zero address=" + address + " for native store");
    }

    // ========== Write Operations with Arrays ==========

    @Test
    @DisplayName("write(byte[]) copies full array into store")
    void shouldWriteByteArray() {
        byte[] data = {1, 2, 3, 4, 5};

        store.write(0, data);

        assertEquals(1, store.readByte(0), "byte array element 0 equals 1");
        assertEquals(5, store.readByte(4), "byte array element 4 equals 5");
    }

    @Test
    @DisplayName("write(byte[], offset, length) copies array slice")
    void shouldWriteByteArrayRange() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};

        store.write(0, data, 2, 4);

        assertEquals(3, store.readByte(0), "array slice first byte equals 3");
        assertEquals(6, store.readByte(3), "array slice fourth byte equals 6");
    }

    // ========== Capacity and Limits ==========

    @Test
    @DisplayName("realCapacity reports 256 bytes for native store created with that capacity")
    void shouldReturnCapacity() {
        // nativeStore creates an elastic store with large max capacity
        // realCapacity returns actual allocation
        assertEquals(256, store.realCapacity(),
                "realCapacity() should return 256 for store created with nativeStore(256)");
    }

    @Test
    @DisplayName("realCapacity remains constant for native store")
    void shouldReturnRealCapacity() {
        assertEquals(256, store.realCapacity(),
                "native store realCapacity remains 256 bytes");
    }

    @Test
    @DisplayName("start offset is zero for native store")
    void shouldReturnZeroStart() {
        assertEquals(0, store.start(),
                "start offset equals zero");
    }

    // ========== underlyingObject ==========

    @Test
    @DisplayName("underlyingObject returns null for direct native allocation")
    void shouldReturnNullUnderlyingObject() {
        assertNull(store.underlyingObject(),
                "underlyingObject() should be null for direct native 256 byte allocation");
    }

    @Test
    @DisplayName("underlyingObject returns ByteBuffer instance when store wraps a direct buffer")
    void shouldReturnByteBufferWhenWrapped() {
        ByteBuffer bb = ByteBuffer.allocateDirect(64);
        NativeBytesStore<ByteBuffer> wrapped = NativeBytesStore.wrap(bb);
        try {
            assertNotNull(wrapped.underlyingObject(),
                    "underlyingObject() should be present for wrapped 64 byte direct buffer");
            assertTrue(wrapped.underlyingObject() instanceof ByteBuffer,
                    "underlyingObject() should be ByteBuffer instance for wrapped store");
        } finally {
            wrapped.releaseLast();
        }
    }

    // ========== Byte Array Read ==========

    @Test
    @DisplayName("read copies bytes into destination array")
    void shouldReadIntoByteArray() {
        store.write(0, new byte[]{10, 20, 30, 40, 50});

        byte[] result = new byte[5];
        store.read(0, result, 0, 5);

        assertArrayEquals(new byte[]{10, 20, 30, 40, 50}, result,
                "read copies expected byte array");
    }

    // ========== String Operations ==========

    @Test
    @DisplayName("write8bit writes length prefixed string data")
    void shouldWrite8bitString() {
        long nextPos = store.write8bit(0, "Hello", 0, 5);

        assertTrue(nextPos > 5, "write8bit(0, 'Hello', 0, 5) should return end position=" + nextPos + " > 5 after payload");

        Bytes<?> bytes = store.bytesForRead();
        try {
            bytes.readPosition(0);
            assertEquals("Hello", bytes.read8bit(),
                    "read8bit returns original string data");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== Edge Cases ==========

    @ParameterizedTest(name = "writeLong and readLong at offset {0} should preserve value")
    @ValueSource(longs = {0, 8, 16, 100, 200})
    @DisplayName("writeLong and readLong work at different offsets")
    void shouldWriteAtVariousOffsets(long offset) {
        store.writeLong(offset, 0x123456789ABCDEF0L);

        assertEquals(0x123456789ABCDEF0L, store.readLong(offset),
                "readLong(" + offset + ") should return 0x123456789ABCDEF0L after writeLong");
    }

    @Test
    @DisplayName("multiple typed writes read back correctly")
    void shouldHandleMultipleOperations() {
        // Write various types
        store.writeByte(0, (byte) 1);
        store.writeShort(1, (short) 2);
        store.writeInt(3, 3);
        store.writeLong(7, 4L);

        // Read back
        assertEquals(1, store.readByte(0), "readByte returns 1 after write");
        assertEquals(2, store.readShort(1), "readShort returns 2 after write");
        assertEquals(3, store.readInt(3), "readInt returns 3 after write");
        assertEquals(4L, store.readLong(7), "readLong returns 4 after write");
    }

    // ========== HashCode and Equals ==========

    @Test
    @DisplayName("hashCode returns same value on repeated calls for unchanged store content")
    void shouldReturnConsistentHashCode() {
        store.writeLong(0, 0x123456789ABCDEF0L);

        int hash1 = store.hashCode();
        int hash2 = store.hashCode();

        assertEquals(hash1, hash2, "hashCode() should return " + hash1 + " on repeated calls for unchanged content");
    }
}
