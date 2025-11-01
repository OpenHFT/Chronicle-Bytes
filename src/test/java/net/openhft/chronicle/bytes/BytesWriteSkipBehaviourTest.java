/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.nio.BufferOverflowException;

import static org.junit.Assert.*;

public class BytesWriteSkipBehaviourTest extends BytesTestCommon {

    @Test
    public void reserveThenFillHeader() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            long start = bytes.writePosition();
            bytes.writeSkip(4); // reserve header
            bytes.writeInt(0x11223344);
            bytes.writeShort((short) 0x55AA);
            long end = bytes.writePosition();
            // backfill header with payload length
            long payloadLen = end - start - 4;
            bytes.writeInt(start, (int) payloadLen);

            bytes.readPosition(start);
            assertEquals(payloadLen, bytes.readInt());
            assertEquals(0x11223344, bytes.readInt());
            assertEquals((short) 0x55AA, bytes.readShort());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void backtrackOneRemovesTrailingSeparator() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            // Use length-prefixed UTF-8 so readUtf8() is valid
            bytes.writeUtf8("abc,");
            // Overwrite the last payload byte (comma) with 'd'
            bytes.writeSkip(-1); // drop comma
            bytes.writeByte((byte) 'd');
            bytes.readPosition(0);
            assertEquals("abcd", bytes.readUtf8());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void excessiveNegativeSkipThrows() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
        try {
            bytes.append("xx");
            // attempt to backtrack beyond start
            bytes.writeSkip(- (bytes.writePosition() + 2));
        } finally {
            bytes.releaseLast();
        }
    }
}
