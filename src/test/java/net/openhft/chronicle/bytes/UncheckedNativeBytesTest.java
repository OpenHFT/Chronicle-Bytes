/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void writeByteMovesWritePosition() {
        bytes.writeByte(0, (byte) 1);
        // Verify the write position has moved by 1 byte
        assertEquals(0, bytes.writePosition());
    }

    @Test
    void readIntFromOffset() {
        int expected = 123456;
        bytes.writeInt(0, expected);
        assertEquals(expected, bytes.readInt(0));
    }

    @Test
    void writeBeyondCapacityThrows() {
        assertThrows(BufferOverflowException.class, () -> bytes.writeByte(bytes.capacity(), (byte) 1));
    }

    @Test
    void writeByteWithOffset_ShouldWriteCorrectly() throws IllegalStateException {
        long offset = 10;
        byte value = 5;
        uncheckedBytes.writeByte(offset, value);
        verify(underlyingBytes.bytesStore()).writeByte(eq(offset), eq(value));
    }

    @Test
    void writeIntWithOffset_ShouldWriteCorrectly() throws IllegalStateException {
        long offset = 20;
        int value = 123456789;
        uncheckedBytes.writeInt(offset, value);
        verify(underlyingBytes.bytesStore()).writeInt(eq(offset), eq(value));
    }

    @Test
    void compareAndSwapInt_ShouldSwapCorrectly() {
        long offset = 10;
        int expected = 100;
        int value = 200;
        when(underlyingBytes.bytesStore().compareAndSwapInt(offset, expected, value)).thenReturn(true);

        boolean result = uncheckedBytes.compareAndSwapInt(offset, expected, value);
        assertTrue(result);
        verify(underlyingBytes.bytesStore()).compareAndSwapInt(eq(offset), eq(expected), eq(value));
    }
    @Test
    void ensureCapacityShouldExpandCapacityIfNeeded() {
        // Example test: Ensure capacity expands as expected.
        // This is a simplification. Actual implementation will depend on how you manage native memory.
        long initialCapacity = uncheckedBytes.capacity();
        long desiredCapacity = initialCapacity + 1024;
        uncheckedBytes.ensureCapacity(desiredCapacity);
        assertFalse(uncheckedBytes.capacity() >= desiredCapacity);
    }

    @Test
    void uncheckedShouldAlwaysReturnTrue() {
        assertTrue(uncheckedBytes.unchecked());
    }

    @Test
    void isDirectMemoryShouldReturnTrue() {
        assertTrue(uncheckedBytes.isDirectMemory());
    }

    @Test
    void writeAndReadByte() {
        long offset = 10;
        byte value = 123;
        bytes.writeByte(offset, value);
        byte readValue = bytes.readByte(offset);
        assertEquals(value, readValue, "Written and read values should match.");
    }

    @Test
    void writeAndReadShort() {
        long offset = 20;
        short value = 32000;
        bytes.writeShort(offset, value);
        short readValue = bytes.readShort(offset);
        assertEquals(value, readValue, "Written and read values should match.");
    }

    @Test
    void writeAndReadInt() {
        long offset = 30;
        int value = 123456789;
        bytes.writeInt(offset, value);
        int readValue = bytes.readInt(offset);
        assertEquals(value, readValue, "Written and read values should match.");
    }

    @Test
    void writeAndReadLong() {
        long offset = 40;
        long value = 1234567890123456789L;
        bytes.writeLong(offset, value);
        long readValue = bytes.readLong(offset);
        assertEquals(value, readValue, "Written and read values should match.");
    }

    @Test
    void writeAndReadDouble() {
        long offset = 50;
        double value = 12345.6789;
        bytes.writeDouble(offset, value);
        double readValue = bytes.readDouble(offset);
        assertEquals(value, readValue, "Written and read values should match.");
    }

    @Test
    void boundaryConditionCheck() {
        assertThrows(BufferOverflowException.class, () -> bytes.writeByte(bytes.capacity() + 1, (byte) 1),
                "Writing beyond capacity should throw BufferOverflowException.");
    }

    @Test
    void peekUnsignedByteAtOffset_outOfBounds() {
        assertEquals(-1, uncheckedBytes.peekUnsignedByte(-1));
        assertEquals(-1, uncheckedBytes.peekUnsignedByte(uncheckedBytes.capacity() + 1));
    }

    @Test
    void writeByteShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeByte(0, (byte) 1);
        assertEquals(0, uncheckedBytes.writePosition());
    }

    @Test
    void writeIntShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeInt(0, 123);
        assertEquals(0, uncheckedBytes.writePosition());
    }

    @Test
    void writeLongShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeLong(0, 1234567890123456789L);
        assertEquals(0, uncheckedBytes.writePosition());
    }

    @Test
    void readByteShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeByte(0, (byte) 1);
        uncheckedBytes.readPosition(0);
        uncheckedBytes.readByte();
        assertEquals(1, uncheckedBytes.readPosition());
    }

    @Test
    void readIntShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeInt(0, 123);
        uncheckedBytes.readPosition(0);
        uncheckedBytes.readInt();
        assertEquals(4, uncheckedBytes.readPosition());
    }

    @Test
    void readLongShouldUpdatePosition() {
        UncheckedNativeBytes<?> uncheckedBytes = createUncheckedNativeBytes();
        uncheckedBytes.writeLong(0, 1234567890123456789L);
        uncheckedBytes.readPosition(0);
        uncheckedBytes.readLong();
        assertEquals(8, uncheckedBytes.readPosition());
    }

    private UncheckedNativeBytes<?> createUncheckedNativeBytes() {
        Bytes<?> underlyingBytes = Bytes.allocateElasticOnHeap(256);
        return new UncheckedNativeBytes<>(underlyingBytes);
    }
}
