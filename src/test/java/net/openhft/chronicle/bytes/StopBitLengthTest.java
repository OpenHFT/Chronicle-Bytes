/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StopBitLengthTest extends BytesTestCommon {

    @Test
    public void boundaries() {
        assertEquals(1, BytesUtil.stopBitLength(0));
        assertEquals(1, BytesUtil.stopBitLength(0x7F));
        assertEquals(2, BytesUtil.stopBitLength(0x80));
        assertEquals(2, BytesUtil.stopBitLength(0x3FFF));
        assertTrue(BytesUtil.stopBitLength(0x4000) >= 3);
        assertTrue(BytesUtil.stopBitLength(Integer.MAX_VALUE) >= 3);
        assertTrue(BytesUtil.stopBitLength(Long.MAX_VALUE) >= 9);
    }
}

