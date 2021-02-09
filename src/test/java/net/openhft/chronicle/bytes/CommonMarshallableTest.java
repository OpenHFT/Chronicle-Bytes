package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CommonMarshallableTest extends BytesTestCommon {

    @Test
    public void usesSelfDescribingMessage() {
        assertTrue(new CommonMarshallable() {
        }.usesSelfDescribingMessage());
    }
}