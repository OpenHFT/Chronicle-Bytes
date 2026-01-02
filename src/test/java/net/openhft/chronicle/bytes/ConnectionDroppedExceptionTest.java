/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionDroppedExceptionTest {

    @Test
    @DisplayName("message constructor preserves provided text message")
    public void testMessageConstructor() {
        String expectedMessage = "Connection dropped unexpectedly.";
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage(),
                "Message constructor keeps supplied text unchanged");
    }

    @Test
    @DisplayName("cause constructor preserves provided throwable cause")
    public void testCauseConstructor() {
        Throwable expectedCause = new RuntimeException("Underlying cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedCause);

        assertEquals(expectedCause, exception.getCause(),
                "Cause constructor keeps supplied throwable unchanged");
    }

    @Test
    @DisplayName("message and cause constructor preserves text and cause")
    public void testMessageAndCauseConstructor() {
        String expectedMessage = "Connection dropped with details.";
        Throwable expectedCause = new RuntimeException("Specific cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage, expectedCause);

        assertEquals(expectedMessage, exception.getMessage(),
                "Message and cause constructor keeps text message");
        assertEquals(expectedCause, exception.getCause(),
                "Message and cause constructor keeps throwable cause");
    }
}
