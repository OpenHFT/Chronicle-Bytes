/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharTester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EscapingStopCharTesterTest {

    @Test
    public void testIsStopCharWithAndWithoutEscape() {
        // Setup a StopCharTester that considers 'x' as a stop character
        StopCharTester baseTester = ch -> ch == 'x';

        EscapingStopCharTester escapingTester = new EscapingStopCharTester(baseTester);

        // Verify that 'x' is normally considered a stop character
        assertTrue(escapingTester.isStopChar('x'), "Expected 'x' to be a stop character");

        // Simulate escaping by passing the escape character before 'x'
        assertFalse(escapingTester.isStopChar('\\'), "Escape character should not be considered a stop character");
        assertFalse(escapingTester.isStopChar('x'), "Escaped 'x' should not be considered a stop character");

        // Ensure 'x' is considered a stop character again after escaping
        assertTrue(escapingTester.isStopChar('x'), "Expected 'x' to be recognized as a stop character when not escaped");
    }

    @Test
    public void testEscapingStopCharTester() {
        StopCharTester baseTester = ch -> ch == 'x'; // Let's say 'x' is a stop character
        EscapingStopCharTester tester = new EscapingStopCharTester(baseTester);

        assertFalse(tester.isStopChar('\\'), "First escape character should not be stop char");
        assertFalse(tester.isStopChar('\\'), "Second escape character should not be stop char");
        assertTrue(tester.isStopChar('x'), "Non-escaped character following escapes should be considered stop char if it matches baseTester");
    }

}
