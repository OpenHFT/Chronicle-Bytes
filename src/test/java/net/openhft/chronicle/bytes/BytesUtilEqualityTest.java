/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.*;

public class BytesUtilEqualityTest extends BytesTestCommon {

    @Test
    public void bytesEqualCoversLongIntShortBytePaths() {
        // length 15 => 8 (long) + 4 (int) + 2 (short) + 1 (byte)
        Bytes<?> a = Bytes.from("ABCDEFGHIJKLMNO");
        Bytes<?> b = Bytes.from("ABCDEFGHIJKLMNO");
        Bytes<?> c = Bytes.from("ABCDEFGH1JKLMNO");
        try {
            assertTrue(BytesUtil.bytesEqual(a, 0, b, 0, 15));
            assertFalse(BytesUtil.bytesEqual(a, 0, c, 0, 15));
        } finally {
            a.releaseLast();
            b.releaseLast();
            c.releaseLast();
        }
    }
}

