package net.openhft.chronicle.bytes;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AppendableUtilTest {

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @SuppressWarnings("rawtypes")
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
        b.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void setCharAt() {
        StringBuilder sb = new StringBuilder("hello world");
        Bytes b = Bytes.elasticHeapByteBuffer(16).append("Hello World");
        AppendableUtil.setCharAt(sb, 5, 'X');
        AppendableUtil.setCharAt(b, 5, 'X');
        assertEquals("helloXworld", sb.toString());
        assertEquals("HelloXWorld", b.toString());
    }

}