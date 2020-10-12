package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesStoreTest {
    @Test
    public void from() {
        BytesStore from = BytesStore.from(", ");
        assertEquals(2, from.capacity());
    }
}
