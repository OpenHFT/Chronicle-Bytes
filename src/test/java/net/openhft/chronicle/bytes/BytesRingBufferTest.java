package net.openhft.chronicle.bytes;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BytesRingBufferTest {

    @Mock
    private BytesRingBuffer bytesRingBuffer;

    @Mock
    private BytesStore<?, Void> mockBytesStore;

    @Before
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

    @Test(expected = ClassNotFoundException.class)
    public void testNewInstanceThrowsException() {
        BytesRingBuffer.newInstance(mockBytesStore);
    }
}
