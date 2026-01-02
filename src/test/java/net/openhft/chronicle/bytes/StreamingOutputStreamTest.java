/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.BufferOverflowException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class StreamingOutputStreamTest {

    private StreamingDataOutput<?> sdo;
    private StreamingOutputStream sos;

    @BeforeEach
    void setUp() {
        // Mock the StreamingDataOutput
        sdo = mock(StreamingDataOutput.class);
        // Initialize StreamingOutputStream with the mocked StreamingDataOutput
        sos = new StreamingOutputStream(sdo);
    }

    @Test
    @DisplayName("write single byte forwards to unsigned byte write")
    void writeSingleByte() {
        assertDoesNotThrow(() -> sos.write(1),
                "Write should forward the single byte without throwing");
        // Verify that writeUnsignedByte was called on the StreamingDataOutput
        verify(sdo, times(1)).writeUnsignedByte(0xff & 1);
    }

    @Test
    @DisplayName("write byte array forwards full range to data output")
    void writeByteArray() throws IOException {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        sos.write(bytes, 0, bytes.length);
        // Verify that write was called on the StreamingDataOutput with the correct arguments
        verify(sdo, times(1)).write(bytes, 0, bytes.length);
    }

    @Test
    @DisplayName("buffer overflow maps to IOException on single byte write")
    void writeThrowsIOExceptionOnBufferOverflow() throws IOException {
        doThrow(BufferOverflowException.class).when(sdo).writeUnsignedByte(anyInt());
        assertThrows(IOException.class,
                () -> sos.write(1),
                "Buffer overflow should be reported as IOException");
    }

    @Test
    @DisplayName("illegal argument maps to IOException for array writes")
    void writeArrayThrowsIOExceptionOnIllegalArgument() throws IOException {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        doThrow(IllegalArgumentException.class).when(sdo).write(any(byte[].class), anyInt(), anyInt());
        assertThrows(IOException.class,
                () -> sos.write(bytes, 0, bytes.length),
                "Illegal argument should be reported as IOException");
    }

    @Test
    @DisplayName("init swaps the streaming data output target")
    void initSetsNewStreamingDataOutput() {
        StreamingDataOutput newSdo = Mockito.mock(StreamingDataOutput.class);
        sos.init(newSdo);
        assertDoesNotThrow(() -> sos.write(1),
                "Write should succeed after reinitialising the output target");
        // Verify that writeUnsignedByte was called on the new StreamingDataOutput
        verify(newSdo, times(1)).writeUnsignedByte(0xff & 1);
    }
}
