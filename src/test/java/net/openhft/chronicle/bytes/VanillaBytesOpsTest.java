/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VanillaBytesOpsTest extends BytesTestCommon {

    @Test
    public void writeReadPrimitivesAndZeroOut() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        try {
            b.writeInt(0x11223344);
            b.writeLong(0x0102030405060708L);

            b.readPosition(0);
            assertEquals(0x11223344, b.readInt());
            assertEquals(0x0102030405060708L, b.readLong());

            // zero out the int we wrote and check
            b.zeroOut(0, 4);
            assertEquals(0, b.peekUnsignedByte(0));
            assertEquals(0, b.peekUnsignedByte(1));
        } finally {
            b.releaseLast();
        }
    }
}

