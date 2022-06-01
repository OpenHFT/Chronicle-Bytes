package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.bytes.internal.SingletonEmptyByteStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ReleasedBytesStoreTest extends BytesTestCommon {

    @Test
    public void release() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        assertEquals(SingletonEmptyByteStore.class, bytes.bytesStore().getClass());
        bytes.writeLong(0, 0);
        assertEquals(NativeBytesStore.class, bytes.bytesStore().getClass());
        bytes.releaseLast();
        assertEquals(0, bytes.bytesStore().refCount());
        try {
            bytes.writeLong(0, 0);
            fail();
        } catch (NullPointerException e) {
            // expected.
        }
    }
}