/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Examples showing how to read and write {@code String} values with Chronicle Bytes.
 *
 * <p>This test uses both 8-bit and UTF-8 encoding to illustrate the behaviour
 * of the string pooling when the same text is written and read in different
 * ways.</p>
 */
@DisplayName("String readme examples for pooled UTF8 and 8bit values")
public class StringsTest extends BytesTestCommon {

    /**
     * Demonstrates writing the same text in two encodings and
     * validating that the pooled instances are reused when read back.
     */
    @Test
    @DisplayName("string pooling across 8bit and UTF8 reads")
    public void testString() {
        assumeFalse(NativeBytes.areNewGuarded(),
                "Native bytes guards must be disabled for string pool test");

        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.writeHexDumpDescription("write8bit").write8bit("£ 1");
            bytes.writeHexDumpDescription("writeUtf8").writeUtf8("£ 1");
            bytes.writeHexDumpDescription("append8bit").append8bit("£ 1").append('\n');
            bytes.writeHexDumpDescription("appendUtf8").appendUtf8("£ 1").append('\n');

            // System.out.println(bytes.toHexString());

            final String a = bytes.read8bit();
            final String b = bytes.readUtf8();
            final String c = bytes.parse8bit(StopCharTesters.CONTROL_STOP);
            final String d = bytes.parseUtf8(StopCharTesters.CONTROL_STOP);
            assertEquals("£ 1", a,
                    "Read 8bit string matches written value");
            assertEquals("£ 1", b,
                    "Read UTF8 string matches written value");
            assertEquals("£ 1", c,
                    "Parsed 8bit string matches written value");
            assertEquals("£ 1", d,
                    "Parsed UTF8 string matches written value");

            // System.out.println(System.identityHashCode(a));
            // System.out.println(System.identityHashCode(b));
            // System.out.println(System.identityHashCode(c));
            // System.out.println(System.identityHashCode(d));

            // uses the pool but a different hash.
            // assertSame(a, c); // uses a string pool
            assertSame(b, c,
                    "Parsed 8bit string should reuse pooled UTF8 instance");
            assertSame(b, d,
                    "Parsed UTF8 string should reuse pooled UTF8 instance");
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * Shows that {@code null} strings can be written and read without raising
     * an exception. Both encodings are handled in the same manner.
     */
    @Test
    @DisplayName("null strings round trip in both encodings")
    public void testNull() {
        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.writeHexDumpDescription("write8bit").write8bit((String) null);
            bytes.writeHexDumpDescription("writeUtf8").writeUtf8(null);

            //System.out.println(bytes.toHexString());

            final String a = bytes.read8bit();
            final String b = bytes.readUtf8();
            assertNull(a,
                    "Null 8bit string reads back as null");
            assertNull(b,
                    "Null UTF8 string reads back as null");
        } finally {
            bytes.releaseLast();
        }
    }
}
