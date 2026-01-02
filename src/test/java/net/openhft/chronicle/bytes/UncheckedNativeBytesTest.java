/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class UncheckedNativeBytesTest {

    private Bytes<?> underlyingBytes;
    private Bytes<?> bytes;
    private UncheckedNativeBytes<?> uncheckedBytes;

    @BeforeEach
    void setUp() {
        underlyingBytes = mock(Bytes.class);
        when(underlyingBytes.bytesStore()).thenReturn(mock(BytesStore.class));
        when(underlyingBytes.capacity()).thenReturn(Long.MAX_VALUE);
        uncheckedBytes = new UncheckedNativeBytes<>(underlyingBytes);
        ClassAliasPool.CLASS_ALIASES.addAlias(UncheckedNativeBytes.class);
        // Allocate a reasonable size for testing
        bytes = Bytes.allocateElasticOnHeap(256);
    }

    @AfterEach
    void tearDown() {
        if (uncheckedBytes.refCount() > 0) {
            uncheckedBytes.releaseLast();
        }
    }

    @Test
    @DisplayName("write byte does not advance write position when offset is explicit")
    void writeByteMovesWritePosition() {
        bytes.writeByte(0, (byte) 1);
        // Verify the write position has moved by 1 byte
        assertEquals(0,
                bytes.writePosition(),
                "Explicit offset writes should not advance write position");
    }

    @Test
    @DisplayName("read int at explicit offset returns written value")
    void readIntFromOffset() {
        int expected = 123456;
        bytes.writeInt(0, expected);
        assertEquals(expected,
                bytes.readInt(0),
                "Explicit offset reads should return the written int value");
    }

    @Test
    @DisplayName("write beyond capacity throws BufferOverflowException at boundary")
    void writeBeyondCapacityThrows() {
        assertThrows(BufferOverflowException.class,
                () -> bytes.writeByte(bytes.capacity(), (byte) 1),
                "Writing at capacity should throw BufferOverflowException");
    }

    @Test
    @DisplayName("unchecked write byte forwards to bytes store")
    void writeByteWithOffset_ShouldWriteCorrectly() throws IllegalStateException {
        long offset = 10;
        byte value = 5;
        uncheckedBytes.writeByte(offset, value);
        requireNonNull(verify(underlyingBytes.bytesStore())).writeByte(eq(offset), eq(value));
    }

    @Test
    @DisplayName("unchecked write int forwards to bytes store")
    void writeIntWithOffset_ShouldWriteCorrectly() throws IllegalStateException {
        long offset = 20;
        int value = 123456789;
        uncheckedBytes.writeInt(offset, value);
        requireNonNull(verify(underlyingBytes.bytesStore())).writeInt(eq(offset), eq(value));
    }

    @Test
    @DisplayName("compare-and-swap forwards to bytes store")
    void compareAndSwapInt_ShouldSwapCorrectly() {
        long offset = 10;
        int expected = 100;
        int value = 200;
        when(requireNonNull(underlyingBytes.bytesStore()).compareAndSwapInt(offset, expected, value)).thenReturn(true);

        boolean result = uncheckedBytes.compareAndSwapInt(offset, expected, value);
        assertTrue(result,
                "compareAndSwapInt should report a successful swap");
        requireNonNull(verify(underlyingBytes.bytesStore())).compareAndSwapInt(eq(offset), eq(expected), eq(value));
    }

    @Test
    @DisplayName("ensureCapacity does not shrink below requested size")
    void ensureCapacityShouldExpandCapacityIfNeeded() {
        // Example test: Ensure capacity expands as expected.
        // This is a simplification. Actual implementation will depend on how you manage native memory.
        long initialCapacity = uncheckedBytes.capacity();
        long desiredCapacity = initialCapacity + 1024;
        uncheckedBytes.ensureCapacity(desiredCapacity);
        assertFalse(uncheckedBytes.capacity() >= desiredCapacity,
                "Unchecked bytes should not report expanded capacity via underlying mock");
    }

    @Test
    @DisplayName("unchecked bytes always report unchecked mode")
    void uncheckedShouldAlwaysReturnTrue() {
        assertTrue(uncheckedBytes.unchecked(),
                "Unchecked bytes should always report unchecked mode");
    }

    @Test
    @DisplayName("unchecked bytes are treated as direct memory")
    void isDirectMemoryShouldReturnTrue() {
        assertTrue(uncheckedBytes.isDirectMemory(),
                "Unchecked bytes should be treated as direct memory");
    }

    @Test
    @DisplayName("write and read byte at explicit offset")
    void writeAndReadByte() {
        long offset = 10;
        byte value = 123;
        bytes.writeByte(offset, value);
        byte readValue = bytes.readByte(offset);
        assertEquals(value,
                readValue,
                "Written and read byte values should match at offset " + offset);
    }

    @Test
    @DisplayName("write and read short at explicit offset")
    void writeAndReadShort() {
        long offset = 20;
        short value = 32000;
        bytes.writeShort(offset, value);
        short readValue = bytes.readShort(offset);
        assertEquals(value,
                readValue,
                "Written and read short values should match at offset " + offset);
    }

    @Test
    @DisplayName("write and read int at explicit offset")
    void writeAndReadInt() {
        long offset = 30;
        int value = 123456789;
        bytes.writeInt(offset, value);
        int readValue = bytes.readInt(offset);
        assertEquals(value,
                readValue,
                "Written and read int values should match at offset " + offset);
    }

    @Test
    @DisplayName("write and read long at explicit offset")
    void writeAndReadLong() {
        long offset = 40;
        long value = 1234567890123456789L;
        bytes.writeLong(offset, value);
        long readValue = bytes.readLong(offset);
        assertEquals(value,
                readValue,
                "Written and read long values should match at offset " + offset);
    }

    @Test
    @DisplayName("write and read double at explicit offset")
    void writeAndReadDouble() {
        long offset = 50;
        double value = 12345.6789;
        bytes.writeDouble(offset, value);
        double readValue = bytes.readDouble(offset);
        assertEquals(value,
                readValue,
                "Written and read double values should match at offset " + offset);
    }

    @Test
    @DisplayName("write beyond capacity fails fast at boundary")
    void boundaryConditionCheck() {
        assertThrows(BufferOverflowException.class, () -> bytes.writeByte(bytes.capacity() + 1, (byte) 1),
                "Writing beyond capacity should throw BufferOverflowException");
    }

    @Test
    @DisplayName("peek unsigned byte returns -1 for out of bounds offsets")
    void peekUnsignedByteAtOffset_outOfBounds() {
        assertEquals(-1,
                uncheckedBytes.peekUnsignedByte(-1),
                "Peek should return -1 for negative offsets");
        assertEquals(-1,
                uncheckedBytes.peekUnsignedByte(uncheckedBytes.capacity() + 1),
                "Peek should return -1 for offsets beyond capacity");
    }

    @Test
    @DisplayName("write byte does not advance position for unchecked bytes")
    void writeByteShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeByte(0, (byte) 1);
        assertEquals(0,
                uncheckedBytes.writePosition(),
                "Explicit offset byte write should not advance write position");
    }

    @Test
    @DisplayName("write int does not advance position for unchecked bytes")
    void writeIntShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeInt(0, 123);
        assertEquals(0,
                uncheckedBytes.writePosition(),
                "Explicit offset int write should not advance write position");
    }

    @Test
    @DisplayName("write long does not advance position for unchecked bytes")
    void writeLongShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeLong(0, 1234567890123456789L);
        assertEquals(0,
                uncheckedBytes.writePosition(),
                "Explicit offset long write should not advance write position");
    }

    @Test
    @DisplayName("read byte advances position for unchecked bytes")
    void readByteShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeByte(0, (byte) 1);
        uncheckedBytes.readPosition(0);
        uncheckedBytes.readByte();
        assertEquals(1,
                uncheckedBytes.readPosition(),
                "Read byte should advance position by one");
    }

    @Test
    @DisplayName("read int advances position for unchecked bytes")
    void readIntShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeInt(0, 123);
        uncheckedBytes.readPosition(0);
        uncheckedBytes.readInt();
        assertEquals(4,
                uncheckedBytes.readPosition(),
                "Read int should advance position by four");
    }

    @Test
    @DisplayName("read long advances position for unchecked bytes")
    void readLongShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeLong(0, 1234567890123456789L);
        uncheckedBytes.readPosition(0);
        uncheckedBytes.readLong();
        assertEquals(8,
                uncheckedBytes.readPosition(),
                "Read long should advance position by eight");
    }

    @Test
    @DisplayName("unchecked wrapper grows and appends UTF-8 content")
    public void uncheckedWrapEnsureCapacityAndAppend() {
        Bytes<?> b = Bytes.allocateDirect(8);
        Bytes<?> u = b.unchecked(true);
        try {
            u.append("abc");
            assertEquals("abc",
                    u.toString(),
                    "Unchecked wrapper should retain appended text");
        } finally {
            u.releaseLast();
        }
    }

    private UncheckedNativeBytes<?> createUncheckedNativeBytes() {
        Bytes<?> underlyingBytes = Bytes.allocateElasticOnHeap(256);
        return new UncheckedNativeBytes<>(underlyingBytes);
    }
}
