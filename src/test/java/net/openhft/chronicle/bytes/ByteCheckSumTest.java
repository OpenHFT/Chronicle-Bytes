/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests byteCheckSum for direct and heap bytes because correct checksums are required
 * for data integrity verification in streaming and persistence scenarios.
 */
@DisplayName("Bytes - byteCheckSum for direct and heap allocations")
public class ByteCheckSumTest extends BytesTestCommon {
    @Test
    @DisplayName("byteCheckSum computes checksum for direct bytes")
    public void test() {
        Bytes<?> bytes = Bytes.allocateDirect(32);
        doTest(bytes);
        bytes.releaseLast();
    }

    @Test
    @DisplayName("byteCheckSum computes checksum for heap bytes")
    public void testHeap() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        doTest(bytes);
        bytes.releaseLast();
    }

    private void doTest(Bytes<?> bytes) {
        bytes.append("abcdef");
        assertEquals(('a' + 'b' + 'c' + 'd' + 'e' + 'f') & 0xff, bytes.byteCheckSum(),
                "byteCheckSum covers full string");
        assertEquals(('b' + 'c' + 'd' + 'e' + 'f') & 0xff, bytes.byteCheckSum(1, 6),
                "byteCheckSum covers range 1..6");
        assertEquals(('b' + 'c' + 'd') & 0xff, bytes.byteCheckSum(1, 4),
                "byteCheckSum covers range 1..4");
        assertEquals(('c' + 'd') & 0xff, bytes.byteCheckSum(2, 4),
                "byteCheckSum covers range 2..4");
        assertEquals(('c') & 0xff, bytes.byteCheckSum(2, 3),
                "byteCheckSum covers range 2..3");
    }
}
