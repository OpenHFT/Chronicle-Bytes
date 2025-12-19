/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StopBitLengthTest extends BytesTestCommon {

    @Test
    public void boundaries() {
        assertEquals(1, BytesUtil.stopBitLength(0), "stop-bit length for 0 should be 1 byte");
        assertEquals(1, BytesUtil.stopBitLength(0x7F), "stop-bit length for 0x7F (max 1-byte value) should be 1 byte");
        assertEquals(2, BytesUtil.stopBitLength(0x80), "stop-bit length for 0x80 (min 2-byte value) should be 2 bytes");
        assertEquals(2, BytesUtil.stopBitLength(0x3FFF), "stop-bit length for 0x3FFF (max 2-byte value) should be 2 bytes");
        assertTrue(BytesUtil.stopBitLength(0x4000) >= 3, "stop-bit length for 0x4000 should be at least 3 bytes");
        assertTrue(BytesUtil.stopBitLength(Integer.MAX_VALUE) >= 3, "stop-bit length for Integer.MAX_VALUE should be at least 3 bytes");
        assertTrue(BytesUtil.stopBitLength(Long.MAX_VALUE) >= 9, "stop-bit length for Long.MAX_VALUE should be at least 9 bytes");
    }
}

