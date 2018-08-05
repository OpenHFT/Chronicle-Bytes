package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HexDumpBytesTest {

    @Test
    public void offsetFormat() {
        HexDumpBytes bytes = new HexDumpBytes()
                .numberWrap(8)
                .offsetFormat((o, b) -> b.appendBase16(o, 4));
        bytes.comment("hi").write(new byte[18]);
        bytes.indent(1);
        bytes.comment("nest").write(new byte[18]);
        assertEquals("0000 00 00 00 00 00 00 00 00 # hi\n" +
                "0008 00 00 00 00 00 00 00 00\n" +
                "0010 00 00\n" +
                "0012    00 00 00 00 00 00 00 00 # nest\n" +
                "001a    00 00 00 00 00 00 00 00\n" +
                "0022    00 00\n", bytes.toHexString());
    }
}