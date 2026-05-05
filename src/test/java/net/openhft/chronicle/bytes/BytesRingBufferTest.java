/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests BytesRingBuffer mock delegation behaviour because correct ring buffer operations
 * are essential for circular buffer scenarios.
 */
@SuppressWarnings({"deprecation", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("Bytes ring buffer mock delegation scenarios")
class BytesRingBufferTest {

    @Mock
    private BytesRingBuffer bytesRingBuffer;

    @Mock
    private BytesStore<?, Void> mockBytesStore;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("clear delegates to ring buffer mock")
    public void testClear() {
        doNothing().when(bytesRingBuffer).clear();
        bytesRingBuffer.clear();
        verify(bytesRingBuffer).clear();
    }

    @Test
    @DisplayName("offer call reports success on ring buffer mock")
    public void testOffer() {
        when(bytesRingBuffer.offer(any())).thenReturn(true);
        assertTrue(bytesRingBuffer.offer(mockBytesStore),
                "Offer should return true when stubbed to succeed");
    }

    @Test
    @DisplayName("read call reports success on ring buffer mock")
    public void testRead() {
        when(bytesRingBuffer.read(any())).thenReturn(true);
        assertTrue(bytesRingBuffer.read(mock(BytesOut.class)),
                "Read should return true when stubbed to succeed");
    }

    @Test
    @DisplayName("readRemaining returns stubbed length in bytes")
    public void testReadRemaining() {
        when(bytesRingBuffer.readRemaining()).thenReturn(10L);
        assertEquals(10L, bytesRingBuffer.readRemaining(),
                "readRemaining should return the stubbed length");
    }

    @Test
    @DisplayName("isEmpty reports empty state on ring buffer mock")
    public void testIsEmpty() {
        when(bytesRingBuffer.isEmpty()).thenReturn(true);
        assertTrue(bytesRingBuffer.isEmpty(),
                "isEmpty should return true when stubbed to succeed");
    }

    @Test
    @DisplayName("newInstance throws when implementation is missing")
    public void testNewInstanceThrowsException() {
        assertThrows(ClassNotFoundException.class, () -> BytesRingBuffer.newInstance(mockBytesStore),
                "newInstance should throw when implementation is missing");
    }
}
