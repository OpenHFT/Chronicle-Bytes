/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.StopCharTesters.CONTROL_STOP;
import static net.openhft.chronicle.bytes.StopCharTesters.SPACE_STOP;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
public class ByteStringParserTest extends BytesTestCommon {
    @NotNull
    private final
    Bytes<?> bytes = Bytes.allocateElastic();

    @AfterEach
    @Override
    public void afterChecks() {
        bytes.releaseLast();

        super.afterChecks();
    }

    @Test
    public void testParseLong() {
        long expected = 123456789012345678L;
        bytes.append(expected);
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap((int) bytes.readRemaining());

        assertEquals(expected, bytes.parseLong(0), "parseLong should return 123456789012345678 for off-heap bytes");
        assertEquals(expected, BytesInternal.parseLong(bytes), "BytesInternal.parseLong should return 123456789012345678 for off-heap bytes");

        bytes2.append(expected);
        assertEquals(expected, bytes2.parseLong(0), "parseLong should return 123456789012345678 for on-heap bytes");
        assertEquals(expected, BytesInternal.parseLong(bytes2), "BytesInternal.parseLong should return 123456789012345678 for on-heap bytes");
        bytes2.releaseLast();

    }

    @Test
    public void testParseInt() {
        int expected = 123;
        bytes.append(expected);

        assertEquals(expected, BytesInternal.parseLong(bytes), "BytesInternal.parseLong should return 123 for integer value");
    }

    @Test
    public void testParseDouble() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        double expected = 123.1234;
        bytes.append(expected);

