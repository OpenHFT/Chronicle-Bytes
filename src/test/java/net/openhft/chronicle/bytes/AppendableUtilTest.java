package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by peter on 17/01/2017.
 */
public class AppendableUtilTest {
    @Test
    public void setLength() {
        StringBuilder sb = new StringBuilder("hello world");
        AppendableUtil.setLength(sb, 5);
        assertEquals("hello", sb.toString());

        Bytes b = Bytes.from("Hello World");
        AppendableUtil.setLength(b, 5);
        assertEquals("Hello", b.toString());

        StringBuffer sb2 = new StringBuffer();
        try {
            AppendableUtil.setLength(sb2, 0);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected.
        }
    }

}