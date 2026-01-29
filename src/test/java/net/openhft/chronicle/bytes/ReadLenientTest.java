/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests lenient read mode because returning default values on empty input
 * is essential to avoid exceptions when parsing optional fields in
 * variable-length messages.
 */
@SuppressWarnings({"deprecation", "MMOverusedWord"}) // read domain terminology
@DisplayName("Lenient reads return default values on empty input")
public class ReadLenientTest extends BytesTestCommon {
    @Test
    @DisplayName("lenient reads return default values for empty input")
    public void testLenient() {
        assumeFalse(NativeBytes.areNewGuarded(),
                "Native bytes guards must be disabled for lenient read test");
        doTest(Bytes.allocateDirect(64));
        doTest(Bytes.allocateElasticOnHeap(64));
        doTest(Bytes.from(""));
    }

    private void doTest(Bytes<?> bytes)
            throws BufferUnderflowException, ArithmeticException, IllegalArgumentException {
        bytes.lenient(true);
        ByteBuffer bb = ByteBuffer.allocateDirect(32);
        bytes.read(bb);
        assertEquals(0, bb.position(),
                "Buffer position remains zero after lenient read");

        assertEquals(BigDecimal.ZERO, bytes.readBigDecimal(),
                "Lenient read returns zero BigDecimal value");
        assertEquals(BigInteger.ZERO, bytes.readBigInteger(),
                "Lenient read returns zero BigInteger value");
        assertFalse(bytes.readBoolean(),
                "Lenient boolean read returns false value");
        assertEquals("", bytes.read8bit(),
                "Lenient 8bit read returns empty string");
        assertEquals("", bytes.readUtf8(),
                "Lenient UTF8 read returns empty string");
        assertEquals(0, bytes.readByte(),
                "Lenient byte read returns zero value");
        assertEquals(-1, bytes.readUnsignedByte(),
                "Lenient unsigned byte read returns minus one"); // note this behaviour is need to find the end of a stream.
        assertEquals(0, bytes.readShort(),
                "Lenient short read returns zero value");
        assertEquals(0, bytes.readUnsignedShort(),
                "Lenient unsigned short read returns zero value");
        assertEquals(0, bytes.readInt(),
                "Lenient int read returns zero value");
        assertEquals(0, bytes.readUnsignedInt(),
                "Lenient unsigned int read returns zero value");
        assertEquals(0.0, bytes.readFloat(), 0.0,
                "Lenient float read returns zero value");
        assertEquals(0.0, bytes.readDouble(), 0.0,
                "Lenient double read returns zero value");
        bytes.readSkip(8);
        assertEquals(0, bytes.readPosition(),
                "Read position remains zero after lenient skip");

        bytes.releaseLast();
    }
}
