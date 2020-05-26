/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.StopCharTesters.CONTROL_STOP;
import static net.openhft.chronicle.bytes.StopCharTesters.SPACE_STOP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class ByteStringParserTest {
    @SuppressWarnings("rawtypes")
    @NotNull
    Bytes bytes = Bytes.elasticByteBuffer();
    private ThreadDump threadDump;

    @After
    public void checkRegisteredBytes() {
        bytes.release();
        BytesUtil.checkRegisteredBytes();
    }

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testParseLong() {
        long expected = 123456789012345678L;
        bytes.append(expected);
        Bytes bytes2 = Bytes.elasticHeapByteBuffer((int) bytes.readRemaining());

        Assert.assertEquals(expected, bytes.parseLong(0));
        Assert.assertEquals(expected, BytesInternal.parseLong(bytes));

        bytes2.append(expected);
        Assert.assertEquals(expected, bytes2.parseLong(0));
        Assert.assertEquals(expected, BytesInternal.parseLong(bytes2));
        bytes2.release();

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
    public void testAppendParse() throws IOException, IORuntimeException {
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
    public void testLastDecimalPlaces() throws IOException, IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
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
    public void testAppendSubstring() throws IOException {
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
}
