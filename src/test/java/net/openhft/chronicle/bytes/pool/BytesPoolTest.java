/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.pool;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests BytesPool thread-local pooling behaviour because efficient buffer
 * reuse is essential to avoid allocation overhead in high-frequency
 * messaging scenarios.
 */
@DisplayName("Bytes pool thread local acquisition and reuse")
class BytesPoolTest {

    @Test
    @DisplayName("thread local pool acquires empty bytes")
    void testAcquireBytes() {
        try (ScopedResource<Bytes<?>> resource = BytesPool.createThreadLocal().get()) {
            Bytes<?> bytes = resource.get();
            assertNotNull(bytes, "Acquired bytes should not be null.");

            assertEquals(0, bytes.readRemaining(), "Acquired bytes should be ready for use.");
        }
    }

    @Test
    @DisplayName("thread local pool reuses bytes after write")
    void testBytesPoolUsage() {
        try (ScopedResource<Bytes<?>> resource = BytesPool.createThreadLocal().get()) {
            Bytes<?> bytes = resource.get();

            bytes.writeUtf8("Hello, World!");
            assertEquals("Hello, World!",
                    bytes.readUtf8(),
                    "Thread local pool should return bytes containing the written text");

            bytes.clear();

            assertEquals(0,
                    bytes.readRemaining(),
                    "Cleared bytes should have no remaining readable data");

            bytes.releaseLast();
        }
    }
}
