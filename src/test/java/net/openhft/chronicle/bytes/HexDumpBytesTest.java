/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("HexDumpBytes formatting for offsets and nesting")
public class HexDumpBytesTest extends BytesTestCommon {

    @Test
    @DisplayName("offset format writes nested hex dump output")
    public void offsetFormat() {
        doTest(new HexDumpBytes());
    }

    private static void doTest(HexDumpBytes bytes) {
        bytes.numberWrap(8)
        .offsetFormat((o, b) -> b.appendBase16(o, 4));
        bytes.writeHexDumpDescription("hi").write(new byte[18]);
        bytes.adjustHexDumpIndentation(1);
        bytes.writeHexDumpDescription("nest").write(new byte[18]);
        assertEquals("" +
                "0000 00 00 00 00 00 00 00 00 # hi\n" +
                "0008 00 00 00 00 00 00 00 00\n" +
                "0010 00 00\n" +
                "0012    00 00 00 00 00 00 00 00 # nest\n" +
                "001a    00 00 00 00 00 00 00 00\n" +
                "0022    00 00\n", bytes.toHexString(),
                "Hex dump output matches expected nested format");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("memory mapped hex dump output matches format")
    public void memoryMapped() throws FileNotFoundException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for mapped hex dump test");

        File file = new File(OS.getTarget(), "HexDumpBytesTest-" + System.nanoTime() + ".dat");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            assertTrue(parent.mkdirs() || parent.exists(),
                    "Parent directory exists for mapped hex dump test");
        }
        try (MappedBytes mappedBytes = MappedBytes.mappedBytes(file, 64 * 1024)) {
            doTest(new HexDumpBytes(mappedBytes));
        } finally {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }
}
