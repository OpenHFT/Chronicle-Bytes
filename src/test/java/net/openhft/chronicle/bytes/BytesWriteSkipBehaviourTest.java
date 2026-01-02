/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BytesWriteSkipBehaviourTest extends BytesTestCommon {

    @Test
    @DisplayName("reserve header space then fill payload")
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
            assertEquals(payloadLen, bytes.readInt(),
                    "Backfilled header stores payload length");
            assertEquals(0x11223344, bytes.readInt(),
                    "Header payload value round trips");
            assertEquals((short) 0x55AA, bytes.readShort(),
                    "Trailing short payload round trips");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("backtrack removes trailing separator character in payload")
    public void backtrackOneRemovesTrailingSeparator() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            // Use length-prefixed UTF-8 so readUtf8() is valid
            bytes.writeUtf8("abc,");
            // Overwrite the last payload byte (comma) with 'd'
            bytes.writeSkip(-1); // drop comma
            bytes.writeByte((byte) 'd');
            bytes.readPosition(0);
            assertEquals("abcd", bytes.readUtf8(),
                    "Backtracked separator removed from UTF-8 content");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("negative skip beyond start throws overflow exception")
    public void excessiveNegativeSkipThrows() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
        try {
            bytes.append("xx");
            // attempt to backtrack beyond start
            assertThrows(BufferOverflowException.class,
                    () -> bytes.writeSkip(-(bytes.writePosition() + 2)),
                    "Negative skip beyond start triggers overflow exception");
        } finally {
            bytes.releaseLast();
        }
    }
}
