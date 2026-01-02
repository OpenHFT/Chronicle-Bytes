/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BytesInternal parse helpers used by wire formats")
public class BytesInternalWireUsageTest extends BytesTestCommon {

    @Test
    @DisplayName("parse UTF8 using native store optimisation")
    public void parseUtf8UsingNativeStoreOptimisation() {
        Bytes<?> direct = Bytes.allocateElasticDirect(64);
        try {
            String text = "wire-field-name";
            BytesInternal.appendUtf8(direct, text, 0, text.length());
            long utfLength = direct.writePosition();

            StringBuilder builder = new StringBuilder();
            BytesInternal.parseUtf8(direct.bytesStore(), 0L, builder, true, (int) utfLength);

            assertEquals(text, builder.toString(),
                    "Native store parse reads field name");
            direct.readPosition(utfLength);
            assertEquals(0, direct.readRemaining(),
                    "All bytes are consumed after parse");
        } finally {
            direct.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF8 with equals delimiter for queries")
    public void parseUtf8WithEqualsDelimiterMimicsQueryParsers() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            String payload = "exchange=EUREX,side=SELL";
            BytesInternal.appendUtf8(bytes, payload, 0, payload.length());
            bytes.readLimit(bytes.writePosition());
            bytes.readPosition(0);

            StringBuilder symbol = new StringBuilder();
            BytesInternal.parseUtf8(bytes, symbol, StopCharTesters.EQUALS);
            assertEquals("exchange", symbol.toString(),
                    "Equals delimiter stops at exchange key");

            StringBuilder venue = new StringBuilder();
            BytesInternal.parseUtf8(bytes, venue, StopCharTesters.COMMA_STOP);
            assertEquals("EUREX", venue.toString(),
                    "Comma delimiter stops at venue value");

            StringBuilder sideKey = new StringBuilder();
            BytesInternal.parseUtf8(bytes, sideKey, StopCharTesters.EQUALS);
            assertEquals("side", sideKey.toString(),
                    "Equals delimiter stops at side key");

            StringBuilder sideValue = new StringBuilder();
            BytesInternal.parseUtf8(bytes, sideValue, StopCharTesters.ALL);
            assertEquals("SELL", sideValue.toString(),
                    "ALL tester parses remaining side value");

            assertTrue(bytes.readRemaining() <= 1,
                    "All bytes consumed after query parse");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF8 stops at quotes for escaped fields")
    public void parseUtf8StopsAtQuotesForEscapedFields() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            String payload = "\"Best,Trader\" remainder";
            BytesInternal.appendUtf8(bytes, payload, 0, payload.length());
            bytes.readLimit(bytes.writePosition());

            bytes.readPosition(1); // skip leading quote as TextWire/CSVWire do
            StringBuilder quoted = new StringBuilder();
            BytesInternal.parseUtf8(bytes, quoted, StopCharTesters.QUOTES);
            assertEquals("Best,Trader", quoted.toString(),
                    "Quoted field parsed without quotes");

            // Move past the separator space and confirm remaining text is intact
            bytes.readSkip(1);
            StringBuilder rest = new StringBuilder();
            BytesInternal.parseUtf8(bytes, rest, StopCharTesters.ALL);
            assertEquals("remainder", rest.toString().trim(),
                    "Remaining payload is preserved");
        } finally {
            bytes.releaseLast();
        }
    }
}
