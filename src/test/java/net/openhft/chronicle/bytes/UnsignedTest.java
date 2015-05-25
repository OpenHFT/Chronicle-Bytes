package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Rob Austin
 */
public class UnsignedTest {


    @Test
    public void testUnsignedByte() throws Exception {
        Bytes b = Bytes.elasticByteBuffer();

        b.writeUnsignedByte(256);
        b.flip();
        Assert.assertEquals(256, b.readUnsignedByte());
    }
}
