package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class Issue462Test {
    final Bytes<ByteBuffer> bytes;

    public Issue462Test(Bytes<ByteBuffer> bytes) {
        this.bytes = bytes;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] tests = {
                {Bytes.elasticByteBuffer()},
                {Bytes.elasticByteBuffer(128)},
                {Bytes.elasticByteBuffer(128, 256)},
                {Bytes.elasticHeapByteBuffer()},
                {Bytes.elasticHeapByteBuffer(128)},
        };
        return Arrays.asList(tests);
    }

    @Test
    public void testByteBufferByteOrder() {
        final long value = 0x0102030405060708L;
        bytes.writeLong(value);
        final ByteBuffer byteBuffer = bytes.underlyingObject();
        assertEquals(ByteOrder.nativeOrder(), byteBuffer.order());
        final long aLong = byteBuffer.getLong();
        assertEquals(Long.toHexString(value),
                Long.toHexString(aLong));
        assertEquals(value, aLong);
        byteBuffer.putDouble(0, 0.1);
        final double actual = bytes.readDouble();
        assertEquals(Long.toHexString(Double.doubleToLongBits(0.1)),
                Long.toHexString(Double.doubleToLongBits(actual)));
        assertEquals(0.1, actual, 0.0);
    }
}
