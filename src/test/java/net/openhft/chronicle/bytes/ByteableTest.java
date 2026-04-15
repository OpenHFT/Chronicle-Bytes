/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class ByteableTest {

    private Byteable byteable;
    private BytesStore<?, ?> bytesStore;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws IOException {
        byteable = mock(Byteable.class);
        bytesStore = mock(BytesStore.class);
        doThrow(UnsupportedOperationException.class).when(byteable).address();
        doThrow(UnsupportedOperationException.class).when(byteable).lock(true);
        doThrow(UnsupportedOperationException.class).when(byteable).tryLock(true);
    }

    @Test
    public void testOffset() {
        long expectedOffset = 5L;
        when(byteable.offset()).thenReturn(expectedOffset);

        assertEquals(expectedOffset, byteable.offset());
    }

    @Test
    public void testAddressThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            when(byteable.address()).thenCallRealMethod();
            byteable.address();
        });
    }

    @Test
    public void testMaxSize() {
        long expectedSize = 1024L;
        when(byteable.maxSize()).thenReturn(expectedSize);

        assertEquals(expectedSize, byteable.maxSize());
    }

    @Test
    public void testLockThrowsUnsupportedOperationException() throws IOException {
        assertThrows(UnsupportedOperationException.class, () ->
            byteable.lock(true));
    }

    @Test
    public void testTryLockThrowsUnsupportedOperationException() throws IOException {
        assertThrows(UnsupportedOperationException.class, () ->
            byteable.tryLock(true));
    }
}
