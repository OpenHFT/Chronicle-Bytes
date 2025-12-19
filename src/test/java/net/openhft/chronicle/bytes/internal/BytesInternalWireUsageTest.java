/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BytesInternalWireUsageTest extends BytesTestCommon {

    @Test
    public void parseUtf8UsingNativeStoreOptimisation() {
        Bytes<?> direct = Bytes.allocateElasticDirect(64);
        try {
            String text = "wire-field-name";
            BytesInternal.appendUtf8(direct, text, 0, text.length());
            long utfLength = direct.writePosition();

            StringBuilder builder = new StringBuilder();
            BytesInternal.parseUtf8(direct.bytesStore(), 0L, builder, true, (int) utfLength);

            assertEquals(text, builder.toString(), "native store parseUtf8 should produce 'wire-field-name'");
            direct.readPosition(utfLength);
            assertEquals(0, direct.readRemaining(), "direct.readRemaining");
        } finally {
            direct.releaseLast();
        }
    }

    @Test
    public void parseUtf8WithEqualsDelimiterMimicsQueryParsers() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            String payload = "exchange=EUREX,side=SELL";
            BytesInternal.appendUtf8(bytes, payload, 0, payload.length());
            bytes.readLimit(bytes.writePosition());
            bytes.readPosition(0);

            StringBuilder symbol = new StringBuilder();
            BytesInternal.parseUtf8(bytes, symbol, StopCharTesters.EQUALS);
            assertEquals("exchange", symbol.toString(), "parsing with EQUALS delimiter should extract 'exchange' key");

            StringBuilder venue = new StringBuilder();
            BytesInternal.parseUtf8(bytes, venue, StopCharTesters.COMMA_STOP);
            assertEquals("EUREX", venue.toString(), "parsing with COMMA_STOP delimiter should extract 'EUREX' value");

            StringBuilder sideKey = new StringBuilder();
            BytesInternal.parseUtf8(bytes, sideKey, StopCharTesters.EQUALS);
            assertEquals("side", sideKey.toString(), "parsing second field with EQUALS delimiter should extract 'side' key");

            StringBuilder sideValue = new StringBuilder();
            BytesInternal.parseUtf8(bytes, sideValue, StopCharTesters.ALL);
            assertEquals("SELL", sideValue.toString(), "parsing with ALL delimiter should extract 'SELL' value");

            assertTrue(bytes.readRemaining() <= 1, "All bytes consumed");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void parseUtf8StopsAtQuotesForEscapedFields() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            String payload = "\"Best,Trader\" remainder";
            BytesInternal.appendUtf8(bytes, payload, 0, payload.length());
            bytes.readLimit(bytes.writePosition());

            bytes.readPosition(1); // skip leading quote as TextWire/CSVWire do
            StringBuilder quoted = new StringBuilder();
            BytesInternal.parseUtf8(bytes, quoted, StopCharTesters.QUOTES);
            assertEquals("Best,Trader", quoted.toString(), "parsing quoted field with QUOTES delimiter should extract 'Best,Trader'");

            // Move past the separator space and confirm remaining text is intact
            bytes.readSkip(1);
            StringBuilder rest = new StringBuilder();
            BytesInternal.parseUtf8(bytes, rest, StopCharTesters.ALL);
            assertEquals("remainder", rest.toString().trim(), "parsing remainder after quoted field should extract 'remainder'");
        } finally {
            bytes.releaseLast();
        }
    }
}
