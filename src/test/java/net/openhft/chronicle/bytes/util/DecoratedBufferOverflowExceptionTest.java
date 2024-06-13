package net.openhft.chronicle.bytes.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class DecoratedBufferOverflowExceptionTest {

    @Test
    public void testMessage() {
        String expectedMessage = "Custom message describing the overflow";
        DecoratedBufferOverflowException exception = new DecoratedBufferOverflowException(expectedMessage);

        // Assert that the message is correctly set and retrieved
        assertEquals(expectedMessage, exception.getMessage());
    }
}
