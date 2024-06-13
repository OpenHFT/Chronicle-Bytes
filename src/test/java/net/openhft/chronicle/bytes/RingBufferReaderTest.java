package net.openhft.chronicle.bytes;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class RingBufferReaderTest {

    private RingBufferReader reader;

    @Before
    public void setUp() {
        reader = mock(RingBufferReader.class);
    }

    @Test
    public void testIsEmpty() {
        when(reader.isEmpty()).thenReturn(true);

        assert(reader.isEmpty());

        verify(reader, times(1)).isEmpty();
    }

    @Test
    public void testIsStopped() {
        when(reader.isStopped()).thenReturn(false);

        assert(!reader.isStopped());

        verify(reader, times(1)).isStopped();
    }

    @Test
    public void testStop() {
        reader.stop();

        verify(reader, times(1)).stop();
    }
}
