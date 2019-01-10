/*
 * Copyright 2016-2018 higherfrequencytrading.com
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
import org.junit.*;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

public class BytesInternalTest {

    public static final int LENGTH;

    static {
        long totalMemory = Runtime.getRuntime().totalMemory();
        int maxLength = 1 << 29;
        LENGTH = (int) Math.min(totalMemory / 9, maxLength);
        if (LENGTH < maxLength)
            System.out.println("Not enough memory to run big test, was " + (LENGTH >> 20) + " MB.");
    }

    private ThreadDump threadDump;

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Test
    public void testParseUTF_SB1() throws UTFDataFormatRuntimeException {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        @NotNull byte[] bytes2 = new byte[128];
        Arrays.fill(bytes2, (byte) '?');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8(bytes, sb, 128);
        assertEquals(128, sb.length());
        assertEquals(new String(bytes2, 0), sb.toString());
        bytes.release();
    }

    @Test
    public void testParseUTF8_LongString() throws UTFDataFormatRuntimeException {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        @NotNull byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8(bytes, sb, length);
        assertEquals(length, sb.length());
        String actual = sb.toString();
        sb = null; // free some memory.
        assertEquals(new String(bytes2, 0), actual);

        bytes.release();
    }

    @Test
    public void testParseUTF81_LongString() throws UTFDataFormatRuntimeException {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        @NotNull byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf81(bytes, sb, length);
        assertEquals(length, sb.length());
        assertEquals(new String(bytes2, 0), sb.toString());

        bytes.release();
    }

    @Test
    public void testParseUTF_SB1_LongString() throws UTFDataFormatRuntimeException {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        @NotNull byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8_SB1(bytes, sb, length);
        assertEquals(length, sb.length());
        assertEquals(new String(bytes2, 0), sb.toString());

        bytes.release();
    }

    @Test
    public void testParse8bit_LongString() throws Exception {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        @NotNull byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parse8bit(0, bytes, sb, length);
        assertEquals(length, sb.length());
        assertEquals(new String(bytes2, 0), sb.toString());

        bytes.release();
    }

    @Test
    public void testWriteUtf8LongString() throws IORuntimeException {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++)
            sb.append('!');

        String test = sb.toString();
        BytesInternal.writeUtf8(bytes, test);

        sb.setLength(0);
        assertTrue(BytesInternal.compareUtf8(bytes, 0, test));

        bytes.release();
    }

    @Test
    public void testAppendUtf8LongString() throws Exception {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++)
            sb.append('!');

        String test = sb.toString();
        BytesInternal.appendUtf8(bytes, test, 0, length);

        sb.setLength(0);
        BytesInternal.parse8bit(0, bytes, sb, length);

        assertEquals(test, sb.toString());
        bytes.release();
    }

    @Test
    public void testAppend8bitLongString() throws Exception {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++)
            sb.append('!');

        String test = sb.toString();
        BytesInternal.append8bit(0, bytes, test, 0, length);

        sb.setLength(0);
        BytesInternal.parse8bit(0, bytes, sb, length);

        assertEquals(test, sb.toString());
        bytes.release();
    }

    @Test
    public void testCompareUTF() throws IORuntimeException {
        @NotNull NativeBytesStore<Void> bs = NativeBytesStore.nativeStore(32);
        bs.writeUtf8(0, "test");
        assertTrue(BytesInternal.compareUtf8(bs, 0, "test"));
        assertFalse(BytesInternal.compareUtf8(bs, 0, null));

        bs.writeUtf8(0, null);
        assertTrue(BytesInternal.compareUtf8(bs, 0, null));
        assertFalse(BytesInternal.compareUtf8(bs, 0, "test"));

        bs.writeUtf8(1, "£€");
        @NotNull StringBuilder sb = new StringBuilder();
        bs.readUtf8(1, sb);
        assertEquals("£€", sb.toString());
        assertTrue(BytesInternal.compareUtf8(bs, 1, "£€"));
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£"));
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£€$"));
    }

    @Test
    public void shouldHandleDifferentSizedStores() throws Exception {
        final BytesStore storeOfThirtyTwoBytes = Bytes.elasticHeapByteBuffer(32).bytesStore();
        storeOfThirtyTwoBytes.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        final BytesStore longerBuffer = Bytes.elasticHeapByteBuffer(512).bytesStore();
        longerBuffer.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        assertTrue(BytesInternal.equalBytesAny(storeOfThirtyTwoBytes, longerBuffer, 512));
    }

    @Test
    public void testParseDouble() {
        @NotNull Object[][] tests = {
                {"-1E-3 ", -1E-3},
                {"12E3 ", 12E3},
                {"-1.1E-3 ", -1.1E-3},
                {"-1.1E3 ", -1.1E3},
                {"-1.16823E70 ", -1.16823E70},
                {"1.17045E70 ", 1.17045E70},
                {"6.85202", 6.85202}
        };
        for (Object[] objects : tests) {
            @NotNull String text = (String) objects[0];
            double expected = (Double) objects[1];

            assertEquals(expected, Bytes.from(text).parseDouble(), 0.0);
        }
    }

    @Test
    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/110")
    public void testWritingDecimalVsJava() {
        Bytes bytes = Bytes.elasticHeapByteBuffer(32);
        long l = Double.doubleToRawLongBits(1.0);
//        Random rand = new Random(1);
        int count = 0;
//        for (int i = 0; i < 1000000; i++) {
        bytes.clear();
        double d = 0.04595828484241039; //Math.pow(1e9, rand.nextDouble()) / 1e3;
        bytes.append(d);
        String s = Double.toString(d);
        if (s.length() != bytes.readRemaining()) {
            assertEquals(d, Double.parseDouble(s), 0.0);
            String s2 = bytes.toString();
            System.out.println(s + " != " + s2);
//                count++;
        }
//        }
        assertEquals(0, count);
    }

    private int checkParse(int different, String s) {
        double d = Double.parseDouble(s);
        double d2 = Bytes.from(s).parseDouble();
        if(d != d2) {
            System.out.println(d + " != " + d2);
            ++different;
        }
        return different;
    }

    @Test
    public void bytesParseDouble_Issue85_SeededRandom() {
        Random random = new Random(1);
        int different = 0;
        int max = 10_000;
        for (int i = 0; i< max; i++) {
            double num = random.nextDouble();
            String s = String.format("%.9f", num);
            different = checkParse(different, s);
        }
        Assert.assertEquals("Different "+(100.0*different)/max+"%", 0, different);
    }
}