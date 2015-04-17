package net.openhft.chronicle.bytes;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by daniel on 17/04/15.
 */
public class NativeBytesTest {
    @Ignore
    @Test
    public void testWriteBytesWhereResizeNeeded() {
        Bytes bytes1 = NativeBytes.nativeBytes(1);
        Bytes<byte[]> wrap1 = Bytes.wrap("Hello".getBytes());
        bytes1.write(wrap1);
        bytes1.flip();
        assertEquals("Hello", bytes1.toString());

        Bytes bytes0 = NativeBytes.nativeBytes();
        Bytes<byte[]> wrap0 = Bytes.wrap("Hello".getBytes());
        bytes0.write(wrap0);
        bytes0.flip();
        assertEquals("Hello", bytes0.toString());
    }
}
