/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consolidates UTF-8 parsing boundary tests (explicit length, stop-char parsing,
 * null sequences and over-length failures).
 */
public class Utf8ParsingBoundaryTest extends BytesTestCommon {

    @Test
    public void parsesExplicitLengthAtBoundaries() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        String ascii = "A";
        String multi = "£€"; // euro escaped; pound is ISO-8859-1
        try {
            b.append(ascii).append(multi);
            b.readPosition(0);

            StringBuilder sb = new StringBuilder();
            // parse up to the first byte (1 char)
            BytesInternal.parseUtf8(b, sb, true, 1);
            assertEquals(ascii, sb.toString(), "parseUtf8 should extract 'A' when parsing 1 byte at ASCII boundary");

            sb.setLength(0);
            // parse remaining (multi-byte sequence)
            BytesInternal.parseUtf8(b, sb, true, (int) b.readRemaining());
            assertEquals(multi, sb.toString(), "parseUtf8 should extract '£€' when parsing remaining multi-byte UTF-8 sequence");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void parsesWithCommonStopChars() {
        Bytes<?> b = Bytes.from("alpha,beta gamma");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(b, sb, StopCharTesters.COMMA_STOP);
            assertEquals("alpha", sb.toString(), "parseUtf8 should extract 'alpha' when parsing with COMMA_STOP tester");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void nullSequenceEncodesAsMinusOneAndReturnsNegativeOffset() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            b.writeStopBit(-1);
            long res = b.readUtf8Limited(0, new StringBuilder(), 10);
            assertTrue(res < 0, "Expected negative return value signalling null");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void throwsWhenUtf8LengthExceedsMax() {
        assertThrows(net.openhft.chronicle.core.io.ClosedIllegalStateException.class, () -> {
            Bytes<?> b = Bytes.allocateElasticOnHeap(32);
            try {
                String payload = "WXYZ";
                b.writeStopBit(AppendableUtil.findUtf8Length(payload));
                b.append(payload);
                StringBuilder sb = new StringBuilder();
                b.readUtf8Limited(0, sb, 3);
            } finally {
                b.releaseLast();
            }
        });
    }
}
