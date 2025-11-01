/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

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
            assertTrue(s.contains("empty"));
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
            assertTrue(s.contains("hdr"));
            assertTrue(s.contains("nested"));
            assertTrue(s.contains("00"));
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
            assertTrue("Expected multiple wrapped lines", lines.length >= 5);
            assertTrue(s.contains("wrap1"));
        } finally {
            hdb.releaseLast();
        }
    }
}

