package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Created by peter.lawrey on 27/02/15.
 */
public class NativeBytesStoreTest {
    @Test
    public void testElasticByteBuffer() {
        Bytes<ByteBuffer> bbb = Bytes.elasticByteBuffer();
        assertEquals(1L << 40, bbb.capacity());
        assertEquals(OS.pageSize(), bbb.realCapacity());
        ByteBuffer bb = bbb.underlyingObject();
        assertNotNull(bb);

        for (int i = 0; i < 16; i++) {
            bbb.skip(1000);
            bbb.writeLong(12345);
        }
        assertEquals(OS.pageSize() * 4, bbb.realCapacity());
        ByteBuffer bb2 = bbb.underlyingObject();
        assertNotNull(bb2);
        assertNotSame(bb, bb2);
    }
}
