package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharsTester;
import org.junit.Test;
import org.junit.Before;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class EscapingStopCharsTesterTest {

    private StopCharsTester baseTester;
    private EscapingStopCharsTester tester;
    @Before
    public void setUp() {
        // Setup the base tester with specific behavior for demonstration
        baseTester = (ch, peekNextCh) -> ch == 'x'; // Let's say 'x' is a stop character
        tester = new EscapingStopCharsTester(baseTester);
    }

    @Test
    public void testIsStopCharWithEscape() {
        // First call with escape character
        assertFalse("Escaped character should not be stop char", tester.isStopChar('\\', 'x'));
        // Next call with the character that would normally be a stop character
        assertFalse("Character following an escape should not be treated as stop char", tester.isStopChar('x', ' '));
        // Subsequent call with a stop character not preceded by an escape
        assertTrue("Non-escaped stop char should be recognized as stop char", tester.isStopChar('x', ' '));
    }

    @Test
    public void testIsStopCharWithoutEscape() {
        assertFalse("Non-stop char should not be recognized as stop char", tester.isStopChar('y', ' '));
    }
}
