/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests stop-bit encoding round trips because this variable-length format
 * must preserve payload integrity across all supported sizes.
 */
@DisplayName("StopBit - validates round-trip encoding with variable payload sizes")
public class StopBitTest extends BytesTestCommon {

    @Test
    @DisplayName("stop-bit encoding round trips with variable payload sizes")
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
                    throw new AssertionError("Stop-bit payload write failed at i=" + i, e);
                }

                // System.out.printf("0x%04x : %02x %02x %02x%n", i, b.readByte(0), b.readByte(1), b.readByte(3));

                assertEquals(expected,
                        b.read8bit(),
                        "Stop-bit decode should match expected string at i=" + i);

            } finally {
                b.releaseLast();
                expectedBytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("stop-bit encoding round trips with single element")
    public void testStopBitShort() {

        final String s = IntStream.range(0, 1)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining());

        final Bytes<byte[]> bytes = Bytes.from(s);

        final Bytes<?> b = Bytes.allocateElastic();
        try {
            long offset = bytes.readPosition();
            long readRemaining = Math.min(b.writeRemaining(), bytes.readLimit() - offset);
            b.writeStopBit(readRemaining);
            try {
                b.write(bytes, offset, readRemaining);
            } catch (BufferUnderflowException | IllegalArgumentException e) {
                throw new AssertionError("Stop-bit short payload write failed", e);
            }

            assertEquals(s,
                    b.read8bit(),
                    "Stop-bit decode should match the single-element string");
        } finally {
            bytes.releaseLast();
            b.releaseLast();
        }
    }
}
