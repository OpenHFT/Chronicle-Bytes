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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.junit.Test;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

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
    public void isDirectMemory_ReturnsExpectedValue() {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        assertTrue("Expected isDirectMemory to return true", bytes.isDirectMemory());
    }

    @Test
    public void canReadDirect_WithSufficientRemaining_ReturnsTrue() throws Exception {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.writePosition(50); // Simulate that we have written some data

        assertTrue("Expected canReadDirect to return true for length <= remaining", bytes.canReadDirect(10));
    }

    @Test
    public void canReadDirect_WithInsufficientRemaining_ReturnsFalse() throws Exception {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.isDirectMemory()).thenReturn(true);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.writePosition(50); // Simulate that we have written some data

        assertFalse("Expected canReadDirect to return false for length > remaining", bytes.canReadDirect(51));
    }

    @Test
    public void clear_ResetsPositionsAndLimits() throws Exception {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.capacity()).thenReturn(100L);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 10, 90);
        bytes.clear();

        assertEquals("Expected readPosition to reset", 0, bytes.readPosition());
        assertEquals("Expected writePosition to reset", 0, bytes.writePosition());
        assertEquals("Expected writeLimit to match capacity", 100, bytes.writeLimit());
    }

    @Test
    public void clearAndPad_SetsPositionsAndLimitsCorrectly() throws Exception {
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> mockBytesStore = mock(BytesStore.class);
        when(mockBytesStore.capacity()).thenReturn(100L);

        ConcreteBytes bytes = new ConcreteBytes(mockBytesStore, 0, 100);
        bytes.clearAndPad(20);

        assertEquals("Expected readPosition to be set correctly after padding", 20, bytes.readPosition());
        assertEquals("Expected writePosition to be set correctly after padding", 20, bytes.writePosition());
        assertEquals("Expected writeLimit to match capacity", 100, bytes.writeLimit());
    }

    @Test
    public void move_ValidParameters_MovesDataCorrectly() {
        bytes.move(0, 10, 20);
        verify(mockBytesStore).move(0, 10, 20);
    }

    @Test
    public void appendAndReturnLength_CallsBytesStore() {
        long expectedLength = 10L;
        when(mockBytesStore.appendAndReturnLength(anyLong(), anyBoolean(), anyLong(), anyInt(), anyBoolean())).thenReturn(expectedLength);

        long length = bytes.appendAndReturnLength(0, false, 123L, 2, true);
        assertEquals(expectedLength, length);
        verify(mockBytesStore).appendAndReturnLength(0, false, 123L, 2, true);
    }

    @Test
    public void readPositionForHeader_WithSkipPadding() throws Exception {
        long newPosition = bytes.readPositionForHeader(true);
    }

    @Test
    public void performRelease_CallsReleaseOnBytesStore() {
        doNothing().when(mockBytesStore).release(any());
        bytes.performRelease();
        verify(mockBytesStore).release(bytes);
    }

    @Test(expected = BufferUnderflowException.class)
    public void readLong_WithInsufficientDataThrowsException() {
        doThrow(new BufferUnderflowException()).when(mockBytesStore).readLong(anyLong());
        bytes.lenient(false);
        bytes.readLong();
    }

    @Test
    public void write8bit_WithNonNullBytesStoreWritesData() {
        BytesStore<?, ?> mockToWrite = mock(BytesStore.class);
        when(mockToWrite.readRemaining()).thenReturn(10L);
        bytes.write8bit(mockToWrite);
        verify(mockBytesStore).write8bit(anyLong(), eq(mockToWrite));
    }

    @Test(expected = BufferOverflowException.class)
    public void prewriteCheckOffset_WithInvalidOffsetThrowsException() {
        bytes.prewriteCheckOffset(150, 10);
    }

    @Test
    public void toString_ReturnsExpectedString() {
        when(mockBytesStore.toString()).thenReturn("MockBytesStore");
        String result = bytes.toString();
        assertNotNull(result);
        assertFalse(result.contains("MockBytesStore"));
    }

    @Test
    public void byteCheckSum_CalculatesCorrectSum() {
        when(mockBytesStore.readByte(anyLong())).thenReturn((byte)1);
        int sum = bytes.byteCheckSum(0, 10);
        assertEquals(10, sum);
    }
}
