package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Test;

public class BytesTest {


    @Test
    public void testName() throws Exception {
        try (NativeBytesStore<Void> nativeStore = NativeBytesStore.nativeStore(30)) {
            Bytes<Void> bytes = nativeStore.bytes();

            long expected = 12345L;
            int offset = 5;

            bytes.writeLong(offset, expected);
            Assert.assertEquals(expected, bytes.readLong(offset));


        }
    }


}