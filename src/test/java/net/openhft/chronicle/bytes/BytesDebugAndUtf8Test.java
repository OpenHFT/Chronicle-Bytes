/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.*;

public class BytesDebugAndUtf8Test extends BytesTestCommon {

    @Test
    public void appendAndParseUtf8AndDebugString() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        try {
            BytesUtil.appendUtf8(b, "hello");
            long rp = b.readPosition();
            StringBuilder sb = new StringBuilder();
            BytesUtil.parseUtf8(b, sb, 5);
            assertEquals("hello", sb.toString());

            // debug string contains representation
            String dbg = BytesUtil.toDebugString(b, rp, 5);
            assertFalse(dbg.isEmpty());
        } finally {
            b.releaseLast();
        }
    }
}
