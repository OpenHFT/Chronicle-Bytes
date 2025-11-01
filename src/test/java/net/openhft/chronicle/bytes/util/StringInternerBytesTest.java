/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringInternerBytesTest extends BytesTestCommon {

    @Test
    public void testIntern() {
        @NotNull StringInternerBytes si = new StringInternerBytes(128);
        for (int i = 0; i < 100; i++) {
            Bytes<?> b = Bytes.from("key" + i);
            si.intern(b, (int) b.readRemaining());
            b.releaseLast();
        }
        assertEquals(89, si.valueCount());
    }
}
