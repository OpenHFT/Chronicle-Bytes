/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StopCharsTesterTest {

    @Test
    public void testCustomStopCharsTester() {
        StopCharsTester tester = (ch, peekNextCh) -> ch == ',' || ch == ';';

        assertTrue(tester.isStopChar(',', 0), "tester.isStopChar");
        assertTrue(tester.isStopChar(';', 0), "tester.isStopChar");
        assertFalse(tester.isStopChar('a', 0), "tester.isStopChar");
    }
}
