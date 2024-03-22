/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.StopCharTesters.CONTROL_STOP;
import static net.openhft.chronicle.bytes.StopCharTesters.SPACE_STOP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

public class ByteStringParserTest extends BytesTestCommon {
    @NotNull
    Bytes<?> bytes = Bytes.elasticByteBuffer();

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

        Assert.assertEquals(expected, bytes.parseLong(0));
        Assert.assertEquals(expected, BytesInternal.parseLong(bytes));

        bytes2.append(expected);
        Assert.assertEquals(expected, bytes2.parseLong(0));
        Assert.assertEquals(expected, BytesInternal.parseLong(bytes2));
        bytes2.releaseLast();

    }

    @Test
    public void testParseInt() {
        int expected = 123;
        bytes.append(expected);

        Assert.assertEquals(expected, BytesInternal.parseLong(bytes));
    }

    @Test
    public void testParseDouble() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        double expected = 123.1234;
        bytes.append(expected);

        Assert.assertEquals(expected, BytesInternal.parseDouble(bytes), 0);
    }

    @Test
    public void testParseFloat() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        float expected = 123;
        bytes.append(expected);

        Assert.assertEquals(expected, BytesInternal.parseDouble(bytes), 0);
    }

    @Test
    public void testParseShort() {
        short expected = 123;
        bytes.append(expected);

        Assert.assertEquals(expected, BytesInternal.parseLong(bytes));
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

        assertEquals("word£€)", bytes.parseUtf8(SPACE_STOP));
        assertEquals("word£€)", bytes.parseUtf8(SPACE_STOP));
        assertEquals(1234, bytes.parseLong());
        assertEquals(123456L, bytes.parseLong());
        assertEquals(1.2345, bytes.parseDouble(), 0);
        assertEquals(0.0012345, bytes.parseDouble(), 0);
    }

    @Test
    public void testLastDecimalPlacesLong() throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        appendVariousNumbers();
        assertEquals(1, bytes.parseLongDecimal());
        assertEquals(0, bytes.lastDecimalPlaces());

        assertEquals(1, bytes.parseLongDecimal());
        assertEquals(0, bytes.lastDecimalPlaces());

        assertEquals(0, bytes.parseLongDecimal());
        assertEquals(1, bytes.lastDecimalPlaces());

        assertEquals(1, bytes.parseLongDecimal());
        assertEquals(1, bytes.lastDecimalPlaces());

        assertEquals(11, bytes.parseLongDecimal());
        assertEquals(1, bytes.lastDecimalPlaces());

        assertEquals(-128, bytes.parseLongDecimal());
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(110, bytes.parseLongDecimal());
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(110000, bytes.parseLongDecimal());
        assertEquals(5, bytes.lastDecimalPlaces());
    }

    @Test
    public void testLastDecimalPlacesDouble() throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        appendVariousNumbers();

        assertEquals(1, bytes.parseDouble(), 0);
        assertEquals(0, bytes.lastDecimalPlaces());

        assertEquals(1., bytes.parseDouble(), 0);
        assertEquals(0, bytes.lastDecimalPlaces());

        assertEquals(0.0, bytes.parseDouble(), 0);
        assertEquals(1, bytes.lastDecimalPlaces());

        assertEquals(0.1, bytes.parseDouble(), 0);
        assertEquals(1, bytes.lastDecimalPlaces());

        assertEquals(1.1, bytes.parseDouble(), 0);
        assertEquals(1, bytes.lastDecimalPlaces());

        assertEquals(-1.28, bytes.parseDouble(), 0);
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(1.10, bytes.parseDouble(), 0);
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(1.10000, bytes.parseDouble(), 0);
        assertEquals(5, bytes.lastDecimalPlaces());

        assertEquals(0.01, bytes.parseDouble(), 0);
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(100, bytes.parseDouble(), 0);
        assertEquals(0, bytes.lastDecimalPlaces());
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
    public void testAppendParseUTF() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull String[] words = "Hello,World!,Bye£€!".split(",");
        for (@NotNull String word : words) {
            bytes.append(word).append('\t');
        }
        bytes.append('\t');

        for (String word : words) {
            assertEquals(word, bytes.parseUtf8(CONTROL_STOP));
        }
        assertEquals("", bytes.parseUtf8(CONTROL_STOP));

        bytes.readPosition(0);
        @NotNull StringBuilder sb = new StringBuilder();
        for (String word : words) {
            bytes.parseUtf8(sb, CONTROL_STOP);
            Assert.assertEquals(word, sb.toString());
        }
        bytes.parseUtf8(sb, CONTROL_STOP);
        Assert.assertEquals("", sb.toString());

        bytes.readPosition(0);
        bytes.skipTo(CONTROL_STOP);
        assertEquals(6, bytes.readPosition());
        bytes.skipTo(CONTROL_STOP);
        assertEquals(13, bytes.readPosition());
        Assert.assertTrue(bytes.skipTo(CONTROL_STOP));
        assertEquals(23, bytes.readPosition());
        Assert.assertTrue(bytes.skipTo(CONTROL_STOP));
        assertEquals(24, bytes.readPosition());
        Assert.assertFalse(bytes.skipTo(CONTROL_STOP));
    }

    @Test
    public void testAppendSubstring() {
        bytes.append("Hello World", 2, 7).append("\n");

        assertEquals("Hello World".substring(2, 7), bytes.parseUtf8(CONTROL_STOP));
    }

    @Test
    public void testWriteBytes() {
        bytes.write("Hello World\n".getBytes(ISO_8859_1), 0, 10);
        bytes.write("good bye\n".getBytes(ISO_8859_1), 4, 4);
        bytes.write(4, "0 w".getBytes(ISO_8859_1));

        assertEquals("Hell0 worl bye", bytes.parseUtf8(CONTROL_STOP));
    }

    @Test
    public void testFlexibleLong() {
        // Test regular longs
        bytes.append("0").append(' ');
        assertEquals(0L, bytes.parseFlexibleLong());

        bytes.append("1").append(' ');
        assertEquals(1L, bytes.parseFlexibleLong());

        bytes.append("-1").append(' ');
        assertEquals(-1L, bytes.parseFlexibleLong());

        bytes.append("6432643").append(' ');
        assertEquals(6432643L, bytes.parseFlexibleLong());

        bytes.append("-16432620987").append(' ');
        assertEquals(-16432620987L, bytes.parseFlexibleLong());

        bytes.append("27209782482844").append(' ');
        assertEquals(27209782482844L, bytes.parseFlexibleLong());

        bytes.append("-37218967980573232").append(' ');
        assertEquals(-37218967980573232L, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE - 20)).append(' ');
        assertEquals(Long.MAX_VALUE - 20, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE + 20)).append(' ');
        assertEquals(Long.MIN_VALUE + 20, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE - 3)).append(' ');
        assertEquals(Long.MAX_VALUE - 3, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE + 3)).append(' ');
        assertEquals(Long.MIN_VALUE + 3, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE)).append(' ');
        assertEquals(Long.MAX_VALUE, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE)).append(' ');
        assertEquals(Long.MIN_VALUE, bytes.parseFlexibleLong());

        // Test regular longs with a point, with varying number of zeros
        bytes.append("0.0").append(' ');
        assertEquals(0L, bytes.parseFlexibleLong());

        bytes.append("1.000").append(' ');
        assertEquals(1L, bytes.parseFlexibleLong());

        bytes.append("-0001.00000").append(' ');
        assertEquals(-1L, bytes.parseFlexibleLong());

        bytes.append("6432643.0").append(' ');
        assertEquals(6432643L, bytes.parseFlexibleLong());

        bytes.append("-16432620987.").append(' ');
        assertEquals(-16432620987L, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE - 20)).append(".0").append(' ');
        assertEquals(Long.MAX_VALUE - 20, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE + 20)).append(".0").append(' ');
        assertEquals(Long.MIN_VALUE + 20, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE - 3)).append(".0").append(' ');
        assertEquals(Long.MAX_VALUE - 3, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE + 3)).append(".0").append(' ');
        assertEquals(Long.MIN_VALUE + 3, bytes.parseFlexibleLong());

    }

    @Test
    public void testFlexibleLong2() {

        bytes.append(String.valueOf(Long.MAX_VALUE)).append(".0").append(' ');
        assertEquals(Long.MAX_VALUE, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE)).append(".0").append(' ');
        assertEquals(Long.MIN_VALUE, bytes.parseFlexibleLong());

        // Test scientific format
        bytes.append("1e1").append(' ');
        assertEquals(10L, bytes.parseFlexibleLong());

        bytes.append("1E1").append(' ');
        assertEquals(10L, bytes.parseFlexibleLong());

        bytes.append("-1E1").append(' ');
        assertEquals(-10L, bytes.parseFlexibleLong());

        bytes.append("-4E6").append(' ');
        assertEquals(-4000000L, bytes.parseFlexibleLong());

        bytes.append("100E10").append(' ');
        assertEquals(1000000000000L, bytes.parseFlexibleLong());

        bytes.append("9E12").append(' ');
        assertEquals(9000000000000L, bytes.parseFlexibleLong());

        bytes.append("6410269E3").append(' ');
        assertEquals(6410269000L, bytes.parseFlexibleLong());

        bytes.append("5000000000E-3").append(' ');
        assertEquals(5000000, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE)).append("e0").append(' ');
        assertEquals(Long.MAX_VALUE, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE)).append("E0").append(' ');
        assertEquals(Long.MIN_VALUE, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE)).append("000000e-6").append(' ');
        assertEquals(Long.MAX_VALUE, bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE)).append("00000000000000000000E-20").append(' ');
        assertEquals(Long.MIN_VALUE, bytes.parseFlexibleLong());

        bytes.append("0.000000000000000000000000000001E33").append(' ');
        assertEquals(1000L, bytes.parseFlexibleLong());

        bytes.append("789000000000000000000000000000000E-25").append(' ');
        assertEquals(78900000L, bytes.parseFlexibleLong());

        // Test values outside long range
        bytes.append("9E40").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("-8473289704324748391027491830").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(Long.MAX_VALUE).append("0").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        // Test rounded fractional numbers
        bytes.append("0.1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("1e-2").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("0.9").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

    }

    @Test
    public void testFlexibleLong3() {

        bytes.append("0.9").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("56765e-2").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("-0.1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("-0.9").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("4.4000000000000000000000000000001E1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("4.3999999999999999999999999999991E1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE - 1)).append(".9").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MAX_VALUE - 1)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE + 1)).append(".9").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE + 1)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE + 5)).append(".1").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append(String.valueOf(Long.MIN_VALUE + 5)).append(".9").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        // Test extreme double values
        bytes.append("Infinity").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("-Infinity").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("+Infinity").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        bytes.append("NaN").append(' ');
        assertThrows(IORuntimeException.class, () -> bytes.parseFlexibleLong());

        // Test regular longs again - to check that input is properly consumed
        bytes.append("0").append(' ');
        assertEquals(0L, bytes.parseFlexibleLong());

        bytes.append("1").append(' ');
        assertEquals(1L, bytes.parseFlexibleLong());

        bytes.append("-1").append(' ');
        assertEquals(-1L, bytes.parseFlexibleLong());
    }
}
