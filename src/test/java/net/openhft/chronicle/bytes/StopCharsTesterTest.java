/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests custom stop-chars tester because applications must define parsing
 * delimiters so that tokenisation extracts the expected substrings.
 */
@DisplayName("StopCharsTester - validates custom delimiter detection logic")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class StopCharsTesterTest {

    @Test
    @DisplayName("custom stop-char tester flags comma and semicolon")
    public void testCustomStopCharsTester() {
        StopCharsTester tester = (ch, peekNextCh) -> ch == ',' || ch == ';';

        org.junit.jupiter.api.Assertions.assertTrue(tester.isStopChar(',', 0),
                "Comma should be treated as a stop character");
        org.junit.jupiter.api.Assertions.assertTrue(tester.isStopChar(';', 0),
                "Semicolon should be treated as a stop character");
        org.junit.jupiter.api.Assertions.assertFalse(tester.isStopChar('a', 0),
                "Letter characters should not be treated as stop characters");
    }
}
