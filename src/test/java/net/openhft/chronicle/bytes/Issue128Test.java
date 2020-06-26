package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Random;

import static net.openhft.chronicle.bytes.UnsafeTextBytesTest.testAppendDouble;
import static org.junit.Assert.assertEquals;

public class Issue128Test {
    private static final DecimalFormat DF;

    static {
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(30);
        DF = df;
    }

    static String toDecimal(double d) {
        return DF.format(d);
    }

    @Test
//    @Ignore("https://github.com/OpenHFT/Chronicle-Bytes/issues/128")
    public void testCorrect() {
        Bytes bytes = Bytes.allocateDirect(32);
        Random rand = new Random();
        try {
            for (int i = 0; i < 10000; i++) {
                // TODO FIX https://github.com/OpenHFT/Chronicle-Bytes/issues/128
                double smallest = 0.001;
                double v = Math.pow(smallest, rand.nextDouble());
                // TODO FIX https://github.com/OpenHFT/Chronicle-Bytes/issues/128
                v = Maths.round8(v);
                doTest(bytes, v);
                doTest(bytes, 1 + v);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    public void doTest(Bytes bytes, double v) {
        String format = DF.format(v);
        String output = testAppendDouble(bytes, v);
        if (Double.parseDouble(output) != v || format.length() != output.length())
            assertEquals(DF.format(v), output);
    }

}