        assertEquals(expected, BytesInternal.parseDouble(bytes), 0, "BytesInternal.parseDouble should return 123.1234 for double value");
    }

    @Test
    public void testParseFloat() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        float expected = 123;
        bytes.append(expected);

        assertEquals(expected, BytesInternal.parseDouble(bytes), 0, "BytesInternal.parseDouble should return 123.0 for float value");
    }

    @Test
    public void testParseShort() {
        short expected = 123;
        bytes.append(expected);

        assertEquals(expected, BytesInternal.parseLong(bytes), "BytesInternal.parseLong should return 123 for short value");
    }

    @Test
    public void testAppendParse()
            throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        bytes.write("word£€) ".getBytes(StandardCharsets.UTF_8));
        bytes.append("word£€)").append(' ');
        bytes.append(1234).append(' ');
        bytes.append(123456L).append(' ');
        bytes.append(1.2345).append(' ');
        bytes.append(0.0012345).append(' ');

        assertEquals("word£€)", bytes.parseUtf8(SPACE_STOP), "parseUtf8 should return 'word£€)' from raw bytes");
        assertEquals("word£€)", bytes.parseUtf8(SPACE_STOP), "parseUtf8 should return 'word£€)' from appended string");
        assertEquals(1234, bytes.parseLong(), "parseLong should return 1234 for integer value");
        assertEquals(123456L, bytes.parseLong(), "parseLong should return 123456 for long value");
        assertEquals(1.2345, bytes.parseDouble(), 0, "parseDouble should return 1.2345 for decimal value");
        assertEquals(0.0012345, bytes.parseDouble(), 0, "parseDouble should return 0.0012345 for scientific notation");
    }

    @Test
    public void testLastDecimalPlacesLong() throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        appendVariousNumbers();
        assertEquals(1, bytes.parseLongDecimal(), "parseLongDecimal should return 1 for input '1'");
        assertEquals(0, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 0 for integer without decimal point");

        assertEquals(1, bytes.parseLongDecimal(), "parseLongDecimal should return 1 for input '1.'");
        assertEquals(0, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 0 for decimal point with no fractional digits");

        assertEquals(0, bytes.parseLongDecimal(), "parseLongDecimal should return 0 for input '0.0'");
        assertEquals(1, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 1 for single decimal place");

        assertEquals(1, bytes.parseLongDecimal(), "parseLongDecimal should return 1 for input '+0.1'");
        assertEquals(1, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 1 for positive decimal with sign");

        assertEquals(11, bytes.parseLongDecimal(), "parseLongDecimal should return 11 for input '1.1'");
        assertEquals(1, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 1 for single decimal digit");

        assertEquals(-128, bytes.parseLongDecimal(), "parseLongDecimal should return -128 for input '-1.28'");
        assertEquals(2, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 2 for negative decimal with two places");

        assertEquals(110, bytes.parseLongDecimal(), "parseLongDecimal should return 110 for input '1.10'");
        assertEquals(2, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 2 for trailing zero in decimal");

        assertEquals(110000, bytes.parseLongDecimal(), "parseLongDecimal should return 110000 for input '1.10000'");
        assertEquals(5, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 5 for five trailing zeros");
    }

    @Test
    public void testLastDecimalPlacesDouble() throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        appendVariousNumbers();

        assertEquals(1, bytes.parseDouble(), 0, "parseDouble should return 1.0 for input '1'");
        assertEquals(0, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 0 for integer without decimal point");

        assertEquals(1., bytes.parseDouble(), 0, "parseDouble should return 1.0 for input '1.'");
        assertEquals(0, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 0 for decimal point with no fractional digits");

        assertEquals(0.0, bytes.parseDouble(), 0, "parseDouble should return 0.0 for input '0.0'");
        assertEquals(1, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 1 for single decimal place");

        assertEquals(0.1, bytes.parseDouble(), 0, "parseDouble should return 0.1 for input '+0.1'");
        assertEquals(1, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 1 for positive decimal with sign");

        assertEquals(1.1, bytes.parseDouble(), 0, "parseDouble should return 1.1 for input '1.1'");
        assertEquals(1, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 1 for single decimal digit");

        assertEquals(-1.28, bytes.parseDouble(), 0, "parseDouble should return -1.28 for input '-1.28'");
        assertEquals(2, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 2 for negative decimal with two places");

        assertEquals(1.10, bytes.parseDouble(), 0, "parseDouble should return 1.10 for input '1.10'");
        assertEquals(2, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 2 for trailing zero in decimal");

        assertEquals(1.10000, bytes.parseDouble(), 0, "parseDouble should return 1.10000 for input '1.10000'");
        assertEquals(5, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 5 for five trailing zeros");

        assertEquals(0.01, bytes.parseDouble(), 0, "parseDouble should return 0.01 for input '1E-2'");
        assertEquals(2, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 2 for scientific notation with negative exponent");

        assertEquals(100, bytes.parseDouble(), 0, "parseDouble should return 100.0 for input '1E+2'");
        assertEquals(0, bytes.lastDecimalPlaces(), "lastDecimalPlaces should be 0 for scientific notation with positive exponent");
    }

    private void appendVariousNumbers() {
        bytes.append('1').append(' ');
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
    public void testAppendParseUTF() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull String[] words = "Hello,World!,Bye£€!".split(",");
        for (@NotNull String word : words) {
            bytes.append(word).append('\t');
        }
        bytes.append('\t');

        for (String word : words) {
            assertEquals(word, bytes.parseUtf8(CONTROL_STOP), "parseUtf8 should return '" + word + "' when parsing with CONTROL_STOP");
        }
        assertEquals("", bytes.parseUtf8(CONTROL_STOP), "parseUtf8 should return empty string for trailing tab");

        bytes.readPosition(0);
        @NotNull StringBuilder sb = new StringBuilder();
        for (String word : words) {
            bytes.parseUtf8(sb, CONTROL_STOP);
            assertEquals(word, sb.toString(), "parseUtf8 should append '" + word + "' to StringBuilder");
        }
        bytes.parseUtf8(sb, CONTROL_STOP);
        assertEquals("", sb.toString(), "parseUtf8 should append empty string to StringBuilder for trailing tab");

        bytes.readPosition(0);
        bytes.skipTo(CONTROL_STOP);
        assertEquals(6, bytes.readPosition(), "skipTo should advance to position 6 after first tab in 'Hello,'");
        bytes.skipTo(CONTROL_STOP);
        assertEquals(13, bytes.readPosition(), "skipTo should advance to position 13 after second tab in 'World!'");
        assertTrue(bytes.skipTo(CONTROL_STOP), "skipTo should return true when stop char found before third tab");
        assertEquals(23, bytes.readPosition(), "skipTo should advance to position 23 after third tab in 'Bye£€!'");
        assertTrue(bytes.skipTo(CONTROL_STOP), "skipTo should return true when stop char found before final tab");
        assertEquals(24, bytes.readPosition(), "skipTo should advance to position 24 after final tab");
        assertFalse(bytes.skipTo(CONTROL_STOP), "skipTo should return false when no more stop chars available");
    }

    @Test
    public void testAppendSubstring() {
        bytes.append("Hello World", 2, 7).append("\n");

        assertEquals("Hello World".substring(2, 7), bytes.parseUtf8(CONTROL_STOP), "parseUtf8 should return 'llo W' for substring from index 2 to 7");
    }

    @Test
    public void testWriteBytes() {
        bytes.write("Hello World\n".getBytes(ISO_8859_1), 0, 10);
        bytes.write("good bye\n".getBytes(ISO_8859_1), 4, 4);
        bytes.write(4, "0 w".getBytes(ISO_8859_1));

        assertEquals("Hell0 worl bye", bytes.parseUtf8(CONTROL_STOP), "parseUtf8 should return 'Hell0 worl bye' after overlapping writes");
    }

    @Test
    public void testFlexibleLong() {
        // Test regular longs
        bytes.append('0').append(' ');
        assertEquals(0L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 0 for input '0'");

        bytes.append('1').append(' ');
        assertEquals(1L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 1 for input '1'");

        bytes.append("-1").append(' ');
        assertEquals(-1L, bytes.parseFlexibleLong(), "parseFlexibleLong should return -1 for input '-1'");

        bytes.append("6432643").append(' ');
        assertEquals(6432643L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 6432643 for seven-digit integer");

        bytes.append("-16432620987").append(' ');
        assertEquals(-16432620987L, bytes.parseFlexibleLong(), "parseFlexibleLong should return -16432620987 for negative eleven-digit integer");

        bytes.append("27209782482844").append(' ');
        assertEquals(27209782482844L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 27209782482844 for fourteen-digit integer");

        bytes.append("-37218967980573232").append(' ');
        assertEquals(-37218967980573232L, bytes.parseFlexibleLong(), "parseFlexibleLong should return -37218967980573232 for negative seventeen-digit integer");

        bytes.append(String.valueOf(Long.MAX_VALUE - 20)).append(' ');
        assertEquals(Long.MAX_VALUE - 20, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MAX_VALUE - 20 for near-maximum value");

        bytes.append(String.valueOf(Long.MIN_VALUE + 20)).append(' ');
        assertEquals(Long.MIN_VALUE + 20, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MIN_VALUE + 20 for near-minimum value");

        bytes.append(String.valueOf(Long.MAX_VALUE - 3)).append(' ');
        assertEquals(Long.MAX_VALUE - 3, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MAX_VALUE - 3 for boundary value");

        bytes.append(String.valueOf(Long.MIN_VALUE + 3)).append(' ');
        assertEquals(Long.MIN_VALUE + 3, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MIN_VALUE + 3 for boundary value");

        bytes.append(String.valueOf(Long.MAX_VALUE)).append(' ');
        assertEquals(Long.MAX_VALUE, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MAX_VALUE for maximum long");

        bytes.append(String.valueOf(Long.MIN_VALUE)).append(' ');
        assertEquals(Long.MIN_VALUE, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MIN_VALUE for minimum long");

        // Test regular longs with a point, with varying number of zeros
        bytes.append("0.0").append(' ');
        assertEquals(0L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 0 for input '0.0' with decimal point");

        bytes.append("1.000").append(' ');
        assertEquals(1L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 1 for input '1.000' with trailing zeros");

        bytes.append("-0001.00000").append(' ');
        assertEquals(-1L, bytes.parseFlexibleLong(), "parseFlexibleLong should return -1 for input '-0001.00000' with leading and trailing zeros");

        bytes.append("6432643.0").append(' ');
        assertEquals(6432643L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 6432643 for input '6432643.0' with single trailing zero");

        bytes.append("-16432620987.").append(' ');
        assertEquals(-16432620987L, bytes.parseFlexibleLong(), "parseFlexibleLong should return -16432620987 for input '-16432620987.' with decimal point only");

        bytes.append(String.valueOf(Long.MAX_VALUE - 20)).append(".0").append(' ');
        assertEquals(Long.MAX_VALUE - 20, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MAX_VALUE - 20 for near-maximum value with decimal point");

        bytes.append(String.valueOf(Long.MIN_VALUE + 20)).append(".0").append(' ');
        assertEquals(Long.MIN_VALUE + 20, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MIN_VALUE + 20 for near-minimum value with decimal point");

        bytes.append(String.valueOf(Long.MAX_VALUE - 3)).append(".0").append(' ');
        assertEquals(Long.MAX_VALUE - 3, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MAX_VALUE - 3 for boundary value with decimal point");

        bytes.append(String.valueOf(Long.MIN_VALUE + 3)).append(".0").append(' ');
        assertEquals(Long.MIN_VALUE + 3, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MIN_VALUE + 3 for boundary value with decimal point");

    }

    @Test
    public void testFlexibleLong2() {

        bytes.append(String.valueOf(Long.MAX_VALUE)).append(".0").append(' ');
        assertEquals(Long.MAX_VALUE, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MAX_VALUE for maximum value with decimal point");

        bytes.append(String.valueOf(Long.MIN_VALUE)).append(".0").append(' ');
        assertEquals(Long.MIN_VALUE, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MIN_VALUE for minimum value with decimal point");

        // Test scientific format
        bytes.append("1e1").append(' ');
        assertEquals(10L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 10 for input '1e1' in scientific notation");

        bytes.append("1E1").append(' ');
        assertEquals(10L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 10 for input '1E1' in uppercase scientific notation");

        bytes.append("-1E1").append(' ');
        assertEquals(-10L, bytes.parseFlexibleLong(), "parseFlexibleLong should return -10 for input '-1E1' in negative scientific notation");

        bytes.append("-4E6").append(' ');
        assertEquals(-4000000L, bytes.parseFlexibleLong(), "parseFlexibleLong should return -4000000 for input '-4E6' with positive exponent");

        bytes.append("100E10").append(' ');
        assertEquals(1000000000000L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 1000000000000 for input '100E10'");

        bytes.append("9E12").append(' ');
        assertEquals(9000000000000L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 9000000000000 for input '9E12'");

        bytes.append("6410269E3").append(' ');
        assertEquals(6410269000L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 6410269000 for input '6410269E3'");

        bytes.append("5000000000E-3").append(' ');
        assertEquals(5000000, bytes.parseFlexibleLong(), "parseFlexibleLong should return 5000000 for input '5000000000E-3' with negative exponent");

        bytes.append(String.valueOf(Long.MAX_VALUE)).append("e0").append(' ');
        assertEquals(Long.MAX_VALUE, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MAX_VALUE for input with exponent 0");

        bytes.append(String.valueOf(Long.MIN_VALUE)).append("E0").append(' ');
        assertEquals(Long.MIN_VALUE, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MIN_VALUE for input with uppercase exponent 0");

        bytes.append(String.valueOf(Long.MAX_VALUE)).append("000000e-6").append(' ');
        assertEquals(Long.MAX_VALUE, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MAX_VALUE for input with trailing zeros and negative exponent");

        bytes.append(String.valueOf(Long.MIN_VALUE)).append("00000000000000000000E-20").append(' ');
        assertEquals(Long.MIN_VALUE, bytes.parseFlexibleLong(), "parseFlexibleLong should return Long.MIN_VALUE for input with many trailing zeros and exponent -20");

        bytes.append("0.000000000000000000000000000001E33").append(' ');
        assertEquals(1000L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 1000 for input '0.000000000000000000000000000001E33' with large exponent");

        bytes.append("789000000000000000000000000000000E-25").append(' ');
        assertEquals(78900000L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 78900000 for input '789000000000000000000000000000000E-25'");

        // Test values outside long range
        bytes.append("9E40").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("-8473289704324748391027491830").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(Long.MAX_VALUE).append('0').append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        // Test rounded fractional numbers
        bytes.append("0.1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("1e-2").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("0.9").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

    }

    @Test
    public void testFlexibleLong3() {

        bytes.append("0.9").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("56765e-2").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("-0.1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("-0.9").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("4.4000000000000000000000000000001E1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("4.3999999999999999999999999999991E1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(String.valueOf(Long.MAX_VALUE)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(String.valueOf(Long.MAX_VALUE - 1)).append(".9").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(String.valueOf(Long.MAX_VALUE - 1)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(String.valueOf(Long.MIN_VALUE)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(String.valueOf(Long.MIN_VALUE + 1)).append(".9").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(String.valueOf(Long.MIN_VALUE + 1)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(String.valueOf(Long.MIN_VALUE + 5)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append(String.valueOf(Long.MIN_VALUE + 5)).append(".9").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        // Test extreme double values
        bytes.append("Infinity").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("-Infinity").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("+Infinity").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        bytes.append("NaN").append(' ');
        assertThrows(IORuntimeException.class, bytes::parseFlexibleLong);

        // Test regular longs again - to check that input is properly consumed
        bytes.append('0').append(' ');
        assertEquals(0L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 0 after error recovery for input '0'");

        bytes.append('1').append(' ');
        assertEquals(1L, bytes.parseFlexibleLong(), "parseFlexibleLong should return 1 after error recovery for input '1'");

        bytes.append("-1").append(' ');
        assertEquals(-1L, bytes.parseFlexibleLong(), "parseFlexibleLong should return -1 after error recovery for input '-1'");
    }
}
