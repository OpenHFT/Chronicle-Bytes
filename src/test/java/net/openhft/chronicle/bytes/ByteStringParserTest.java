/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.StopCharTesters.CONTROL_STOP;
import static net.openhft.chronicle.bytes.StopCharTesters.SPACE_STOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ByteStringParserTest extends BytesTestCommon {
    @NotNull
    private
    Bytes<?> bytes = Bytes.allocateElastic();

    @Override
    public void afterChecks() {
        bytes.releaseLast();

        super.afterChecks();
    }

    @Test
    @DisplayName("parseLong reads appended long values correctly")
    public void testParseLong() {
        long expected = 123456789012345678L;
        bytes.append(expected);
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap((int) bytes.readRemaining());

        assertEquals(expected, bytes.parseLong(0),
                "parseLong(offset) should read the appended value");
        assertEquals(expected, BytesInternal.parseLong(bytes),
                "BytesInternal.parseLong should read the appended value");

        bytes2.append(expected);
        assertEquals(expected, bytes2.parseLong(0),
                "parseLong(offset) should read the value from heap bytes");
        assertEquals(expected, BytesInternal.parseLong(bytes2),
                "BytesInternal.parseLong should read the value from heap bytes");
        bytes2.releaseLast();

    }

    @Test
    @DisplayName("parseInt reads appended int values correctly")
    public void testParseInt() {
        int expected = 123;
        bytes.append(expected);

        assertEquals(expected, BytesInternal.parseLong(bytes),
                "BytesInternal.parseLong should parse the appended int");
    }

    @Test
    @DisplayName("parseDouble reads appended double values correctly")
    public void testParseDouble() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for double parsing");
        double expected = 123.1234;
        bytes.append(expected);

        assertEquals(expected, BytesInternal.parseDouble(bytes), 0,
                "BytesInternal.parseDouble should parse the appended double");
    }

    @Test
    @DisplayName("parseFloat reads appended float values correctly")
    public void testParseFloat() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for float parsing");
        float expected = 123;
        bytes.append(expected);

        assertEquals(expected, BytesInternal.parseDouble(bytes), 0,
                "BytesInternal.parseDouble should parse the appended float");
    }

    @Test
    @DisplayName("parseShort reads appended short values correctly")
    public void testParseShort() {
        short expected = 123;
        bytes.append(expected);

        assertEquals(expected, BytesInternal.parseLong(bytes),
                "BytesInternal.parseLong should parse the appended short");
    }

    @Test
    @DisplayName("append and parse round-trip mixed values")
    public void testAppendParse()
            throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for mixed parsing");
        bytes.write("word£€) ".getBytes(StandardCharsets.UTF_8));
        bytes.append("word£€)").append(' ');
        bytes.append(1234).append(' ');
        bytes.append(123456L).append(' ');
        bytes.append(1.2345).append(' ');
        bytes.append(0.0012345).append(' ');

        assertEquals("word£€)", bytes.parseUtf8(SPACE_STOP),
                "parseUtf8 should read the first UTF-8 token");
        assertEquals("word£€)", bytes.parseUtf8(SPACE_STOP),
                "parseUtf8 should read the second UTF-8 token");
        assertEquals(1234, bytes.parseLong(),
                "parseLong should read the appended integer");
        assertEquals(123456L, bytes.parseLong(),
                "parseLong should read the appended long");
        assertEquals(1.2345, bytes.parseDouble(), 0,
                "parseDouble should read the appended double");
        assertEquals(0.0012345, bytes.parseDouble(), 0,
                "parseDouble should read the appended fractional double");
    }

    @Test
    @DisplayName("lastDecimalPlaces reflects long decimal input tokens")
    public void testLastDecimalPlacesLong() throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for long decimals");
        appendVariousNumbers();
        assertEquals(1, bytes.parseLongDecimal(),
                "parseLongDecimal should read first decimal token \"1\"");
        assertEquals(0, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be zero for an integer");

        assertEquals(1, bytes.parseLongDecimal(),
                "parseLongDecimal should read second decimal token \"1.\"");
        assertEquals(0, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should remain zero for an integer");

        assertEquals(0, bytes.parseLongDecimal(),
                "parseLongDecimal should read third decimal token \"0.0\"");
        assertEquals(1, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be one for a single decimal place");

        assertEquals(1, bytes.parseLongDecimal(),
                "parseLongDecimal should read fourth decimal token \"+0.1\"");
        assertEquals(1, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should remain one for a single decimal place");

        assertEquals(11, bytes.parseLongDecimal(),
                "parseLongDecimal should read fifth decimal token \"1.1\"");
        assertEquals(1, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should remain one after decimal input");

        assertEquals(-128, bytes.parseLongDecimal(),
                "parseLongDecimal should read the sixth value");
        assertEquals(2, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be two for -1.28 in long parsing");

        assertEquals(110, bytes.parseLongDecimal(),
                "parseLongDecimal should read the seventh value");
        assertEquals(2, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be two for 1.10 in long parsing");

        assertEquals(110000, bytes.parseLongDecimal(),
                "parseLongDecimal should read the eighth value");
        assertEquals(5, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be five for 1.10000 in long parsing");
    }

    @Test
    @DisplayName("lastDecimalPlaces reflects double decimal input tokens")
    public void testLastDecimalPlacesDouble() throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for double decimals");
        appendVariousNumbers();

        assertEquals(1, bytes.parseDouble(), 0,
                "parseDouble should read first decimal token \"1\"");
        assertEquals(0, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be zero for 1");

        assertEquals(1., bytes.parseDouble(), 0,
                "parseDouble should read second decimal token \"1.\"");
        assertEquals(0, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be zero for 1.");

        assertEquals(0.0, bytes.parseDouble(), 0,
                "parseDouble should read the third value");
        assertEquals(1, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be one for 0.0");

        assertEquals(0.1, bytes.parseDouble(), 0,
                "parseDouble should read the fourth value");
        assertEquals(1, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be one for 0.1");

        assertEquals(1.1, bytes.parseDouble(), 0,
                "parseDouble should read the fifth value");
        assertEquals(1, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be one for 1.1");

        assertEquals(-1.28, bytes.parseDouble(), 0,
                "parseDouble should read the sixth value");
        assertEquals(2, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be two for -1.28 in double parsing");

        assertEquals(1.10, bytes.parseDouble(), 0,
                "parseDouble should read the seventh value");
        assertEquals(2, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be two for 1.10 in double parsing");

        assertEquals(1.10000, bytes.parseDouble(), 0,
                "parseDouble should read the eighth value");
        assertEquals(5, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be five for 1.10000 in double parsing");

        assertEquals(0.01, bytes.parseDouble(), 0,
                "parseDouble should read the ninth value");
        assertEquals(2, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be two for 1E-2");

        assertEquals(100, bytes.parseDouble(), 0,
                "parseDouble should read the tenth value");
        assertEquals(0, bytes.lastDecimalPlaces(),
                "lastDecimalPlaces should be zero for 1E+2");
    }

    private void appendVariousNumbers() {
        bytes.append("1").append(' ');
        bytes.append("1.").append(' ');
        bytes.append("0.0").append(' ');
        bytes.append("+0.1").append(' ');
        bytes.append("1.1").append(' ');
        bytes.append("-1.28").append(' ');
        bytes.append("1.10").append(' ');
        bytes.append("1.10000").append(' ');
        bytes.append("1E-2").append(' ');
        bytes.append("1E+2").append(' ');
        bytes.readPosition(0);
    }

    @Test
    @DisplayName("append and parse UTF-8 tokens with stop chars")
    public void testAppendParseUTF() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for UTF-8 parsing");
        @NotNull String[] words = "Hello,World!,Bye£€!".split(",");
        for (@NotNull String word : words) {
            bytes.append(word).append('\t');
        }
        bytes.append('\t');

        for (String word : words) {
            assertEquals(word, bytes.parseUtf8(CONTROL_STOP),
                    "parseUtf8 should return UTF-8 token \"" + word + "\"");
        }
        assertEquals("", bytes.parseUtf8(CONTROL_STOP),
                "parseUtf8 should return empty string after final token");

        bytes.readPosition(0);
        @NotNull StringBuilder sb = new StringBuilder();
        for (String word : words) {
            bytes.parseUtf8(sb, CONTROL_STOP);
            assertEquals(word, sb.toString(),
                    "parseUtf8 should return control-delimited word \"" + word + "\"");
        }
        bytes.parseUtf8(sb, CONTROL_STOP);
        assertEquals("", sb.toString(),
                "parseUtf8 should return empty string after final delimiter");

        bytes.readPosition(0);
        bytes.skipTo(CONTROL_STOP);
        assertEquals(6, bytes.readPosition(),
                "skipTo should position after first control delimiter");
        bytes.skipTo(CONTROL_STOP);
        assertEquals(13, bytes.readPosition(),
                "skipTo should position after second control delimiter");
        assertTrue(bytes.skipTo(CONTROL_STOP),
                "skipTo should find the third control delimiter");
        assertEquals(23, bytes.readPosition(),
                "skipTo should position after third control delimiter");
        assertTrue(bytes.skipTo(CONTROL_STOP),
                "skipTo should find the final control delimiter");
        assertEquals(24, bytes.readPosition(),
                "skipTo should position after final control delimiter");
        assertFalse(bytes.skipTo(CONTROL_STOP),
                "skipTo should return false when no delimiter remains");
    }

    @Test
    @DisplayName("parseUtf8 reads appended substring segments correctly")
    public void testAppendSubstring() {
        bytes.append("Hello World", 2, 7).append("\n");

        assertEquals("Hello World".substring(2, 7), bytes.parseUtf8(CONTROL_STOP),
                "parseUtf8 should read the appended substring");
    }

    @Test
    @DisplayName("writeBytes merges segments into expected text")
    public void testWriteBytes() {
        bytes.write("Hello World\n".getBytes(ISO_8859_1), 0, 10);
        bytes.write("good bye\n".getBytes(ISO_8859_1), 4, 4);
        bytes.write(4, "0 w".getBytes(ISO_8859_1));

        assertEquals("Hell0 worl bye", bytes.parseUtf8(CONTROL_STOP),
                "write operations should produce the expected merged output");
    }

    @Test
    @DisplayName("parseFlexibleLong handles integer and decimal inputs")
    public void testFlexibleLong() {
        // Test regular longs
        verifyFlexibleLong("0", 0L);
        verifyFlexibleLong("1", 1L);
        verifyFlexibleLong("-1", -1L);
        verifyFlexibleLong("6432643", 6432643L);
        verifyFlexibleLong("-16432620987", -16432620987L);
        verifyFlexibleLong("27209782482844", 27209782482844L);
        verifyFlexibleLong("-37218967980573232", -37218967980573232L);
        verifyFlexibleLong(String.valueOf(Long.MAX_VALUE - 20), Long.MAX_VALUE - 20);
        verifyFlexibleLong(String.valueOf(Long.MIN_VALUE + 20), Long.MIN_VALUE + 20);
        verifyFlexibleLong(String.valueOf(Long.MAX_VALUE - 3), Long.MAX_VALUE - 3);
        verifyFlexibleLong(String.valueOf(Long.MIN_VALUE + 3), Long.MIN_VALUE + 3);
        verifyFlexibleLong(String.valueOf(Long.MAX_VALUE), Long.MAX_VALUE);
        verifyFlexibleLong(String.valueOf(Long.MIN_VALUE), Long.MIN_VALUE);

        // Test regular longs with a point, with varying number of zeros
        verifyFlexibleLong("0.0", 0L);
        verifyFlexibleLong("1.000", 1L);
        verifyFlexibleLong("-0001.00000", -1L);
        verifyFlexibleLong("6432643.0", 6432643L);
        verifyFlexibleLong("-16432620987.", -16432620987L);
        verifyFlexibleLong(String.valueOf(Long.MAX_VALUE - 20) + ".0", Long.MAX_VALUE - 20);
        verifyFlexibleLong(String.valueOf(Long.MIN_VALUE + 20) + ".0", Long.MIN_VALUE + 20);
        verifyFlexibleLong(String.valueOf(Long.MAX_VALUE - 3) + ".0", Long.MAX_VALUE - 3);
        verifyFlexibleLong(String.valueOf(Long.MIN_VALUE + 3) + ".0", Long.MIN_VALUE + 3);

    }

    @Test
    @DisplayName("parseFlexibleLong handles scientific and edge values")
    public void testFlexibleLong2() {

        verifyFlexibleLong(String.valueOf(Long.MAX_VALUE) + ".0", Long.MAX_VALUE);
        verifyFlexibleLong(String.valueOf(Long.MIN_VALUE) + ".0", Long.MIN_VALUE);

        // Test scientific format
        verifyFlexibleLong("1e1", 10L);
        verifyFlexibleLong("1E1", 10L);
        verifyFlexibleLong("-1E1", -10L);
        verifyFlexibleLong("-4E6", -4000000L);
        verifyFlexibleLong("100E10", 1000000000000L);
        verifyFlexibleLong("9E12", 9000000000000L);
        verifyFlexibleLong("6410269E3", 6410269000L);
        verifyFlexibleLong("5000000000E-3", 5000000L);
        verifyFlexibleLong(String.valueOf(Long.MAX_VALUE) + "e0", Long.MAX_VALUE);
        verifyFlexibleLong(String.valueOf(Long.MIN_VALUE) + "E0", Long.MIN_VALUE);
        verifyFlexibleLong(String.valueOf(Long.MAX_VALUE) + "000000e-6", Long.MAX_VALUE);
        verifyFlexibleLong(String.valueOf(Long.MIN_VALUE) + "00000000000000000000E-20", Long.MIN_VALUE);
        verifyFlexibleLong("0.000000000000000000000000000001E33", 1000L);
        verifyFlexibleLong("789000000000000000000000000000000E-25", 78900000L);

        // Test values outside long range
        verifyFlexibleLongRejects("9E40", "overflowing exponent");
        verifyFlexibleLongRejects("-8473289704324748391027491830", "overflowing digits");
        verifyFlexibleLongRejects(String.valueOf(Long.MAX_VALUE) + "0", "overflowing magnitude");

        // Test rounded fractional numbers
        verifyFlexibleLongRejects("0.1", "rounded fractional value");
        verifyFlexibleLongRejects("1e-2", "rounded scientific fraction");
        verifyFlexibleLongRejects("0.9", "rounded fractional value");

    }

    @Test
    @DisplayName("parseFlexibleLong rejects fractional and special values")
    public void testFlexibleLong3() {

        verifyFlexibleLongRejects("0.9", "rounded fractional value");
        verifyFlexibleLongRejects("56765e-2", "rounded scientific fraction");
        verifyFlexibleLongRejects("-0.1", "rounded fractional value");
        verifyFlexibleLongRejects("-0.9", "rounded fractional value");
        verifyFlexibleLongRejects("4.4000000000000000000000000000001E1", "fractional overflow");
        verifyFlexibleLongRejects("4.3999999999999999999999999999991E1", "fractional overflow");
        verifyFlexibleLongRejects(String.valueOf(Long.MAX_VALUE) + ".1", "overflowing fractional long");
        verifyFlexibleLongRejects(String.valueOf(Long.MAX_VALUE - 1) + ".9", "overflowing fractional long");
        verifyFlexibleLongRejects(String.valueOf(Long.MAX_VALUE - 1) + ".1", "overflowing fractional long");
        verifyFlexibleLongRejects(String.valueOf(Long.MIN_VALUE) + ".1", "overflowing fractional long");
        verifyFlexibleLongRejects(String.valueOf(Long.MIN_VALUE + 1) + ".9", "overflowing fractional long");
        verifyFlexibleLongRejects(String.valueOf(Long.MIN_VALUE + 1) + ".1", "overflowing fractional long");
        verifyFlexibleLongRejects(String.valueOf(Long.MIN_VALUE + 5) + ".1", "overflowing fractional long");
        verifyFlexibleLongRejects(String.valueOf(Long.MIN_VALUE + 5) + ".9", "overflowing fractional long");

        // Test extreme double values
        verifyFlexibleLongRejects("Infinity", "infinite value");
        verifyFlexibleLongRejects("-Infinity", "infinite value");
        verifyFlexibleLongRejects("+Infinity", "infinite value");
        verifyFlexibleLongRejects("NaN", "NaN value");

        // Test regular longs again - to check that input is properly consumed
        verifyFlexibleLong("0", 0L);
        verifyFlexibleLong("1", 1L);
        verifyFlexibleLong("-1", -1L);
    }

    private void verifyFlexibleLong(String input, long expected) {
        bytes.append(input).append(' ');
        assertEquals(expected, bytes.parseFlexibleLong(),
                "parseFlexibleLong should parse \"" + input + "\"");
    }

    private void verifyFlexibleLongRejects(String input, String reason) {
        bytes.append(input).append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong(),
                "parseFlexibleLong should reject " + reason + " input \"" + input + "\"");
    }
}
