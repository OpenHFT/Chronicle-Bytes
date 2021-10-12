package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Issue225Test {
    @Test
    public void testTrailingZeros() {
        for (int i = 1000; i < 10_000; i++) {
            double value = i / 1000.0;
            final String valueStr = "" + value;
            Bytes<?> bytes = Bytes.elasticByteBuffer();
            byte[] rbytes = new byte[24];
            bytes.append(value);
            assertEquals(value, bytes.parseDouble(), 0.0);
            assertEquals(valueStr.length() - 2, bytes.lastDecimalPlaces());
            bytes.readPosition(0);
            int length = bytes.read(rbytes);
            assertEquals(valueStr.length(), length);
            final String substring = new String(rbytes).substring(0, (int) bytes.writePosition());
            assertEquals(valueStr, substring);
        }
    }
}
