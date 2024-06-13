package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.BufferOverflowException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StreamingOutputStreamTest {

    private StreamingDataOutput sdo;
    private StreamingOutputStream sos;

    @BeforeEach
    void setUp() {
        // Mock the StreamingDataOutput
        sdo = mock(StreamingDataOutput.class);
        // Initialize StreamingOutputStream with the mocked StreamingDataOutput
        sos = new StreamingOutputStream(sdo);
    }

    @Test
    void writeSingleByte() {
        assertDoesNotThrow(() -> sos.write(1));
        // Verify that writeUnsignedByte was called on the StreamingDataOutput
        verify(sdo, times(1)).writeUnsignedByte(0xff & 1);
    }

    @Test
    void writeByteArray() throws IOException {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        sos.write(bytes, 0, bytes.length);
        // Verify that write was called on the StreamingDataOutput with the correct arguments
        verify(sdo, times(1)).write(bytes, 0, bytes.length);
    }

    @Test
    void writeThrowsIOExceptionOnBufferOverflow() throws IOException {
        doThrow(BufferOverflowException.class).when(sdo).writeUnsignedByte(anyInt());
        assertThrows(IOException.class, () -> sos.write(1));
    }

    @Test
    void writeArrayThrowsIOExceptionOnIllegalArgument() throws IOException {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        doThrow(IllegalArgumentException.class).when(sdo).write(any(byte[].class), anyInt(), anyInt());
        assertThrows(IOException.class, () -> sos.write(bytes, 0, bytes.length));
    }

    @Test
    void initSetsNewStreamingDataOutput() {
        StreamingDataOutput newSdo = Mockito.mock(StreamingDataOutput.class);
        sos.init(newSdo);
        assertDoesNotThrow(() -> sos.write(1));
        // Verify that writeUnsignedByte was called on the new StreamingDataOutput
        verify(newSdo, times(1)).writeUnsignedByte(0xff & 1);
    }
}
