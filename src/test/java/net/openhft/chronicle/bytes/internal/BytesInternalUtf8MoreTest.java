/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BytesInternalUtf8MoreTest extends BytesTestCommon {

    @Test
    public void appendUtf8WithLatin1MultibyteChars() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(64);
        try {
            String s = "ab£écd"; // contains '£' and 'é'
            BytesInternal.appendUtf8(out, s, 0, s.length());
            // Bytes.toString decodes ISO-8859-1; compare using the same codec on the UTF-8 bytes
            String expected = new String(s.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.charset.StandardCharsets.ISO_8859_1);
            assertEquals(expected, out.toString());
        } finally {
            out.releaseLast();
        }
    }

    @Test
    public void appendUtf8ToRandomDataOutputHandlesSupplementaryChars() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(64);
        try {
            String text = "ascii £ €";
            long endOffset = BytesInternal.appendUtf8(out, out.writePosition(), text, 0, text.length());
            out.writePosition(endOffset);
            out.readLimit(endOffset);
            out.readPosition(0);
            byte[] actual = BytesInternal.toByteArray(out);
            assertEquals(text, new String(actual, StandardCharsets.UTF_8));
            assertEquals(endOffset, out.writePosition());
        } finally {
            out.releaseLast();
        }
    }

    @Test
    public void parseUtf8WithExplicitLengthHonoursUtfFlag() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            String text = "£elastic";
            BytesInternal.appendUtf8(bytes, text, 0, text.length());
            bytes.readLimit(bytes.writePosition());
            bytes.readPosition(0);

            StringBuilder utfBuilder = new StringBuilder();
            BytesInternal.parseUtf8(bytes, utfBuilder, true, (int) bytes.readRemaining());
            assertEquals(text, utfBuilder.toString());

            bytes.readPosition(0);
            StringBuilder latinBuilder = new StringBuilder();
            BytesInternal.parseUtf8(bytes, latinBuilder, false, (int) bytes.readRemaining());
            assertEquals(text, latinBuilder.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void parseUtf8StopsAtTesterBoundary() {
        Bytes<?> source = Bytes.allocateElasticOnHeap(64);
        try {
            String payload = "token1,token2";
            BytesInternal.appendUtf8(source, payload, 0, payload.length());
            source.readLimit(source.writePosition());
            source.readPosition(0);

            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(source, sb, StopCharTesters.COMMA_STOP);
            assertEquals("token1", sb.toString());
            assertTrue(source.readRemaining() > 0);
        } finally {
            source.releaseLast();
        }
    }

    @Test
    public void parseUtf8IntoBytesBuilder() {
        Bytes<?> t1 = Bytes.from("token1");
        Bytes<?> t2 = Bytes.from("token2");
        Bytes<?> builder = Bytes.allocateElasticOnHeap(32);
        try {
            BytesInternal.parseUtf8(t1, builder, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("token1", builder.toString());
            builder.clear();
            BytesInternal.parseUtf8(t2, builder, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("token2", builder.toString());
        } finally {
            builder.releaseLast();
            t1.releaseLast();
            t2.releaseLast();
        }
    }
}
