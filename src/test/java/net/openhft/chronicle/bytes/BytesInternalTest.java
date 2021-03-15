/*
 * Copyright 2016-2018-2020 chronicle.software
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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static net.openhft.chronicle.bytes.BytesInternalTest.Nested.LENGTH;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

@SuppressWarnings({"rawtypes"})
@RunWith(Parameterized.class)
public class BytesInternalTest extends BytesTestCommon {

    private final boolean guarded;
    private ThreadDump threadDump;

    public BytesInternalTest(String name, boolean guarded) {
        this.guarded = guarded;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Unguarded", false},
                {"Guarded", true}
        });
    }

    @BeforeClass
    public void notMacArm() {
        assumeFalse(Jvm.isMacArm());
    }

    @AfterClass
    public static void resetGuarded() {
        NativeBytes.resetNewGuarded();
    }

    @Before
    public void setGuarded() {
        NativeBytes.setNewGuarded(guarded);
    }

    @Test
    public void testParse8bitAndStringBuilderWithUtf16Coder()
            throws BufferUnderflowException, IOException {
        @NotNull NativeBytesStore<Void> bs = NativeBytesStore.nativeStore(32);
        bs.write(0, new byte[]{0x76, 0x61, 0x6c, 0x75, 0x65}); // "value" string

        StringBuilder sb = new StringBuilder();
        sb.append("你好");

        BytesInternal.parse8bit(0, bs, sb, 5);
        String actual = sb.toString();

        assertEquals("value", actual);
        assertEquals(5, actual.length());
        bs.releaseLast();
    }

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }
    /*
    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
    */

    @Test
    public void testParseUTF_SB1()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        @NotNull byte[] bytes2 = new byte[128];
        Arrays.fill(bytes2, (byte) '?');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8(bytes, sb, true, 128);
        assertEquals(128, sb.length());
        assertEquals(new String(bytes2, US_ASCII), sb.toString());
        bytes.readPosition(0);
        sb.setLength(0);
        BytesInternal.parseUtf8(bytes, sb, false, 128);
        assertEquals(128, sb.length());
        assertEquals(new String(bytes2, US_ASCII), sb.toString());
        bytes.releaseLast();
    }

    @Test
    public void testParseUTF8_LongString()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        @NotNull byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8(bytes, sb, true, length);
        assertEquals(length, sb.length());
        String actual = sb.toString();
        sb = null; // free some memory.
        assertEquals(new String(bytes2, US_ASCII), actual);

        bytes.releaseLast();
    }

    @Test
    public void parseDoubleScientificNegative() {
        String strDouble = "6.1E-4";
        double expected = 6.1E-4;
        int expectedDp = 5; //0.00061 needs dp 5
        Bytes<?> from = Bytes.from(strDouble);
        assertEquals(expected, from.parseDouble(), 0.0);
        assertEquals(expectedDp, from.lastDecimalPlaces());
        from.releaseLast();
    }

    @Test
    public void parseDoubleScientificNegative1() {
        String strDouble = "6.123E-4";
        double expected = 6.123E-4;
        int expectedDp = 7; //0.0006123 needs dp 7
        Bytes<?> from = Bytes.from(strDouble);
        assertEquals(expected, from.parseDouble(), 0.0);
        assertEquals(expectedDp, from.lastDecimalPlaces());  //Last dp should be 7.
        from.releaseLast();
    }

    @Test
    public void parseDoubleScientificPositive1() {
        String strDouble = "6.12345E4";
        double expected = 6.12345E4;
        int expectedDp = 1; //6.12345 x 10^4 = 61234.5 needs 1
        Bytes<?> from = Bytes.from(strDouble);
        assertEquals(expected, from.parseDouble(), 0.0);
        assertEquals(expectedDp, from.lastDecimalPlaces());
        from.releaseLast();
    }

    @Test
    public void testParseUTF81_LongString()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        @NotNull byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf81(bytes, sb, true, length);
        assertEquals(length, sb.length());
        assertEquals(new String(bytes2, US_ASCII), sb.toString());

        bytes.readPosition(0);
        sb.setLength(0);

        BytesInternal.parseUtf81(bytes, sb, false, length);
        assertEquals(length, sb.length());
        assertEquals(new String(bytes2, US_ASCII), sb.toString());

        bytes.releaseLast();
    }

    @Test
    public void testParseUTF_SB1_LongString()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        @NotNull byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8_SB1(bytes, sb, true, length);
        assertEquals(length, sb.length());
        assertEquals(new String(bytes2, US_ASCII), sb.toString());

        bytes.readPosition(0);
        sb.setLength(0);

