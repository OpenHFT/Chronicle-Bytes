/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

public class BytesRingBufferTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testNewInstanceThrowsException() {
        BytesStore<?, Void> mockBytesStore = mock(BytesStore.class);
        // The optional enterprise implementation is absent from this module's test dependencies.
        ClassNotFoundException exception = assertThrows(ClassNotFoundException.class,
                () -> BytesRingBuffer.newInstance(mockBytesStore));
        assertEquals("software.chronicle.enterprise.ring.EnterpriseRingBuffer", exception.getMessage());
    }
}
