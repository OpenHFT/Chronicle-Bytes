package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesStoreTest {
    @Test
    public void from() {
        BytesStore from = BytesStore.from(", ");
        assertEquals(2, from.capacity());
    }

    @Test
    public void from2() {
        assertEquals("Hello", Bytes.from("Hello").subBytes(0, 5)
                .bytesForRead()
                .toString());
    }
}
