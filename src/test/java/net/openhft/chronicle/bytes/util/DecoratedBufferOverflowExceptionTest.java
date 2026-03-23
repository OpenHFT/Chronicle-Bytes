/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DecoratedBufferOverflowExceptionTest {

    @Test
    public void testMessage() {
        String expectedMessage = "Custom message describing the overflow";
        DecoratedBufferOverflowException exception = new DecoratedBufferOverflowException(expectedMessage);

        // Assert that the message is correctly set and retrieved
        assertEquals(expectedMessage, exception.getMessage());
    }
}
