package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StopBitTest {

    @Test
    public void testStopBit() {

        for (int i = 0; i < (1 << 10) + 1; i++) {
            final String expected = IntStream.range(0, i)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining());

            final Bytes<byte[]> expectedBytes = Bytes.from(expected);

            final Bytes<ByteBuffer> b = Bytes.elasticByteBuffer();

            b.write8bit(expectedBytes);

            // System.out.printf("0x%04x : %02x %02x %02x%n", i, b.readByte(0), b.readByte(1), b.readByte(3));

            Assert.assertEquals("failed at "+i, expected, b.read8bit());
        }

    }

    @Test
    public void testStopBitShort() {

        final String s = IntStream.range(0, 1)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining());

        final Bytes<byte[]> bytes = Bytes.from(s);

        final Bytes<ByteBuffer> b = Bytes.elasticByteBuffer();

        b.write8bit(bytes);

        Assert.assertEquals(s, b.read8bit());

    }

}
