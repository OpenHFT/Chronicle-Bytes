/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteCheckSumTest extends BytesTestCommon {
    @Test
    public void test() {
        Bytes<?> bytes = Bytes.allocateDirect(32);
        assertEquals(('a' + 'b' + 'c' + 'd' + 'e' + 'f') & 0xff, doTest(bytes), "test: checksum for direct bytes");
        bytes.releaseLast();
    }

    @Test
    public void testHeap() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        assertEquals(('a' + 'b' + 'c' + 'd' + 'e' + 'f') & 0xff, doTest(bytes), "testHeap: checksum for heap bytes");
        bytes.releaseLast();
    }

    private int doTest(Bytes<?> bytes) {
        bytes.append("abcdef");
        int checkSum = bytes.byteCheckSum();
        assertEquals(('b' + 'c' + 'd' + 'e' + 'f') & 0xff, bytes.byteCheckSum(1, 6), "byteCheckSum(1,6) should sum 'bcdef' from position 1 to 6");
        assertEquals(('b' + 'c' + 'd') & 0xff, bytes.byteCheckSum(1, 4), "byteCheckSum(1,4) should sum 'bcd' from position 1 to 4");
        assertEquals(('c' + 'd') & 0xff, bytes.byteCheckSum(2, 4), "byteCheckSum(2,4) should sum 'cd' from position 2 to 4");
        assertEquals(('c') & 0xff, bytes.byteCheckSum(2, 3), "byteCheckSum(2,3) should sum 'c' from position 2 to 3");
        return checkSum;
    }
}
