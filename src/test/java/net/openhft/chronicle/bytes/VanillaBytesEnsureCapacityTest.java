/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests ensureCapacity behaviour because correct elastic growth is
 * essential to avoid BufferOverflowException during bulk writes.
 */
@DisplayName("VanillaBytesEnsureCapacity - validates elastic growth on demand")
public class VanillaBytesEnsureCapacityTest extends BytesTestCommon {

    @Test
    @DisplayName("elastic bytes grows capacity when writing beyond initial size")
    public void elasticEnsureCapacityGrows() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            long rc = b.realCapacity();
            byte[] chunk = new byte[1024];
            b.write(chunk);
            long expanded = b.realCapacity();
            assertTrue(expanded > rc,
                    "Real capacity should grow beyond " + rc + " after write, but was " + expanded);
        } finally {
            b.releaseLast();
        }
    }
}
