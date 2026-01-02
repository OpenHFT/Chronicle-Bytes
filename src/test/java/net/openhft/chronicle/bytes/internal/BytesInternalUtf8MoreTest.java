/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BytesInternal UTF8 edge cases and multibyte parsing")
public class BytesInternalUtf8MoreTest extends BytesTestCommon {

    @Test
    @DisplayName("append UTF8 handles latin1 multibyte characters")
    public void appendUtf8WithLatin1MultibyteChars() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(64);
        try {
            String s = "ab£écd"; // contains '£' and 'é'
            BytesInternal.appendUtf8(out, s, 0, s.length());
            // Bytes.toString decodes ISO-8859-1; compare using the same codec on the UTF-8 bytes
            String expected = new String(s.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.charset.StandardCharsets.ISO_8859_1);
            assertEquals(expected, out.toString(),
                    "UTF8 append preserves latin1 multibyte chars");
        } finally {
            out.releaseLast();
        }
    }

    @Test
    @DisplayName("append UTF8 handles supplementary characters correctly in output")
    public void appendUtf8ToRandomDataOutputHandlesSupplementaryChars() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(64);
        try {
            String text = "ascii £ €";
            long endOffset = BytesInternal.appendUtf8(out, out.writePosition(), text, 0, text.length());
            out.writePosition(endOffset);
            out.readLimit(endOffset);
            out.readPosition(0);
            byte[] actual = BytesInternal.toByteArray(out);
            assertEquals(text, new String(actual, StandardCharsets.UTF_8),
                    "UTF8 append preserves supplementary characters");
            assertEquals(endOffset, out.writePosition(),
                    "Write position advances to end offset");
        } finally {
            out.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF8 honours explicit length and flag")
    public void parseUtf8WithExplicitLengthHonoursUtfFlag() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            String text = "£elastic";
            BytesInternal.appendUtf8(bytes, text, 0, text.length());
            bytes.readLimit(bytes.writePosition());
            bytes.readPosition(0);

            StringBuilder utfBuilder = new StringBuilder();
            BytesInternal.parseUtf8(bytes, utfBuilder, true, (int) bytes.readRemaining());
            assertEquals(text, utfBuilder.toString(),
                    "UTF8 parse preserves text with utf flag");

            bytes.readPosition(0);
            StringBuilder latinBuilder = new StringBuilder();
            BytesInternal.parseUtf8(bytes, latinBuilder, false, (int) bytes.readRemaining());
            assertEquals(text, latinBuilder.toString(),
                    "UTF8 parse preserves text without utf flag");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF8 stops at tester boundary")
    public void parseUtf8StopsAtTesterBoundary() {
        Bytes<?> source = Bytes.allocateElasticOnHeap(64);
        try {
            String payload = "token1,token2";
            BytesInternal.appendUtf8(source, payload, 0, payload.length());
            source.readLimit(source.writePosition());
            source.readPosition(0);

            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(source, sb, StopCharTesters.COMMA_STOP);
            assertEquals("token1", sb.toString(),
                    "Parser stops at comma boundary");
            assertTrue(source.readRemaining() > 0,
                    "Remaining bytes exist after stop tester");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF8 into bytes builder output")
    public void parseUtf8IntoBytesBuilder() {
        Bytes<?> t1 = Bytes.from("token1");
        Bytes<?> t2 = Bytes.from("token2");
        Bytes<?> builder = Bytes.allocateElasticOnHeap(32);
        try {
            BytesInternal.parseUtf8(t1, builder, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("token1", builder.toString(),
                    "Parser writes token1 into bytes builder");
            builder.clear();
            BytesInternal.parseUtf8(t2, builder, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("token2", builder.toString(),
                    "Parser writes token2 into bytes builder");
        } finally {
            builder.releaseLast();
            t1.releaseLast();
            t2.releaseLast();
        }
    }
}
