package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class CommonMarshallableTest {

    @Test
    public void usesSelfDescribingMessage() {
        assertFalse(new CommonMarshallable() {
        }.usesSelfDescribingMessage());
    }
}