/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
public class BytesRingBufferTest {

    @Mock
    private BytesRingBuffer bytesRingBuffer;

    @Mock
    private BytesStore<?, Void> mockBytesStore;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testClear() {
        doNothing().when(bytesRingBuffer).clear();
        bytesRingBuffer.clear();
        verify(bytesRingBuffer).clear();
    }

    @Test
    public void testOffer() {
        when(bytesRingBuffer.offer(any())).thenReturn(true);
        assertTrue(bytesRingBuffer.offer(mockBytesStore), "bytesRingBuffer.offer");
    }

    @Test
    public void testRead() {
        when(bytesRingBuffer.read(any())).thenReturn(true);
        assertTrue(bytesRingBuffer.read(mock(BytesOut.class)), "bytesRingBuffer.read");
    }

    @Test
    public void testReadRemaining() {
        when(bytesRingBuffer.readRemaining()).thenReturn(10L);
        assertEquals(10L, bytesRingBuffer.readRemaining(), "bytesRingBuffer.readRemaining");
    }

    @Test
    public void testIsEmpty() {
        when(bytesRingBuffer.isEmpty()).thenReturn(true);
        assertTrue(bytesRingBuffer.isEmpty(), "emptiness should be true");
    }

    @Test
    public void testNewInstanceThrowsException() {
        assertThrows(ClassNotFoundException.class, () ->
                BytesRingBuffer.newInstance(mockBytesStore));
    }
}
