/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
            assertTrue(capAfter >= 10, "Expected capacity to grow beyond initial");

            // zeroOut a large range including unwritten tail
            long start = 2;
            long end = Math.min(b.writePosition() + 16, b.capacity());
            b.zeroOut(start, end);

            // Verify visible zeroing only on written region
            b.readPosition(0);
            byte first = b.readByte();
            assertEquals('X', first, "first byte should remain 'X' (before zeroed range)");
            byte third = b.readByte(2);
            assertEquals(0, third, "third byte should be zeroed after zeroOut()");
        } finally {
            b.releaseLast();
        }
    }
}

