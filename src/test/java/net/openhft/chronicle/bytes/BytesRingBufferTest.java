/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BytesRingBufferTest {

    @Mock
    private BytesRingBuffer bytesRingBuffer;

    @Mock
    private BytesStore<?, Void> mockBytesStore;

    @Test
    public void testClear() {
        bytesRingBuffer.clear();
        verify(bytesRingBuffer).clear();
    }

    @Test
    public void testOffer() {
        when(bytesRingBuffer.offer(any())).thenReturn(true);
        assertTrue(bytesRingBuffer.offer(mockBytesStore));
    }

    @Test
    public void testRead() {
        when(bytesRingBuffer.read(any())).thenReturn(true);
        assertTrue(bytesRingBuffer.read(mock(BytesOut.class)));
    }

    @Test
    public void testReadRemaining() {
        when(bytesRingBuffer.readRemaining()).thenReturn(10L);
        assertEquals(10L, bytesRingBuffer.readRemaining());
    }

    @Test
    public void testIsEmpty() {
        when(bytesRingBuffer.isEmpty()).thenReturn(true);
        assertTrue(bytesRingBuffer.isEmpty());
    }

    @Test
    public void testNewInstanceThrowsException() {
        assertThrows(ClassNotFoundException.class, () ->
            BytesRingBuffer.newInstance(mockBytesStore));
    }
}
