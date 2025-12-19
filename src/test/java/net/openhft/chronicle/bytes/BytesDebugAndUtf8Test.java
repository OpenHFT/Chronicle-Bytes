/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BytesDebugAndUtf8Test extends BytesTestCommon {

    @Test
    public void appendAndParseUtf8AndDebugString() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        try {
            BytesUtil.appendUtf8(b, "hello");
            long rp = b.readPosition();
            StringBuilder sb = new StringBuilder();
            BytesUtil.parseUtf8(b, sb, 5);
            assertEquals("hello", sb.toString(), "BytesUtil.parseUtf8 should extract UTF-8 string into StringBuilder");

            // debug string contains representation
            String dbg = BytesUtil.toDebugString(b, rp, 5);
            assertFalse(dbg.isEmpty(), "BytesUtil.toDebugString should return non-empty debug representation");
        } finally {
            b.releaseLast();
        }
    }
}
