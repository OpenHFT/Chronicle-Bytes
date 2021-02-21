package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesStoreTest extends BytesTestCommon {
    @Test
    public void from() {
        BytesStore from = BytesStore.from(", ");
        assertEquals(2, from.capacity());
        from.releaseLast();
    }

    @Test
    public void from2() {
        Bytes hello = Bytes.from("Hello").subBytes(0, 5).bytesForRead();
        assertEquals("Hello", hello.toString());

        Bytes hell = Bytes.from("Hello").subBytes(0, 4).bytesForRead();
        assertEquals("Hell", hell.toString());

        Bytes ell = Bytes.from("Hello").subBytes(1, 3).bytesForRead();
        assertEquals("ell", ell.toString());
    }
}
