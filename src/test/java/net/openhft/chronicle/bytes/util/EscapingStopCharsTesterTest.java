/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharsTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class EscapingStopCharsTesterTest {

    private StopCharsTester baseTester;
    private EscapingStopCharsTester tester;

    @BeforeEach
    void setUp() {
        // Setup the base tester with specific behavior for demonstration
        baseTester = (ch, peekNextCh) -> ch == 'x'; // Let's say 'x' is a stop character
        tester = new EscapingStopCharsTester(baseTester);
    }

    @Test
    void testIsStopCharWithEscape() {
        // First call with escape character
        assertFalse(tester.isStopChar('\\', 'x'), "Escaped character should not be stop char");
        // Next call with the character that would normally be a stop character
        assertFalse(tester.isStopChar('x', ' '), "Character following an escape should not be treated as stop char");
        // Subsequent call with a stop character not preceded by an escape
        assertTrue(tester.isStopChar('x', ' '), "Non-escaped stop char should be recognized as stop char");
    }

    @Test
    void testIsStopCharWithoutEscape() {
        assertFalse(tester.isStopChar('y', ' '), "Non-stop char should not be recognized as stop char");
    }
}
