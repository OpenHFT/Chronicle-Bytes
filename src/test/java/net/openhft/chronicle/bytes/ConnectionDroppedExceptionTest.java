package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Test;

public class ConnectionDroppedExceptionTest {

    @Test
    public void testMessageConstructor() {
        String expectedMessage = "Connection dropped unexpectedly.";
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage);

        Assert.assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void testCauseConstructor() {
        Throwable expectedCause = new RuntimeException("Underlying cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedCause);

        Assert.assertEquals(expectedCause, exception.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String expectedMessage = "Connection dropped with details.";
        Throwable expectedCause = new RuntimeException("Specific cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage, expectedCause);

        Assert.assertEquals(expectedMessage, exception.getMessage());
        Assert.assertEquals(expectedCause, exception.getCause());
    }
}
