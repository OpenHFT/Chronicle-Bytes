package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Bit8StringInternerTest {

    @Test
    public void testGetValue() {
        BytesStore bytesStore = Bytes.from("Hello World");
        int length = (int) bytesStore.readRemaining();

        Bit8StringInterner interner = new Bit8StringInterner(16);

        String internedString = interner.getValue(bytesStore, length);

        assertEquals("Hello World", internedString);
    }
}
