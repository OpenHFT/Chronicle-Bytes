/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecoratedBufferOverflowExceptionTest {

    @Test
    @DisplayName("exception retains the supplied overflow message")
    public void testMessage() {
        String expectedMessage = "Custom message describing the overflow";
        DecoratedBufferOverflowException exception = new DecoratedBufferOverflowException(expectedMessage);

        assertEquals(expectedMessage,
                exception.getMessage(),
                "Exception message should match the supplied overflow description");
    }
}
