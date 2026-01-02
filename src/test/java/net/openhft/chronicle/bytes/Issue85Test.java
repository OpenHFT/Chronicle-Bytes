/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.render.GeneralDecimaliser;
import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("Issue 85 decimal parsing and formatting")
public class Issue85Test extends BytesTestCommon {
    private int different = 0;
    private int different2 = 0;
    private DecimalFormat df = new DecimalFormat();

    {
        df.setMaximumIntegerDigits(99);
        df.setMaximumFractionDigits(99);
        df.setMinimumFractionDigits(1);
        df.setGroupingUsed(false);
        df.setDecimalFormatSymbols(
                DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    }

    static double parseDouble(Bytes<?> bytes) {
        long value = 0;
        int deci = Integer.MIN_VALUE;
        while (bytes.readRemaining() > 0) {
            byte ch = bytes.readByte();
            if (ch == '.') {
                deci = 0;
            } else if (ch >= '0' && ch <= '9') {
                value *= 10;
                value += ch - '0';
                deci++;
            } else {
                break;
            }
        }
        if (deci <= 0) {
            return value;
        }
        return asDouble(value, deci);
    }

    private static double asDouble(long value, int deci) {
        int scale2 = 0;
        int leading = Long.numberOfLeadingZeros(value);
        if (leading > 1) {
            scale2 = leading - 1;
            value <<= scale2;
        }
        long fives = Maths.fives(deci);
        long whole = value / fives;
        long rem = value % fives;
        double d = whole + (double) rem / fives;
        double scalb = Math.scalb(d, -deci - scale2);
        return scalb;
    }

    @Test
    @DisplayName("bytes parse double for many decimal ranges")
    public void bytesParseDouble_Issue85_Many0() {
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);
        bytes.decimaliser(GeneralDecimaliser.GENERAL);
        assumeFalse(NativeBytes.areNewGuarded(),
                "Guarded native bytes are disabled for issue 85 test");
        int max = 100, count = 0;
        for (double d0 = 1e15; d0 >= 1e-8; d0 /= 10) {
            long val = Double.doubleToRawLongBits(d0);
            for (int i = -max / 2; i <= max / 2; i++) {
                double d = Double.longBitsToDouble(val + i);
                doTest(bytes, i, d);
            }
            count += max + 1;
        }
        for (int i = 0; i < max * 1000; i++) {
            double d = Math.pow(1e12, ThreadLocalRandom.current().nextDouble()) / 1e3;
            doTest(bytes, 0, d);
            count++;
        }
        if (different + different2 > 0)
            fail("Decimaliser mismatch for toString " + 100.0 * different / count + "%, parsing "
                    + 100.0 * different2 / count + "%");
    }

    private void doTest(Bytes<ByteBuffer> bytes, int i, double d) {
        String s = df.format(d);
        bytes.clear().append(s);
        double d2 = bytes.parseDouble();
        if (d != d2) {
            ++different2;
        }

        String s2 = bytes.append(d).toString();
        double d3 = Double.parseDouble(s2);
        if (d != d3) {
            ++different;
        }
    }

    @Test
    @DisplayName("direct bytes preserve trailing zeros formatting")
    public void loseTrainingZeros() {
        double d = -541098.2421;
        assertEquals("" + d,
                Bytes.allocateElasticDirect()
                        .append(d)
                        .toString(),
                "Direct bytes preserve trailing zero formatting");

    }

    @Test
    @DisplayName("heap bytes preserve trailing zeros formatting")
    public void loseTrainingZerosHeap() {
        double d = -541098.2421;
        assertEquals("" + d,
                Bytes.allocateElasticOnHeap()
                        .append(d)
                        .toString(),
                "Heap bytes preserve trailing zero formatting");

    }
}
