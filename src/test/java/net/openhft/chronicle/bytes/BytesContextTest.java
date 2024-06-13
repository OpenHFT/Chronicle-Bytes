package net.openhft.chronicle.bytes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BytesContextTest {

    private BytesContext context;

    @Before
    public void setUp() {
        // Mock the BytesContext interface
        context = mock(BytesContext.class);
        doThrow(UnsupportedOperationException.class).when(context).isClosed();
    }

    @Test
    public void testKey() {
        // Setup a specific key to return
        final int expectedKey = 42;
        when(context.key()).thenReturn(expectedKey);

        int actualKey = context.key();
        assertEquals("Key should match the expected value", expectedKey, actualKey);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsClosedThrowsUnsupportedOperationException() {
        context.isClosed();
    }

    @Test
    public void testRollbackOnClose() {
        try {
            context.rollbackOnClose();
        } catch (Exception e) {
            fail("rollbackOnClose should not throw any exception");
        }
    }
}
