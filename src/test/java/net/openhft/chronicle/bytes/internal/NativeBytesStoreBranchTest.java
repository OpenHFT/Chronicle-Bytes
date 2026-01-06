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
 * Tests for NativeBytesStore covering branch coverage for native memory operations.
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
    @DisplayName("nativeStoreWithFixedCapacity returns fixed native store")
    void shouldCreateFixedNativeStore() {
        NativeBytesStore<Void> fixed = NativeBytesStore.nativeStoreWithFixedCapacity(128);
        try {
            assertTrue(fixed.isDirectMemory(),
                    "fixed native store uses direct memory");
            assertEquals(128, fixed.realCapacity(),
                    "fixed native store real capacity is 128 bytes");
        } finally {
            fixed.releaseLast();
        }
    }

    @Test
    @DisplayName("lazyNativeBytesStoreWithFixedCapacity returns lazy native store")
    void shouldCreateLazyStore() {
        NativeBytesStore<Void> lazy = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(64);
        try {
            assertNotNull(lazy, "lazy native store instance is present");
            assertEquals(64, lazy.capacity(), "lazy native store capacity is 64 bytes");
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
    @DisplayName("elasticByteBuffer honours initial size minimum")
    void shouldCreateElasticByteBufferWithSize() {
        NativeBytesStore<ByteBuffer> elastic = NativeBytesStore.elasticByteBuffer(128, 1024);
        try {
            // Elastic buffer may round up capacity but should be at least requested size
            assertTrue(elastic.capacity() >= 128,
                    "elastic buffer capacity is at least 128 bytes");
        } finally {
            elastic.releaseLast();
        }
    }

    @Test
    @DisplayName("from(String) creates store from string data")
    void shouldCreateFromString() {
        NativeBytesStore<?> fromString = NativeBytesStore.from("Hello");
        try {
            assertEquals(5, fromString.capacity(),
                    "string store capacity equals string length");
            assertEquals('H', fromString.readByte(0),
                    "string store first byte equals 'H'");
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
    @DisplayName("native store reports direct memory allocation")
    void shouldReturnTrueForDirectMemory() {
        assertTrue(store.isDirectMemory(),
                "native store reports direct memory allocation");
    }

    @Test
    @DisplayName("canReadDirect honours length within capacity")
    void shouldCheckCanReadDirect() {
        assertTrue(store.canReadDirect(100),
                "canReadDirect accepts length 100 within capacity");
        assertFalse(store.canReadDirect(1000),
                "canReadDirect rejects length 1000 beyond capacity");
    }

    // ========== Read Operations ==========

    @Test
    @DisplayName("readByte returns written byte value")
    void shouldReadByte() {
        store.writeByte(0, (byte) 42);

        assertEquals(42, store.readByte(0),
                "readByte returns 42 after write");
    }

    @Test
    @DisplayName("readShort returns written short value")
    void shouldReadShort() {
        store.writeShort(0, (short) 1234);

        assertEquals(1234, store.readShort(0),
                "readShort returns 1234 after write");
    }

    @Test
    @DisplayName("readInt returns written int value")
    void shouldReadInt() {
        store.writeInt(0, 0x12345678);

        assertEquals(0x12345678, store.readInt(0),
                "readInt returns 0x12345678 after write");
    }

    @Test
    @DisplayName("readLong returns written long value")
    void shouldReadLong() {
        store.writeLong(0, 0x123456789ABCDEF0L);

        assertEquals(0x123456789ABCDEF0L, store.readLong(0),
                "readLong returns 0x123456789ABCDEF0 after write");
    }

    @Test
    @DisplayName("readFloat returns written float value")
    void shouldReadFloat() {
        store.writeFloat(0, 3.14f);

        assertEquals(3.14f, store.readFloat(0), 0.001f,
                "readFloat returns 3.14 within tolerance");
    }

    @Test
    @DisplayName("readDouble returns written double value")
    void shouldReadDouble() {
        store.writeDouble(0, 3.14159);

        assertEquals(3.14159, store.readDouble(0), 0.00001,
                "readDouble returns 3.14159 within tolerance");
    }

    // ========== Volatile Operations ==========

    @Test
    @DisplayName("readVolatileByte returns latest written value")
    void shouldReadVolatileByte() {
        store.writeVolatileByte(0, (byte) 77);

        assertEquals(77, store.readVolatileByte(0),
                "readVolatileByte returns 77 after volatile write");
    }

    @Test
    @DisplayName("readVolatileShort returns latest written value")
    void shouldReadVolatileShort() {
        store.writeVolatileShort(0, (short) 1234);

        assertEquals(1234, store.readVolatileShort(0),
                "readVolatileShort returns 1234 after volatile write");
    }

    @Test
    @DisplayName("readVolatileInt returns latest written value")
    void shouldReadVolatileInt() {
        store.writeVolatileInt(0, 0x12345678);

        assertEquals(0x12345678, store.readVolatileInt(0),
                "readVolatileInt returns 0x12345678 after volatile write");
    }

    @Test
    @DisplayName("readVolatileLong returns latest written value")
    void shouldReadVolatileLong() {
        store.writeVolatileLong(0, 0x123456789ABCDEF0L);

        assertEquals(0x123456789ABCDEF0L, store.readVolatileLong(0),
                "readVolatileLong returns 0x123456789ABCDEF0 after volatile write");
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
    @DisplayName("compareAndSwapInt updates value on match")
    void shouldCompareAndSwapInt() {
        store.writeInt(0, 100);

        boolean result = store.compareAndSwapInt(0, 100, 200);

        assertTrue(result, "compareAndSwapInt returns true on match");
        assertEquals(200, store.readInt(0), "compareAndSwapInt updates value to 200");
    }

    @Test
    @DisplayName("compareAndSwapInt leaves value on mismatch")
    void shouldFailCompareAndSwapInt() {
        store.writeInt(0, 100);

        boolean result = store.compareAndSwapInt(0, 50, 200);

        assertFalse(result, "compareAndSwapInt returns false on mismatch");
        assertEquals(100, store.readInt(0), "compareAndSwapInt keeps value at 100");
    }

    @Test
    @DisplayName("compareAndSwapLong updates value on match")
    void shouldCompareAndSwapLong() {
        store.writeLong(0, 100L);

        boolean result = store.compareAndSwapLong(0, 100L, 200L);

        assertTrue(result, "compareAndSwapLong returns true on match");
        assertEquals(200L, store.readLong(0), "compareAndSwapLong updates value to 200");
    }

    @Test
    @DisplayName("compareAndSwapDouble updates value on match")
    void shouldCompareAndSwapDouble() {
        store.writeDouble(0, 1.0);

        boolean result = store.compareAndSwapDouble(0, 1.0, 2.0);

        assertTrue(result, "compareAndSwapDouble returns true on match");
        assertEquals(2.0, store.readDouble(0), 0.0001,
                "compareAndSwapDouble updates value to 2.0");
    }

    @Test
    @DisplayName("compareAndSwapFloat updates value on match")
    void shouldCompareAndSwapFloat() {
        store.writeFloat(0, 1.0f);

        boolean result = store.compareAndSwapFloat(0, 1.0f, 2.0f);

        assertTrue(result, "compareAndSwapFloat returns true on match");
        assertEquals(2.0f, store.readFloat(0), 0.0001f,
                "compareAndSwapFloat updates value to 2.0");
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
    @DisplayName("move rejects negative source offset")
    void shouldThrowForNegativeMoveOffsets() {
        assertThrows(IllegalArgumentException.class,
                () -> store.move(-1, 0, 8),
                "move rejects negative source offset");
    }

    // ========== bytesForWrite ==========

    @Test
    @DisplayName("bytesForWrite returns writable VanillaBytes view")
    void shouldReturnBytesForWrite() {
        VanillaBytes<?> vanillaBytes = store.bytesForWrite();
        try {
            assertNotNull(vanillaBytes, "bytesForWrite returns non-null VanillaBytes");
            vanillaBytes.writeLong(0x123456789ABCDEF0L);
            assertEquals(0x123456789ABCDEF0L, store.readLong(0),
                    "bytesForWrite writeLong updates native store");
        } finally {
            vanillaBytes.releaseLast();
        }
    }

    // ========== Address Operations ==========

    @Test
    @DisplayName("addressForRead returns non-zero native address")
    void shouldReturnAddressForRead() {
        long address = store.addressForRead(0);

        assertTrue(address != 0, "addressForRead returns non-zero native address");
    }

    @Test
    @DisplayName("addressForWrite returns non-zero native address")
    void shouldReturnAddressForWrite() {
        long address = store.addressForWrite(0);

        assertTrue(address != 0, "addressForWrite returns non-zero native address");
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
    @DisplayName("realCapacity reports native allocation size")
    void shouldReturnCapacity() {
        // nativeStore creates an elastic store with large max capacity
        // realCapacity returns actual allocation
        assertEquals(256, store.realCapacity(),
                "realCapacity returns 256 byte allocation");
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
    @DisplayName("underlyingObject is null for native allocation")
    void shouldReturnNullUnderlyingObject() {
        assertNull(store.underlyingObject(),
                "underlyingObject is null for native allocation");
    }

    @Test
    @DisplayName("underlyingObject is ByteBuffer when wrapped")
    void shouldReturnByteBufferWhenWrapped() {
        ByteBuffer bb = ByteBuffer.allocateDirect(64);
        NativeBytesStore<ByteBuffer> wrapped = NativeBytesStore.wrap(bb);
        try {
            assertNotNull(wrapped.underlyingObject(),
                    "wrapped store underlyingObject is present");
            assertTrue(wrapped.underlyingObject() instanceof ByteBuffer,
                    "wrapped store underlyingObject is ByteBuffer");
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

        assertTrue(nextPos > 5, "write8bit returns end position after payload");

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

    @ParameterizedTest(name = "write at offset {0}")
    @ValueSource(longs = {0, 8, 16, 100, 200})
    @DisplayName("writeLong and readLong work at different offsets")
    void shouldWriteAtVariousOffsets(long offset) {
        store.writeLong(offset, 0x123456789ABCDEF0L);

        assertEquals(0x123456789ABCDEF0L, store.readLong(offset),
                "readLong returns value at offset " + offset);
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
    @DisplayName("hashCode remains consistent across calls")
    void shouldReturnConsistentHashCode() {
        store.writeLong(0, 0x123456789ABCDEF0L);

        int hash1 = store.hashCode();
        int hash2 = store.hashCode();

        assertEquals(hash1, hash2, "hashCode returns same value across calls");
    }
}
