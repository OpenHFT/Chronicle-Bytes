/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class RingBufferReaderTest {

    private RingBufferReader reader;

    @BeforeEach
    void setUp() {
        reader = mock(RingBufferReader.class);
    }

    @Test
    void testIsEmpty() {
        when(reader.isEmpty()).thenReturn(true);

        assert (reader.isEmpty());

        verify(reader, times(1)).isEmpty();
    }

    @Test
    void testIsStopped() {
        when(reader.isStopped()).thenReturn(false);

        assert (!reader.isStopped());

        verify(reader, times(1)).isStopped();
    }

    @Test
    void testStop() {
        reader.stop();

        verify(reader, times(1)).stop();
    }
}
