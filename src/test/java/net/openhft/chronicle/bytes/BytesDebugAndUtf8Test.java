/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BytesDebugAndUtf8Test extends BytesTestCommon {

    @Test
    @DisplayName("append utf8, parse utf8, and debug string output")
    public void appendAndParseUtf8AndDebugString() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        try {
            BytesUtil.appendUtf8(b, "hello");
            long rp = b.readPosition();
            StringBuilder sb = new StringBuilder();
            BytesUtil.parseUtf8(b, sb, 5);
            assertEquals("hello", sb.toString(),
                    "parseUtf8 reads appended content");

            // debug string contains representation
            String dbg = BytesUtil.toDebugString(b, rp, 5);
            assertFalse(dbg.isEmpty(),
                    "debug string output is not empty: " + dbg);
        } finally {
            b.releaseLast();
        }
    }
}
