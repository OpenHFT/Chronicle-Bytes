package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharTester;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EscapingStopCharTesterTest {

    @Test
    public void testIsStopCharWithAndWithoutEscape() {
        // Setup a StopCharTester that considers 'x' as a stop character
        StopCharTester baseTester = ch -> ch == 'x';

        EscapingStopCharTester escapingTester = new EscapingStopCharTester(baseTester);

        // Verify that 'x' is normally considered a stop character
        assertTrue("Expected 'x' to be a stop character", escapingTester.isStopChar('x'));

        // Simulate escaping by passing the escape character before 'x'
        assertFalse("Escape character should not be considered a stop character", escapingTester.isStopChar('\\'));
        assertFalse("Escaped 'x' should not be considered a stop character", escapingTester.isStopChar('x'));

        // Ensure 'x' is considered a stop character again after escaping
        assertTrue("Expected 'x' to be recognized as a stop character when not escaped", escapingTester.isStopChar('x'));
    }

    @Test
    public void testEscapingStopCharTester() {
        StopCharTester baseTester = ch -> ch == 'x'; // Let's say 'x' is a stop character
        EscapingStopCharTester tester = new EscapingStopCharTester(baseTester);

        assertFalse("First escape character should not be stop char", tester.isStopChar('\\'));
        assertFalse("Second escape character should not be stop char", tester.isStopChar('\\'));
        assertTrue("Non-escaped character following escapes should be considered stop char if it matches baseTester", tester.isStopChar('x'));
    }

}
