/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
public class BytesContextTest {

    private BytesContext context;

    @BeforeEach
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
        assertEquals(expectedKey, actualKey, "Key should match the expected value");
    }

    @Test
    public void testIsClosedThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () ->
                context.isClosed());
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
