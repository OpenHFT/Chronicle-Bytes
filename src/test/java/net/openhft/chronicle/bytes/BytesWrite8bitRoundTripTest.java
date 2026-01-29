/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests write8bit round-trip behaviour for heap and direct bytes because correct
 * 8-bit string encoding is essential for ISO-8859-1 content storage.
 */
@DisplayName("write8bit round-trip for heap and direct bytes")
public class BytesWrite8bitRoundTripTest extends BytesTestCommon {

    @Test
    @DisplayName("round trip write8bit on heap bytes")
    public void roundTripOnHeap() {
        roundTrip(Bytes.allocateElasticOnHeap());
    }

    @Test
    @DisplayName("round trip write8bit on direct bytes")
    public void roundTripDirect() {
        roundTrip(Bytes.allocateElasticDirect());
    }

    private void roundTrip(Bytes<?> bytes) {
        try {
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
                assertEquals(s, got,
                        "write8bit round trip matches input [" + s + "]");
                // read position should catch up to write
                assertEquals(pos1, bytes.readPosition(),
                        "read position catches up after round trip for [" + s + "]");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
