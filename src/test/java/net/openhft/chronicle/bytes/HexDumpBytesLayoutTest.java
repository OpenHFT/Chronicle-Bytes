/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consolidated layout tests for HexDumpBytes covering wrap widths,
 * offset formatting and description handling without data.
 */
public class HexDumpBytesLayoutTest extends BytesTestCommon {

    @Test
    public void zeroLengthDescriptionIsEmitted() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(8).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("empty");
            // write a single byte so the description line is emitted
            hdb.write(new byte[1]);
            String s = hdb.toHexString();
            assertTrue(s.contains("empty"), "s.contains");
        } finally {
            hdb.releaseLast();
        }
    }

    @Test
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
            assertTrue(s.contains("hdr"), "s.contains");
            assertTrue(s.contains("nested"), "s.contains");
            assertTrue(s.contains("00"), "s.contains");
        } finally {
            hdb.releaseLast();
        }
    }

    @Test
    public void wrapWidthOneProducesPerByteLines() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(1).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("wrap1");
            hdb.write(new byte[5]);
            String s = hdb.toHexString();
            String[] lines = s.split("\\R");
            // 1 header + 5 data lines (wrapping every byte) + possibly a trailing empty line
            assertTrue(lines.length >= 5, "Expected multiple wrapped lines");
            assertTrue(s.contains("wrap1"), "s.contains");
        } finally {
            hdb.releaseLast();
        }
    }
}

