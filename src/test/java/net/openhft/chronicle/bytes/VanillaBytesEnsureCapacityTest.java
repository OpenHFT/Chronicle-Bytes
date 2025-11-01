/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class VanillaBytesEnsureCapacityTest extends BytesTestCommon {

    @Test
    public void elasticEnsureCapacityGrows() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            long rc = b.realCapacity();
            byte[] chunk = new byte[1024];
            b.write(chunk);
            assertTrue(b.realCapacity() > rc);
        } finally {
            b.releaseLast();
        }
    }
}
