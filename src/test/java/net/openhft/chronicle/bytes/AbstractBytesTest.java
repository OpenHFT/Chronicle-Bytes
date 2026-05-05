/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "deprecation"})
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
        public BytesStore<Bytes<ByteBuffer>, ByteBuffer> copy() throws IllegalStateException {
            return null;
        }
    }

    @Before
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
    public void isDirectMemoryReturnsExpectedValue() {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        assertTrue("Expected isDirectMemory to return true", bytes.isDirectMemory());
    }

    @Test
    public void canReadDirectWithSufficientRemainingReturnsTrue() {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.writePosition(50); // Simulate that we have written some data

        assertTrue("Expected canReadDirect to return true for length <= remaining", bytes.canReadDirect(10));
    }

    @Test
    public void canReadDirectWithInsufficientRemainingReturnsFalse() {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.writePosition(50); // Simulate that we have written some data

        assertFalse("Expected canReadDirect to return false for length > remaining", bytes.canReadDirect(51));
    }

    @Test
    public void clearResetsPositionsAndLimits() {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.capacity()).thenReturn(100L);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 10, 90);
        bytes.clear();

        assertEquals("Expected readPosition to reset", 0, bytes.readPosition());
        assertEquals("Expected writePosition to reset", 0, bytes.writePosition());
        assertEquals("Expected writeLimit to match capacity", 100, bytes.writeLimit());
    }

    @Test
    public void clearAndPadSetsPositionsAndLimitsCorrectly() {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.capacity()).thenReturn(100L);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.clearAndPad(20);

        assertEquals("Expected readPosition to be set correctly after padding", 20, bytes.readPosition());
        assertEquals("Expected writePosition to be set correctly after padding", 20, bytes.writePosition());
        assertEquals("Expected writeLimit to match capacity", 100, bytes.writeLimit());
    }

    @Test
    public void moveValidParametersMovesDataCorrectly() {
        bytes.move(0, 10, 20);
        verify(mockBytesStore).move(0, 10, 20);
    }

    @Test
    public void appendAndReturnLengthCallsBytesStore() {
        long expectedLength = 10L;
        when(mockBytesStore.appendAndReturnLength(anyLong(), anyBoolean(), anyLong(), anyInt(), anyBoolean())).thenReturn(expectedLength);

        long length = bytes.appendAndReturnLength(0, false, 123L, 2, true);
        assertEquals(expectedLength, length);
        verify(mockBytesStore).appendAndReturnLength(0, false, 123L, 2, true);
    }

    @Test
    public void readPositionForHeaderWithSkipPadding() {
        long newPosition = bytes.readPositionForHeader(true);
        assertEquals(0, newPosition);
    }

    @Test
    public void performReleaseCallsReleaseOnBytesStore() {
        doNothing().when(mockBytesStore).release(any());
        bytes.performRelease();
        verify(mockBytesStore).release(bytes);
    }

    @Test(expected = BufferUnderflowException.class)
    public void readLongWithInsufficientDataThrowsException() {
        doThrow(new BufferUnderflowException()).when(mockBytesStore).readLong(anyLong());
        bytes.lenient(false);
        bytes.readLong();
    }

    @Test
    public void write8bitWithNonNullBytesStoreWritesData() {
        BytesStore<?, ?> mockToWrite = mock(BytesStore.class);
        when(mockToWrite.readRemaining()).thenReturn(10L);
        bytes.write8bit(mockToWrite);
        verify(mockBytesStore).write8bit(anyLong(), eq(mockToWrite));
    }

    @Test(expected = BufferOverflowException.class)
    public void prewriteCheckOffsetWithInvalidOffsetThrowsException() {
        bytes.prewriteCheckOffset(150, 10);
    }

    @Test
    public void toStringReturnsExpectedString() {
        when(mockBytesStore.toString()).thenReturn("MockBytesStore");
        String result = bytes.toString();
        assertNotNull(result);
        assertFalse(result.contains("MockBytesStore"));
    }

    @Test
    public void byteCheckSumCalculatesCorrectSum() {
        when(mockBytesStore.readByte(anyLong())).thenReturn((byte) 1);
        int sum = bytes.byteCheckSum(0, 10);
        assertEquals(10, sum);
    }
}
