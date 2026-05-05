/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests HexDumpBytes advanced formatting because correct indentation and offset
 * display are essential for debugging binary data. Behaviour checks verify
 * nested headers in order to ensure readable diagnostic output.
 */
@SuppressWarnings({"checkstyle:MMLacksPurpose", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("HexDumpBytes advanced formatting and indentation behaviour checks")
class HexDumpBytesAdvancedTest extends BytesTestCommon {

    @Test
    @DisplayName("hex dump includes nested headers and offsets in output")
    public void numberWrapAndIndentation() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(16).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("hdr");
            hdb.write("1234567890abcdefghij".getBytes(StandardCharsets.ISO_8859_1));
            hdb.adjustHexDumpIndentation(2);
            hdb.writeHexDumpDescription("nest");
            hdb.write("zz".getBytes(StandardCharsets.ISO_8859_1));

            final String s = hdb.toHexString();
            assertTrue(s.contains("hdr"),
                    "Hex dump output " + s + " contains header description");
            assertTrue(s.contains("nest"),
                    "Hex dump output " + s + " contains nested description");
            assertTrue(s.contains("00"),
                    "Hex dump output " + s + " contains base16 offset values");
        } finally {
            hdb.releaseLast();
        }
    }
}
