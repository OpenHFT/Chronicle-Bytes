package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StreamingDataInputTest {
    @Test
    public void read() {
        Bytes b = Bytes.allocateDirect(128);
        b.append("0123456789");
        byte[] byteArr = "ABCDEFGHIJKLMNOP".getBytes();
        b.read(byteArr, 2, 6);
        assertEquals("AB012345IJKLMNOP", new String(byteArr, 0));
        assertEquals('6', b.readByte());
        b.release();
    }
}