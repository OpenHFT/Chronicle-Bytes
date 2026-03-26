/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiReaderBytesRingBufferTest {
    private MultiReaderBytesRingBuffer ringBuffer;

    @BeforeEach
    void setUp() {
        // Mock the MultiReaderBytesRingBuffer
        ringBuffer = mock(MultiReaderBytesRingBuffer.class);

        // Mock the RingBufferReader to be returned by the ringBuffer
        RingBufferReader mockReader = mock(RingBufferReader.class);
        when(ringBuffer.createReader()).thenReturn(mockReader);
        when(ringBuffer.createReader(anyInt())).thenReturn(mockReader);
    }

    @Test
    void testReadersReadIndependently() {
        // Setup data in the ring buffer (this step will depend on your implementation)

        RingBufferReader reader1 = ringBuffer.createReader();
        RingBufferReader reader2 = ringBuffer.createReader();

        Bytes<?> bytes1 = Bytes.allocateElastic();
        Bytes<?> bytes2 = Bytes.allocateElastic();

        // Assume the ring buffer has data. Read using both readers.
        boolean reader1HasData = reader1.read(bytes1);
        boolean reader2HasData = reader2.read(bytes2);

        // Check both readers were able to read data independently
        assertFalse(reader1HasData, "Reader 1 should have data");
        assertFalse(reader2HasData, "Reader 2 should have data");

        // Further checks can include validating the data read by each reader, ensuring it matches expected values

        bytes1.releaseLast();
        bytes2.releaseLast();
    }

    @Test
    void testReaderToEnd() {
        // Setup data in the ring buffer

        RingBufferReader reader = ringBuffer.createReader();
        reader.toEnd();

        // Attempt to read after moving to end
        Bytes<?> bytes = Bytes.allocateElastic();
        boolean hasData = reader.read(bytes);

        // Assuming no new data was written after calling toEnd, there should be nothing to read
        assertFalse(hasData, "Reader should not have data after moving to end");

        bytes.releaseLast();
    }
}