/*
        BytesInternal.parseUtf8_SB1(bytes, sb, false, length);
        assertEquals(length, sb.length());
        assertEquals(new String(bytes2, US_ASCII), sb.toString());
*/

        bytes.releaseLast();
    }

    @Test
    public void testParse8bit_LongString()
            throws Exception {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        @NotNull byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parse8bit(0, bytes, sb, length);
        assertEquals(length, sb.length());
        assertEquals(new String(bytes2, US_ASCII), sb.toString());

        bytes.releaseLast();
    }

    @Test
    public void testAllParseDouble() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        for (String s : "0.,1.,9.".split(",")) {
            // todo FIX for i == 7 && d == 8
            for (int d = 0; d < 8; d++) {
                s += '0';
                for (int i = 1; i < 10; i += 2) {
                    String si = s + i;
                    Bytes<?> from = Bytes.from(si);
                    assertEquals(si,
                            Double.parseDouble(si),
                            from.parseDouble(), 0.0);
                    from.releaseLast();
                }
            }
        }
    }

    @Test
    public void testWriteUtf8LongString()
            throws IORuntimeException, BufferUnderflowException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++)
            sb.append('!');

        String test = sb.toString();
        BytesInternal.writeUtf8(bytes, test);

        sb.setLength(0);
        assertTrue(BytesInternal.compareUtf8(bytes, 0, test));

        bytes.releaseLast();
    }

    @Test
    public void testAppendUtf8LongString()
            throws Exception {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
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
        bytes.releaseLast();
    }

    @Test
    public void testAppend8bitLongString()
            throws Exception {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
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
        bytes.releaseLast();
    }

    @Test
    public void testCompareUTF()
            throws IORuntimeException {
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
        bs.releaseLast();
    }

    @Test
    public void shouldHandleDifferentSizedStores() {
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(32);
        final BytesStore storeOfThirtyTwoBytes = bytes.bytesStore();
        storeOfThirtyTwoBytes.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        Bytes<ByteBuffer> bytes2 = Bytes.elasticHeapByteBuffer(512);
        final BytesStore longerBuffer = bytes2.bytesStore();
        longerBuffer.writeUtf8(0, "thirty_two_bytes_of_utf8_chars_");

        assertTrue(BytesInternal.equalBytesAny(storeOfThirtyTwoBytes, longerBuffer, 32));
        bytes2.releaseLast();
        bytes.releaseLast();
    }

    @Test
    public void testParseDouble() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
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

            Bytes<?> from = Bytes.from(text);
            assertEquals(expected, from.parseDouble(), 0.0);
            from.releaseLast();
        }
    }

    private int checkParse(int different, String s) {
        double d = Double.parseDouble(s);
        Bytes<?> from = Bytes.from(s);
        double d2 = from.parseDouble();
        from.releaseLast();
        if (d != d2) {
//            System.out.println(d + " != " + d2);
            ++different;
        }
        return different;
    }

    @Test
    public void testWritingDecimalVsJava() {
        Bytes bytes = Bytes.allocateElasticOnHeap(32);
        bytes.clear();
        double d = 0.04595828484241039; //Math.pow(1e9, rand.nextDouble()) / 1e3;
        bytes.append(d);
        String s = Double.toString(d);
        if (s.length() != bytes.readRemaining()) {
            assertEquals(d, Double.parseDouble(s), 0.0);
            String s2 = bytes.toString();
//            System.out.println(s + " != " + s2);
        }
        bytes.releaseLast();
    }

    @Test
    public void bytesParseDouble_Issue85_SeededRandom() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        Random random = new Random(1);
        int different = 0;
        int max = 10_000;
        for (int i = 0; i < max; i++) {
            double num = random.nextDouble();
            String s = String.format("%.9f", num);
            different = checkParse(different, s);
        }
        Assert.assertEquals("Different " + (100.0 * different) / max + "%", 0, different);
    }

    @Test
    public void contentsEqual() {
        Bytes<?> a = Bytes.elasticByteBuffer(9, 20)
                .append(Bytes.from("Hello"))
                .readLimit(16);
        Bytes b = Bytes.elasticByteBuffer(5, 20)
                .append(Bytes.from("Hello"))
                .readLimit(16);
        Bytes c = Bytes.elasticByteBuffer(15, 20)
                .append(Bytes.from("Hello"))
                .readLimit(16);
        String actual1 = a.toString();
        assertEquals("Hello\0\0\0\0", actual1);
        String actual2 = b.toString();
        assertEquals("Hello", actual2);
        String actual3 = c.toString();
        assertEquals("Hello\0\0\0\0\0\0\0\0\0\0", actual3);
        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(c, a);
        a.releaseLast();
        b.releaseLast();
        c.releaseLast();
    }

    static class Nested {
        public static final int LENGTH;

        static {
            long maxMemory = Runtime.getRuntime().maxMemory();
            int maxLength = OS.isLinux() ? 1 << 30 : 1 << 28;
            LENGTH = (int) Math.min(maxMemory / 16, maxLength);
            if (LENGTH < maxLength)
                System.out.println("Not enough memory to run big test, was " + (LENGTH >> 20) + " MB.");
        }
    }
}
