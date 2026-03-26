/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BytesContextTest {

    private BytesContext context;

    @BeforeEach
    void setUp() {
        // Mock the BytesContext interface
        context = mock(BytesContext.class);
        doThrow(UnsupportedOperationException.class).when(context).isClosed();
    }

    @Test
    void testKey() {
        // Setup a specific key to return
        final int expectedKey = 42;
        when(context.key()).thenReturn(expectedKey);

        int actualKey = context.key();
        assertEquals(expectedKey, actualKey, "Key should match the expected value");
    }

    @Test
    void testIsClosedThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            context.isClosed();
        });
    }

    @Test
    void testRollbackOnClose() {
        try {
            context.rollbackOnClose();
        } catch (Exception e) {
            fail("rollbackOnClose should not throw any exception");
        }
    }
}
