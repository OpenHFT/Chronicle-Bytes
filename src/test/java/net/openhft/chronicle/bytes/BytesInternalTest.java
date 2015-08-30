package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class BytesInternalTest {

    @Test
    public void testParseUTF_SB1() throws Exception {
        VanillaBytes bytes = Bytes.allocateElasticDirect();
        byte[] bytes2 = new byte[128];
        Arrays.fill(bytes2, (byte) '?');
        bytes.write(bytes2);

        StringBuilder sb = new StringBuilder();

        BytesInternal.parseUTF(bytes, sb, 128);
        assertEquals(128, sb.length());
        assertEquals(new String(bytes2, 0), sb.toString());
    }
}