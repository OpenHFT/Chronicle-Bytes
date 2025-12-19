/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionDroppedExceptionTest {

    @Test
    public void testMessageConstructor() {
        String expectedMessage = "Connection dropped unexpectedly.";
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage(), "exception.getMessage");
    }

    @Test
    public void testCauseConstructor() {
        Throwable expectedCause = new RuntimeException("Underlying cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedCause);

        assertEquals(expectedCause, exception.getCause(), "exception.getCause");
    }

    @Test
    public void testMessageAndCauseConstructor() {
        String expectedMessage = "Connection dropped with details.";
        Throwable expectedCause = new RuntimeException("Specific cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage, expectedCause);

        assertEquals(expectedMessage, exception.getMessage(), "exception.getMessage");
        assertEquals(expectedCause, exception.getCause(), "exception.getCause");
    }
}
