/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EscapingStopCharTesterTest {

    @Test
    @DisplayName("escape character suppresses a single stop character")
    public void testIsStopCharWithAndWithoutEscape() {
        // Setup a StopCharTester that considers 'x' as a stop character
        StopCharTester baseTester = ch -> ch == 'x';

        EscapingStopCharTester escapingTester = new EscapingStopCharTester(baseTester);

        // Verify that 'x' is normally considered a stop character
        assertTrue("EscapingStopCharTester should treat 'x' as a stop character",
                escapingTester.isStopChar('x'));

        // Simulate escaping by passing the escape character before 'x'
        assertFalse("Escape character should not be considered a stop character", escapingTester.isStopChar('\\'));
        assertFalse("Escaped 'x' should not be considered a stop character", escapingTester.isStopChar('x'));

        // Ensure 'x' is considered a stop character again after escaping
        assertTrue("EscapingStopCharTester should treat 'x' as a stop character when not escaped",
                escapingTester.isStopChar('x'));
    }

    @Test
    @DisplayName("escape characters do not change base tester behaviour")
    public void testEscapingStopCharTester() {
        StopCharTester baseTester = ch -> ch == 'x'; // Let's say 'x' is a stop character
        EscapingStopCharTester tester = new EscapingStopCharTester(baseTester);

        assertFalse("First escape character should not be stop char", tester.isStopChar('\\'));
        assertFalse("Second escape character should not be stop char", tester.isStopChar('\\'));
        assertTrue("Non-escaped character following escapes should be considered stop char if it matches baseTester", tester.isStopChar('x'));
    }
}
