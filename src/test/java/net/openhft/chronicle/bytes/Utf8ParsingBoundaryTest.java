/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consolidates UTF-8 parsing boundary tests because explicit length handling,
 * stop-char parsing, null sequences and over-length failures require validation
 * to avoid data corruption or unexpected exceptions during deserialisation.
 */
@DisplayName("Utf8ParsingBoundary - validates UTF-8 length and stop-char boundaries")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class Utf8ParsingBoundaryTest extends BytesTestCommon {

    @Test
    @DisplayName("parses explicit UTF-8 length boundaries for ASCII and multi-byte")
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
            assertEquals(ascii,
                    sb.toString(),
                    "Explicit length should parse the ASCII prefix");

            sb.setLength(0);
            // parse remaining (multi-byte sequence)
            BytesInternal.parseUtf8(b, sb, true, (int) b.readRemaining());
            assertEquals(multi,
                    sb.toString(),
                    "Explicit length should parse the remaining multi-byte sequence");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("parses UTF-8 tokens using common stop characters")
    public void parsesWithCommonStopChars() {
        Bytes<?> b = Bytes.from("alpha,beta gamma");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(b, sb, StopCharTesters.COMMA_STOP);
            assertEquals("alpha",
                    sb.toString(),
                    "Stop character should terminate the parsed token");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("null sequence encodes as -1 and yields negative offset")
    public void nullSequenceEncodesAsMinusOneAndReturnsNegativeOffset() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            b.writeStopBit(-1);
            long res = b.readUtf8Limited(0, new StringBuilder(), 10);
            assertTrue(res < 0,
                    "Null sequence should return a negative offset, but was " + res);
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("over-length UTF-8 payload throws ClosedIllegalStateException")
    public void throwsWhenUtf8LengthExceedsMax() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            String payload = "WXYZ";
            b.writeStopBit(AppendableUtil.findUtf8Length(payload));
            b.append(payload);
            StringBuilder sb = new StringBuilder();
            assertThrows(net.openhft.chronicle.core.io.ClosedIllegalStateException.class,
                    () -> b.readUtf8Limited(0, sb, 3),
                    "Over-length UTF-8 read should raise ClosedIllegalStateException");
        } finally {
            b.releaseLast();
        }
    }
}
