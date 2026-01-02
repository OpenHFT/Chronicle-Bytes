/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BytesUtilEqualityTest extends BytesTestCommon {

    @Test
    @DisplayName("bytesEqual covers long, int, short, and byte paths")
    public void bytesEqualCoversLongIntShortBytePaths() {
        // length 15 => 8 (long) + 4 (int) + 2 (short) + 1 (byte)
        Bytes<?> a = Bytes.from("ABCDEFGHIJKLMNO");
        Bytes<?> b = Bytes.from("ABCDEFGHIJKLMNO");
        Bytes<?> c = Bytes.from("ABCDEFGH1JKLMNO");
        try {
            assertTrue(BytesUtil.bytesEqual(a, 0, b, 0, 15),
                    "byte-wise equality should match identical content across sizes");
            assertFalse(BytesUtil.bytesEqual(a, 0, c, 0, 15),
                    "byte-wise equality should detect the mismatched segment");
        } finally {
            a.releaseLast();
            b.releaseLast();
            c.releaseLast();
        }
    }
}

