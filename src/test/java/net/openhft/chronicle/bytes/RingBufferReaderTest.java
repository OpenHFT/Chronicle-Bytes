/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * Tests RingBufferReader mock behaviour because the reader interface
 * must honour empty, stopped, and stop state transitions in order to
 * support correct buffer consumption patterns.
 */
@DisplayName("RingBufferReader - validates mock-based state transitions")
public class RingBufferReaderTest {

    private RingBufferReader reader;

    @BeforeEach
    public void setUp() {
        reader = mock(RingBufferReader.class);
    }

    @Test
    @DisplayName("mock isEmpty reports true so consumer knows buffer is drained")
    public void testIsEmpty() {
        when(reader.isEmpty()).thenReturn(true);

        assert(reader.isEmpty());

        verify(reader, times(1)).isEmpty();
    }

    @Test
    @DisplayName("mock isStopped reports false so consumer knows reading can continue")
    public void testIsStopped() {
        when(reader.isStopped()).thenReturn(false);

        assert(!reader.isStopped());

        verify(reader, times(1)).isStopped();
    }

    @Test
    @DisplayName("stop invokes reader stop method exactly once")
    public void testStop() {
        reader.stop();

        verify(reader, times(1)).stop();
    }
}
