/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class StopBitTest extends BytesTestCommon {

    @Test
    public void testStopBit() {

        for (int i = 0; i < (1 << 10) + 1; i++) {
            final String expected = IntStream.range(0, i)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining());

            final Bytes<byte[]> expectedBytes = Bytes.from(expected);

            final Bytes<?> b = Bytes.allocateElastic();
            try {
                long offset = expectedBytes.readPosition();
                long readRemaining = Math.min(b.writeRemaining(), expectedBytes.readLimit() - offset);
                b.writeStopBit(readRemaining);
                try {
                    b.write(expectedBytes, offset, readRemaining);
                } catch (BufferUnderflowException | IllegalArgumentException e) {
                    throw new AssertionError(e);
                }

                assertEquals(expected, b.read8bit(), "failed at " + i);

            } finally {
                b.releaseLast();
                expectedBytes.releaseLast();
            }
        }
    }

    @Test
    public void testStopBitShort() {

        final String s = IntStream.range(0, 1)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining());

        final Bytes<byte[]> bytes = Bytes.from(s);

        final Bytes<?> b = Bytes.allocateElastic();
        try {
            if (bytes == null) {
                b.writeStopBit(-1);
            } else {
                long offset = bytes.readPosition();
                long readRemaining = Math.min(b.writeRemaining(), bytes.readLimit() - offset);
                b.writeStopBit(readRemaining);
                try {
                    b.write(bytes, offset, readRemaining);
                } catch (BufferUnderflowException | IllegalArgumentException e) {
                    throw new AssertionError(e);
                }
            }

            assertEquals(s, b.read8bit(), "read8bit should return string written after stop-bit length");
        } finally {
            bytes.releaseLast();
            b.releaseLast();
        }
    }
}
