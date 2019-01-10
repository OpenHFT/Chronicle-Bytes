package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;

public class Issue85Test {
    static double parseDouble(Bytes bytes) {
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
    @Ignore("https://github.com/OpenHFT/Chronicle-Bytes/issues/85")
    public void bytesParseDouble_Issue85_Many0() {
        int different = 0;
        int different2 = 0;
        int max = 1000;
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);
        long val = Double.doubleToRawLongBits(1e-3);
        for (int i = 3; i < max; i++) {
            double d = Double.longBitsToDouble(val + i);
            String s = Double.toString(d);
/*
            if (!s.equals(s2)) {
                System.out.println("ToString " + s + " != " + s2+" should be " + new BigDecimal(d));
                ++different;
            }
*/
            String s2 = bytes.clear().append(s).toString();
            double d2 = parseDouble(bytes);
            if (d != d2) {
                System.out.println(i + ": Parsing " + d + " != " + d2);
                ++different2;
            }
        }
        System.out.println("Different toString: " + 100.0 * different / max + "%," +
                " parsing: " + 100.0 * different2 / max + "%");
    }
}
