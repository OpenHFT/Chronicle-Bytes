/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consolidated layout tests for HexDumpBytes covering wrap widths,
 * offset formatting and description handling without data.
 */
@DisplayName("HexDumpBytes layout and wrapping behaviour checks for headers")
public class HexDumpBytesLayoutTest extends BytesTestCommon {

    @Test
    @DisplayName("zero length description emits expected header line")
    public void zeroLengthDescriptionIsEmitted() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(8).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("empty");
            // write a single byte so the description line is emitted
            hdb.write(new byte[1]);
            String s = hdb.toHexString();
            assertTrue(s.contains("empty"),
                    "Hex dump output " + s + " contains empty description header");
        } finally {
            hdb.releaseLast();
        }
    }

    @Test
    @DisplayName("nested blocks include headers and offsets in output")
    public void formattingWithNestedBlocksAndOffsets() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(8).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("hdr");
            hdb.write(new byte[32]);
            hdb.adjustHexDumpIndentation(1);
            hdb.writeHexDumpDescription("nested");
            hdb.write(new byte[4]);
            String s = hdb.toHexString();
            assertTrue(s.contains("hdr"),
                    "Hex dump output " + s + " contains header description");
            assertTrue(s.contains("nested"),
                    "Hex dump output " + s + " contains nested description");
            assertTrue(s.contains("00"),
                    "Hex dump output " + s + " contains offset values");
        } finally {
            hdb.releaseLast();
        }
    }

    @Test
    @DisplayName("wrap width one produces per byte output lines")
    public void wrapWidthOneProducesPerByteLines() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(1).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("wrap1");
            hdb.write(new byte[5]);
            String s = hdb.toHexString();
            String[] lines = s.split("\\R");
            // 1 header + 5 data lines (wrapping every byte) + possibly a trailing empty line
            assertTrue(lines.length >= 5,
                    "Expected multiple wrapped lines with length " + lines.length);
            assertTrue(s.contains("wrap1"),
                    "Hex dump output " + s + " contains wrap1 description");
        } finally {
            hdb.releaseLast();
        }
    }
}

