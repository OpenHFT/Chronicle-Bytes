/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharsTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

public class EscapingStopCharsTesterTest {

    private StopCharsTester baseTester;
    private EscapingStopCharsTester tester;
    @BeforeEach
    public void setUp() {
        // Setup the base tester with specific behavior for demonstration
        baseTester = (ch, peekNextCh) -> ch == 'x'; // Let's say 'x' is a stop character
        tester = new EscapingStopCharsTester(baseTester);
    }

    @Test
    @DisplayName("escape character suppresses stop character for one subsequent call")
    public void testIsStopCharWithEscape() {
        // First call with escape character
        assertFalse("Escaped character should not be stop char", tester.isStopChar('\\', 'x'));
        // Next call with the character that would normally be a stop character
        assertFalse("Character following an escape should not be treated as stop char", tester.isStopChar('x', ' '));
        // Subsequent call with a stop character not preceded by an escape
        assertTrue("Non-escaped stop char should be recognized as stop char", tester.isStopChar('x', ' '));
    }

    @Test
    @DisplayName("non-stop characters are not treated as stop characters by tester")
    public void testIsStopCharWithoutEscape() {
        assertFalse("Non-stop char should not be recognized as stop char", tester.isStopChar('y', ' '));
    }
}
