package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.UnsafeText;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class UnsafeTextBytesTest extends BytesTestCommon {
    @Test
    public void appendBase10() {
        Bytes bytes = Bytes.allocateDirect(32);
        for (long l = Long.MAX_VALUE; l > 0; l /= 2) {
            testAppendBase10(bytes, l);
            testAppendBase10(bytes, 1 - l);
        }
        bytes.release();
    }

    static void testAppendBase10(Bytes bytes, long l) {
        long address = bytes.clear().addressForRead(0);
        long end = UnsafeText.appendFixed(address, l);
        bytes.readLimit(end - address);
        String message = bytes.toString();
        assertEquals(message, l, bytes.parseLong());
    }

    static void testAppendDouble(Bytes bytes, double l) {
        long address = bytes.clear().addressForRead(0);
        long end = UnsafeText.appendDouble(address, l);
        bytes.readLimit(end - address);
        String message = bytes.toString();
        assertEquals(message, l, bytes.parseDouble(), Math.ulp(l));
    }

    static void testAppendFixed(Bytes bytes, double l, int digits) {
        long address = bytes.clear().addressForRead(0);
        long end = UnsafeText.appendFixed(address, l, digits);
        bytes.readLimit(end - address);
        String message = bytes.toString();
        double expected = Maths.round4(l);
        double actual = bytes.parseDouble();
        assertEquals(message, expected, actual, 0.0);
    }

    @Test
    public void appendDouble() {
        Random rand = new Random(1);
        Bytes bytes = Bytes.allocateDirect(32);
        testAppendFixed(bytes, 0.0003, 4);
        for (int i = 0; i < 1000000; i++) {
            double d = Math.pow(1e16, rand.nextDouble()) / 1e4;
            testAppendDouble(bytes, d);
            testAppendFixed(bytes, d, 4);
        }
        bytes.release();
    }

    @Test
    public void appendDouble2() {
        Bytes bytes = Bytes.allocateDirect(32);
        for (double d : new double[]{
                741138311171.555,
                0.0, -0.0, 0.1, 0.012, 0.00123, 1.0, Double.NaN, 1 / 0.0, -1 / 0.0})
            testAppendDouble(bytes, d);
        bytes.release();
    }
}
