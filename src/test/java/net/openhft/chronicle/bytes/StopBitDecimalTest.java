package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assume.assumeFalse;

public class StopBitDecimalTest {
    @Test
    public void testDecimals() {
        assumeFalse(NativeBytes.areNewGuarded());

        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(16);
        Random rand = new Random();
        for (int i = 0; i < 10_000; i++) {
            rand.setSeed(i);
            bytes.clear();
            int scale = rand.nextInt(10);
            double d = (rand.nextLong() % 1e14) / Maths.tens(scale);
            bytes.writeStopBitDecimal(d);
            BigDecimal bd = BigDecimal.valueOf(d);
            long v = bytes.readStopBit();
            BigDecimal ebd = new BigDecimal(BigInteger.valueOf(v / 10), (int) (Math.abs(v) % 10));
            assertEquals("i: " + i + ", d: " + d + ", v: " + v, ebd.doubleValue(), bd.doubleValue(), 0.0);
            bytes.readPosition(0);
            double d2 = bytes.readStopBitDecimal();
            assertEquals("i: " + i + ", d: " + d + ", v: " + v, d, d2, 0.0);
        }

    }
}
