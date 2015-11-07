package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testCompareUTF() {
        NativeBytesStore<Void> bs = NativeBytesStore.nativeStore(32);
        bs.writeUtf8(0, "test");
        assertTrue(BytesInternal.compareUTF(bs, 0, "test"));
        assertFalse(BytesInternal.compareUTF(bs, 0, null));

        bs.writeUtf8(0, null);
        assertTrue(BytesInternal.compareUTF(bs, 0, null));
        assertFalse(BytesInternal.compareUTF(bs, 0, "test"));

        bs.writeUtf8(1, "£€");
        StringBuilder sb = new StringBuilder();
        bs.readUtf8(1, sb);
        assertEquals("£€", sb.toString());
        assertTrue(BytesInternal.compareUTF(bs, 1, "£€"));
    }
}