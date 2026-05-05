/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests stop-bit length computation because correct byte counts are essential
 * to avoid buffer overruns when reserving space for variable-length fields.
 */
@DisplayName("StopBitLength - validates byte count boundaries for stop-bit encoding")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class StopBitLengthTest extends BytesTestCommon {

    @Test
    @DisplayName("stop-bit length boundaries match expected byte counts")
    public void boundaries() {
        assertEquals(1,
                BytesUtil.stopBitLength(0),
                "Zero value should use one stop-bit byte");
        assertEquals(1,
                BytesUtil.stopBitLength(0x7F),
                "Maximum one-byte value should use one stop-bit byte");
        assertEquals(2,
                BytesUtil.stopBitLength(0x80),
                "First two-byte value should use two stop-bit bytes");
        assertEquals(2,
                BytesUtil.stopBitLength(0x3FFF),
                "Maximum two-byte value should use two stop-bit bytes");
        assertTrue(BytesUtil.stopBitLength(0x4000) >= 3,
                "First three-byte value should use at least three stop-bit bytes");
        assertTrue(BytesUtil.stopBitLength(Integer.MAX_VALUE) >= 3,
                "Maximum int value should use at least three stop-bit bytes");
        assertTrue(BytesUtil.stopBitLength(Long.MAX_VALUE) >= 9,
                "Maximum long value should use at least nine stop-bit bytes");
    }
}
