/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BytesBoundsAndLimitsTest extends BytesTestCommon {

    @Test
    @DisplayName("write beyond writeLimit throws overflow exception")
    public void writeBeyondWriteLimitThrows() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            b.writeLimit(4);
            assertThrows(BufferOverflowException.class,
                    () -> b.writeLong(1L),
                    "writeLong beyond writeLimit throws overflow");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("read beyond readLimit throws underflow exception")
    public void readBeyondReadLimitThrows() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            b.writeInt(123);
            b.readPosition(0);
            assertThrows(BufferUnderflowException.class,
                    b::readLong,
                    "readLong beyond readLimit throws underflow");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("clear resets flags and zeroOut clears bytes")
    public void clearIsClearAndZeroOut() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(16);
        try {
            assertTrue(b.isClear(),
                    "New Bytes instance starts in clear state");
            b.writeLimit(b.capacity() - 1);
            assertFalse(b.isClear(),
                    "Non-default writeLimit clears clear flag");
            b.clear();
            assertTrue(b.isClear(),
                    "clear restores clear flag");

            b.append("abcdef");
            b.zeroOut(0, 6);
            for (int i = 0; i < 6; i++) {
                assertEquals(0, b.peekUnsignedByte(i),
                        "zeroOut clears byte at index " + i);
            }
        } finally {
            b.releaseLast();
        }
    }
}
