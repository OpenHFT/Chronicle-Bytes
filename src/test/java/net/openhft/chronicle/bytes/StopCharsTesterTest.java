package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Test;

public class StopCharsTesterTest {

    @Test
    public void testCustomStopCharsTester() {
        StopCharsTester tester = (ch, peekNextCh) -> ch == ',' || ch == ';';

        Assert.assertTrue(tester.isStopChar(',', 0));
        Assert.assertTrue(tester.isStopChar(';', 0));
        Assert.assertFalse(tester.isStopChar('a', 0));
    }
}
