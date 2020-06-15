package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ReleasedBytesStoreTest extends BytesTestCommon {

    @Test
    public void release() {
        Bytes bytes = Bytes.allocateElasticDirect();
        assertEquals(NoBytesStore.class, bytes.bytesStore().getClass());
        bytes.writeLong(0, 0);
        assertEquals(NativeBytesStore.class, bytes.bytesStore().getClass());
        bytes.releaseLast();
        assertEquals(ReleasedBytesStore.class, bytes.bytesStore().getClass());
        try {
            bytes.writeLong(0, 0);
            fail();
        } catch (IllegalStateException e) {
            // expected.
        }
    }
}