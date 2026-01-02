/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class AbstractBytesTest {

    private ConcreteBytes bytes;
    private BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore;
    static class ConcreteBytes extends AbstractBytes<ByteBuffer> {
        ConcreteBytes(BytesStore<Bytes<ByteBuffer>, ByteBuffer> bytesStore, long writePosition, long writeLimit) throws ClosedIllegalStateException, ThreadingIllegalStateException {
            super(bytesStore, writePosition, writeLimit);
        }

        @Override
        public long capacity() {
            return bytesStore.capacity();
        }

        @Override
        public long start() {
            return 0;
        }

        @Override
        public BytesStore<Bytes<ByteBuffer>, ByteBuffer> copy() throws IllegalStateException, ClosedIllegalStateException, ThreadingIllegalStateException {
            return null;
        }
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks annotated with @Mock
        mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.capacity()).thenReturn(100L);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        when(mockBytesStore.capacity()).thenReturn(128L);
        when(mockBytesStore.writePosition()).thenReturn(128L); // Simulate that BytesStore is fully written
        when(mockBytesStore.readLimit()).thenReturn(128L); // Ensure readLimit is sufficient
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        bytes = new ConcreteBytes(mockBytesStore, 0, 100);
    }

    @Test
    @DisplayName("isDirectMemory returns true for direct bytes store")
    public void isDirectMemory_ReturnsExpectedValue() {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        assertTrue(bytes.isDirectMemory(),
                "isDirectMemory returns true for direct store");
    }

    @Test
    @DisplayName("canReadDirect returns true with sufficient remaining bytes")
    public void canReadDirect_WithSufficientRemaining_ReturnsTrue() throws Exception {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.writePosition(50); // Simulate that we have written some data

        assertTrue(bytes.canReadDirect(10),
                "canReadDirect returns true for available remaining bytes");
    }

    @Test
    @DisplayName("canReadDirect returns false when remaining bytes are insufficient")
    public void canReadDirect_WithInsufficientRemaining_ReturnsFalse() throws Exception {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.writePosition(50); // Simulate that we have written some data

        assertFalse(bytes.canReadDirect(51),
                "canReadDirect returns false when length exceeds remaining");
    }

    @Test
    @DisplayName("clear resets positions and write limit")
    public void clear_ResetsPositionsAndLimits() throws Exception {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.capacity()).thenReturn(100L);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 10, 90);
        bytes.clear();

        assertEquals(0, bytes.readPosition(),
                "clear resets read position to zero");
        assertEquals(0, bytes.writePosition(),
                "clear resets write position to zero");
        assertEquals(100, bytes.writeLimit(),
                "clear resets write limit to capacity");
    }

    @Test
    @DisplayName("clearAndPad sets positions and write limit")
    public void clearAndPad_SetsPositionsAndLimitsCorrectly() throws Exception {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.capacity()).thenReturn(100L);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.clearAndPad(20);

        assertEquals(20, bytes.readPosition(),
                "clearAndPad sets read position after padding");
        assertEquals(20, bytes.writePosition(),
                "clearAndPad sets write position after padding");
        assertEquals(100, bytes.writeLimit(),
                "clearAndPad keeps write limit at capacity");
    }

    @Test
    @DisplayName("move delegates request to underlying bytes store")
    public void move_ValidParameters_MovesDataCorrectly() {
        bytes.move(0, 10, 20);
        verify(mockBytesStore).move(0, 10, 20);
    }

    @Test
    @DisplayName("appendAndReturnLength delegates and returns updated length")
    public void appendAndReturnLength_CallsBytesStore() {
        long expectedLength = 10L;
        when(mockBytesStore.appendAndReturnLength(anyLong(), anyBoolean(), anyLong(), anyInt(), anyBoolean())).thenReturn(expectedLength);

        long length = bytes.appendAndReturnLength(0, false, 123L, 2, true);
        assertEquals(expectedLength, length,
                "appendAndReturnLength returns bytes store length");
        verify(mockBytesStore).appendAndReturnLength(0, false, 123L, 2, true);
    }

    @Test
    @DisplayName("readPositionForHeader returns expected header position after skip")
    public void readPositionForHeader_WithSkipPadding() throws Exception {
        long newPosition = bytes.readPositionForHeader(true);
        assertEquals(0, newPosition,
                "readPositionForHeader returns zero for header");
    }

    @Test
    @DisplayName("performRelease delegates to underlying bytes store")
    public void performRelease_CallsReleaseOnBytesStore() {
        doNothing().when(mockBytesStore).release(any());
        bytes.performRelease();
        verify(mockBytesStore).release(bytes);
    }

    @Test
    @DisplayName("readLong throws when insufficient data remains")
    public void readLong_WithInsufficientDataThrowsException() {
        doThrow(new BufferUnderflowException()).when(mockBytesStore).readLong(anyLong());
        bytes.lenient(false);
        assertThrows(BufferUnderflowException.class, bytes::readLong,
                "readLong throws when buffer underflows");
    }

    @Test
    @DisplayName("write8bit delegates when bytes store is present")
    public void write8bit_WithNonNullBytesStoreWritesData() {
        BytesStore<?, ?> mockToWrite = mock(BytesStore.class);
        when(mockToWrite.readRemaining()).thenReturn(10L);
        bytes.write8bit(mockToWrite);
        verify(mockBytesStore).write8bit(anyLong(), eq(mockToWrite));
    }

    @Test
    @DisplayName("prewriteCheckOffset throws on invalid offset values")
    public void prewriteCheckOffset_WithInvalidOffsetThrowsException() {
        assertThrows(BufferOverflowException.class,
                () -> bytes.prewriteCheckOffset(150, 10),
                "prewriteCheckOffset throws for overflowed offset");
    }

    @Test
    @DisplayName("toString hides bytes store implementation details")
    public void toString_ReturnsExpectedString() {
        when(mockBytesStore.toString()).thenReturn("MockBytesStore");
        String result = bytes.toString();
        assertNotNull(result,
                "toString returns a non-null string value");
        assertFalse(result.contains("MockBytesStore"),
                "toString output " + result + " hides MockBytesStore");
    }

    @Test
    @DisplayName("byteCheckSum sums expected byte values over range")
    public void byteCheckSum_CalculatesCorrectSum() {
        when(mockBytesStore.readByte(anyLong())).thenReturn((byte)1);
        int sum = bytes.byteCheckSum(0, 10);
        assertEquals(10, sum,
                "byteCheckSum returns sum of ten bytes");
    }
}
