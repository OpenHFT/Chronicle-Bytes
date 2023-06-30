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

import net.openhft.chronicle.bytes.render.GeneralDecimaliser;
import net.openhft.chronicle.core.Maths;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.junit.Assume.assumeFalse;

public class Issue85Test extends BytesTestCommon {
    int different = 0;
    int different2 = 0;
    DecimalFormat df = new DecimalFormat();

    {
        df.setMaximumIntegerDigits(99);
        df.setMaximumFractionDigits(99);
        df.setMinimumFractionDigits(1);
        df.setGroupingUsed(false);
        df.setDecimalFormatSymbols(
                DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    }

    @SuppressWarnings("rawtypes")
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
    public void bytesParseDouble_Issue85_Many0() {
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);
        bytes.decimaliser(GeneralDecimaliser.GENERAL);
        assumeFalse(NativeBytes.areNewGuarded());
        int max = 100, count = 0;
        for (double d0 = 1e15; d0 >= 1e-8; d0 /= 10) {
            long val = Double.doubleToRawLongBits(d0);
            for (int i = -max / 2; i <= max / 2; i++) {
                double d = Double.longBitsToDouble(val + i);
                doTest(bytes, i, d);
            }
            count += max + 1;
        }
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < max * 1000; i++) {
            double d = Math.pow(1e12, rand.nextDouble()) / 1e3;
            doTest(bytes, 0, d);
            count++;
        }
        if (different + different2 > 0)
            Assert.fail("Different toString: " + 100.0 * different / count + "%," +
                    " parsing: " + 100.0 * different2 / count + "%");
    }

    protected void doTest(Bytes<ByteBuffer> bytes, int i, double d) {
        String s = df.format(d);
        bytes.clear().append(s);
        double d2 = bytes.parseDouble();
        if (d != d2) {
//            System.out.println(i + ": Parsing " + s + " != " + d2);
            ++different2;
        }

        String s2 = bytes.append(d).toString();
        double d3 = Double.parseDouble(s2);
        if (d != d3) {
//            System.out.println(i + ": ToString " + s + " != " + s2 + " should be " + new BigDecimal(d));
            ++different;
        }
    }

    @Test
    public void loseTrainingZeros() {
        double d = -541098.2421;
        Assert.assertEquals("" + d,
                Bytes.allocateElasticDirect()
                        .append(d)
                        .toString());

    }

    @Test
    public void loseTrainingZerosHeap() {
        double d = -541098.2421;
        Assert.assertEquals("" + d,
                Bytes.allocateElasticOnHeap()
                        .append(d)
                        .toString());

    }
}
