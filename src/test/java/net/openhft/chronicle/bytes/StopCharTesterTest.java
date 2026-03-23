/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StopCharTesterTest {

    @Test
    public void testIsStopChar() {
        StopCharTester tester = ch -> ch == ';' || ch == ',';

        assertTrue(tester.isStopChar(';'), "Semicolon should be a stop char");
        assertTrue(tester.isStopChar(','), "Comma should be a stop char");
        assertFalse(tester.isStopChar('A'), "Letter should not be a stop char");
    }

    @Test
    public void testEscaping() {
        StopCharTester baseTester = ch -> ch == ';';
        StopCharTester escapingTester = baseTester.escaping();

        assertTrue(baseTester.isStopChar(';'), "Semicolon should be a stop char without escaping");
        assertFalse(escapingTester.isStopChar('\\'), "Escaped semicolon should not be a stop char");
    }
}
