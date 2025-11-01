/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesWrite8bitRoundTripTest extends BytesTestCommon {

    @Test
    public void roundTripOnHeap() {
        roundTrip(Bytes.allocateElasticOnHeap());
    }

    @Test
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
                assertEquals(s, got);
                // read position should catch up to write
                assertEquals(pos1, bytes.readPosition());
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

