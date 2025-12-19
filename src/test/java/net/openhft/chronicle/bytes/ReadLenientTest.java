/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
public class ReadLenientTest extends BytesTestCommon {
    @Test
    public void testLenient() {
        assumeFalse(NativeBytes.areNewGuarded());
        assertEquals(0, doTest(Bytes.allocateDirect(64)), "testLenient: direct readPosition");
        assertEquals(0, doTest(Bytes.allocateElasticOnHeap(64)), "testLenient: heap readPosition");
        assertEquals(0, doTest(Bytes.from("")), "testLenient: empty readPosition");
    }

    private long doTest(Bytes<?> bytes)
            throws BufferUnderflowException, ArithmeticException, IllegalArgumentException {
        try {
            bytes.lenient(true);
            ByteBuffer bb = ByteBuffer.allocateDirect(32);
            bytes.read(bb);
            assertEquals(0, bb.position(), "bb.position");

            assertEquals(BigDecimal.ZERO, bytes.readBigDecimal(), "readBigDecimal value");
            assertEquals(BigInteger.ZERO, bytes.readBigInteger(), "readBigInteger value");
            assertFalse(bytes.readBoolean(), "readBoolean value");
            assertEquals("", bytes.read8bit(), "read8bit should return empty string when no data available in lenient mode");
            assertEquals("", bytes.readUtf8(), "readUtf8 should return empty string when no data available in lenient mode");
            assertEquals(0, bytes.readByte(), "readByte should return 0 when no data available in lenient mode");
            assertEquals(-1, bytes.readUnsignedByte(), "readUnsignedByte value"); // note this behaviour is need to find the end of a stream.
            assertEquals(0, bytes.readShort(), "readShort should return 0 when no data available in lenient mode");
            assertEquals(0, bytes.readUnsignedShort(), "readUnsignedShort value");
            assertEquals(0, bytes.readInt(), "readInt should return 0 when no data available in lenient mode");
            assertEquals(0, bytes.readUnsignedInt(), "readUnsignedInt value");
            assertEquals(0.0, bytes.readFloat(), 0.0, "readFloat should return 0.0 when no data available in lenient mode");
            assertEquals(0.0, bytes.readDouble(), 0.0, "readDouble should return 0.0 when no data available in lenient mode");
            bytes.readSkip(8);
            assertEquals(0, bytes.readPosition(), "read position should remain at 0 in lenient mode when reading from empty buffer");

            return bytes.readPosition();
        } finally {
            bytes.releaseLast();
        }
    }
}
