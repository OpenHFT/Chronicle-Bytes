/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static net.openhft.chronicle.bytes.StopCharTesters.CONTROL_STOP;
import static net.openhft.chronicle.bytes.StopCharTesters.SPACE_STOP;
import static org.junit.Assert.assertEquals;

public class ByteStringParserTest   {
    Bytes bytes = Bytes.elasticByteBuffer();

    @Test
    public void testParseLong() {
        long expected = 123L;
        bytes.append(expected);

        Assert.assertEquals(expected, BytesInternal.parseLong(bytes));
    }

    @Test
    public void testParseInt() {
        int expected = 123;
        bytes.append(expected);

        Assert.assertEquals(expected, BytesInternal.parseLong(bytes));
    }

    @Test
    public void testParseDouble() {
        double expected = 123.1234;
        bytes.append(expected);

        Assert.assertEquals(expected, BytesInternal.parseDouble(bytes), 0);
    }

    @Test
    public void testParseFloat() {
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
    public void testAppendParse() throws IOException {
        bytes.write("word£€) ".getBytes(StandardCharsets.UTF_8));
        bytes.append("word£€)").append(' ');
        bytes.append(1234).append(' ');
        bytes.append(123456L).append(' ');
        bytes.append(1.2345).append(' ');

        assertEquals("word£€)", bytes.parseUtf8(SPACE_STOP));
        assertEquals("word£€)", bytes.parseUtf8(SPACE_STOP));
        assertEquals(1234, bytes.parseLong());
        assertEquals(123456L, bytes.parseLong());
        assertEquals(1.2345, bytes.parseDouble(), 0);
    }

    @Test
    public void testLastDecimalPlaces() throws IOException {
        bytes.append("1").append(' ');
        bytes.append("1.").append(' ');
        bytes.append("0.0").append(' ');
        bytes.append("0.1").append(' ');
        bytes.append("1.1").append(' ');
        bytes.append("1.28").append(' ');
        bytes.append("1.10").append(' ');
        bytes.append("1.10000").append(' ');

        // test long first
        bytes.readPosition(0);
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

        assertEquals(128, bytes.parseLongDecimal());
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(110, bytes.parseLongDecimal());
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(110000, bytes.parseLongDecimal());
        assertEquals(5, bytes.lastDecimalPlaces());

        // test double
        bytes.readPosition(0);
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

        assertEquals(1.28, bytes.parseDouble(), 0);
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(1.10, bytes.parseDouble(), 0);
        assertEquals(2, bytes.lastDecimalPlaces());

        assertEquals(1.10000, bytes.parseDouble(), 0);
        assertEquals(5, bytes.lastDecimalPlaces());
    }

    @Test
    public void testAppendParseUTF() throws IOException {
        String[] words = "Hello,World!,Bye£€!".split(",");
        for (String word : words) {
            bytes.append(word).append('\t');
        }
        bytes.append('\t');

        for (String word : words) {
            assertEquals(word, bytes.parseUtf8(CONTROL_STOP));
        }
        assertEquals("", bytes.parseUtf8(CONTROL_STOP));

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
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
    public void testAppendSubstring() throws IOException {
        bytes.append("Hello World", 2, 7).append("\n");

        assertEquals("Hello World".substring(2, 7), bytes.parseUtf8(CONTROL_STOP));
    }

    @Test
    public void testWriteBytes() {
        bytes.write("Hello World\n".getBytes(), 0, 10);
        bytes.write("good bye\n".getBytes(), 4, 4);
        bytes.write(4, "0 w".getBytes());

        assertEquals("Hell0 worl bye", bytes.parseUtf8(CONTROL_STOP));
    }

}