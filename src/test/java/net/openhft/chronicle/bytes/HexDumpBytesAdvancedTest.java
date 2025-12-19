/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HexDumpBytesAdvancedTest extends BytesTestCommon {

    @Test
    public void numberWrapAndIndentation() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(16).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("hdr");
            hdb.write("1234567890abcdefghij".getBytes(ISO_8859_1));
            hdb.adjustHexDumpIndentation(2);
            hdb.writeHexDumpDescription("nest");
            hdb.write("zz".getBytes(ISO_8859_1));

            final String s = hdb.toHexString();
            assertTrue(s.contains("hdr"), "s.contains");
            assertTrue(s.contains("nest"), "s.contains");
            assertTrue(s.contains("00"), "s.contains");
        } finally {
            hdb.releaseLast();
        }
    }
}
