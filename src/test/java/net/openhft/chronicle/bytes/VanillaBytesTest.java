package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Peter on 17/06/2016.
 */
public class VanillaBytesTest {
    @Test
    public void testBytesForRead() {
        byte[] byteArr = new byte[128];
        for (int i = 0; i < byteArr.length; i++)
            byteArr[i] = (byte) i;
        Bytes bytes = Bytes.wrapForRead(byteArr);
        bytes.readSkip(8);
        Bytes bytes2 = bytes.bytesForRead();
        assertEquals(128 - 8, bytes2.readRemaining());
        assertEquals(8, bytes2.readPosition());
        assertEquals(8, bytes2.readByte(bytes2.start()));
        assertEquals(9, bytes2.readByte(bytes2.start() + 1));
        assertEquals(9, bytes.readByte(9));
        bytes2.writeByte(bytes2.start() + 1, 99);
        assertEquals(99, bytes.readByte(99));
    }
}
