package net.openhft.chronicle.bytes;

import org.junit.Test;
import static org.junit.Assert.*;

public class StopCharTesterTest {

    @Test
    public void testIsStopChar() {
        StopCharTester tester = ch -> ch == ';' || ch == ',';

        assertTrue("Semicolon should be a stop char", tester.isStopChar(';'));
        assertTrue("Comma should be a stop char", tester.isStopChar(','));
        assertFalse("Letter should not be a stop char", tester.isStopChar('A'));
    }

    @Test
    public void testEscaping() {
        StopCharTester baseTester = ch -> ch == ';';
        StopCharTester escapingTester = baseTester.escaping();

        assertTrue("Semicolon should be a stop char without escaping", baseTester.isStopChar(';'));
        assertFalse("Escaped semicolon should not be a stop char", escapingTester.isStopChar('\\'));
    }
}
