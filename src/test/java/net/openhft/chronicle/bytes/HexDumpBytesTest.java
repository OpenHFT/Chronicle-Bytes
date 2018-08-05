package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HexDumpBytesTest {

    @Test
    public void offsetFormat() {
        HexDumpBytes bytes = new HexDumpBytes()
                .offsetFormat((o, b) -> b.appendBase16(o, 4));
        bytes.comment("hi").write(new byte[17]);
        assertEquals("0000 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # hi\n" +
                "0010 00\n", bytes.toHexString());
    }
}