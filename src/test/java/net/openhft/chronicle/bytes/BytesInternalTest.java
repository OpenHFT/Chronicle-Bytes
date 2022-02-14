package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.Random;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static net.openhft.chronicle.bytes.BytesInternalTest.Nested.LENGTH;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class BytesInternalTest extends BytesTestCommon {
    @Test
    public void testParseUTF_SB1()
            throws UTFDataFormatRuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        byte[] bytes2 = new byte[128];
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
        byte[] bytes2 = new byte[length];
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
    public void parseLongEmpty() {
        for (String s : ", , .,-,x, .e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(s, 0, from.parseLong());
            assertFalse(s, from.lastNumberHadDigits());
        }
    }

    @Test
    public void parseLongNonEmpty() {
        for (String s : "0, 0, 0..,0-, 0e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(s, 0, from.parseLong());
            assertTrue(s, from.lastNumberHadDigits());
        }
    }

    @Test
    public void parseLongDecimalEmpty() {
        for (String s : ", , .,-,x, .e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(s, 0, from.parseLongDecimal());
            assertFalse(s, from.lastNumberHadDigits());
        }
    }

    @Test
    public void parseLongDecimalNonEmpty() {
        for (String s : "0, 0, .0,0-,0x, .0e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(s, 0, from.parseLongDecimal());
            assertTrue(s, from.lastNumberHadDigits());
        }
    }

    @Test
    public void parseDoubleEmpty() {
        for (String s : ", , .,-,x, .e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(s, 0, Double.compare(-0.0, from.parseDouble()));
            assertFalse(s, from.lastNumberHadDigits());
        }
    }

    @Test
    public void parseDoubleEmptyZero() {
        for (String s : "0, 0, .0,0-,0x, .0e".split(",")) {
            final Bytes<byte[]> from = Bytes.from(s);
            assertEquals(s, 0, Double.compare(0.0, from.parseDouble()));
            assertTrue(s, from.lastNumberHadDigits());
        }
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
        byte[] bytes2 = new byte[length];
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
        byte[] bytes2 = new byte[length];
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
        byte[] bytes2 = new byte[length];
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
    public void testParseDouble() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
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
            assertEquals(expected, from.parseDouble(), 0.0);
            assertTrue(from.lastNumberHadDigits());
            from.releaseLast();
        }
    }

    @Test
    public void testCopyAfterSkip() {
        final Bytes<byte[]> src = Bytes.from("hello again");

        src.readSkip(7);
        assertEquals(src.copy(), src);
    }

    @Test
    public void testCopyToArrayAfterSkip() {
        final Bytes<byte[]> src = Bytes.from("hello again");
        src.readSkip(7);

        final byte[] buffer = new byte[100];
        final int copiedLen = src.copyTo(buffer);
        assertEquals(new String(buffer, 0, copiedLen), src.toString());
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

    @Test
    public void testNoneDirectWritePerformance() {
        final int size = 64;
        Bytes a = Bytes.allocateElasticOnHeap(size + 8);
        Bytes b = Bytes.allocateElasticOnHeap(size + 8);
        Bytes c = Bytes.allocateElasticOnHeap(size + 8);
        Bytes d = Bytes.allocateElasticOnHeap(size + 8);
        Bytes e = Bytes.allocateElasticOnHeap(size + 8);
        Bytes f = Bytes.allocateElasticOnHeap(size + 8);
        Bytes g = Bytes.allocateElasticOnHeap(size + 8);
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
/*
            if (t > 0) {
                assertTrue(time1 < time2);
                assertTrue(time1 < time3 * 0.9);
            }


*/
            // This is a performance test so just assert it ran
            assertTrue(time1 > 0);
            assertTrue(time2 > 0);
            assertTrue(time3 > 0);
            assertTrue(time4 > 0);
            assertTrue(time5 > 0);
            assertTrue(time6 > 0);
            Thread.yield();
        }
    }

    public static void simpleWriteFully1(@NotNull RandomDataInput bytes, long offset, long length, @NotNull StreamingDataOutput sdo)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        long i = 0;
        for (; i < length - 7; i += 8)
            sdo.rawWriteLong(bytes.readLong(offset + i));
        for (; i < length; i++)
            sdo.rawWriteByte(bytes.readByte(offset + i));
    }

    public static void simpleWriteFully2(@NotNull RandomDataInput bytes, long offset, long length, @NotNull StreamingDataOutput sdo)
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

    public static void simpleWriteFully3(@NotNull RandomDataInput bytes, long offset, long length, @NotNull StreamingDataOutput sdo)
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

    public static void simpleWriteFully4(@NotNull RandomDataInput bytes, long offset, long length, @NotNull StreamingDataOutput sdo)
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

    public static void oldWriteFully(@NotNull RandomDataInput bytes, long offset, long length, @NotNull StreamingDataOutput sdo)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        long i = 0;
        for (; i < length - 3; i += 4)
            sdo.rawWriteInt(bytes.readInt(offset + i));
        for (; i < length; i++)
            sdo.rawWriteByte(bytes.readByte(offset + i));
    }

}
