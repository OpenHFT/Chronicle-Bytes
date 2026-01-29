/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests capacity growth and zeroOut operations because correct elastic
 * expansion and clearing are essential to avoid stale data leaks.
 */
@DisplayName("VanillaBytesCapacityAndZeroOut - validates growth and clearing")
public class VanillaBytesCapacityAndZeroOutTest extends BytesTestCommon {

    @Test
    @DisplayName("capacity grows and zeroOut clears the written range")
    public void ensureCapacityGrowsAndZeroOutsRange() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            // Grow in small steps
            for (int i = 0; i < 10; i++) {
                b.append('X');
            }
            long capAfter = b.capacity();
            assertTrue(capAfter >= 10,
                    "Capacity should be at least 10 after writes, but was " + capAfter);

            // zeroOut a large range including unwritten tail
            long start = 2;
            long end = Math.min(b.writePosition() + 16, b.capacity());
            b.zeroOut(start, end);

            // Verify visible zeroing only on written region
            b.readPosition(0);
            byte first = b.readByte();
            assertEquals('X',
                    first,
                    "First byte should remain the written value after zeroOut");
            byte third = b.readByte(2);
            assertEquals(0,
                    third,
                    "Zeroed range should clear the third byte at index 2");
        } finally {
            b.releaseLast();
        }
    }
}
