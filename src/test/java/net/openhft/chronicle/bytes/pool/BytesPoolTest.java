/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.pool;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BytesPoolTest {

    @Test
    void testAcquireBytes() {
        try (ScopedResource<Bytes<?>> resource = BytesPool.createThreadLocal().get()) {
            Bytes<?> bytes = resource.get();
            assertNotNull(bytes, "Acquired bytes should not be null.");

            assertEquals(0, bytes.readRemaining(), "Acquired bytes should be ready for use.");
        }
    }

    @Test
    void testBytesPoolUsage() {
        try (ScopedResource<Bytes<?>> resource = BytesPool.createThreadLocal().get()) {
            Bytes<?> bytes = resource.get();

            bytes.writeUtf8("Hello, World!");
            assertEquals("Hello, World!", bytes.readUtf8());

            bytes.clear();

            assertEquals(0, bytes.readRemaining());

            bytes.releaseLast();
        }
    }
}
