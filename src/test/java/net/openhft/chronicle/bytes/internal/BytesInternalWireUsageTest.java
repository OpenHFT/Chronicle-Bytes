/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

            assertEquals(text, builder.toString());
            direct.readPosition(utfLength);
            assertEquals(0, direct.readRemaining());
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
            assertEquals("exchange", symbol.toString());

            StringBuilder venue = new StringBuilder();
            BytesInternal.parseUtf8(bytes, venue, StopCharTesters.COMMA_STOP);
            assertEquals("EUREX", venue.toString());

            StringBuilder sideKey = new StringBuilder();
            BytesInternal.parseUtf8(bytes, sideKey, StopCharTesters.EQUALS);
            assertEquals("side", sideKey.toString());

            StringBuilder sideValue = new StringBuilder();
            BytesInternal.parseUtf8(bytes, sideValue, StopCharTesters.ALL);
            assertEquals("SELL", sideValue.toString());

            assertTrue("All bytes consumed", bytes.readRemaining() <= 1);
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
            assertEquals("Best,Trader", quoted.toString());

            // Move past the separator space and confirm remaining text is intact
            bytes.readSkip(1);
            StringBuilder rest = new StringBuilder();
            BytesInternal.parseUtf8(bytes, rest, StopCharTesters.ALL);
            assertEquals("remainder", rest.toString().trim());
        } finally {
            bytes.releaseLast();
        }
    }
}
