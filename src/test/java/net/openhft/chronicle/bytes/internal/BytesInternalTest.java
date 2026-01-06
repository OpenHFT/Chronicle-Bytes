/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static net.openhft.chronicle.bytes.internal.BytesInternalTest.Nested.LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
@DisplayName("BytesInternal parsing and write behaviour checks")
public class BytesInternalTest extends BytesTestCommon {
    @Test
    @DisplayName("parse UTF8 into string builder with flags")
    public void testParseUTF_SB1()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for UTF8 parse test");
        @NotNull VanillaBytes<Void> bytes = Bytes.allocateElasticDirect();
        byte[] bytes2 = new byte[128];
        Arrays.fill(bytes2, (byte) '?');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8(bytes, sb, true, 128);
        assertEquals(128, sb.length(),
                "UTF8 parse length matches buffer size");
        assertEquals(new String(bytes2, US_ASCII), sb.toString(),
                "UTF8 parse content matches ASCII buffer");
        bytes.readPosition(0);
        sb.setLength(0);
        BytesInternal.parseUtf8(bytes, sb, false, 128);
        assertEquals(128, sb.length(),
                "Non-UTF parse length matches buffer size");
        assertEquals(new String(bytes2, US_ASCII), sb.toString(),
                "Non-UTF parse content matches ASCII buffer");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("parse long UTF8 string into builder")
    public void testParseUTF8_LongString()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for long UTF8 parse test");
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8(bytes, sb, true, length);
        assertEquals(length, sb.length(),
                "UTF8 parse length matches long buffer size");
        String actual = sb.toString();
        sb = null; // free some memory.
        assertEquals(new String(bytes2, US_ASCII), actual,
                "UTF8 parse content matches long ASCII buffer");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("parse empty long tokens as zero")
    public void parseLongEmpty() {
        for (String s : ", , .,-,x, .e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(0, from.parseLong(),
                    "parseLong empty token yields zero for [" + s + "]");
            assertFalse(from.lastNumberHadDigits(),
                    "parseLong empty token has no digits for [" + s + "]");
        }
    }

    @Test
    @DisplayName("parse numeric long tokens with digits")
    public void parseLongNonEmpty() {
        for (String s : "0, 0, 0..,0-, 0e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(0, from.parseLong(),
                    "parseLong numeric token yields zero for [" + s + "]");
            assertTrue(from.lastNumberHadDigits(),
                    "parseLong numeric token has digits for [" + s + "]");
        }
    }

    @Test
    @DisplayName("parse empty decimal long tokens as zero")
    public void parseLongDecimalEmpty() {
        for (String s : ", , .,-,x, .e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(0, from.parseLongDecimal(),
                    "parseLongDecimal empty token yields zero for [" + s + "]");
            assertFalse(from.lastNumberHadDigits(),
                    "parseLongDecimal empty token has no digits for [" + s + "]");
        }
    }

    @Test
    @DisplayName("parse numeric decimal long tokens with digits")
    public void parseLongDecimalNonEmpty() {
        for (String s : "0, 0, .0,0-,0x, .0e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(0, from.parseLongDecimal(),
                    "parseLongDecimal numeric token yields zero for [" + s + "]");
            assertTrue(from.lastNumberHadDigits(),
                    "parseLongDecimal numeric token has digits for [" + s + "]");
        }
    }

    @Test
    @DisplayName("parse empty double tokens as negative zero")
    public void parseDoubleEmpty() {
        for (String s : ", , .,-,x, .e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(0, Double.compare(-0.0, from.parseDouble()),
                    "parseDouble empty token yields -0.0 for [" + s + "]");
            assertFalse(from.lastNumberHadDigits(),
                    "parseDouble empty token has no digits for [" + s + "]");
        }
    }

    @Test
    @DisplayName("parse double zero tokens with digits")
    public void parseDoubleEmptyZero() {
        for (String s : "0, 0, .0,0-,0x, .0e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(0, Double.compare(0.0, from.parseDouble()),
                    "parseDouble numeric token yields 0.0 for [" + s + "]");
            assertTrue(from.lastNumberHadDigits(),
                    "parseDouble numeric token has digits for [" + s + "]");
        }
    }

    @Test
    @DisplayName("parse scientific negative double values with exponents correctly")
    public void parseDoubleScientificNegative() {
        parseDoubleScientific("6.1E-4", 6.1E-4, 5  /*0.00061 needs dp 5*/);
    }

    @Test
    @DisplayName("parse scientific negative double with digits")
    public void parseDoubleScientificNegative1() {
        parseDoubleScientific("6.123E-4", 6.123E-4, 7 /* 0.0006123 needs dp 7 */);
    }

    @Test
    @DisplayName("parse scientific positive double with digits")
    public void parseDoubleScientificPositive1() {
        parseDoubleScientific("6.12345E4", 6.12345E4, 1 /* 6.12345 x 10^4 = 61234.5 needs 1 */);
    }

    private void parseDoubleScientific(final String strDouble,
                                       final double expected,
                                       final int expectedDp) {
        final Bytes<?> from = Bytes.from(strDouble);
        try {
            assertEquals(expected, from.parseDouble(), 0.0,
                    "Scientific parse matches expected for " + strDouble);
            assertEquals(expectedDp, from.lastDecimalPlaces(),
                    "Decimal places match expected for " + strDouble);
        } finally {
            from.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF81 long string into builder")
    public void testParseUTF81_LongString()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for UTF81 parse test");
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf81(bytes, sb, true, length);
        assertEquals(length, sb.length(),
                "UTF81 parse length matches buffer size");
        assertEquals(new String(bytes2, US_ASCII), sb.toString(),
                "UTF81 parse content matches ASCII buffer");

        bytes.readPosition(0);
        sb.setLength(0);

        BytesInternal.parseUtf81(bytes, sb, false, length);
        assertEquals(length, sb.length(),
                "UTF81 parse length matches buffer size on second pass");
        assertEquals(new String(bytes2, US_ASCII), sb.toString(),
                "UTF81 parse content matches ASCII buffer on second pass");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("parse UTF8 SB1 long string into builder")
    public void testParseUTF_SB1_LongString()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for UTF8 SB1 parse test");
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8_SB1(bytes, sb, true, length);
        assertEquals(length, sb.length(),
                "UTF8 SB1 parse length matches buffer size");
        assertEquals(new String(bytes2, US_ASCII), sb.toString(),
                "UTF8 SB1 parse content matches ASCII buffer");

        bytes.readPosition(0);
        sb.setLength(0);


        bytes.releaseLast();
    }

    @Test
    @DisplayName("parse 8bit long string into builder")
    public void testParse8bit_LongString()
            throws Exception {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for 8bit parse test");
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        byte[] bytes2 = new byte[length];
        Arrays.fill(bytes2, (byte) '!');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parse8bit(0, bytes, sb, length);
        assertEquals(length, sb.length(),
                "8bit parse length matches buffer size");
        assertEquals(new String(bytes2, US_ASCII), sb.toString(),
                "8bit parse content matches ASCII buffer");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("parse double across many decimal patterns")
    public void testAllParseDouble() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for parseDouble coverage");
        for (String s : "0.,1.,9.".split(",")) {
            // todo FIX for i == 7 && d == 8
            for (int d = 0; d < 8; d++) {
                s += '0';
                for (int i = 1; i < 10; i += 2) {
                    String si = s + i;
                    Bytes<?> from = Bytes.from(si);
                    assertEquals(Double.parseDouble(si),
                            from.parseDouble(), 0.0,
                            "parseDouble matches expected for base " + s + " d " + d + " i " + i);
                    from.releaseLast();
                }
            }
        }
    }

    @Test
    @DisplayName("write UTF8 long string and compare output")
    public void testWriteUtf8LongString()
            throws IORuntimeException, BufferUnderflowException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for UTF8 write test");
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++)
            sb.append('!');

        String test = sb.toString();
        BytesInternal.writeUtf8(bytes, test);

        sb.setLength(0);
        assertTrue(BytesInternal.compareUtf8(bytes, 0, test),
                "UTF8 write round trip matches source text");

        bytes.releaseLast();
    }

    @Test
    @DisplayName("append UTF8 long string and read back")
    public void testAppendUtf8LongString()
            throws Exception {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for UTF8 append test");
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++)
            sb.append('!');

        String test = sb.toString();
        BytesInternal.appendUtf8(bytes, test, 0, length);

        sb.setLength(0);
        BytesInternal.parse8bit(0, bytes, sb, length);

        assertEquals(test, sb.toString(),
                "UTF8 append long string round trip matches");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("append 8bit long string and read back")
    public void testAppend8bitLongString()
            throws Exception {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for 8bit append test");
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect();
        int length = LENGTH;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++)
            sb.append('!');

        String test = sb.toString();
        BytesInternal.append8bit(0, bytes, test, 0, length);

        sb.setLength(0);
        BytesInternal.parse8bit(0, bytes, sb, length);

        assertEquals(test, sb.toString(),
                "8bit append long string round trip matches");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("parse double handles exponent and decimal forms")
    public void testParseDouble() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for parseDouble test");
        @NotNull Object[][] tests = {
                {"0e0 ", 0.0},
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
            assertEquals(expected, from.parseDouble(), 0.0,
                    "Parsed double matches expected for " + Arrays.toString(objects));
            assertTrue(from.lastNumberHadDigits(),
                    "Last number had digits for " + Arrays.toString(objects));
            from.releaseLast();
        }
    }

    @Test
    @DisplayName("copy bytes after skip matches source")
    public void testCopyAfterSkip() {
        final Bytes<byte[]> src = Bytes.from("hello again");

        src.readSkip(7);
        BytesStore<Bytes<byte[]>, byte[]> copy = src.copy();
        assertEquals(copy.toString(), src.toString(),
                "Copy matches source after skip");
        // shouldn't need to do this
        copy.releaseLast();
    }

    @Test
    @DisplayName("copy to array after skip matches source")
    public void testCopyToArrayAfterSkip() {
        final Bytes<byte[]> src = Bytes.from("hello again");
        src.readSkip(7);

        final byte[] buffer = new byte[100];
        final int copiedLen = src.copyTo(buffer);
        assertEquals(new String(buffer, 0, copiedLen, StandardCharsets.ISO_8859_1), src.toString(),
                "Copy to array matches source after skip");
    }

    private int checkParse(int different, String s) {
        double d = Double.parseDouble(s);
        Bytes<?> from = Bytes.from(s);
        double d2 = from.parseDouble();
        from.releaseLast();
        if (d != d2) {
            ++different;
        }
        return different;
    }

    @Test
    @DisplayName("parse double seeded random values consistently across runs")
    public void bytesParseDouble_Issue85_SeededRandom() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "Guarded native bytes disabled for seeded parse test");
        Random random = new Random(1);
        int different = 0;
        int max = 10_000;
        for (int i = 0; i < max; i++) {
            double num = random.nextDouble();
            String s = String.format(Locale.UK, "%.9f", num);
            different = checkParse(different, s);
        }
        assertEquals(0, different,
                "Different " + (100.0 * different) / max + "%");
    }

    @Test
    @Disabled("Performance test for direct write strategies")
    @DisplayName("compare direct write performance strategies with consistent ordering")
    public void testNoneDirectWritePerformance() {
        final int size = 64;
        Bytes<?> a = Bytes.allocateElasticOnHeap(size + 8);
        Bytes<?> b = Bytes.allocateElasticOnHeap(size + 8);
        Bytes<?> c = Bytes.allocateElasticOnHeap(size + 8);
        Bytes<?> d = Bytes.allocateElasticOnHeap(size + 8);
        Bytes<?> e = Bytes.allocateElasticOnHeap(size + 8);
        Bytes<?> f = Bytes.allocateElasticOnHeap(size + 8);
        Bytes<?> g = Bytes.allocateElasticOnHeap(size + 8);
        int retry = 1;
        for (int t = 0; t <= 4; t++) {
            long time1 = 0, time2 = 0, time3 = 0;
            long time4 = 0, time5 = 0, time6 = 0;
            final int runs = t == 0 ? 1_000 : 5_000;
            int count = 0;
            for (int i = 0; i < runs; i++) {
                for (int o = 0; o <= 8; o++)
                    for (int s = 0; s <= size - o; s++) {
                        long start1 = 0, end1 = 0, start2 = 0, end2 = 0, start3 = 0, end3 = 0;
                        long start4 = 0, end4 = 0, start5 = 0, end5 = 0, start6 = 0, end6 = 0;
                        for (int r = 0; r < retry; r++) {
                            a.clear().writeSkip(size);
                            b.clear().writeSkip(t);
                            start1 = System.nanoTime();
                            BytesInternal.writeFully(a, o, s, b);
                            end1 = System.nanoTime();
                        }

                        for (int r = 0; r < retry; r++) {
                            a.clear().writeSkip(size);
                            c.clear().writeSkip(t);
                            start2 = System.nanoTime();
                            simpleWriteFully1(a, o, s, c);
                            end2 = System.nanoTime();
                        }
                        for (int r = 0; r < retry; r++) {
                            a.clear().writeSkip(size);
                            d.clear().writeSkip(t);
                            start3 = System.nanoTime();
                            oldWriteFully(a, o, s, d);
                            end3 = System.nanoTime();
                        }

                        for (int r = 0; r < retry; r++) {
                            a.clear().writeSkip(size);
                            d.clear().writeSkip(t);
                            start4 = System.nanoTime();
                            simpleWriteFully2(a, o, s, d);
                            end4 = System.nanoTime();
                        }
                        for (int r = 0; r < retry; r++) {
                            a.clear().writeSkip(size);
                            e.clear().writeSkip(t);
                            start5 = System.nanoTime();
                            simpleWriteFully3(a, o, s, e);
                            end5 = System.nanoTime();
                        }
                        for (int r = 0; r < retry; r++) {
                            a.clear().writeSkip(size);
                            g.clear().writeSkip(t);
                            start6 = System.nanoTime();
                            simpleWriteFully4(a, o, s, g);
                            end6 = System.nanoTime();
                        }
                        time1 += end1 - start1;
                        time2 += end2 - start2;
                        time3 += end3 - start3;
                        time4 += end4 - start4;
                        time5 += end5 - start5;
                        time6 += end6 - start6;
                        count++;
                    }
            }
            time1 /= count;
            time2 /= count;
            time3 /= count;
            time4 /= count;
            time5 /= count;
            time6 /= count;

            System.out.println("time1 " + time1 + ", time2 " + time2 + ", time3: " + time3);
            System.out.println("time4 " + time4 + ", time5 " + time5 + ", time6: " + time6);

            // This is a performance test so just assert it ran
            assertTrue(time1 > 0, "time1 recorded positive duration at t " + t);
            assertTrue(time2 > 0, "time2 recorded positive duration at t " + t);
            assertTrue(time3 > 0, "time3 recorded positive duration at t " + t);
            assertTrue(time4 > 0, "time4 recorded positive duration at t " + t);
            assertTrue(time5 > 0, "time5 recorded positive duration at t " + t);
            assertTrue(time6 > 0, "time6 recorded positive duration at t " + t);
            Thread.yield();
        }
    }

    static class Nested {
        static final int LENGTH;

        static {
            long maxMemory = Runtime.getRuntime().maxMemory();
            int maxLength = OS.isLinux() ? 1 << 30 : 1 << 28;
            LENGTH = (int) Math.min(maxMemory / 32, maxLength);
            if (LENGTH < maxLength)
                System.out.println("Not enough memory to run big test, was " + (LENGTH >> 20) + " MB.");
        }
    }

    private static void simpleWriteFully1(@NotNull RandomDataInput bytes, @NonNegative long offset, long length, @NotNull StreamingDataOutput<?> sdo)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        long i = 0;
        for (; i < length - 7; i += 8)
            sdo.rawWriteLong(bytes.readLong(offset + i));
        for (; i < length; i++)
            sdo.rawWriteByte(bytes.readByte(offset + i));
    }

    private static void simpleWriteFully2(@NotNull RandomDataInput bytes, @NonNegative long offset, long length, @NotNull StreamingDataOutput<?> sdo)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        long i = 0;
        for (; i < length - 7; i += 8)
            sdo.rawWriteLong(bytes.readLong(offset + i));
        if (i < length - 3) {
            sdo.rawWriteInt(bytes.readInt(offset + i));
            i += 4;
        }
        for (; i < length; i++)
            sdo.rawWriteByte(bytes.readByte(offset + i));
    }

    private static void simpleWriteFully3(@NotNull RandomDataInput bytes, @NonNegative long offset, long length, @NotNull StreamingDataOutput<?> sdo)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        int i = 0;
        for (; i < length - 7; i += 8)
            sdo.rawWriteLong(bytes.readLong(offset + i));
        if (i < length - 3) {
            sdo.rawWriteInt(bytes.readInt(offset + i));
            i += 4;
        }
        for (; i < length; i++)
            sdo.rawWriteByte(bytes.readByte(offset + i));
    }

    private static void simpleWriteFully4(@NotNull RandomDataInput bytes, @NonNegative long offset, long length, @NotNull StreamingDataOutput<?> sdo)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        int i = 0;
        for (; i < length - 7; i += 8)
            sdo.rawWriteLong(bytes.readLong(offset + i));
        if (i < length - 3) {
            sdo.rawWriteInt(bytes.readInt(offset + i));
            i += 4;
        }
        for (; i < length; i++)
            sdo.rawWriteByte(bytes.readByte(offset + i));
    }

    private static void oldWriteFully(@NotNull RandomDataInput bytes, @NonNegative long offset, long length, @NotNull StreamingDataOutput<?> sdo)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        long i = 0;
        for (; i < length - 3; i += 4)
            sdo.rawWriteInt(bytes.readInt(offset + i));
        for (; i < length; i++)
            sdo.rawWriteByte(bytes.readByte(offset + i));
    }
}
