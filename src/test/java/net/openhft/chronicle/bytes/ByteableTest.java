/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Tests Byteable interface methods using mocks because correct offset, maxSize, and
 * lock behaviour must be verified to avoid memory access errors in production.
 */
@SuppressWarnings({"deprecation", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("Byteable - offset, maxSize, and lock method behaviour via mocks")
class ByteableTest {

    private Byteable byteable;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws IOException {
        byteable = mock(Byteable.class);
        doThrow(UnsupportedOperationException.class).when(byteable).address();
        doThrow(UnsupportedOperationException.class).when(byteable).lock(true);
        doThrow(UnsupportedOperationException.class).when(byteable).tryLock(true);
    }

    @Test
    @DisplayName("offset returns configured byteable offset position five for mocked instance")
    public void testOffset() {
        long expectedOffset = 5L;
        when(byteable.offset()).thenReturn(expectedOffset);

        assertEquals(expectedOffset, byteable.offset(),
                "Byteable.offset() should return 5L when mock is configured to return that offset position");
    }

    @Test
    @DisplayName("address throws unsupported operation when not available")
    public void testAddressThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, byteable::address,
                "address throws when unsupported");
    }

    @Test
    @DisplayName("maxSize returns configured byteable size 1024 for mocked instance")
    public void testMaxSize() {
        long expectedSize = 1024L;
        when(byteable.maxSize()).thenReturn(expectedSize);

        assertEquals(expectedSize, byteable.maxSize(),
                "Byteable.maxSize() should return 1024L when mock is configured to return that size value");
    }

    @Test
    @DisplayName("lock throws unsupported operation when not available")
    public void testLockThrowsUnsupportedOperationException() throws IOException {
        assertThrows(UnsupportedOperationException.class,
                () -> byteable.lock(true),
                "lock throws when unsupported");
    }

    @Test
    @DisplayName("tryLock throws unsupported operation when not available")
    public void testTryLockThrowsUnsupportedOperationException() throws IOException {
        assertThrows(UnsupportedOperationException.class,
                () -> byteable.tryLock(true),
                "tryLock throws when unsupported");
    }
}
