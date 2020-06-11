package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Test;

public class StopBitTest extends BytesTestCommon {
    @Test
    public void testString() {
        HexDumpBytes bytes = new HexDumpBytes();

        for (long i : new long[]{
                0, -1,
                127, -127,
                128, -128,
                1 << 14, 1 << 21,
                1 << 28, 1L << 35,
                1L << 42, 1L << 49,
                1L << 56, Long.MAX_VALUE,
                Long.MIN_VALUE}) {
            bytes.comment(i + "L").writeStopBit(i);
        }

        for (double d : new double[]{
                0.0,
                -0.0,
                1.0,
                1.0625,
                -128,
                -Double.MIN_NORMAL,
                Double.NEGATIVE_INFINITY,
                Double.NaN,
                Double.POSITIVE_INFINITY}) {
            bytes.comment(d + "").writeStopBit(d);
        }

        System.out.println(bytes.toHexString());
        bytes.releaseLast();
    }
}
