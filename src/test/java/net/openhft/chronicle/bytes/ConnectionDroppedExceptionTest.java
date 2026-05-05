/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for ConnectionDroppedException constructors, because exception classes
 * must properly propagate message and cause to support debugging and logging.
 */
@DisplayName("Connection dropped exception constructor and field validation")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class ConnectionDroppedExceptionTest {

    @Test
    @DisplayName("String constructor stores the error description for later retrieval")
    public void testMessageConstructor() {
        String expectedMessage = "Connection dropped unexpectedly.";
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage);

        assertEquals(expectedMessage, exception.getMessage(),
                "getMessage() should return the exact string 'Connection dropped unexpectedly.' passed to constructor");
    }

    @Test
    @DisplayName("Throwable constructor stores the root cause for exception chaining")
    public void testCauseConstructor() {
        Throwable expectedCause = new RuntimeException("Underlying cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedCause);

        assertEquals(expectedCause, exception.getCause(),
                "getCause() should return the RuntimeException passed to the Throwable constructor");
    }

    @Test
    @DisplayName("String and Throwable constructor stores both description and root cause")
    public void testMessageAndCauseConstructor() {
        String expectedMessage = "Connection dropped with details.";
        Throwable expectedCause = new RuntimeException("Specific cause");
        ConnectionDroppedException exception = new ConnectionDroppedException(expectedMessage, expectedCause);

        assertEquals(expectedMessage, exception.getMessage(),
                "getMessage() should return 'Connection dropped with details.' from combined constructor");
        assertEquals(expectedCause, exception.getCause(),
                "getCause() should return RuntimeException from combined constructor");
    }
}
