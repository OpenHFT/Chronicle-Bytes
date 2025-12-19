/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BytesWrite8bitRoundTripTest extends BytesTestCommon {

    @Test
    public void roundTripOnHeap() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            long finalWritePosition = roundTrip(bytes);
            assertTrue(finalWritePosition > 0, "roundTripOnHeap: bytes written");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void roundTripDirect() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        try {
            long finalWritePosition = roundTrip(bytes);
            assertTrue(finalWritePosition > 0, "roundTripDirect: bytes written");
        } finally {
            bytes.releaseLast();
        }
    }

    private long roundTrip(Bytes<?> bytes) {
        String[] names = {
                "", // empty
                "a",
                "helloWorld",
                // ISO-8859-1 content
                "price£",
                // near 255 boundary
                repeat('x', 250)
        };

        for (String s : names) {
            long pos0 = bytes.writePosition();
            bytes.write8bit(s);
            long pos1 = bytes.writePosition();
            bytes.readPosition(pos0);
            String got = bytes.read8bit();
            assertEquals(s, got, "write8bit/read8bit round-trip should preserve string (empty, short, long, ISO-8859-1)");
            // read position should catch up to write
            assertEquals(pos1, bytes.readPosition(), "Read position should advance to write position after read8bit");
        }
        return bytes.writePosition();
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
