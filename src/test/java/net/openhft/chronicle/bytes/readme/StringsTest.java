/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Examples showing how to read and write {@code String} values with Chronicle Bytes.
 *
 * <p>This test uses both 8-bit and UTF-8 encoding to illustrate the behaviour
 * of the string pooling when the same text is written and read in different
 * ways.</p>
 */
@SuppressWarnings("deprecation")
public class StringsTest extends BytesTestCommon {

    /**
     * Demonstrates writing the same text in two encodings and
     * validating that the pooled instances are reused when read back.
     */
    @Test
    public void testString() {
        assumeFalse(NativeBytes.areNewGuarded());

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
            assertEquals("£ 1", a, "read8bit should decode 8-bit encoded string");
            assertEquals("£ 1", b, "readUtf8 should decode UTF-8 encoded string");
            assertEquals("£ 1", c, "parse8bit should decode appended 8-bit string");
            assertEquals("£ 1", d, "parseUtf8 should decode appended UTF-8 string");

            // System.out.println(System.identityHashCode(a));
            // System.out.println(System.identityHashCode(b));
            // System.out.println(System.identityHashCode(c));
            // System.out.println(System.identityHashCode(d));

            // uses the pool but a different hash.
            // assertSame(a, c); // uses a string pool
            assertSame(b, c, "readUtf8 and parse8bit should return same pooled instance"); // uses a string pool
            assertSame(b, d, "readUtf8 and parseUtf8 should return same pooled instance"); // uses a string pool
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * Shows that {@code null} strings can be written and read without raising
     * an exception. Both encodings are handled in the same manner.
     */
    @Test
    public void testNull() {
        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.writeHexDumpDescription("write8bit").write8bit(null);
            bytes.writeHexDumpDescription("writeUtf8").writeUtf8(null);

            //System.out.println(bytes.toHexString());

            final String a = bytes.read8bit();
            final String b = bytes.readUtf8();
            assertNull(a, "read8bit should return null when null was written");
            assertNull(b, "readUtf8 should return null when null was written");
        } finally {
            bytes.releaseLast();
        }
    }
}
