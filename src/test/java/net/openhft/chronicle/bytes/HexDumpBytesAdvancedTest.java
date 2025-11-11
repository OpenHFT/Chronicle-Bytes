/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HexDumpBytesAdvancedTest extends BytesTestCommon {

    @Test
    public void numberWrapAndIndentation() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(16).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("hdr");
            hdb.write("1234567890abcdefghij".getBytes());
            hdb.adjustHexDumpIndentation(2);
            hdb.writeHexDumpDescription("nest");
            hdb.write("zz".getBytes());

            final String s = hdb.toHexString();
            assertTrue(s.contains("hdr"));
            assertTrue(s.contains("nest"));
            assertTrue(s.contains("00"));
        } finally {
            hdb.releaseLast();
        }
    }
}

