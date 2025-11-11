/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.*;

public class VanillaBytesCapacityAndZeroOutTest extends BytesTestCommon {

    @Test
    public void ensureCapacityGrowsAndZeroOutsRange() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            // Grow in small steps
            for (int i = 0; i < 10; i++) {
                b.append('X');
            }
            long capAfter = b.capacity();
            assertTrue("Expected capacity to grow beyond initial", capAfter >= 10);

            // zeroOut a large range including unwritten tail
            long start = 2;
            long end = Math.min(b.writePosition() + 16, b.capacity());
            b.zeroOut(start, end);

            // Verify visible zeroing only on written region
            b.readPosition(0);
            byte first = b.readByte();
            assertEquals('X', first);
            byte third = b.readByte(2);
            assertEquals(0, third);
        } finally {
            b.releaseLast();
        }
    }
}

